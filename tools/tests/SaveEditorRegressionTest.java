package org.touchhle.android;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Real R1 save schemas: minimal level, typed checkpoint and settings without DLC objects. */
public final class SaveEditorRegressionTest {
    static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    static void copy(File source,File dest)throws Exception {
        if(source.isDirectory()){check(dest.mkdirs(),"fresh directory");for(File f:source.listFiles())copy(f,new File(dest,f.getName()));}
        else Files.copy(source.toPath(),dest.toPath());
    }
    static Map<String,byte[]> snapshot(File dir)throws Exception {
        Map<String,byte[]> result=new TreeMap<>();try(java.util.stream.Stream<Path> files=Files.walk(dir.toPath())){
            for(Path p:(Iterable<Path>)files.filter(Files::isRegularFile)::iterator)result.put(dir.toPath().relativize(p).toString(),Files.readAllBytes(p));
        }return result;
    }
    static void same(Map<String,byte[]> a,Map<String,byte[]> b){check(a.keySet().equals(b.keySet()),"file set unchanged");for(String k:a.keySet())check(Arrays.equals(a.get(k),b.get(k)),"unchanged "+k);}
    public static void main(String[] args)throws Exception {
        File original=new File(args[0]),base=new File(args[1]),docs=SaveArchiveCore.documents(base);
        Map<String,byte[]> protectedBefore=snapshot(original);copy(original,docs);
        SaveEditorSnapshot read=SaveEditorSnapshot.read(base,0);
        check(read.slot!=null && read.slot.availableFields==63,"all six checkpoint fields, including BOOL and signed clip");
        check(read.fields.size()>=22,"full fields read");
        check(read.settings!=null && !read.settings.complete,"minimal settings read independently");
        SaveEditorSnapshot empty=SaveEditorSnapshot.read(base,1);
        check(empty.slot==null && empty.settings!=null,"empty story slot keeps plugin editor available");
        Map<String,String> oldSettings=SaveScalarEditor.readFields(new File(docs,"var/settings.sb"));
        File pluginBackup=SaveSettingsEditor.save(base,true,false,true);
        SaveSettingsEditor.Snapshot plugins=SaveSettingsEditor.read(base);
        check(pluginBackup.isFile() && plugins.complete && plugins.attack && !plugins.defense && plugins.loot,"missing DLC objects added, flags verified");
        check(SaveScalarEditor.readFields(new File(docs,"var/settings.sb")).entrySet().containsAll(oldSettings.entrySet()),"existing settings preserved");
        Map<String,byte[]> before=snapshot(docs);
        SaveScalarEditor.save(base,0,95275,21,1,true,37,1,63);
        SaveScalarEditor.Slot changed=SaveScalarEditor.read(base,0);
        check(changed.credits==95275 && changed.nodes==21 && changed.rig==1 && changed.burstAvailable && changed.burstClip==37 && changed.burstAlt==1,"all six typed fields roundtrip");
        Map<String,byte[]> after=snapshot(docs);
        for(String key:before.keySet())if(!key.replace('\\','/').equals("var/00_CheckpointSave.sb"))check(Arrays.equals(before.get(key),after.get(key)),"unselected/missing fields preserved "+key);
        SaveScalarEditor checkParser=new SaveScalarEditor(new File(docs,"var/00_CheckpointSave.sb"));
        File oldFile=new File(base,"checkpoint-before.sb");Files.write(oldFile.toPath(),before.get("var"+File.separator+"00_CheckpointSave.sb"));
        SaveScalarEditor old=new SaveScalarEditor(oldFile);
        check(Arrays.equals(checkParser.edit(old.get(SaveScalarEditor.CREDITS),old.get(SaveScalarEditor.NODES),(int)old.get(SaveScalarEditor.RIG),old.get(SaveScalarEditor.BURST_AVAILABLE)!=0,old.get(SaveScalarEditor.BURST_CLIP),old.get(SaveScalarEditor.BURST_ALTMODE),63),before.get("var"+File.separator+"00_CheckpointSave.sb")),"typed edit reversal byte exact");
        Map<String,String> fields=new LinkedHashMap<>();fields.put("misc/plus","1");
        SaveScalarEditor.saveFields(base,0,fields);
        for(String suffix:new String[]{"CheckpointSave","LevelSave"})check("1".equals(SaveScalarEditor.readFields(new File(docs,"var/00_"+suffix+".sb")).get("misc/plus")),"existing companion field synchronized");
        before=snapshot(docs);fields.clear();fields.put("player/invented_field","1");
        try{SaveScalarEditor.saveFields(base,0,fields);throw new AssertionError("missing field accepted");}catch(IOException expected){}same(before,snapshot(docs));
        check(new File(docs,"var/00_CheckpointSave.sb").delete(),"remove private checkpoint only");
        SaveEditorSnapshot minimal=SaveEditorSnapshot.read(base,0);
        check(minimal.slot!=null && minimal.slot.availableFields==0 && minimal.fields.size()==2 && minimal.settings!=null,"new level-only slot remains usable");
        same(protectedBefore,snapshot(original));
        System.out.println("PASS: settings offsets/DLC insertion, six typed fields, companion preservation, empty/minimal slots, existing-field sync, rejected missing field, original unchanged");
    }
}
