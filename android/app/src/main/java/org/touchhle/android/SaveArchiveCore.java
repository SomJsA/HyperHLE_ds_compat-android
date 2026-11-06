package org.touchhle.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Imports a user-selected ZIP archive into the Dead Space Documents sandbox.
 *
 * Accepted layouts are a Documents-prefixed tree, a package/Documents tree, or
 * a flat set of .sb files. Known version/account dependencies are retained.
 * SBIN section checksums are verified before publication. Existing saves are
 * archived before replacement so a malformed import cannot silently erase the
 * last known-good set.
 */
final class SaveArchiveCore {
    static final long MAX_ARCHIVE_BYTES = 256L * 1024L * 1024L;
    private static final int MAX_ENTRIES = 10_000;
    private static final String PACKAGE_ID = "com.ea.deadspace.bv";

    static final class Result {
        final int importedFiles;
        final File backup;
        final int totalFiles;

        Result(int importedFiles, int totalFiles, File backup) {
            this.importedFiles = importedFiles;
            this.totalFiles = totalFiles;
            this.backup = backup;
        }
    }

    private SaveArchiveCore() {}

    static File documents(File base) {
        return new File(new File(new File(base, "touchHLE_sandbox"), PACKAGE_ID), "Documents");
    }

    /** One archive contains every existing slot plus shared settings/dependencies. */
    static void exportZip(File base, OutputStream output) throws IOException {
        File root = documents(base);
        List<File> files = new ArrayList<>();
        collect(root, root, files, new long[]{0});
        if (files.isEmpty()) throw new IOException("No saves to export");
        for (File file : files) {
            if (file.getName().toLowerCase(Locale.US).endsWith(".sb")) validateSbin(file);
        }
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            String prefix = root.getCanonicalPath() + File.separator;
            byte[] buffer = new byte[32768];
            for (File file : files) {
                String name = file.getCanonicalPath().substring(prefix.length()).replace(File.separatorChar, '/');
                zip.putNextEntry(new ZipEntry("Documents/" + name));
                try (InputStream input = new FileInputStream(file)) {
                    int count; while ((count = input.read(buffer)) != -1) zip.write(buffer, 0, count);
                }
                zip.closeEntry();
            }
        }
    }

    static File backupDocuments(File base) throws IOException {
        File directory = new File(base, "save-backups");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Could not create backup directory");
        File backup = new File(directory, "Three-slots-" + System.currentTimeMillis() + "-" + UUID.randomUUID() + ".zip");
        File partial = new File(backup.getPath() + ".partial");
        if (!partial.createNewFile()) throw new IOException("Backup already exists");
        try (OutputStream output = new FileOutputStream(partial)) { exportZip(base, output); }
        if (backup.exists() || !partial.renameTo(backup)) throw new IOException("Could not publish verified backup");
        return backup;
    }

    private static void collect(File root, File directory, List<File> files, long[] bytes) throws IOException {
        String prefix = root.getCanonicalPath() + File.separator;
        File[] children = directory.listFiles();
        if (children == null) throw new IOException("Could not read save directory");
        Arrays.sort(children, (a, b) -> a.getName().compareTo(b.getName()));
        for (File child : children) {
            if (child.getName().toLowerCase(Locale.US).startsWith(".save-editor-")) continue;
            if (!child.getCanonicalPath().startsWith(prefix)
                    || !child.getCanonicalPath().equals(directory.getCanonicalPath() + File.separator + child.getName()))
                throw new IOException("Linked save path is not supported");
            if (child.isDirectory()) collect(root, child, files, bytes);
            else if (child.isFile()) {
                bytes[0] += child.length();
                if (bytes[0] > MAX_ARCHIVE_BYTES || files.size() >= MAX_ENTRIES) throw new IOException("Save archive exceeds limits");
                files.add(child);
            }
        }
    }

    static Result importZip(InputStream input, File base) throws IOException {
        File packageRoot = new File(new File(base, "touchHLE_sandbox"), PACKAGE_ID);
        File documents = new File(packageRoot, "Documents");
        if (!packageRoot.isDirectory() && !packageRoot.mkdirs()) {
            throw new IOException("Could not create sandbox: " + packageRoot);
        }

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + "-" + UUID.randomUUID();
        File staging = new File(packageRoot, "Documents.import-" + stamp);
        File previous = new File(packageRoot, "Documents.previous-" + stamp);
        deleteTree(staging);
        if (!staging.mkdirs()) {
            throw new IOException("Could not create staging directory");
        }

        int importedFiles = 0;
        int totalFiles = 0;
        Set<String> importedPaths = new HashSet<>();
        long importedBytes = 0;
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(input)) {
            byte[] buffer = new byte[32 * 1024];
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw new IOException("ZIP contains too many entries");
                }
                if (entry.isDirectory()) {
                    continue;
                }
                String relative = normalizeEntry(entry.getName());
                if (relative == null) {
                    continue;
                }
                if (!importedPaths.add(relative.toLowerCase(Locale.US))) throw new IOException("Duplicate ZIP save path");
                File output = new File(staging, relative);
                String canonicalRoot = staging.getCanonicalPath() + File.separator;
                if (!output.getCanonicalPath().startsWith(canonicalRoot)) {
                    throw new IOException("Unsafe ZIP path: " + entry.getName());
                }
                File parent = output.getParentFile();
                if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
                    throw new IOException("Could not create " + parent);
                }
                try (OutputStream file = new FileOutputStream(output)) {
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        importedBytes += read;
                        if (importedBytes > MAX_ARCHIVE_BYTES) {
                            throw new IOException("ZIP expands beyond 256 MB");
                        }
                        file.write(buffer, 0, read);
                    }
                }
                if (relative.toLowerCase(Locale.US).endsWith(".sb")) {
                    validateSbin(output);
                    importedFiles++;
                }
                totalFiles++;
            }
        } catch (IOException | RuntimeException error) {
            deleteTree(staging);
            throw error;
        }

        if (importedFiles == 0) {
            deleteTree(staging);
            throw new IOException("ZIP contains no .sb save files");
        }

        File backup = null;
        if (hasFiles(documents)) {
            File backupDir = new File(base, "save-backups");
            if (!backupDir.isDirectory() && !backupDir.mkdirs()) {
                throw new IOException("Could not create save backup directory");
            }
            backup = new File(backupDir, "Documents-" + stamp + ".zip");
            zipTree(documents, backup);
        }

        if (documents.exists() && !documents.renameTo(previous)) {
            deleteTree(staging);
            throw new IOException("Could not move the current save directory");
        }
        if (!staging.renameTo(documents)) {
            if (previous.exists()) {
                previous.renameTo(documents);
            }
            deleteTree(staging);
            throw new IOException("Could not activate imported saves");
        }
        deleteTree(previous);
        return new Result(importedFiles, totalFiles, backup);
    }

    private static String normalizeEntry(String raw) throws IOException {
        if (raw == null) {
            return null;
        }
        String path = raw.replace('\\', '/');
        while (path.startsWith("./")) {
            path = path.substring(2);
        }
        if (path.isEmpty() || path.startsWith("/") || path.indexOf(':') >= 0) {
            throw new IOException("Absolute ZIP path is not allowed");
        }
        String[] originalParts = path.split("/");
        for (String part : originalParts) {
            if (part.isEmpty() || part.equals("..") || part.equals(".")) {
                throw new IOException("Unsafe ZIP path: " + raw);
            }
            if (part.toLowerCase(Locale.US).startsWith(".save-editor-")) return null;
        }
        for (String prefix : new String[]{
                "touchHLE_sandbox/" + PACKAGE_ID + "/Documents/",
                PACKAGE_ID + "/Documents/",
                "Documents/",
                "save/Documents/"
        }) {
            if (path.regionMatches(true, 0, prefix, 0, prefix.length())) {
                path = path.substring(prefix.length());
                break;
            }
        }
        if (path.isEmpty()) {
            return null;
        }
        boolean save = path.toLowerCase(Locale.US).endsWith(".sb");
        boolean version = path.equals("last_version.txt") || path.equals("var/last_version.txt");
        boolean dependency = path.equals("loggedIn") || path.equals("mtxprods") || path.equals("adtrackingevents") || path.equals(".dat1fda.006");
        if (!save && !version && !dependency) return null;
        if (path.indexOf('/') < 0 && !dependency) {
            path = "var/" + path;
        }
        return path;
    }

    /** Container validation only; semantic field checks remain in Save Lab. */
    static void validateSbin(File source) throws IOException {
        if (source.length() < 8 || source.length() > 16 * 1024 * 1024) throw new IOException("Invalid SBIN size");
        try (RandomAccessFile file = new RandomAccessFile(source, "r")) {
            if (file.readInt() != 0x5342494e || readU32(file) != 2) throw new IOException("Expected SBIN v2");
            Set<String> tags = new HashSet<>();
            byte[] buffer = new byte[32768];
            while (file.getFilePointer() < file.length()) {
                if (file.length() - file.getFilePointer() < 12) throw new IOException("Truncated SBIN section");
                byte[] tagBytes = new byte[4]; file.readFully(tagBytes);
                String tag = new String(tagBytes, java.nio.charset.StandardCharsets.US_ASCII);
                if (!tags.add(tag)) throw new IOException("Duplicate SBIN section");
                long length = readU32(file), expected = readU32(file);
                if (length > file.length() - file.getFilePointer()) throw new IOException("Invalid SBIN section size");
                int hash = 0x811c9dc5;
                long remaining = length;
                while (remaining > 0) {
                    int read = (int) Math.min(buffer.length, remaining); file.readFully(buffer, 0, read);
                    for (int index = 0; index < read; index++) hash = (hash * 0x01000193) ^ (buffer[index] & 255);
                    remaining -= read;
                }
                if ((hash & 0xffffffffL) != expected) throw new IOException("SBIN checksum mismatch");
                long aligned = (file.getFilePointer() + 3) & ~3L;
                if (aligned > file.length()) throw new IOException("Truncated SBIN padding");
                file.seek(aligned);
            }
            if (!tags.contains("DATA") || !tags.contains("OHDR") || !tags.contains("STRS")) throw new IOException("Missing SBIN sections");
        }
    }

    private static long readU32(RandomAccessFile file) throws IOException {
        return Integer.reverseBytes(file.readInt()) & 0xffffffffL;
    }

    private static boolean hasFiles(File directory) {
        File[] children = directory.listFiles();
        if (children == null) {
            return false;
        }
        for (File child : children) {
            if (child.isFile() || hasFiles(child)) {
                return true;
            }
        }
        return false;
    }

    private static void zipTree(File root, File output) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(output))) {
            addZipEntries(zip, root, "");
        }
    }

    private static void addZipEntries(ZipOutputStream zip, File file, String parent)
            throws IOException {
        File[] children = file.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String relative = parent.isEmpty() ? child.getName() : parent + "/" + child.getName();
            if (child.isDirectory()) {
                addZipEntries(zip, child, relative);
            } else if (child.isFile()) {
                zip.putNextEntry(new ZipEntry(relative));
                try (InputStream input = new FileInputStream(child)) {
                    byte[] buffer = new byte[32 * 1024];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        zip.write(buffer, 0, read);
                    }
                }
                zip.closeEntry();
            }
        }
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child);
            }
        }
        file.delete();
    }
}
