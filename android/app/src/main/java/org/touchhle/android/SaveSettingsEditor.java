package org.touchhle.android;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Editor for the shared Documents/var/settings.sb DLC plugin flags. */
final class SaveSettingsEditor {
    private static final String[] MODULES = {"attack", "defense", "loot"};
    private final byte[] raw;
    private final int dataStart, dataLength, dataHeader;
    private final List<String> strings;
    private final int[] entries;
    private final Map<String, Integer> fields = new HashMap<>();
    private final Map<String, Boolean> values = new HashMap<>();

    static final class Snapshot {
        final boolean attack, defense, loot, complete;
        final String missing;
        Snapshot(boolean attack, boolean defense, boolean loot, String missing) {
            this.attack = attack; this.defense = defense; this.loot = loot;
            this.complete = missing.isEmpty(); this.missing = missing;
        }
    }

    SaveSettingsEditor(File file) throws IOException {
        SaveArchiveCore.validateSbin(file);
        this.raw = readBytes(file);
        Parsed parsed = parseParsed(raw);
        dataStart = parsed.data.start; dataLength = parsed.data.length; dataHeader = parsed.data.header;
        strings = parsed.strings;
        entries = parsed.entries;
        scanDlc();
    }

    private SaveSettingsEditor(byte[] bytes) throws IOException {
        this.raw = bytes;
        Parsed parsed = parseParsed(raw);
        dataStart = parsed.data.start; dataLength = parsed.data.length; dataHeader = parsed.data.header;
        strings = parsed.strings;
        entries = parsed.entries;
        scanDlc();
    }

    static Snapshot read(File base) throws IOException {
        return new SaveSettingsEditor(settings(base)).snapshot();
    }

    static File save(File base, boolean attack, boolean defense, boolean loot) throws IOException {
        File file = settings(base);
        SaveSettingsEditor original = new SaveSettingsEditor(file);
        byte[] now = readBytes(file);
        if (!Arrays.equals(now, original.raw)) throw bad("settings.sb 在读取后发生变化，请重新读取");
        byte[] edited = original.edit(attack, defense, loot);
        SaveSettingsEditor check = new SaveSettingsEditor(edited);
        Snapshot result = check.snapshot();
        if (!result.complete || result.attack != attack || result.defense != defense || result.loot != loot)
            throw bad("内购插件写入验证失败");
        File backup = SaveArchiveCore.backupDocuments(base);
        File temporary = new File(file.getPath() + "." + UUID.randomUUID() + ".tmp");
        File previous = new File(file.getPath() + "." + UUID.randomUUID() + ".bak");
        write(temporary, edited);
        if (!file.renameTo(previous)) { temporary.delete(); throw bad("无法保留原 settings.sb"); }
        if (!temporary.renameTo(file)) {
            previous.renameTo(file);
            throw bad("无法发布新的 settings.sb；原文件已恢复");
        }
        previous.delete();
        return backup;
    }

    private static File settings(File base) throws IOException {
        File file = new File(SaveArchiveCore.documents(base), "var/settings.sb");
        if (!file.isFile()) throw bad("找不到 Documents/var/settings.sb");
        return file;
    }

    Snapshot snapshot() {
        StringBuilder missing = new StringBuilder();
        for (String name : MODULES) if (!fields.containsKey(name)) {
            if (missing.length() > 0) missing.append("、");
            missing.append(name);
        }
        return new Snapshot(bool("attack"), bool("defense"), bool("loot"), missing.toString());
    }

    private boolean bool(String name) {
        Boolean value = values.get(name);
        return value != null && value;
    }

    private byte[] edit(boolean attack, boolean defense, boolean loot) throws IOException {
        byte[] base = fields.size() == 3 ? raw : ensureDlcModules(attack, defense, loot);
        SaveSettingsEditor ready = fields.size() == 3 ? this : new SaveSettingsEditor(base);
        byte[] edited = ready.raw.clone();
        edited[ready.fields.get("attack")] = (byte) (attack ? 1 : 0);
        edited[ready.fields.get("defense")] = (byte) (defense ? 1 : 0);
        edited[ready.fields.get("loot")] = (byte) (loot ? 1 : 0);
        put32(edited, ready.dataHeader + 8, hash(edited, ready.dataStart, ready.dataLength));
        return edited;
    }

