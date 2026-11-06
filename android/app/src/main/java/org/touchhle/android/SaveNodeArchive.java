package org.touchhle.android;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Read-only source capture. Node Documents are compatible with cold-profile.v1. */
final class SaveNodeArchive {
    static final long MAX_PROFILE = 256L * 1024 * 1024;
    static final long MAX_BANK = 1024L * 1024 * 1024;
    final File documents, bank, nodes, versions;
    private final Set<String> fingerprints = new HashSet<>();
    private final String session;
    private long events, nodeCount, versionCount, duplicates, failures, used;
    private String lastError = "", lastNode = "", lastLevel = "";

    static final class Entry {
        final long bytes;
        final String sha;
        Entry(long bytes, String sha) { this.bytes = bytes; this.sha = sha; }
        Map<String, Object> json() { return map("bytes", bytes, "sha256", sha); }
        @Override public boolean equals(Object other) {
            return other instanceof Entry && bytes == ((Entry) other).bytes && sha.equals(((Entry) other).sha);
        }
        @Override public int hashCode() { return sha.hashCode(); }
    }

    static final class Listing {
        final TreeMap<String, Entry> files = new TreeMap<>();
        final List<String> directories = new ArrayList<>();
        long total;
        Map<String, Object> jsonFiles() {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, Entry> entry : files.entrySet()) result.put(entry.getKey(), entry.getValue().json());
            return result;
        }
        boolean same(Listing other) { return files.equals(other.files) && directories.equals(other.directories); }
    }

    SaveNodeArchive(File documents, File bank, String session) throws IOException {
        this.documents = documents.getCanonicalFile(); this.bank = bank.getCanonicalFile();
        this.session = session == null ? "一周目采集" : session;
        require(!inside(this.documents, this.bank) && !inside(this.bank, this.documents), "Bank and Documents overlap");
        nodes = new File(this.bank, "nodes"); versions = new File(this.bank, "versions");
        mkdir(nodes); mkdir(versions);
        Properties state = new Properties();
        File stateFile = new File(this.bank, "counters.properties");
        if (stateFile.isFile()) try (InputStream in = new FileInputStream(stateFile)) { state.load(in); }
        events = counter(state, "events"); duplicates = counter(state, "duplicates"); failures = counter(state, "failures");
        recoverJournal();
        for (File node : children(nodes)) {
            if (!node.isDirectory() || node.getName().startsWith(".")) continue;
            File fingerprint = new File(node, "fingerprint.txt");
            if (new File(node, "manifest.json").isFile() && fingerprint.isFile()) {
                fingerprints.add(readText(fingerprint)); nodeCount++; lastNode = node.getName();
            }
        }
        for (File file : children(versions)) if (file.getName().endsWith(".sb")) versionCount++;
        if (!lastNode.isEmpty()) {
            Map<String, Object> metadata = SaveNodeMetadata.read(new File(new File(nodes, lastNode), "Documents"));
            lastLevel = String.valueOf(metadata.containsKey("level_id") ? metadata.get("level_id") : "未知关卡");
        }
        used = treeBytes(this.bank);
        writeStatus();
    }

    static boolean primary(String path) { return path.matches("(?:var/)?[0-9]{2}_(?:Checkpoint|Level|Survival)Save\\.sb"); }

    private void recoverJournal() throws IOException {
        File journal = new File(bank, "events.jsonl");
        if (!journal.isFile() || journal.length() == 0) return;
        try (RandomAccessFile file = new RandomAccessFile(journal, "rw")) {
            long start = Math.max(0, file.length() - 65536);
            byte[] tail = new byte[(int) (file.length() - start)]; file.seek(start); file.readFully(tail);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\\"sequence\\\":([0-9]+)").matcher(new String(tail, StandardCharsets.UTF_8));
            while (matcher.find()) events = Math.max(events, Long.parseLong(matcher.group(1)));
            if (tail[tail.length - 1] != '\n') {
                int end = tail.length - 1; while (end >= 0 && tail[end] != '\n') end--;
                require(end >= 0 || start == 0, "Interrupted journal line is too large to repair automatically");
                File interrupted = new File(bank, "events-interrupted-" + UUID.randomUUID() + ".txt");
                try (FileOutputStream out = new FileOutputStream(interrupted)) { out.write(tail, end + 1, tail.length - end - 1); out.getFD().sync(); }
                file.setLength(start + end + 1); file.getFD().sync(); failures++;
                lastError = "Interrupted event record retained for inspection";
            }
        }
    }

    /** Preserve a content-addressed version immediately after a close/move event. */
    synchronized void recordVersion(String relative, String event) throws IOException {
        safeRelative(relative);
        events++;
        Map<String, Object> record = map("sequence", events, "time_utc", utc(), "file", relative, "event", event);
        try {
            File source = source(relative); SaveArchiveCore.validateSbin(source);
            Entry info = inspect(source);
            File version = new File(versions, info.sha + ".sb");
            if (!version.exists()) {
                capacity(info.bytes);
                File pending = new File(versions, ".version-" + UUID.randomUUID());
                try {
                    copyChecked(source, pending, info); SaveArchiveCore.validateSbin(pending);
                    require(pending.renameTo(version), "Could not publish file version");
                    versionCount++; used += info.bytes;
                } finally { if (pending.exists()) require(pending.delete(), "Could not remove private version staging"); }
            }
            record.put("sha256", info.sha); record.put("bytes", info.bytes); record.put("stored", true);
        } catch (IOException error) {
            record.put("stored", false); record.put("error", error.toString());
            append(new File(bank, "events.jsonl"), json(record) + "\n");
            failure(error); throw error;
        }
        append(new File(bank, "events.jsonl"), json(record) + "\n");
        writeStatus();
    }

    /** Null means an already archived primary save state, not a failed capture. */
    synchronized File capture(String reason, String label, boolean force) throws IOException {
        Listing before = scan(documents);
        Map<String, Object> primaryFiles = new TreeMap<>();
        for (Map.Entry<String, Entry> entry : before.files.entrySet())
            if (primary(entry.getKey())) primaryFiles.put(entry.getKey(), entry.getValue().json());
        require(!primaryFiles.isEmpty(), "No checkpoint/level/survival saves yet");
        String fingerprint = sha((session + "\n" + json(primaryFiles)).getBytes(StandardCharsets.UTF_8));
        if (!force && fingerprints.contains(fingerprint)) {
            if (!reason.equals("poll-fallback")) { duplicates++; writeStatus(); }
            return null;
        }
        capacity(before.total + 256 * 1024);
        String id = "node-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(new Date()) + "-" + UUID.randomUUID().toString().substring(0, 8);
        File stage = new File(nodes, ".stage-" + UUID.randomUUID()), target = new File(nodes, id);
        mkdir(stage);
        try {
            File destination = new File(stage, "Documents"); mkdir(destination);
            for (String directory : before.directories) mkdir(new File(destination, directory));
            for (Map.Entry<String, Entry> entry : before.files.entrySet()) {
                File output = new File(destination, entry.getKey()); mkdir(output.getParentFile());
                copyChecked(source(entry.getKey()), output, entry.getValue());
            }
            require(before.same(scan(documents)), "Source changed during capture; retry after settling");
            require(before.same(scan(destination)), "Copied node verification failed");
            Map<String, Object> metadata = SaveNodeMetadata.read(destination);
            Map<String, Object> manifest = map("format", "deadspace.cold-profile.v1", "created_utc", utc(),
                    "files", before.jsonFiles(), "directories", before.directories);
            writeNew(new File(stage, "manifest.json"), json(manifest));
            writeNew(new File(stage, "fingerprint.txt"), fingerprint);
            writeNew(new File(stage, "node.json"), json(map("format", "deadspace.save-node.v1", "id", id,
                    "created_utc", utc(), "session", session, "reason", reason, "label", label,
                    "event_sequence", events, "primary_fingerprint", fingerprint, "metadata", metadata,
                    "capture_kind", "stable-filesystem-snapshot", "gameplay_resume_validated", false)));
            require(stage.renameTo(target), "Could not publish node");
            fingerprints.add(fingerprint); nodeCount++; used += treeBytes(target);
            lastNode = id; lastLevel = String.valueOf(metadata.containsKey("level_id") ? metadata.get("level_id") : "未知关卡"); lastError = "";
            writeStatus(); return target;
        } finally { if (stage.exists()) deletePrivate(stage, nodes); }
    }

    synchronized void failure(Exception error) throws IOException {
        failures++; lastError = error.toString(); writeStatus();
    }

    synchronized void attachment(File node, String file, String status) throws IOException {
        require(node.getCanonicalFile().getParentFile().equals(nodes), "Attachment outside node bank");
        writeNew(new File(node, "screenshot.json"), json(map("status", status, "file", file,
                "captured_utc", utc(), "note", "Captured after the save settled; image is not a GPU/CPU savestate")));
        if (file != null) used += new File(node, file).length();
        writeStatus();
    }

    synchronized void writeStatus() throws IOException {
        replace(new File(bank, "status.json"), json(map("format", "deadspace.save-node-status.v1", "session", session,
                "nodes", nodeCount, "versions", versionCount, "events", events, "duplicates", duplicates,
                "failures", failures, "estimated_bytes", used, "max_bytes", MAX_BANK,
                "last_node", lastNode, "last_level", lastLevel, "last_error", lastError, "updated_utc", utc())));
        replace(new File(bank, "counters.properties"), "events=" + events + "\nduplicates=" + duplicates + "\nfailures=" + failures + "\n");
    }

    static File exportZip(File bank, File exportDirectory) throws IOException {
        bank = bank.getCanonicalFile(); exportDirectory = exportDirectory.getCanonicalFile();
        require(!inside(bank, exportDirectory), "Export must stay outside the node bank"); mkdir(exportDirectory);
        File out = new File(exportDirectory, "SaveNodes-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + ".zip");
        File temporary = new File(exportDirectory, ".export-" + UUID.randomUUID() + ".tmp");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(temporary))) { zipTree(zip, bank, bank); }
            require(temporary.renameTo(out), "Could not publish export"); return out;
        } finally { if (temporary.exists() && !temporary.delete()) throw new IOException("Could not remove private export staging file"); }
    }

    private static void zipTree(ZipOutputStream zip, File root, File directory) throws IOException {
        for (File file : children(directory)) {
            require(file.getCanonicalFile().equals(file.getAbsoluteFile()), "Links are not archived");
            boolean managed = directory.equals(root) || directory.equals(new File(root, "nodes")) || directory.equals(new File(root, "versions"));
            if (managed && file.getName().startsWith(".")) continue;
            String name = root.toURI().relativize(file.toURI()).getPath();
            if (file.isDirectory()) {
                zip.putNextEntry(new ZipEntry(name.endsWith("/") ? name : name + "/")); zip.closeEntry();
                zipTree(zip, root, file);
            } else {
                zip.putNextEntry(new ZipEntry(name));
                try (InputStream in = new FileInputStream(file)) { transfer(in, zip, null); }
                zip.closeEntry();
            }
        }
    }

    static Listing scan(File root) throws IOException {
        Listing listing = new Listing(); walk(root.getCanonicalFile(), root.getCanonicalFile(), listing);
        Collections.sort(listing.directories); return listing;
    }
    private static void walk(File root, File directory, Listing listing) throws IOException {
        for (File file : children(directory)) {
            require(file.getCanonicalFile().equals(file.getAbsoluteFile()), "Source links rejected");
            String relative = root.toURI().relativize(file.toURI()).getPath();
            if (file.isDirectory()) {
                relative = relative.replaceAll("/$", ""); safeRelative(relative);
                listing.directories.add(relative); walk(root, file, listing);
            } else {
                safeRelative(relative); require(file.isFile(), "Special file rejected");
                Entry entry = inspect(file); listing.total += entry.bytes;
                require(listing.total <= MAX_PROFILE, "Documents exceed 256 MiB");
                if (relative.toLowerCase(Locale.US).endsWith(".sb")) SaveArchiveCore.validateSbin(file);
                listing.files.put(relative, entry);
            }
            require(listing.files.size() + listing.directories.size() <= 10000, "Too many Documents entries");
        }
        require(!listing.files.isEmpty() || !root.equals(directory), "Documents empty");
    }

    private File source(String relative) throws IOException {
        File file = new File(documents, relative);
        require(file.getCanonicalFile().equals(file.getAbsoluteFile()) && inside(documents, file), "Source path escaped Documents");
        return file;
    }
    private void capacity(long additional) throws IOException {
        require(used + additional <= MAX_BANK && bank.getUsableSpace() > additional + 64L * 1024 * 1024,
                "Node bank/storage full; export recordings before continuing capture");
    }
    private static Entry inspect(File file) throws IOException {
        long length = file.length(), modified = file.lastModified();
        require(file.isFile() && length <= 64L * 1024 * 1024, "Invalid/oversized source file");
        MessageDigest digest = messageDigest();
        try (InputStream input = new FileInputStream(file)) { transfer(input, null, digest); }
        require(file.length() == length && file.lastModified() == modified, "Source changed while reading");
        return new Entry(length, hex(digest.digest()));
    }
    private static void copyChecked(File source, File destination, Entry expected) throws IOException {
        require(!destination.exists() && destination.createNewFile(), "Output already exists");
        MessageDigest digest = messageDigest(); long count;
        try (InputStream in = new FileInputStream(source); FileOutputStream out = new FileOutputStream(destination)) {
            count = transfer(in, out, digest); out.getFD().sync();
        }
        require(count == expected.bytes && hex(digest.digest()).equals(expected.sha), "Source changed while copying");
    }
    private static long transfer(InputStream in, OutputStream out, MessageDigest digest) throws IOException {
        byte[] buffer = new byte[32768]; long total = 0; int read;
        while ((read = in.read(buffer)) != -1) {
            total += read; if (out != null) out.write(buffer, 0, read); if (digest != null) digest.update(buffer, 0, read);
        }
        return total;
    }
    private static MessageDigest messageDigest() { try { return MessageDigest.getInstance("SHA-256"); } catch (Exception e) { throw new AssertionError(e); } }
    static String sha(byte[] bytes) { return hex(messageDigest().digest(bytes)); }
    private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(); for (byte value : bytes) out.append(String.format(Locale.US, "%02x", value & 255)); return out.toString(); }
    static String utc() { SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US); f.setTimeZone(TimeZone.getTimeZone("UTC")); return f.format(new Date()); }
    static Map<String, Object> map(Object... pairs) { Map<String, Object> out = new LinkedHashMap<>(); for (int i = 0; i < pairs.length; i += 2) out.put((String) pairs[i], pairs[i+1]); return out; }
    static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Number) { double n = ((Number) value).doubleValue(); return Double.isNaN(n) || Double.isInfinite(n) ? "null" : value.toString(); }
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Map) { List<String> parts = new ArrayList<>(); for (Object o : ((Map<?, ?>) value).entrySet()) { Map.Entry<?, ?> e = (Map.Entry<?, ?>) o; parts.add(json(e.getKey().toString()) + ":" + json(e.getValue())); } return "{" + join(parts) + "}"; }
        if (value instanceof Collection) { List<String> parts = new ArrayList<>(); for (Object item : (Collection<?>) value) parts.add(json(item)); return "[" + join(parts) + "]"; }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 32) out.append(String.format(Locale.US, "\\u%04x", (int) c)); else out.append(c);
        }
        return out.append('"').toString();
    }
    private static String join(List<String> parts) { StringBuilder out = new StringBuilder(); for (String part : parts) { if (out.length() > 0) out.append(','); out.append(part); } return out.toString(); }
    private static void append(File path, String content) throws IOException { try (FileOutputStream out = new FileOutputStream(path, true)) { out.write(content.getBytes(StandardCharsets.UTF_8)); out.getFD().sync(); } }
    static void writeNew(File path, String content) throws IOException { require(!path.exists() && path.createNewFile(), "Output exists"); try (FileOutputStream out = new FileOutputStream(path)) { out.write(content.getBytes(StandardCharsets.UTF_8)); out.getFD().sync(); } }
    private static void replace(File path, String text) throws IOException { File tmp = new File(path.getParentFile(), ".status-" + UUID.randomUUID()); writeNew(tmp, text); if (path.exists()) require(path.delete(), "Could not replace status"); require(tmp.renameTo(path), "Could not publish status"); }
    private static long counter(Properties state, String key) { try { return Long.parseLong(state.getProperty(key, "0")); } catch (NumberFormatException e) { return 0; } }
    private static String readText(File path) throws IOException { ByteArrayOutputStream out = new ByteArrayOutputStream(); try (InputStream in = new FileInputStream(path)) { transfer(in, out, null); } return new String(out.toByteArray(), StandardCharsets.UTF_8); }
    private static long treeBytes(File root) throws IOException { long total = 0; for (File file : children(root)) { require(file.getCanonicalFile().equals(file.getAbsoluteFile()), "Bank links rejected"); total += file.isDirectory() ? treeBytes(file) : file.length(); } return total; }
    private static File[] children(File directory) throws IOException { File[] list = directory.listFiles(); require(list != null, "Cannot list " + directory); Arrays.sort(list, (left, right) -> left.getName().compareTo(right.getName())); return list; }
    private static void mkdir(File directory) throws IOException { require(directory.isDirectory() || directory.mkdirs(), "Cannot create " + directory); }
    private static boolean inside(File root, File child) { return child.equals(root) || child.getPath().startsWith(root.getPath() + File.separator); }
    private static void safeRelative(String path) throws IOException { require(!path.startsWith("/") && !path.contains("\\") && !path.contains(":"), "Unsafe relative path"); for (String part : path.split("/", -1)) require(!part.isEmpty() && !part.equals(".") && !part.equals(".."), "Unsafe relative path"); }
    private static void deletePrivate(File path, File allowedParent) throws IOException { require(path.getCanonicalFile().getParentFile().equals(allowedParent) && path.getName().startsWith(".stage-"), "Cleanup outside private staging"); deleteBelow(path, path); }
    private static void deleteBelow(File root, File path) throws IOException { require(path.getCanonicalFile().equals(path.getAbsoluteFile()) && inside(root, path), "Cleanup escaped root"); if (path.isDirectory()) for (File child : children(path)) deleteBelow(root, child); require(path.delete(), "Cannot remove private staging"); }
    private static void require(boolean value, String error) throws IOException { if (!value) throw new IOException(error); }
}
