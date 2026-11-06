package org.touchhle.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class SaveArchiveCoreProbe {
    public static void main(String[] args) throws Exception {
        verifyImportAndBackup();
        verifyRejectWithoutSave();
        verifyDependenciesAndRejectCorruptOrDuplicate();
        System.out.println("save_import_probe=ok");
    }

    private static void verifyImportAndBackup() throws Exception {
        File base = new File(System.getProperty("java.io.tmpdir"),
                "deadspace-save-import-probe-" + System.nanoTime());
        File documents = new File(base,
                "touchHLE_sandbox/com.ea.deadspace.bv/Documents");
        File oldSave = new File(documents, "var/old.sb");
        require(oldSave.getParentFile().mkdirs(), "create old save directory");
        try (FileOutputStream output = new FileOutputStream(oldSave)) {
            output.write("old-save".getBytes(StandardCharsets.UTF_8));
        }

        byte[] zip = zip("Documents/var/new.sb", "new-save");
        SaveArchiveCore.Result result = SaveArchiveCore.importZip(
                new ByteArrayInputStream(zip), base);
        require(result.importedFiles == 1, "one imported save");
        require(new File(documents, "var/new.sb").isFile(), "new save exists");
        require(!new File(documents, "var/old.sb").exists(), "old save replaced");
        require(result.backup != null && result.backup.isFile(), "backup exists");
        require(contains(result.backup, "var/old.sb"), "backup contains old save");
        deleteTree(base);
    }

    private static void verifyRejectWithoutSave() throws Exception {
        File base = new File(System.getProperty("java.io.tmpdir"),
                "deadspace-save-import-reject-" + System.nanoTime());
        try {
            SaveArchiveCore.importZip(
                    new ByteArrayInputStream(zip("Documents/readme.txt", "not-a-save")), base);
            throw new AssertionError("archive without .sb was accepted");
        } catch (java.io.IOException expected) {
            require(expected.getMessage().contains("no .sb"), "rejection reason");
        } finally {
            deleteTree(base);
        }
    }

    private static byte[] zip(String name, String body) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(name.endsWith(".sb") ? validSbin() : body.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static byte[] validSbin() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("SBIN".getBytes(StandardCharsets.US_ASCII)); u32(out, 2);
        String[] tags = {"DATA", "OHDR", "STRS"};
        int[] values = {4 << 16, 1, 0};
        for (int i = 0; i < tags.length; i++) {
            ByteArrayOutputStream payload = new ByteArrayOutputStream(); u32(payload, values[i]);
            byte[] data = payload.toByteArray(); int hash = 0x811c9dc5;
            for (byte value : data) hash = (hash * 0x01000193) ^ (value & 255);
            out.write(tags[i].getBytes(StandardCharsets.US_ASCII)); u32(out, data.length); u32(out, hash); out.write(data);
        }
        return out.toByteArray();
    }

    private static void u32(ByteArrayOutputStream out, int value) {
        out.write(value); out.write(value >>> 8); out.write(value >>> 16); out.write(value >>> 24);
    }

    private static void verifyDependenciesAndRejectCorruptOrDuplicate() throws Exception {
        File base = new File(System.getProperty("java.io.tmpdir"), "deadspace-dependencies-" + System.nanoTime());
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                for (String name : new String[]{"Documents/var/Core.sb", "Documents/var/last_version.txt", "Documents/loggedIn", "Documents/.dat1fda.006"}) {
                    zip.putNextEntry(new ZipEntry(name));
                    zip.write(name.endsWith(".sb") ? validSbin() : "opaque".getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
            SaveArchiveCore.Result result = SaveArchiveCore.importZip(new ByteArrayInputStream(bytes.toByteArray()), base);
            require(result.importedFiles == 1 && result.totalFiles == 4, "dependency count");
            File documents = new File(base, "touchHLE_sandbox/com.ea.deadspace.bv/Documents");
            require(new File(documents, "loggedIn").isFile(), "root dependency retained");
            require(new File(documents, "var/last_version.txt").isFile(), "version retained");
            for (boolean duplicate : new boolean[]{false, true}) {
                bytes.reset();
                try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                    byte[] save = validSbin();
                    if (!duplicate) save[save.length - 1] ^= 1;
                    zip.putNextEntry(new ZipEntry("Documents/var/new.sb")); zip.write(save); zip.closeEntry();
                    if (duplicate) { zip.putNextEntry(new ZipEntry("var/NEW.sb")); zip.write(save); zip.closeEntry(); }
                }
                try {
                    SaveArchiveCore.importZip(new ByteArrayInputStream(bytes.toByteArray()), base);
                    throw new AssertionError("corrupt/duplicate archive accepted");
                } catch (java.io.IOException expected) {
                    require(new File(documents, "var/Core.sb").isFile(), "failure preserved old saves");
                    require(!new File(documents, "var/new.sb").exists(), "failed import unpublished");
                }
            }
        } finally { deleteTree(base); }
    }

    private static boolean contains(File zipFile, String name) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (name.equals(entry.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void require(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void deleteTree(File file) {
        try {
            File parent = new File(System.getProperty("java.io.tmpdir")).getCanonicalFile();
            File root = file.getCanonicalFile();
            require(root.getParentFile().equals(parent) && root.getName().startsWith("deadspace-"), "cleanup stays in generated temp root");
            deleteChecked(root, root);
        } catch (java.io.IOException error) { throw new AssertionError(error); }
    }

    private static void deleteChecked(File root, File file) throws java.io.IOException {
        require(file.getCanonicalFile().equals(root) || file.getCanonicalPath().startsWith(root.getPath() + File.separator), "cleanup path escaped root");
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteChecked(root, child);
            }
        }
        file.delete();
    }
}