    private void scanDlc() throws IOException {
        int root = (int) (entries[0] >>> 3);
        int dlcIndex = referenceAt(root, "dlc_modules");
        if (dlcIndex < 0 || dlcIndex >= entries.length) return;
        int parent = (int) (entries[dlcIndex] >>> 3);
        for (String name : MODULES) {
            int child = referenceAt(parent, name);
            if (child < 0 || child >= entries.length) continue;
            int start = (int) (entries[child] >>> 3);
            Field unlocked = field(start, "unlocked");
            if (unlocked != null && unlocked.type == 9) {
                fields.put(name, dataStart + unlocked.offset);
                values.put(name, raw[dataStart + unlocked.offset] != 0);
            }
        }
    }

    private byte[] ensureDlcModules(boolean attack, boolean defense, boolean loot) throws IOException {
        byte[] data = new byte[dataLength];
        System.arraycopy(raw, dataStart, data, 0, dataLength);
        List<String> names = new ArrayList<>(strings);
        List<Integer> table = new ArrayList<>();
        for (int i = 0; i < entries.length; i++) table.add(entries[i]);
        int rootStart = (int) (table.get(0) >>> 3);
        Field rootDlc = field(data, rootStart, names, "dlc_modules");
        int oldParent = rootDlc == null || rootDlc.type != 15 ? -1 : (int) u32(data, rootDlc.offset);
        List<RawField> parentFields = oldParent >= 0 && oldParent < table.size()
                ? readFields(data, (int) (table.get(oldParent) >>> 3), names) : new ArrayList<>();
        Set<String> existing = new HashSet<>();
        for (RawField field : parentFields) existing.add(field.name);
        String[] namesToAdd = {"nodes_2", "nodes_1_registration", "attack", "defense", "loot"};
        boolean[] defaults = {false, false, attack, defense, loot};
        List<String> missing = new ArrayList<>();
        for (String name : namesToAdd) if (!existing.contains(name)) missing.add(name);
        if (missing.isEmpty() && rootDlc != null) return raw;

        int parentIndex = table.size();
        int next = parentIndex + 1;
        Map<String, Integer> childIndices = new HashMap<>();
        for (String name : missing) childIndices.put(name, next++);
        for (String name : missing) parentFields.add(new RawField(name, 15, childIndices.get(name), null));
        List<Byte> dataOut = bytes(data);
        align4(dataOut);
        int parentOffset = dataOut.size(); dataOut.addAll(packObject(parentFields, names)); align4(dataOut);
        table.add((parentOffset << 3) | 1);
        for (String name : missing) {
            int index = Arrays.asList(namesToAdd).indexOf(name);
            int childOffset = dataOut.size(); dataOut.addAll(boolObject(stringId(names, "unlocked"), defaults[index])); align4(dataOut);
            table.add((childOffset << 3) | 1);
        }
        if (rootDlc != null) {
            put32At(dataOut, rootDlc.offset, parentIndex);
        } else {
            List<RawField> rootFields = readFields(data, rootStart, names);
            rootFields.add(new RawField("dlc_modules", 15, parentIndex, null));
            int rootOffset = dataOut.size(); dataOut.addAll(packObject(rootFields, names)); align4(dataOut);
            table.set(0, (rootOffset << 3) | 1);
        }
        byte[] ohdr = new byte[table.size() * 4];
        for (int i = 0; i < table.size(); i++) put32(ohdr, i * 4, table.get(i));
        byte[] strs = packStrings(names);
        Map<String, byte[]> replacement = new HashMap<>(); replacement.put("DATA", toBytes(dataOut));
        replacement.put("OHDR", ohdr); replacement.put("STRS", strs);
        return rebuildSections(replacement);
    }

