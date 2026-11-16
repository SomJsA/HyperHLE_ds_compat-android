package org.touchhle.android;

import android.content.res.AssetManager;
import java.io.*;
import java.util.UUID;

/** Copies an optional bundled HD texture tree to a private filesystem path. */
final class BundledTextureStore {
    interface Source { InputStream open(String path) throws IOException; }
    private BundledTextureStore() {}

    static synchronized File ensure(File base, AssetManager assets, int scale) throws IOException {
        if (scale < 1 || scale > 4) throw new IOException("Invalid bundled texture scale");
        File root = new File(base, "touchHLE_textures/" + scale + "x").getCanonicalFile();
        File marker = new File(root, ".bundle-scale");
        if (root.isDirectory() && marker.isFile() && Integer.toString(scale).equals(read(marker))) return root;
        File parent = root.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Could not create texture cache");
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
            if (!moved) deleteEmpty(staging);
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
    private static void deleteEmpty(File directory) { if (directory != null && directory.isDirectory()) { File[] children = directory.listFiles(); if (children != null && children.length == 0) directory.delete(); } }
}
