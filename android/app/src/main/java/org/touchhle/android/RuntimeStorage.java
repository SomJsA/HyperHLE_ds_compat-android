package org.touchhle.android;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** One versioned root for native files, Android configuration and diagnostics. */
final class RuntimeStorage {
    static final String MODE = "storage_mode_v1", ROOT = "storage_root_v1";
    static final String DOCUMENTS = "documents", LEGACY = "legacy";
    private static final String GAME = "com.ea.deadspace.bv";
    private RuntimeStorage() {}
    private static Context app(Context c) { return c.getApplicationContext(); }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(LauncherActivity.PREFS, Context.MODE_PRIVATE); }
    private static File legacy(Context c) {
        File root=app(c).getExternalFilesDir(null);
        return root == null ? app(c).getFilesDir() : root;
    }
    private static File location(Context c, String mode) {
        if (LEGACY.equals(mode)) return legacy(c);
        File docs=app(c).getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (docs == null) docs=new File(app(c).getFilesDir(), "Documents");
        return new File(docs, "DeadSpace IOS移植");
    }
    static String mode(Context c) {
        String explicit=prefs(c).getString(MODE, "");
        if (!explicit.isEmpty()) return explicit;
        // Existing preferences or native progress are an explicit legacy choice.
        return !prefs(c).getAll().isEmpty() || new File(legacy(c), "touchHLE_sandbox").exists() ? LEGACY : DOCUMENTS;
    }
    static File dataBase(Context c) {
        if (BuildConfig.SAVE_LAB) return new File(app(c).getFilesDir(), "save-lab");
        String fixed=prefs(c).getString(ROOT, "");
        // Keep a selected volume even while unavailable; never silently start
        // another empty profile in the internal fallback directory.
        return fixed.isEmpty() ? location(c, mode(c)) : new File(fixed);
    }
    static File requireCompatible(Context c) throws IOException {
        if(!BuildConfig.SAVE_LAB && !DOCUMENTS.equals(mode(c)) && !LEGACY.equals(mode(c)))throw new IOException("未知资料位置类型，已保留原设置。");
        File selected=dataBase(c);if(!selected.isAbsolute())throw new IOException("资料位置不是绝对路径，已保留原设置。");
        File root=selected.getCanonicalFile();
        ensureLayout(root);
        return root;
    }
    static synchronized File initialize(Context c) throws IOException {
        File root=requireCompatible(c);
        JSONObject configuration=readConfiguration(root);
        if (!BuildConfig.SAVE_LAB && !prefs(c).edit().putString(MODE,mode(c)).putString(ROOT,root.getAbsolutePath()).commit()) throw new IOException("无法保存资料位置选择。");
        applyConfiguration(c,configuration);
        return root;
    }
    static void requireGameStopped(Context c) throws IOException {
        ActivityManager manager=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes=manager==null?null:manager.getRunningAppProcesses();
        if (processes==null) throw new IOException("无法确认游戏是否已经退出。");
        for (ActivityManager.RunningAppProcessInfo process:processes)
            if ((c.getPackageName()+":game").equals(process.processName)) throw new IOException("请先退出游戏，再切换资料位置。");
        if (SaveArchiveImporter.isBusy()) throw new IOException("存档操作进行中，请稍后再试。");
    }
    static synchronized void select(Context c,String mode) throws IOException {
        if (!DOCUMENTS.equals(mode) && !LEGACY.equals(mode)) throw new IOException("未知资料位置。");
        if (BuildConfig.SAVE_LAB) throw new IOException("实验包使用独立资料位置。");
        requireGameStopped(c);
        File root=location(c,mode).getCanonicalFile();ensureLayout(root);
        // Validate the target configuration before changing the selected root.
        JSONObject configuration=readConfiguration(root);
        if(!prefs(c).edit().putString(MODE,mode).putString(ROOT,root.getAbsolutePath()).commit()) throw new IOException("资料位置选择未保存。");
        applyConfiguration(c,configuration);
    }
    private static File child(File root,String name) throws IOException {
        File target=new File(root,name);
        if (!target.getCanonicalFile().equals(target.getAbsoluteFile())) throw new IOException("资料路径包含链接，已保留原文件。");
        return target;
    }
    private static JSONObject read(File file,int limit) throws IOException {
        if (file.length()>limit) throw new IOException("资料描述文件过大，已保留原文件。");
        try (InputStream input=new FileInputStream(file);ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
            byte[] block=new byte[4096];int count;
            while((count=input.read(block))!=-1) {if(bytes.size()+count>limit)throw new IOException("资料描述文件过大。");bytes.write(block,0,count);}
            return new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
        } catch (org.json.JSONException error) {throw new IOException("资料描述文件损坏，已保留原文件。",error);}
    }
    private static void validateLayout(File marker) throws IOException {
        JSONObject m=read(marker,65536);
        if (!(m.opt("schema") instanceof Number) || m.optDouble("schema",0)!=1
            || !GAME.equals(m.optString("game")) || !"three-slot-zip-v1".equals(m.optString("exchange"))
            || !("touchHLE_sandbox/"+GAME).equals(m.optString("native_profile"))
            || !"config/pc".equals(m.optString("pc_config")) || !"config/android".equals(m.optString("android_config")))
            throw new IOException("此资料布局版本不兼容；本版本不会写入该位置。");
    }
    private static synchronized void ensureLayout(File root) throws IOException {
        File marker=child(root,"storage-layout.json");
        if (marker.exists()) {validateLayout(marker);return;}
        if (!root.isDirectory() && !root.mkdirs()) throw new IOException("无法打开选定的资料位置。");
        File lockFile=child(root,".storage-layout.lock");
        try (RandomAccessFile lock=new RandomAccessFile(lockFile,"rw");FileLock held=lock.getChannel().lock()) {
            if(marker.exists()) {validateLayout(marker);return;}
            JSONObject m=new JSONObject();
            try {m.put("schema",1).put("game",GAME).put("exchange","three-slot-zip-v1")
                .put("native_profile","touchHLE_sandbox/"+GAME).put("pc_config","config/pc").put("android_config","config/android");}
            catch(org.json.JSONException error){throw new IOException(error);}
            atomicWrite(marker,m.toString().getBytes(StandardCharsets.UTF_8));
            File note=child(root,"配置和存档说明.txt");
            if(note.createNewFile()) try(FileOutputStream out=new FileOutputStream(note)) {
                out.write(("DeadSpace Legacy 资料布局 v1\n\n原生进度：touchHLE_sandbox/"+GAME+"\nAndroid配置：config/android\nPC配置：config/pc\n诊断：diagnostics\n\n三槽ZIP是双端交换原生存档和公共依赖的格式，平台启动器配置分别保存。未知布局拒绝写入；切换位置不会搬移、合并或覆盖已有进度。\nAndroid默认位置是应用专属Documents，可通过系统文件选择器的本应用入口访问。卸载前请导出存档；此目录并非公共Documents授权目录。\nsomjs宿主包与旧宿主包资料独立，旧进度请通过三槽ZIP导入。\n").getBytes(StandardCharsets.UTF_8));out.getFD().sync();
            }
        }
    }
    private static File configurationFile(File root) throws IOException {
        File config=child(root,"config"),android=child(config,"android");
        return child(android,"launcher.json");
    }
    private static JSONObject readConfiguration(File root) throws IOException {
        File file=configurationFile(root);if(!file.exists())return null;
        JSONObject d=read(file,262144);
        if (!(d.opt("schema") instanceof Number) || d.optDouble("schema",0)!=1 || !GAME.equals(d.optString("game")) || !(d.opt("values") instanceof JSONObject))
            throw new IOException("Android配置版本不兼容，原文件已保留。");
        JSONObject values=d.optJSONObject("values");
        for(Iterator<String> keys=values.keys();keys.hasNext();) {
            String key=keys.next();char type=preferenceType(key);Object value=values.opt(key);
            if(type=='b' && !(value instanceof Boolean) || type=='s' && !(value instanceof String)
                || type=='i' && (!(value instanceof Integer) || !integerInRange(key,(Integer)value)))
                throw new IOException("Android配置字段类型或范围异常，原文件已保留："+key);
        }
        return d;
    }
    private static boolean portablePreference(String key) {
        return !MODE.equals(key) && !ROOT.equals(key) && !key.startsWith("hardware_") && !key.startsWith("benchmark_");
    }
    private static char preferenceType(String key) {
        if ("quality_preset".equals(key)||SaveNodeMonitor.KEY_SESSION.equals(key))return 's';
        for(String name:new String[]{"scale_hack","fps_cap","present_fsr","present_sharpen","shadow_boost"})if(name.equals(key))return 'i';
        for(String name:new String[]{"show_fps","debug_logs","dump_frames","pc_compat","viewport_stretch","fast_present","skip_readback","gpu_present_copy","aa_mode","fsr3_ahb_probe","fsr3_frame_generation","fsr3_real_depth","fsr3_gpu_depth","fsr3_async","fsr3_direct_output","fsr3_force_unsupported","overlay_enabled",SaveNodeMonitor.KEY_ENABLED,SaveNodeMonitor.KEY_IMAGES})if(name.equals(key))return 'b';
        return 0;
    }
    private static boolean integerInRange(String key,int value) {
        if("scale_hack".equals(key))return value>=1&&value<=3;
        if("fps_cap".equals(key))return value>=0&&value<=1000;
        if("present_fsr".equals(key))return value>=0&&value<=2;
        if("present_sharpen".equals(key))return value>=0&&value<=20;
        return value>=0&&value<=100;
    }
    private static void applyConfiguration(Context c,JSONObject d) throws IOException {
        if(d==null)return;
        JSONObject values=d.optJSONObject("values");SharedPreferences.Editor edit=prefs(c).edit();
        for(Iterator<String> keys=values.keys();keys.hasNext();) {
            String key=keys.next();if(!portablePreference(key)||preferenceType(key)==0)continue;Object value=values.opt(key);
            if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);
            else if(value instanceof Integer)edit.putInt(key,(Integer)value);
            else if(value instanceof Long)edit.putLong(key,(Long)value);
            else if(value instanceof String)edit.putString(key,(String)value);
        }
        if(!edit.commit())throw new IOException("无法读取该位置的配置。");
    }
    private static void loadConfiguration(Context c,File root) throws IOException {applyConfiguration(c,readConfiguration(root));}
    static void checkConfiguration(Context c) throws IOException {readConfiguration(requireCompatible(c));}
    static synchronized void saveConfiguration(Context c) throws IOException {
        File root=requireCompatible(c);JSONObject d=readConfiguration(root);
        if(d==null)d=new JSONObject();JSONObject values=d.optJSONObject("values");if(values==null)values=new JSONObject();
        try {
            for(Map.Entry<String,?> entry:prefs(c).getAll().entrySet())
                if(portablePreference(entry.getKey()) && (entry.getValue() instanceof Boolean || entry.getValue() instanceof Number || entry.getValue() instanceof String))values.put(entry.getKey(),entry.getValue());
            d.put("schema",1).put("game",GAME).put("values",values);
        } catch(org.json.JSONException error){throw new IOException(error);}
        File file=configurationFile(root);if(!file.getParentFile().isDirectory()&&!file.getParentFile().mkdirs())throw new IOException("无法保存Android配置。");
        atomicWrite(file,d.toString().getBytes(StandardCharsets.UTF_8));
    }
    private static void atomicWrite(File file,byte[] bytes) throws IOException {
        AtomicFile atomic=new AtomicFile(file);FileOutputStream out=null;
        try{out=atomic.startWrite();out.write(bytes);atomic.finishWrite(out);}
        catch(IOException error){if(out!=null)atomic.failWrite(out);throw error;}
    }
}