    private static final class RawField {
        final String name; final int type; final int reference; final byte[] bytes;
        RawField(String name, int type, int reference, byte[] bytes) { this.name=name; this.type=type; this.reference=reference; this.bytes=bytes; }
    }
    private static final class Field { final String name; final int type, offset; Field(String n,int t,int o){name=n;type=t;offset=o;} }
    private static Field field(byte[] data,int start,List<String> names,String wanted)throws IOException {
        int header=u32(data,start), count=header&65535, end=start+(header>>>16), cursor=start+4;
        for(int i=0;i<count;i++){int key=u16(data,cursor),type=u16(data,cursor+2),offset=start+u32(data,cursor+4); if(key<1||key>names.size())throw bad("Invalid string ID"); if(names.get(key-1).equals(wanted))return new Field(wanted,type,offset); cursor=offset+width(type); if(cursor>end+1)throw bad("Field outside object");}
        return null;
    }
    private Field field(int start,String wanted)throws IOException {
        // OHDR offsets and object-relative field offsets address DATA, not
        // the SBIN file header/section table preceding it.
        return field(Arrays.copyOfRange(raw,dataStart,dataStart+dataLength),start,strings,wanted);
    }
    private int referenceAt(int start,String name)throws IOException { Field f=field(start,name); if(f==null||f.type!=15)return -1; int ref=(int)u32(raw,dataStart+f.offset); return ref; }
    private static int referenceAt(byte[] data,int start,List<String> names,String name)throws IOException { Field f=field(data,start,names,name); return f==null||f.type!=15?-1:u32(data,f.offset); }
    private static List<RawField> readFields(byte[] data,int start,List<String> names)throws IOException { List<RawField> out=new ArrayList<>(); int header=u32(data,start),count=header&65535,end=start+(header>>>16),cursor=start+4; for(int i=0;i<count;i++){int key=u16(data,cursor),type=u16(data,cursor+2),offset=start+u32(data,cursor+4);String name=names.get(key-1); if(type==15)out.add(new RawField(name,type,u32(data,offset),null));else{int n=type==9?2:width(type);out.add(new RawField(name,type,0,Arrays.copyOfRange(data,offset,offset+n)));}cursor=offset+width(type);if(cursor>end+1)throw bad("Object overflow");}return out; }
    private static List<Byte> packObject(List<RawField> fields,List<String> names)throws IOException { List<Byte> body=new ArrayList<>(); for(RawField f:fields){put16(body,stringId(names,f.name));put16(body,f.type);put32(body,body.size()+8);if(f.type==15)put32(body,f.reference);else for(byte b:f.bytes)body.add(b);} List<Byte> out=new ArrayList<>();put32(out,fields.size()|((4+body.size())<<16));out.addAll(body);return out; }
    private static List<Byte> boolObject(int nameId,boolean value){List<Byte> out=new ArrayList<>();put32(out,(13<<16)|1);put16(out,nameId);put16(out,9);put32(out,12);out.add((byte)(value?1:0));return out;}
    private static int stringId(List<String> names,String value){int i=names.indexOf(value);if(i>=0)return i+1;if(names.size()>=65535)throw new IllegalArgumentException("String table full");names.add(value);return names.size();}
    private static byte[] packStrings(List<String> names){ByteArrayOutputStream out=new ByteArrayOutputStream();put32(out,names.size());for(String s:names){byte[] b=s.getBytes(StandardCharsets.UTF_8);put32(out,b.length);out.write(b,0,b.length);}return out.toByteArray();}
    private byte[] rebuildSections(Map<String,byte[]> replacements)throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(raw,0,8);for(int at=8;at<raw.length;){String tag=new String(raw,at,4,StandardCharsets.US_ASCII);int old=u32(raw,at+4);byte[] payload=replacements.getOrDefault(tag,Arrays.copyOfRange(raw,at+12,at+12+old));out.write(tag.getBytes(StandardCharsets.US_ASCII));put32(out,payload.length);put32(out,hash(payload,0,payload.length));out.write(payload);while((out.size()&3)!=0)out.write(0);at=(at+12+old+3)&~3;}return out.toByteArray();}
    private Section section(String name)throws IOException {return parseSectionMap(raw).get(name);}
    private static final class Section {final int start,length,header;Section(int s,int l,int h){start=s;length=l;header=h;}}
    private Map<String,Section> parseSectionMap(byte[] bytes)throws IOException{Map<String,Section> map=new HashMap<>();for(int at=8;at<bytes.length;){String tag=new String(bytes,at,4,StandardCharsets.US_ASCII);int len=u32(bytes,at+4);map.put(tag,new Section(at+12,len,at));at=(at+12+len+3)&~3;}return map;}
    private static final class Parsed {final Section data;final List<String> strings;final int[] entries;Parsed(Section d,List<String>s,int[]e){data=d;strings=s;entries=e;}}
    private Parsed parseParsed(byte[] bytes)throws IOException{Map<String,Section> s=parseSectionMap(bytes);Section d=s.get("DATA"),o=s.get("OHDR"),t=s.get("STRS");if(d==null||o==null||t==null)throw bad("Missing SBIN section");int[] e=new int[o.length/4];for(int i=0;i<e.length;i++)e[i]=u32(bytes,o.start+i*4);List<String> names=new ArrayList<>();int count=u32(bytes,t.start),cursor=t.start+4;for(int i=0;i<count;i++){int n=u32(bytes,cursor);cursor+=4;names.add(new String(bytes,cursor,n,StandardCharsets.UTF_8));cursor+=n;}return new Parsed(d,names,e);}
    private static int width(int type){return type==9||type==13?2:(type==5||type==6||type==10||type==15?4:0);}
    private static byte[] toBytes(List<Byte> list){byte[] out=new byte[list.size()];for(int i=0;i<out.length;i++)out[i]=list.get(i);return out;}
    private static List<Byte> bytes(byte[] b){List<Byte> out=new ArrayList<>(b.length);for(byte v:b)out.add(v);return out;}
    private static void align4(List<Byte> b){while((b.size()&3)!=0)b.add((byte)0);}
    private static void put16(List<Byte>b,int v){b.add((byte)v);b.add((byte)(v>>>8));} private static void put32(List<Byte>b,int v){b.add((byte)v);b.add((byte)(v>>>8));b.add((byte)(v>>>16));b.add((byte)(v>>>24));}
    private static void put32At(List<Byte>b,int at,int v){for(int i=0;i<4;i++)b.set(at+i,(byte)(v>>>(8*i)));}
    private static void put32(ByteArrayOutputStream b,int v){b.write(v);b.write(v>>>8);b.write(v>>>16);b.write(v>>>24);}
    private static void put32(byte[] b,int at,int v){for(int i=0;i<4;i++)b[at+i]=(byte)(v>>>(8*i));}
    private static int u16(byte[] b,int at){return(b[at]&255)|((b[at+1]&255)<<8);} private static int u32(byte[]b,int at){return(b[at]&255)|((b[at+1]&255)<<8)|((b[at+2]&255)<<16)|((b[at+3]&255)<<24);}
    private static int hash(byte[] b,int at,int n){int h=0x811c9dc5;for(int i=at;i<at+n;i++)h=(h*0x01000193)^(b[i]&255);return h;}
    private static byte[] readBytes(File f)throws IOException{try(FileInputStream in=new FileInputStream(f)){byte[] b=new byte[(int)f.length()];int p=0,n;while(p<b.length&&(n=in.read(b,p,b.length-p))>0)p+=n;if(p!=b.length)throw bad("Save changed during read");return b;}}
    private static void write(File f,byte[]b)throws IOException{try(FileOutputStream out=new FileOutputStream(f)){out.write(b);out.getFD().sync();}}
    private static IOException bad(String msg){return new IOException(msg);}
}
