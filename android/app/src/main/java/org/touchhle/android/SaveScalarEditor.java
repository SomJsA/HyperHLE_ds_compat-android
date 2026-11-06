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
        if(found==null)throw bad("Field not present: "+name);return found;
    }
    private int scalar(String path)throws IOException {
        int index=0;Set<Integer> visited=new HashSet<>();String[] parts=path.split("/");
        for(int i=0;i<parts.length;i++) {
            if(!visited.add(index)||visited.size()>64)throw bad("Cyclic save graph");int[] f=field(index,parts[i]);
            if(i==parts.length-1){if(f[0]!=5&&!(path.equals(RIG)&&f[0]==6))throw bad("Unsupported scalar type");return f[1];}
            if(f[0]!=15)throw bad("Expected reference");index=dataInt(f[1]);
        }
        throw bad("Empty path");
    }
    long get(String path)throws IOException{return((long)dataInt(scalar(path)))&0xffffffffL;}
    byte[] edit(long credits,long nodes,int rig)throws IOException {
        if(credits<0||credits>9_999_999||nodes<0||nodes>99_999||rig<0||rig>4)throw bad("Value outside supported range");
        int[] offsets={scalar(CREDITS),scalar(NODES),scalar(RIG)};long[] values={credits,nodes,rig};
        byte[] edited=raw.clone();
        for(int i=0;i<offsets.length;i++)put(edited,dataStart+offsets[i],(int)values[i]);
        int hash=0x811c9dc5;for(int i=dataStart;i<dataStart+dataLength;i++)hash=(hash*0x01000193)^(edited[i]&255);
        put(edited,dataHeader+8,hash);return edited;
    }
    private static void put(byte[] bytes,int offset,int value){for(int i=0;i<4;i++)bytes[offset+i]=(byte)(value>>>(i*8));}

    static final class Slot {
        final int index;final long credits,nodes;final int rig;final int files;
        Slot(int index,SaveScalarEditor save,int files)throws IOException {
            this.index=index;credits=save.get(CREDITS);nodes=save.get(NODES);rig=(int)save.get(RIG);this.files=files;
            if(rig<0||rig>4)throw bad("Unsupported armor level in this save");
        }
    }
    static Slot read(File base,int slot)throws IOException {
        List<File> files=slotFiles(base,slot);if(files.isEmpty())throw bad("This story slot has no saved progress");
        return new Slot(slot,new SaveScalarEditor(files.get(0)),files.size());
    }
    private static List<File> slotFiles(File base,int slot)throws IOException {
        if(slot<0||slot>2)throw bad("Invalid slot");List<File> files=new ArrayList<>();
        for(String suffix:new String[]{"CheckpointSave","LevelSave"}) {
            File file=new File(SaveArchiveCore.documents(base),String.format(Locale.US,"var/%02d_%s.sb",slot,suffix));
            if(file.isFile())files.add(file);
        }
        return files;
    }
    static File save(File base,int slot,long credits,long nodes,int rig)throws IOException {
        return save(base,slot,credits,nodes,rig,7);
    }
    static File save(File base,int slot,long credits,long nodes,int rig,int changedFields)throws IOException {
        if(changedFields<1||changedFields>7)throw bad("No changed fields selected");
        List<File> originals=slotFiles(base,slot);if(originals.isEmpty())throw bad("This story slot has no saved progress");
        Map<String,byte[]> edits=new HashMap<>();File documents=SaveArchiveCore.documents(base);
        for(File file:originals) {
            SaveScalarEditor original=new SaveScalarEditor(file);
            edits.put(file.getName(),original.edit((changedFields&1)!=0?credits:original.get(CREDITS),
                    (changedFields&2)!=0?nodes:original.get(NODES),(changedFields&4)!=0?rig:(int)original.get(RIG)));
        }
        File staging=new File(documents.getParentFile(),"Documents.edit-"+UUID.randomUUID());
        copy(documents,documents,staging);
        for(File file:originals) {
            File changed=new File(staging,"var/"+file.getName());write(changed,edits.get(file.getName()));
            SaveScalarEditor check=new SaveScalarEditor(changed);
            if(((changedFields&1)!=0&&check.get(CREDITS)!=credits)||((changedFields&2)!=0&&check.get(NODES)!=nodes)
                    ||((changedFields&4)!=0&&check.get(RIG)!=rig))throw bad("Edited save verification failed");
        }
        File backup=SaveArchiveCore.backupDocuments(base);
        File previous=new File(documents.getParentFile(),"Documents.before-edit-"+UUID.randomUUID());
        if(!documents.renameTo(previous))throw bad("Could not preserve original saves");
        if(!staging.renameTo(documents)) {
            if(!previous.renameTo(documents))throw bad("Restore required: original saves preserved at "+previous);
            throw bad("Could not publish edited saves; original restored");
        }
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
