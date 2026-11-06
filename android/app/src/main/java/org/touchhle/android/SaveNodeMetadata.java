package org.touchhle.android;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Small read-only metadata reader; complete raw saves remain in every node. */
final class SaveNodeMetadata {
    static Map<String, Object> read(File documents) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            File var = new File(documents, "var");
            int slot = 0;
            File settings = new File(var, "settings.sb");
            if (settings.isFile()) {
                Object value = new Reader(settings).get("profile_index");
                if (value instanceof Number) slot = ((Number) value).intValue();
            }
            File save = new File(var, String.format(Locale.US, "%02d_CheckpointSave.sb", slot));
            if (!save.isFile()) save = new File(var, String.format(Locale.US, "%02d_LevelSave.sb", slot));
            if (!save.isFile()) throw new IOException("No save for active slot " + slot);
            Reader reader = new Reader(save);
            result.put("active_slot", slot); result.put("file", "var/" + save.getName());
            for (String path : new String[]{"current_level/level_id", "current_level/savetype", "misc/difficulty",
                    "misc/gameCompleted", "misc/plus", "player/hp", "player/inventory/credits", "player/inventory/nodes"}) {
                try { result.put(path.equals("current_level/level_id") ? "level_id" : path, reader.get(path)); }
                catch (IOException ignored) { }
            }
            try { result.put("xyz", Arrays.asList(reader.get("player/transform/9"), reader.get("player/transform/10"), reader.get("player/transform/11"))); }
            catch (IOException ignored) { }
        } catch (Exception error) { result.put("metadata_error", error.toString()); }
        return result;
    }

    private static final class Value {
        final int type, offset;
        Value(int type, int offset) { this.type = type; this.offset = offset; }
    }
    private static final class Reader {
        final byte[] data, objects;
        final List<String> strings = new ArrayList<>();
        Reader(File source) throws IOException {
            SaveArchiveCore.validateSbin(source);
            Map<String, byte[]> sections = new HashMap<>();
            try (RandomAccessFile file = new RandomAccessFile(source, "r")) {
                file.seek(8);
                while (file.getFilePointer() < file.length()) {
                    byte[] tag = new byte[4]; file.readFully(tag);
                    int size = Integer.reverseBytes(file.readInt()); file.readInt();
                    byte[] payload = new byte[size]; file.readFully(payload);
                    sections.put(new String(tag, StandardCharsets.US_ASCII), payload);
                    file.seek((file.getFilePointer() + 3) & ~3L);
                }
            }
            data = sections.get("DATA"); objects = sections.get("OHDR");
            byte[] names = sections.get("STRS");
            int count = (int) u32(names, 0), cursor = 4;
            check(count < 65536 && objects.length >= 4, "Invalid metadata tables");
            for (int i = 0; i < count; i++) {
                int size = (int) u32(names, cursor); cursor += 4;
                check(size >= 0 && cursor <= names.length - size, "String outside table");
                strings.add(new String(names, cursor, size, StandardCharsets.UTF_8)); cursor += size;
            }
        }
        Object get(String path) throws IOException {
            long root = u32(objects, 0); check((root & 7) == 1, "Invalid root");
            int start = (int) (root >>> 3), flag = 1;
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length; i++) {
                Value value;
                if (flag == 1) value = field(start, parts[i]);
                else {
                    int type = (int) u32(data, start), count = (int) u32(data, start + 4);
                    int index;
                    try { index = Integer.parseInt(parts[i]); } catch (NumberFormatException e) { throw new IOException("Expected list index"); }
                    check(index >= 0 && index < count, "List index outside range");
                    value = new Value(type, start + 8 + index * width(type));
                }
                if (i == parts.length - 1) return scalar(value);
                check(value.type == 15, "Metadata path is not a reference");
                long reference = u32(data, value.offset); check(reference < objects.length / 4, "Reference outside OHDR");
                long entry = u32(objects, (int) reference * 4); flag = (int) entry & 7; start = (int) (entry >>> 3);
                check(flag == 1 || flag == 2, "Unknown reference kind");
            }
            throw new IOException("Empty path");
        }
        Value field(int start, String name) throws IOException {
            long head = u32(data, start); int count = (int) head & 65535, end = start + (int) (head >>> 16), cursor = start + 4;
            check(end <= data.length, "Object outside DATA");
            for (int i = 0; i < count; i++) {
                check(cursor + 8 <= end, "Field outside object");
                String actual = name(u16(data, cursor)); int type = u16(data, cursor + 2);
                int value = start + (int) u32(data, cursor + 4);
                check(value >= cursor + 8 && value <= cursor + 10 && value < end, "Invalid value offset");
                if (name.equals(actual)) return new Value(type, value);
                cursor = value + width(type);
            }
            throw new IOException("No field: " + name);
        }
        Object scalar(Value value) throws IOException {
            switch (value.type) {
                case 9: check(value.offset < data.length, "Boolean outside DATA"); return data[value.offset] & 255;
                case 13: return name(u16(data, value.offset));
                case 10: return Float.intBitsToFloat((int) u32(data, value.offset));
                case 5: case 6: return u32(data, value.offset);
                default: throw new IOException("Unsupported metadata scalar");
            }
        }
        String name(int id) throws IOException { check(id > 0 && id <= strings.size(), "Invalid string ID"); return strings.get(id - 1); }
        int width(int type) throws IOException { if (type == 9 || type == 13) return 2; if (type == 5 || type == 6 || type == 10 || type == 15) return 4; throw new IOException("Unknown field type"); }
    }
    private static int u16(byte[] bytes, int offset) throws IOException { check(offset >= 0 && offset <= bytes.length - 2, "u16 outside data"); return (bytes[offset] & 255) | ((bytes[offset+1] & 255) << 8); }
    private static long u32(byte[] bytes, int offset) throws IOException { check(offset >= 0 && offset <= bytes.length - 4, "u32 outside data"); return (bytes[offset] & 255L) | ((bytes[offset+1] & 255L) << 8) | ((bytes[offset+2] & 255L) << 16) | ((bytes[offset+3] & 255L) << 24); }
    private static void check(boolean condition, String message) throws IOException { if (!condition) throw new IOException(message); }
}
