package org.touchhle.android;

import android.content.res.AssetManager;
import java.io.*;
import java.nio.channels.FileLock;
import java.util.UUID;
import java.util.regex.Pattern;

/** Copies an optional bundled HD texture tree to a private filesystem path. */
final class BundledTextureStore {
    interface Source { InputStream open(String path) throws IOException; }
    private static final Pattern PENDING = Pattern.compile("[.]pending-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    private BundledTextureStore() {}

    static synchronized File ensure(File base, AssetManager assets, int scale) throws IOException {
        if (scale < 1 || scale > 4) throw new IOException("Invalid bundled texture scale");
        File parent = textureDirectory(base);
        if (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Could not create texture cache");
        }
        try (RandomAccessFile file = new RandomAccessFile(lockPath(parent), "rw");
             FileLock lock = file.getChannel().lock()) {
            cleanPending(parent);
            return ensureLocked(parent, assets, scale);
        }
    }

    /** Both launcher and game can prepare textures. Use the same lock before deleting staging. */
    static synchronized void prunePending(File base) throws IOException {
        File parent = textureDirectory(base);
        if (!parent.isDirectory()) return;
        try (RandomAccessFile file = new RandomAccessFile(lockPath(parent), "rw");
             FileLock lock = file.getChannel().lock()) {
            cleanPending(parent);
        }
    }

    private static File textureDirectory(File base) throws IOException {
        File parent = new File(base.getCanonicalFile(), "touchHLE_textures");
        if (!parent.equals(parent.getCanonicalFile())) throw new IOException("Linked texture directory");
        return parent;
    }

    private static File lockPath(File parent) throws IOException {
        File path = new File(parent, ".bundle-lock");
        if (!path.equals(path.getCanonicalFile())) throw new IOException("Linked texture lock");
        return path;
    }

    private static File ensureLocked(File parent, AssetManager assets, int scale) throws IOException {
        File root = new File(parent, scale + "x");
        if (!root.equals(root.getCanonicalFile())) throw new IOException("Linked texture tier");
        File marker = new File(root, ".bundle-scale");
        if (root.isDirectory() && marker.isFile() && Integer.toString(scale).equals(read(marker))) return root;
        File staging = new File(parent, ".pending-" + UUID.randomUUID());
        if (!staging.mkdir()) throw new IOException("Could not create texture staging");
        boolean moved = false;
        try {
            // The HD APK may carry both 2x and 4x packs. Select the requested
            // tier at extraction time; older single-tier APKs keep the flat
            // asset layout and continue through the fallback path.
            String tierPath = "touchHLE_textures/" + scale + "x";
            String[] tierChildren = assets.list(tierPath);
            copyTree(assets, tierChildren != null && tierChildren.length > 0 ? tierPath : "touchHLE_textures", staging);
            write(new File(staging, ".bundle-scale"), Integer.toString(scale));
            if (root.exists()) return root;
            if (!staging.renameTo(root)) {
                if (root.isDirectory()) return root;
                throw new IOException("Could not publish bundled textures");
            }
            moved = true;
            return root;
        } finally {
            if (!moved) deleteStaging(staging, 0);
        }
    }

    private static void copyTree(AssetManager assets, String assetPath, File destination) throws IOException {
        String[] children = assets.list(assetPath);
        if (children == null || children.length == 0) {
            File parent = destination.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Could not create texture directory");
            try (InputStream input = assets.open(assetPath); FileOutputStream output = new FileOutputStream(destination)) {
                byte[] buffer = new byte[1024 * 1024]; int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                output.getFD().sync();
            }
            return;
        }
        if (!destination.isDirectory() && !destination.mkdirs()) throw new IOException("Could not create texture directory");
        for (String child : children) copyTree(assets, assetPath + "/" + child, new File(destination, child));
    }

    private static String read(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] bytes = new byte[16]; int count = input.read(bytes);
            return count < 0 ? "" : new String(bytes, 0, count, "UTF-8").trim();
        }
    }
    private static void write(File file, String value) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) { output.write(value.getBytes("UTF-8")); output.getFD().sync(); }
    }
    private static void cleanPending(File parent) {
        File[] candidates = parent.listFiles();
        if (candidates == null) return;
        for (File candidate : candidates) {
            if (PENDING.matcher(candidate.getName()).matches() && candidate.isDirectory()) {
                deleteStaging(candidate, 0);
            }
        }
    }

    private static void deleteStaging(File entry, int depth) {
        // Only canonical children of our own staging tree are disposable.
        try {
            if (depth >= 32 || !entry.equals(entry.getCanonicalFile())) return;
            if (entry.isDirectory()) {
                File[] children = entry.listFiles();
                if (children == null) return;
                for (File child : children) deleteStaging(child, depth + 1);
            }
            entry.delete();
        } catch (IOException ignored) {
            // If a volume is unavailable the next launch can retry.
        }
    }
}
