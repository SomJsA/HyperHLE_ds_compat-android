package org.touchhle.android;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Offline probe. Writes only a new explicit output directory; never runs a game. */
public final class SaveNodeArchiveProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Documents variant1.sb variant2.sb settings-variant.sb output");
        File fixture = new File(args[0]).getCanonicalFile();
        File output = new File(args[4]).getCanonicalFile();
        require(!output.exists() && output.mkdirs(), "new dedicated output directory");
        File source = new File(output, "source/Documents"); copyTree(fixture, source);
        File hidden = new File(source, ".capture-test-hidden"); write(hidden, "keep-me".getBytes(StandardCharsets.UTF_8));
        File bank = new File(output, "bank"), checkpoint = new File(source, "var/00_CheckpointSave.sb");
        byte[] original = read(checkpoint);
        SaveNodeArchive archive = new SaveNodeArchive(source, bank, "离线一周目测试");
        File first = archive.capture("automatic", "", false);
        require(first != null && Arrays.equals(original, read(checkpoint)), "read-only initial capture");
        require(archive.capture("automatic", "", false) == null, "duplicate source state not copied twice");
        require(SaveNodeMetadata.read(new File(first, "Documents")).containsKey("level_id"), "metadata decodes actual fixture");
        require(SaveNodeMetadata.read(new File(first, "Documents")).containsKey("xyz"), "saved position decoded");
        write(new File(source, "var/settings.sb"), read(new File(args[3])));
        archive.recordVersion("var/settings.sb", "close_write");
        require(archive.capture("automatic", "", false) == null, "settings-only writes do not create fake checkpoints");
        byte[] broken = original.clone(); broken[21] ^= 1; write(checkpoint, broken);
        try { archive.recordVersion("var/00_CheckpointSave.sb", "close_write"); throw new AssertionError("corrupt version accepted"); }
        catch (IOException expected) { }
        try { archive.capture("automatic", "", false); throw new AssertionError("corrupt group accepted"); }
        catch (IOException expected) { }
        require(Arrays.equals(original, read(new File(first, "Documents/var/00_CheckpointSave.sb"))), "older node remains immutable after failed capture");
        byte[] one = read(new File(args[1])), two = read(new File(args[2]));
        write(checkpoint, one); archive.recordVersion("var/00_CheckpointSave.sb", "close_write");
        File second = archive.capture("automatic", "", false);
        require(second != null, "recovered valid save captured");
        write(checkpoint, two); archive.recordVersion("var/00_CheckpointSave.sb", "close_write");
        File third = archive.capture("automatic", "", false);
        require(third != null, "later overwrite becomes another node");
        require(new File(bank, "versions/" + SaveNodeArchive.sha(one) + ".sb").isFile(), "first overwritten version retained");
        require(new File(bank, "versions/" + SaveNodeArchive.sha(two) + ".sb").isFile(), "second overwritten version retained");
        try (FileOutputStream append = new FileOutputStream(new File(bank, "events.jsonl"), true)) {
            append.write("{\"sequence\":5,\"interrupted\":".getBytes(StandardCharsets.UTF_8));
        }
        SaveNodeArchive restarted = new SaveNodeArchive(source, bank, "离线一周目测试");
        require(restarted.capture("automatic", "", false) == null, "restart does not duplicate same checkpoint");
        require(new String(read(new File(bank, "events.jsonl")), StandardCharsets.UTF_8).endsWith("\n"), "interrupted journal is repaired before future append");
        require(bank.listFiles((dir, name) -> name.startsWith("events-interrupted-")).length == 1, "interrupted event retained for inspection");
        File marked = restarted.capture("manual-label-on-saved-files", "电梯前\"标签\n第二行", true);
        require(marked != null && Arrays.equals(two, read(new File(marked, "Documents/var/00_CheckpointSave.sb"))), "manual label preserves actual saved bytes");
        File exported = SaveNodeArchive.exportZip(bank, new File(output, "exports"));
        boolean hiddenFound = false; int manifests = 0;
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(exported))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith("/Documents/.capture-test-hidden")) hiddenFound = true;
                if (entry.getName().endsWith("/manifest.json")) manifests++;
            }
        }
        require(hiddenFound && manifests == 4, "export contains hidden dependencies and all four nodes");
        try { new SaveNodeArchive(source, new File(source, "bad-bank"), "x"); throw new AssertionError("recursive bank accepted"); }
        catch (IOException expected) { require(!new File(source, "bad-bank").exists(), "overlap rejected before output creation"); }
        System.out.println("save_node_archive_probe=ok nodes=" + manifests + " export=" + exported.getAbsolutePath());
    }
    private static void copyTree(File source, File target) throws IOException {
        if (source.isDirectory()) {
            require(target.mkdirs(), "copy directory");
            File[] children = source.listFiles(); if (children == null) throw new IOException("cannot list fixture");
            for (File child : children) copyTree(child, new File(target, child.getName()));
        } else write(target, read(source));
    }
    private static byte[] read(File file) throws IOException { ByteArrayOutputStream out = new ByteArrayOutputStream(); try (InputStream in = new FileInputStream(file)) { byte[] buffer = new byte[8192]; int n; while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n); } return out.toByteArray(); }
    private static void write(File file, byte[] bytes) throws IOException { try (OutputStream out = new FileOutputStream(file)) { out.write(bytes); } }
    private static void require(boolean value, String message) throws IOException { if (!value) throw new IOException(message); }
}
