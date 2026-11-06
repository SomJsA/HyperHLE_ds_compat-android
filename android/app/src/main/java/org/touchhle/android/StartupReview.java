package org.touchhle.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.AtomicFile;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/** Current-run checks. Reading is off the UI thread; no pixels or input capture. */
final class StartupReview implements AutoCloseable {
    private final Activity activity;
    private final File report;
    private final Intent settings;
    private final HandlerThread thread=new HandlerThread("startup-check");
    private final Handler worker;
    private volatile boolean closed,visible,complete;
    private volatile String summary="正在等待本次运行的特性检查。待确认不表示已启用。";
    private View panel;
    private TextView body;
    private String lastBenchmark="",lastRenderer="";
    static File newReport(Context c) throws IOException {
        File root=RuntimeStorage.requireCompatible(c);
        File session=new File(root,"diagnostics/startup/session-"+System.currentTimeMillis()+"-"+UUID.randomUUID().toString().substring(0,8));
        if(!session.mkdirs())throw new IOException("无法创建本次启动检查目录。");
        return new File(session,"startup.json");
    }
    static String hardwareKey() {return Build.MODEL+"|"+Build.DEVICE+"|"+Build.VERSION.SDK_INT+"|"+Build.FINGERPRINT;}
    static String runtimeKey(Context c) {
        try{return BuildConfig.VERSION_NAME+"|"+c.getPackageManager().getPackageInfo(c.getPackageName(),0).lastUpdateTime;}
        catch(android.content.pm.PackageManager.NameNotFoundException error){return "unconfirmed";}
    }
    static String settingsKey(Intent intent) {
        StringBuilder key=new StringBuilder();
        for(String name:new String[]{"scale_hack","fps_cap","present_fsr","present_sharpen","shadow_boost"})key.append(name).append('=').append(intent.getIntExtra(name,0)).append('|');
        for(String name:new String[]{"aa","gpu_present_copy","fast_present","skip_readback","pc_compat","viewport_stretch","overlay_enabled","fsr3_ahb_probe","fsr3_force","fsr3_fg","fsr3_real_depth","fsr3_gpu_depth","fsr3_async","fsr3_direct_output"})key.append(name).append('=').append(intent.getBooleanExtra(name,false)).append('|');
        String diagnostic=intent.getStringExtra("diagnostic_env");
        if(diagnostic!=null&&!diagnostic.isEmpty())try{key.append(java.util.Arrays.toString(java.security.MessageDigest.getInstance("SHA-256").digest(diagnostic.getBytes(StandardCharsets.UTF_8))));}
        catch(java.security.NoSuchAlgorithmException error){key.append("diagnostic-unconfirmed");}
        return key.toString();
    }
    private static SharedPreferences prefs(Context c) {return c.getSharedPreferences(LauncherActivity.PREFS,Context.MODE_PRIVATE);}
    private static boolean known(Context c) {
        JSONObject cache=readAdviceCache(c);String renderer=cache.optString("renderer");
        return hardwareKey().equals(cache.optString("hardware")) && "2405CRPFDC".equals(Build.MODEL)
            && "dizi".equals(Build.DEVICE) && Build.VERSION.SDK_INT==34 && renderer.contains("Adreno") && renderer.contains("610");
    }
    interface Start {void run(boolean benchmark);}
    static void askBeforeStart(Activity a,boolean alreadyRequested,Start start) {
        SharedPreferences p=prefs(a);
        if(alreadyRequested || known(a) || hardwareKey().equals(p.getString("hardware_reviewed",""))) {start.run(alreadyRequested);return;}
        new AlertDialog.Builder(a).setTitle("为这台设备推荐画质")
            .setMessage("尚无可匹配的硬件实测。是否在进入游戏后采集约30秒有效场景，提供画质建议？\n\n请照常游玩；菜单、加载和过场不计入。不会读取画面或记录按键，现有设置保持原样。")
            .setPositiveButton("同意短测并启动",(d,w)->{p.edit().putString("hardware_reviewed",hardwareKey()).commit();start.run(true);})
            .setNegativeButton("保留设置启动",(d,w)->{p.edit().putString("hardware_reviewed",hardwareKey()).commit();start.run(false);})
            .setNeutralButton("取消",null).show();
    }
    static void showAdvice(Activity a,int currentScale,String settingsKey,Runnable request) {
        String text;JSONObject cache=readAdviceCache(a),benchmark=null;
        if(hardwareKey().equals(cache.optString("hardware")) && settingsKey.equals(cache.optString("settings"))
            && runtimeKey(a).equals(cache.optString("build")))benchmark=cache.optJSONObject("benchmark");
        if(benchmark!=null && "complete".equals(benchmark.optString("phase")) && benchmark.optDouble("seconds",0)>=29) {
            double fps=benchmark.optDouble("fps",0),p95=benchmark.optDouble("p95_ms",0);
            text=String.format(Locale.ROOT,"当前组合的有效场景短测：%.1f FPS，p95 %.1fms。\n",fps,p95);
            if(fps<55 || p95>25)text+="建议先试 "+Math.max(1,currentScale-1)+"x 原生倍率，再做同场景比较；3x/60目标尚未因此通过。";
            else text+="建议先保留当前组合；这一场景的结果不能保证所有章节稳定60帧。";
        } else if(known(a)) {
            text="这台 Adreno 610 设备已有项目实测：3x 场景未达到稳定60帧，MSAA和过滤曾出现回退。可先试较低倍率；当前设置不会自动更改，3x性能目标继续保留。";
        } else text="没有匹配当前硬件及设置的有效短测。可请求下次进入可操作游戏后采集约30秒，再提供建议。";
        new AlertDialog.Builder(a).setTitle("本机画质建议").setMessage(text+"\n\n建议需要您自行应用，硬件识别不会覆盖已有设置。")
            .setPositiveButton("请求下次短测",(d,w)->request.run()).setNegativeButton("保留设置",null).show();
    }
    StartupReview(Activity a,File report,Intent intent,ViewGroup parent,boolean show) {
        activity=a;this.report=report;settings=intent;
        if(show) {
            LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(24),dp(20),dp(24),dp(20));card.setBackgroundColor(Color.rgb(7,15,22));
            TextView title=new TextView(a);title.setText("本次启动自检");title.setTextColor(Color.rgb(102,224,232));title.setTextSize(24);card.addView(title);
            body=new TextView(a);body.setText(summary);body.setTextColor(Color.rgb(235,247,249));body.setTextSize(16);body.setPadding(0,dp(16),0,dp(12));
            ScrollView scroll=new ScrollView(a);scroll.addView(body);card.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            TextView hint=new TextView(a);hint.setText("待确认项目不会算作通过；可先继续，稍后从暂停菜单查看。\n本次报告保存在资料目录 diagnostics/startup。");hint.setTextColor(Color.LTGRAY);hint.setTextSize(13);card.addView(hint);
            Button next=new Button(a);next.setText("继续游戏");next.setOnClickListener(v->dismiss());card.addView(next,new LinearLayout.LayoutParams(-1,dp(48)));
            parent.addView(card,new ViewGroup.LayoutParams(-1,-1));panel=card;visible=true;
        }
        thread.start();worker=new Handler(thread.getLooper());worker.post(poll);
    }
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    boolean isShowing(){return visible;}
    void dismiss(){visible=false;if(panel!=null)panel.setVisibility(View.GONE);}
    // Only on explicit pause-menu access; the live-frame path never reads files.
    String summary(){try{return format(readCurrent());}catch(IOException error){return "本次检查仍待确认："+error.getMessage();}}
    private static File adviceFile(Context c) {return new File(RuntimeStorage.dataBase(c),"diagnostics/startup/hardware-advice.json");}
    private static JSONObject readAdviceCache(Context c) {
        try {JSONObject d=readJson(adviceFile(c));if(d!=null && d.optInt("schema",0)==1)return d;}
        catch(IOException ignored){}
        return new JSONObject();
    }
    private void updateAdviceCache(JSONObject current,JSONObject benchmark) throws IOException {
        JSONObject cache=readJson(adviceFile(activity));
        if(cache!=null && cache.optInt("schema",0)!=1)throw new IOException("建议缓存版本未知，已保留。");
        if(cache==null)cache=new JSONObject();
        String renderer=current.optString("renderer");
        boolean same=hardwareKey().equals(cache.optString("hardware")) && renderer.equals(cache.optString("renderer")) && runtimeKey(activity).equals(cache.optString("build"));
        try {
            if(!same){cache.remove("benchmark");cache.remove("settings");}
            cache.put("schema",1).put("hardware",hardwareKey()).put("renderer",renderer).put("build",runtimeKey(activity));
            if(benchmark!=null)cache.put("benchmark",benchmark).put("settings",settingsKey(settings));
        }catch(org.json.JSONException error){throw new IOException(error);}
        File file=adviceFile(activity);RuntimeStorage.requireCompatible(activity);
        if(!file.getParentFile().isDirectory()&&!file.getParentFile().mkdirs())throw new IOException("建议缓存目录不可用。");
        AtomicFile atomic=new AtomicFile(file);FileOutputStream output=null;
        try {output=atomic.startWrite();output.write(cache.toString().getBytes(StandardCharsets.UTF_8));atomic.finishWrite(output);}
        catch(IOException error){if(output!=null)atomic.failWrite(output);throw error;}
    }
    private JSONObject readCurrent() throws IOException {return readCurrent(report,Process.myPid());}
    static JSONObject readCurrent(File file,long expectedPid) throws IOException {
        JSONObject d=readJson(file);
        return d!=null && d.optLong("pid",-1)==expectedPid && d.optInt("schema",0)==1 ? d:null;
    }
    private static JSONObject readJson(File report) throws IOException {
        if(!report.isFile())return null;
        if(report.length()>65536)throw new IOException("本次报告过大。");
        try(InputStream in=new FileInputStream(report);ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
            byte[] block=new byte[4096];int n;while((n=in.read(block))!=-1){if(bytes.size()+n>65536)throw new IOException("本次报告过大。");bytes.write(block,0,n);}
            JSONObject d=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            return d;
        }catch(org.json.JSONException error){return null;}
    }
    static String format(JSONObject d) {
        if(d==null)return "等待当前运行报告。文本、分辨率、过滤及抗锯齿尚待确认。";
        StringBuilder text=new StringBuilder();
        Object mounted=d.opt("text_patch");text.append("中文资源：").append(Boolean.TRUE.equals(mounted)?"已挂载":Boolean.FALSE.equals(mounted)?"保留原资源（中文修订未挂载）":"待确认");
        text.append("\n渲染分辨率：");
        if(d.isNull("width")||d.isNull("height"))text.append("待确认");else text.append(d.optInt("width")).append(" × ").append(d.optInt("height"));
        text.append("\n多重采样：");
        if(d.isNull("msaa_actual"))text.append("待确认");else {int requested=d.optInt("msaa_requested",1),actual=d.optInt("msaa_actual",1);text.append("请求").append(requested).append("x，生效").append(actual).append("x");if(actual<requested)text.append("（边缘平滑减弱）");}
        text.append("\n纹理过滤：");
        if(d.isNull("af_actual"))text.append("待确认");else {text.append("请求").append(d.optDouble("af_requested",1)).append("x，报告").append(d.optDouble("af_actual",1)).append("x");if(!d.optBoolean("af_verified",false))text.append("（尚未读回确认）");else if(d.optDouble("af_actual",1)<d.optDouble("af_requested",1))text.append("（远处贴图清晰度降低）");}
        text.append("\n设备渲染器：").append(d.isNull("renderer")?"待确认":d.optString("renderer"));
        JSONObject b=d.optJSONObject("benchmark");String phase=b==null?"":b.optString("phase");
        if("complete".equals(phase))text.append(String.format(Locale.ROOT,"\n短测：%.1f FPS，p95 %.1fms，p99 %.1fms\n仅代表这次有效场景，不保证全关60帧。",b.optDouble("fps",0),b.optDouble("p95_ms",0),b.optDouble("p99_ms",0)));
        else if("waiting_play".equals(phase))text.append("\n短测：已同意，等待可操作游戏累计约30秒；请照常游玩。");
        else text.append("\n短测：未申请，保持当前画质设置。");
        return text.toString();
    }
    private final Runnable poll=new Runnable(){public void run(){
        if(closed)return;
        try {
            JSONObject d=readCurrent();summary=format(d);
            if(d!=null && !d.isNull("renderer") && !d.optString("renderer").equals(lastRenderer)) {
                lastRenderer=d.optString("renderer");try{updateAdviceCache(d,null);}catch(IOException error){Log.w("StartupReview","Advice cache unavailable",error);}
            }
            JSONObject b=d==null?null:d.optJSONObject("benchmark");
            if(b!=null && "complete".equals(b.optString("phase"))) {
                String value=b.toString();complete=true;
                if(!value.equals(lastBenchmark)){lastBenchmark=value;try{updateAdviceCache(d,b);}catch(IOException error){Log.w("StartupReview","Benchmark cache unavailable",error);}}
            }
        }catch(IOException error){summary="本次检查报告暂时无法读取，特性保持待确认。\n"+error.getMessage();}
        activity.runOnUiThread(()->{if(!closed&&body!=null&&visible)body.setText(summary);});
        boolean benchmark=settings.getBooleanExtra("benchmark",false);
        if(!closed && (visible || (benchmark&&!complete)))worker.postDelayed(this,visible?500:2000);
    }};
    public void close(){closed=true;worker.removeCallbacksAndMessages(null);thread.quitSafely();}
}
