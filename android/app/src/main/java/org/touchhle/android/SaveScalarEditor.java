package org.touchhle.android;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Existing scalar fields only. OHDR references are object indices, not offsets. */
final class SaveScalarEditor {
    static final String CREDITS = "player/inventory/credits";
    static final String NODES = "player/inventory/nodes";
    static final String RIG = "player/upgrade_level_rig";
    static final String BURST_AVAILABLE = "player/weapons/burst_rifle/available";
    static final String BURST_CLIP = "player/weapons/burst_rifle/clip";
    static final String BURST_ALTMODE = "player/weapons/burst_rifle/altmode";
    private final byte[] raw;
    private final int dataStart, dataLength, dataHeader;
    private final int[] objects, boundaries;
    private final String[] strings;

    SaveScalarEditor(File file) throws IOException {
        SaveArchiveCore.validateSbin(file);
        raw = read(file);
        Map<String,int[]> sections = new HashMap<>();
        for (int at=8; at<raw.length;) {
            String tag=new String(raw,at,4,StandardCharsets.US_ASCII); int length=integer(at+4);
            sections.put(tag,new int[]{at+12,length,at}); at=(at+12+length+3)&~3;
        }
        int[] data=required(sections,"DATA"), table=required(sections,"OHDR"), text=required(sections,"STRS");
        dataStart=data[0];dataLength=data[1];dataHeader=data[2];
        if (table[1]==0 || table[1]%4!=0 || table[1]/4>65535) throw bad("Invalid object table");
        objects=new int[table[1]/4];TreeSet<Integer> ends=new TreeSet<>();ends.add(dataLength);
        for(int i=0;i<objects.length;i++) {
            int entry=integer(table[0]+i*4), offset=entry>>>3, flag=entry&7;
            if((flag!=1&&flag!=2)||offset>dataLength-4)throw bad("Invalid object reference");
            objects[i]=entry;ends.add(offset);
        }
        if((objects[0]&7)!=1)throw bad("Invalid root object");
        boundaries=new int[ends.size()];int boundary=0;for(int value:ends)boundaries[boundary++]=value;
        int count=integer(text[0]), cursor=text[0]+4, end=text[0]+text[1];
        if(count<0||count>65535)throw bad("Invalid string count");strings=new String[count];
        for(int i=0;i<count;i++) {
            if(cursor>end-4)throw bad("Truncated string");int size=integer(cursor);cursor+=4;
            if(size<0||size>end-cursor)throw bad("Invalid string size");
            strings[i]=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(raw,cursor,size)).toString();cursor+=size;
        }
        if(cursor!=end)throw bad("Extra string bytes");
    }
    private static IOException bad(String message){return new IOException(message);}
    private static final class MissingField extends IOException {
        MissingField(String name) { super("Field not present: " + name); }
    }
    private static int[] required(Map<String,int[]> s,String key)throws IOException {
        int[] value=s.get(key);if(value==null)throw bad("Missing "+key);return value;
    }
    private int integer(int at)throws IOException {
        if(at<0||at>raw.length-4)throw bad("Read outside save");
        return (raw[at]&255)|((raw[at+1]&255)<<8)|((raw[at+2]&255)<<16)|(raw[at+3]<<24);
    }
    private int word(int at)throws IOException {
        if(at<0||at>raw.length-2)throw bad("Read outside save");return(raw[at]&255)|((raw[at+1]&255)<<8);
    }
    private int dataInt(int at)throws IOException {
        if(at<0||at>dataLength-4)throw bad("Read outside DATA");return integer(dataStart+at);
    }
    private static int width(int kind)throws IOException {
        switch(kind){case 5:case 6:case 10:case 15:return 4;case 9:case 13:return 2;default:throw bad("Unsupported field type");}
    }
    private int[] field(int index,String name)throws IOException {
        if(index<0||index>=objects.length||(objects[index]&7)!=1)throw bad("Expected object");
        int start=objects[index]>>>3, header=dataInt(start), end=start+(header>>>16), count=header&65535;
        int next=Arrays.binarySearch(boundaries,start);next=boundaries[next+1];
        if((header>>>16)<4||end>next)throw bad("Overlapping object");
        int cursor=start+4;int[] found=null;Set<String> names=new HashSet<>();
        for(int i=0;i<count;i++) {
            if(cursor>end-8)throw bad("Truncated field");int key=word(dataStart+cursor),kind=word(dataStart+cursor+2);
            if(key<1||key>strings.length||!names.add(strings[key-1]))throw bad("Invalid field name");
            long relative=((long)dataInt(cursor+4))&0xffffffffL;long position=start+relative;
            if(position<cursor+8||position>cursor+10)throw bad("Invalid field layout");
            int offset=(int)position, size=width(kind);
            if(offset> end-(kind==9?1:size))throw bad("Field outside object");
            if(strings[key-1].equals(name))found=new int[]{kind,offset};cursor=offset+size;
        }
        if(cursor>end+1)throw bad("Field overflow");
        if(found==null)throw new MissingField(name);return found;
    }
    private int[] fieldInfo(String path)throws IOException {
        int index=0;Set<Integer> visited=new HashSet<>();String[] parts=path.split("/");
        for(int i=0;i<parts.length;i++) {
            if(!visited.add(index)||visited.size()>64)throw bad("Cyclic save graph");int[] f=field(index,parts[i]);
            if(i==parts.length-1)return f;
            if(f[0]!=15)throw bad("Expected reference");index=dataInt(f[1]);
        }
        throw bad("Empty path");
    }
    private int scalar(String path)throws IOException {
        int index=0;Set<Integer> visited=new HashSet<>();String[] parts=path.split("/");
        for(int i=0;i<parts.length;i++) {
            if(!visited.add(index)||visited.size()>64)throw bad("Cyclic save graph");int[] f=field(index,parts[i]);
            if(i==parts.length-1){validateScalar(path,f[0]);return f[1];}
            if(f[0]!=15)throw bad("Expected reference");index=dataInt(f[1]);
        }
        throw bad("Empty path");
    }
    private static void validateScalar(String path,int type)throws IOException {
        if(type==5||type==6)return;
        if(type==9&&(path.equals(BURST_AVAILABLE)||path.equals(BURST_ALTMODE)))return;
        throw bad("Unsupported scalar type: "+path);
    }
    long get(String path)throws IOException {
        int[] info=fieldInfo(path);validateScalar(path,info[0]);
        return info[0]==9 ? raw[dataStart+info[1]]&255 : ((long)dataInt(info[1]))&0xffffffffL;
    }
    private boolean hasScalar(String path)throws IOException {
        try { scalar(path); return true; } catch (MissingField absent) { return false; }
    }

    static LinkedHashMap<String,String> readFields(File file)throws IOException {
        SaveScalarEditor save=new SaveScalarEditor(file); LinkedHashMap<String,String> result=new LinkedHashMap<>();
        save.walkObject(save.objects[0]>>>3,"",new HashSet<>(),0,result);
        return result;
    }

    private void walkObject(int start,String prefix,Set<Integer> active,int depth,Map<String,String> result)throws IOException {
        if(depth>24||!active.add(start))throw bad("Cyclic or overly deep save graph");
        try {
            int header=dataInt(start), size=header>>>16, count=header&65535, end=start+size;
            if(size<4||end>dataLength)throw bad("Invalid object bounds");
            int cursor=start+4;
            for(int i=0;i<count;i++) {
                if(cursor>end-8)throw bad("Truncated object field");
                int key=word(dataStart+cursor), type=word(dataStart+cursor+2); if(key<1||key>strings.length)throw bad("Invalid field name");
                int offset=start+dataInt(cursor+4), width=width(type); if(offset<cursor+8||offset>end-(type==9?1:width))throw bad("Field outside object");
                String path=prefix+strings[key-1];
                if(type==15) walkReference(dataInt(offset),path,active,depth+1,result);
                else if(isEditable(path,type) && !isTopField(path)) result.put(path,formatValue(offset,type));
                cursor=offset+width;
            }
        } finally { active.remove(start); }
    }
    private void walkReference(int reference,String path,Set<Integer> active,int depth,Map<String,String> result)throws IOException {
        if(reference<0||reference>=objects.length)throw bad("Object reference out of range"); int entry=objects[reference], flag=entry&7, offset=entry>>>3;
        if(flag==1){walkObject(offset,path+"/",active,depth,result);return;}
        // Pool-backed arrays (inventory/actor lists) remain byte-exact here;
        // object fields cover the editable scalar schema used by the launcher.
        if(flag!=2)throw bad("Unknown object reference type");
    }
    private String formatValue(int offset,int type)throws IOException {
        if(type==9)return Integer.toString(raw[dataStart+offset]&255);
        if(type==10)return Float.toString(ByteBuffer.wrap(raw,dataStart+offset,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getFloat());
        if(type==13){int id=word(dataStart+offset);if(id<1||id>strings.length)throw bad("String id out of range");return strings[id-1];}
        return Long.toString(((long)dataInt(offset))&0xffffffffL);
    }
    private static boolean isTopField(String path){return CREDITS.equals(path)||NODES.equals(path)||RIG.equals(path)||BURST_AVAILABLE.equals(path)||BURST_CLIP.equals(path)||BURST_ALTMODE.equals(path);}
    private static boolean isEditable(String path,int type){
        if(type!=5&&type!=6&&type!=9&&type!=10&&type!=13)return false;
        return path.equals("player/hp")||path.equals("player/inventory/credits")||path.equals("player/inventory/nodes")
                ||path.equals("player/upgrade_level_rig")||path.equals("player/upgrade_level_health")||path.equals("player/upgrade_level_o2")
                ||path.equals("player/telekinesis_available")||path.equals("player/plasmasaw_available")
                ||(path.startsWith("player/weapons/")&&(path.endsWith("/clip")||path.endsWith("/available")||path.endsWith("/altmode")))
                ||path.equals("misc/gameCompleted")||path.equals("misc/plus")||path.equals("plus_unlocked")
                ||path.equals("auto_reload")||path.equals("subtitles_enabled")||path.equals("tutorials_enabled")||path.equals("y_flipped")
                ||path.equals("sfx_volume")||path.equals("music_volume")||path.equals("brightness")||path.equals("sensitivity")
                ||(path.startsWith("levelCompletes/")&&path.endsWith("/complete"))
                ||(path.startsWith("achievementCompletes/")&&path.endsWith("/complete"));
    }

    byte[] editFields(Map<String,String> changes)throws IOException {
        if(changes==null||changes.isEmpty())throw bad("No changed fields selected");
        byte[] edited=raw.clone();
        for(Map.Entry<String,String> change:changes.entrySet()) {
            int[] info=fieldInfo(change.getKey()); int type=info[0], offset=dataStart+info[1]; String value=change.getValue().trim();
            if (!isEditable(change.getKey(),type) || isTopField(change.getKey())) throw bad("Field is not editable: " + change.getKey());
            if(type==9) {
                if(!"0".equals(value)&&!"1".equals(value))throw bad("布尔值只能为 0 或 1: "+change.getKey());
                edited[offset]=(byte)("1".equals(value)?1:0);
            } else if(type==10) {
                float number; try { number=Float.parseFloat(value); } catch(NumberFormatException e) { throw bad("浮点数值无效: "+change.getKey()); }
                if(Float.isNaN(number)||Float.isInfinite(number)||number<0||number>1_000_000)throw bad("浮点数值超出范围: "+change.getKey());
                if((change.getKey().endsWith("sfx_volume")||change.getKey().endsWith("music_volume")||change.getKey().endsWith("brightness"))&&number>1)throw bad("音量/亮度范围为 0–1: "+change.getKey());
                ByteBuffer.wrap(edited,offset,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putFloat(number);
            } else if(type==5||type==6) {
                long number; try { number=Long.parseLong(value); } catch(NumberFormatException e) { throw bad("请输入整数: "+change.getKey()); }
                if(number<0||number>1_000_000_000L)throw bad("整数超出范围: "+change.getKey());
                put(edited,offset,(int)number);
            } else throw bad("字段类型不可编辑: "+change.getKey());
        }
        int hash=0x811c9dc5;for(int i=dataStart;i<dataStart+dataLength;i++)hash=(hash*0x01000193)^(edited[i]&255);put(edited,dataHeader+8,hash);
        return edited;
    }
    byte[] edit(long credits,long nodes,int rig,boolean burstAvailable,long burstClip,long burstAlt,int changedFields)throws IOException {
        if(credits<0||credits>9_999_999||nodes<0||nodes>99_999||rig<0||rig>4||burstClip<0||burstClip>999||burstAlt<0||burstAlt>1)throw bad("Value outside supported range");
        String[] paths={CREDITS,NODES,RIG,BURST_AVAILABLE,BURST_CLIP,BURST_ALTMODE};
        long[] values={credits,nodes,rig,burstAvailable?1:0,burstClip,burstAlt};
        byte[] edited=raw.clone();
        for(int i=0;i<paths.length;i++)if((changedFields&(1<<i))!=0) {
            int[] info=fieldInfo(paths[i]);validateScalar(paths[i],info[0]);
            if(info[0]==9)edited[dataStart+info[1]]=(byte)values[i];
            else put(edited,dataStart+info[1],(int)values[i]);
        }
        int hash=0x811c9dc5;for(int i=dataStart;i<dataStart+dataLength;i++)hash=(hash*0x01000193)^(edited[i]&255);
        put(edited,dataHeader+8,hash);return edited;
    }

    byte[] edit(long credits,long nodes,int rig)throws IOException {return edit(credits,nodes,rig,false,0,0,7);}
    private static long optional(SaveScalarEditor save,String path,long fallback)throws IOException {try{return save.get(path);}catch(MissingField absent){return fallback;}}
    private static void put(byte[] bytes,int offset,int value){for(int i=0;i<4;i++)bytes[offset+i]=(byte)(value>>>(i*8));}

    static final class Slot {
        final int index;final long credits,nodes;final int rig;final boolean burstAvailable;final long burstClip,burstAlt;final int files, availableFields;
        Slot(int index,SaveScalarEditor save,int files)throws IOException {
            this.index=index;credits=optional(save,CREDITS,0);nodes=optional(save,NODES,0);rig=(int)optional(save,RIG,0);
            burstAvailable=optional(save,BURST_AVAILABLE,0)!=0;burstClip=optional(save,BURST_CLIP,0);burstAlt=optional(save,BURST_ALTMODE,0);this.files=files;
            String[] paths={CREDITS,NODES,RIG,BURST_AVAILABLE,BURST_CLIP,BURST_ALTMODE};
            int present=0;for(int i=0;i<paths.length;i++)if(save.hasScalar(paths[i]))present|=1<<i;availableFields=present;
            if(rig<0||rig>4)throw bad("Unsupported armor level in this save");
        }
    }
    static Slot read(File base,int slot)throws IOException {
        List<File> files=slotFiles(base,slot);if(files.isEmpty())throw bad("This story slot has no saved progress");
        return new Slot(slot,new SaveScalarEditor(files.get(0)),files.size());
    }
    static List<File> slotFiles(File base,int slot)throws IOException {
        if(slot<0||slot>2)throw bad("Invalid slot");List<File> files=new ArrayList<>();
        for(String suffix:new String[]{"CheckpointSave","LevelSave"}) {
            File file=new File(SaveArchiveCore.documents(base),String.format(Locale.US,"var/%02d_%s.sb",slot,suffix));
            if(file.isFile())files.add(file);
        }
        return files;
    }
    static File save(File base,int slot,long credits,long nodes,int rig,boolean burstAvailable,long burstClip,long burstAlt,int changedFields)throws IOException {
        if(changedFields<1||changedFields>63)throw bad("No changed fields selected");
        List<File> originals=slotFiles(base,slot);if(originals.isEmpty())throw bad("This story slot has no saved progress");
        Map<String,byte[]> edits=new HashMap<>();File documents=SaveArchiveCore.documents(base);
        int applied=0;
        for(File file:originals) {
            SaveScalarEditor original=new SaveScalarEditor(file);
            int present=new Slot(slot,original,1).availableFields & changedFields; applied |= present;
            long oldCredits=optional(original,CREDITS,0),oldNodes=optional(original,NODES,0),oldRig=optional(original,RIG,0);
            long oldBurstAvailable=optional(original,BURST_AVAILABLE,burstAvailable?1:0),oldBurstClip=optional(original,BURST_CLIP,burstClip),oldBurstAlt=optional(original,BURST_ALTMODE,burstAlt);
            edits.put(file.getName(),original.edit((present&1)!=0?credits:oldCredits,(present&2)!=0?nodes:oldNodes,(present&4)!=0?rig:(int)oldRig,(present&8)!=0?burstAvailable:oldBurstAvailable!=0,(present&16)!=0?burstClip:oldBurstClip,(present&32)!=0?burstAlt:oldBurstAlt,present));
        }
        if(applied!=changedFields)throw bad("Selected fields are not present in this story slot");
        File staging=new File(documents.getParentFile(),"Documents.edit-"+UUID.randomUUID());copy(documents,documents,staging);
        for(File file:originals){
            File changed=new File(staging,"var/"+file.getName());write(changed,edits.get(file.getName()));
            SaveScalarEditor check=new SaveScalarEditor(changed);
            int present=new Slot(slot,check,1).availableFields & changedFields;
            if(((present&1)!=0&&check.get(CREDITS)!=credits)||((present&2)!=0&&check.get(NODES)!=nodes)
                    ||((present&4)!=0&&check.get(RIG)!=rig)
                    ||((present&8)!=0&&check.get(BURST_AVAILABLE)!=(burstAvailable?1:0))
                    ||((present&16)!=0&&check.get(BURST_CLIP)!=burstClip)
                    ||((present&32)!=0&&check.get(BURST_ALTMODE)!=burstAlt)) throw bad("Edited save verification failed");
        }
        File backup=SaveArchiveCore.backupDocuments(base);File previous=new File(documents.getParentFile(),"Documents.before-edit-"+UUID.randomUUID());
        if(!documents.renameTo(previous))throw bad("Could not preserve original saves");
        if(!staging.renameTo(documents)){previous.renameTo(documents);throw bad("Could not publish edited saves; original restored");}
        return backup;
    }

    static File save(File base,int slot,long credits,long nodes,int rig)throws IOException {
        return save(base,slot,credits,nodes,rig,false,0,0,7);
    }
    static File save(File base,int slot,long credits,long nodes,int rig,int changedFields)throws IOException {
        if(changedFields<1||changedFields>7)throw bad("No changed fields selected");
        List<File> originals=slotFiles(base,slot);if(originals.isEmpty())throw bad("This story slot has no saved progress");
        SaveScalarEditor first=new SaveScalarEditor(originals.get(0));
        return save(base,slot,credits,nodes,rig,optional(first,BURST_AVAILABLE,0)!=0,optional(first,BURST_CLIP,0),optional(first,BURST_ALTMODE,0),changedFields);
    }
    static File saveFields(File base,int slot,Map<String,String> changes)throws IOException {
        if(changes==null||changes.isEmpty())throw bad("No changed fields selected");
        List<File> originals=slotFiles(base,slot);if(originals.isEmpty())throw bad("This story slot has no saved progress");
        Map<String,byte[]> edits=new HashMap<>();File documents=SaveArchiveCore.documents(base);
        Set<String> applied=new HashSet<>();
        for(File file:originals) {
            SaveScalarEditor original=new SaveScalarEditor(file);Map<String,String> present=new LinkedHashMap<>();
            for(Map.Entry<String,String> change:changes.entrySet()) {
                try { original.fieldInfo(change.getKey());present.put(change.getKey(),change.getValue()); }
                catch(MissingField absent) { /* Preserve fields not yet generated in this companion save. */ }
            }
            applied.addAll(present.keySet());edits.put(file.getName(),present.isEmpty()?original.raw:original.editFields(present));
        }
        if(!applied.containsAll(changes.keySet()))throw bad("Selected fields are not present in this story slot");
        File staging=new File(documents.getParentFile(),"Documents.edit-"+UUID.randomUUID());copy(documents,documents,staging);
        for(File file:originals){File changed=new File(staging,"var/"+file.getName());write(changed,edits.get(file.getName()));new SaveScalarEditor(changed);}
        File backup=SaveArchiveCore.backupDocuments(base);File previous=new File(documents.getParentFile(),"Documents.before-edit-"+UUID.randomUUID());
        if(!documents.renameTo(previous))throw bad("Could not preserve original saves");
        if(!staging.renameTo(documents)){previous.renameTo(documents);throw bad("Could not publish edited saves; original restored");}
        return backup;
    }
    private static void copy(File root,File source,File destination)throws IOException {
        if(!source.getCanonicalPath().equals(root.getCanonicalPath())
                &&!source.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))throw bad("Linked save path");
        if(source.isDirectory()) {
            if(!destination.mkdir())throw bad("Could not create edit staging");File[] files=source.listFiles();if(files==null)throw bad("Could not read saves");
            for(File child:files){if(!child.getCanonicalPath().equals(source.getCanonicalPath()+File.separator+child.getName()))throw bad("Linked save path");copy(root,child,new File(destination,child.getName()));}
        }else {
            if(!destination.createNewFile())throw bad("Edit staging file exists");
            try(FileInputStream input=new FileInputStream(source);FileOutputStream output=new FileOutputStream(destination)){
                byte[] buffer=new byte[32768];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);output.getFD().sync();
            }
        }
    }
    private static byte[] read(File file)throws IOException {
        long length=file.length();if(length<0||length>16*1024*1024)throw bad("Save exceeds size limit");
        byte[] data=new byte[(int)length];try(DataInputStream input=new DataInputStream(new FileInputStream(file))){input.readFully(data);if(input.read()!=-1)throw bad("Save changed during read");}return data;
    }
    private static void write(File file,byte[] data)throws IOException {
        try(FileOutputStream output=new FileOutputStream(file)){output.write(data);output.getFD().sync();}
    }
}
