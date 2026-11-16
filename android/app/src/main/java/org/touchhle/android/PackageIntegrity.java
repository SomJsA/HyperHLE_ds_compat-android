package org.touchhle.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** A visible, cancellable preflight. Never changes graphics choices or user saves. */
final class PackageIntegrity {
    interface Ready { void accept(File ipa); }
    private static final AtomicBoolean BUSY=new AtomicBoolean();
    private PackageIntegrity() { }
    private static String t(String zh,String en){return LauncherLanguage.english()?en:zh;}
    private static String stamp(Context context,File apk) throws Exception {
        return apk.getCanonicalPath()+"|"+apk.length()+"|"+apk.lastModified()+"|"
                +context.getPackageManager().getPackageInfo(context.getPackageName(),0).lastUpdateTime+"|"+BuildConfig.RELEASE_TAG;
    }
    static void add(Activity activity,LinearLayout parent) {
        Button button=new Button(activity);button.setAllCaps(false);
        button.setText(t("检查安装包与内置资源","Verify package and bundled resources"));
        button.setOnClickListener(v->run(activity,true,
                activity.getSharedPreferences(LauncherActivity.PREFS,Context.MODE_PRIVATE)
                        .getBoolean("bundled_textures",BuildConfig.BUNDLED_TEXTURES),null));
        parent.addView(button);
    }
    static void run(Activity activity,boolean force,boolean textures,Ready ready) {
        if(!BUSY.compareAndSet(false,true))return;
        Context context=activity.getApplicationContext();
        SharedPreferences cache=context.getSharedPreferences("package_integrity_v1",Context.MODE_PRIVATE);
        File apk=new File(context.getApplicationInfo().sourceDir);
        AtomicBoolean cancelled=new AtomicBoolean();
        ProgressDialog dialog=new ProgressDialog(activity);
        dialog.setTitle(t("文件完整性检查","File integrity check"));
        dialog.setMessage(t("首次启动会检查安装包并准备内置资源，可能需要一些时间。","The first launch verifies the package and prepares bundled resources. This may take a little time."));
        dialog.setIndeterminate(true);dialog.setCancelable(true);dialog.setCanceledOnTouchOutside(false);
        Thread worker=new Thread(()->{
            String stage="package",sha=cache.getString("sha256","");
            try {
                RuntimeStorage.requireGameStopped(context);
                String identity=stamp(context,apk);
                if(force||!identity.equals(cache.getString("verified",""))) {
                    PackageIntegrityCore.Result result=PackageIntegrityCore.verify(apk);sha=result.apkSha256;
                    if(!identity.equals(stamp(context,apk)))throw new IOException("Installed package changed during verification");
                    if(!cancelled.get())cache.edit().putString("verified",identity).putString("sha256",sha).commit();
                    Log.i("touchHLE","[DSintegrity] package passed files="+result.files+" bytes="+result.bytes+" sha256="+sha);
                } else Log.i("touchHLE","[DSintegrity] unchanged installation; prior package check reused");
                if(cancelled.get())return;
                stage="resources";
                File ipa=BundledGameResources.ipa(context);
                BundledGameResources.textures(context,textures);
                if(cancelled.get())return;
                final String report="Release: "+BuildConfig.RELEASE_TAG+"\nAPK SHA-256: "+sha+"\nGame resource: "+ipa.getAbsolutePath();
                Log.i("touchHLE","[DSintegrity] resources passed; game launch permitted");
                activity.runOnUiThread(()->{
                    if(activity.isFinishing()||activity.isDestroyed()||cancelled.get())return;
                    dialog.dismiss();
                    if(ready!=null)ready.accept(ipa);
                    else new AlertDialog.Builder(activity).setTitle(t("自检通过","Integrity check passed"))
                            .setMessage(t("安装包与内置游戏资源检查通过。高清文字组件设置保持不变。","The installed package and bundled game resource passed verification. Your HD text setting is unchanged."))
                            .setPositiveButton(t("确定","OK"),null)
                            .setNeutralButton(t("复制详情","Copy details"),(d,w)->copy(context,report)).show();
                });
            } catch(Exception error) {
                if("package".equals(stage))cache.edit().remove("verified").apply();
                String member=error instanceof PackageIntegrityCore.Failure?((PackageIntegrityCore.Failure)error).member:stage;
                if(error instanceof PackageIntegrityCore.Failure)sha=((PackageIntegrityCore.Failure)error).apkSha256;
                String report="Release: "+BuildConfig.RELEASE_TAG+"\nStage: "+stage+"\nFile: "+member+"\nAPK SHA-256: "+sha+"\nReason: "+error.getMessage();
                Log.e("touchHLE","[DSintegrity] failed; native game not started\n"+report,error);
                if(!cancelled.get())activity.runOnUiThread(()->{
                    if(activity.isFinishing()||activity.isDestroyed()||cancelled.get())return;
                    dialog.dismiss();
                    new AlertDialog.Builder(activity).setTitle(t("文件自检未通过","File integrity check failed"))
                            .setMessage(t("文件损坏或资源准备失败，已留在启动器。请重新下载完整安装包后覆盖安装；若仍有问题，请复制详情到 Community 反馈。无需关闭高清文字组件。\n\n",
                                    "A file is damaged or resource preparation failed. The game has not been started. Download the complete APK and install it over this installation. If the issue persists, copy the details to Community Issues. Keep the HD text component enabled.\n\n")+report)
                            .setPositiveButton(t("确定","OK"),null)
                            .setNeutralButton(t("复制详情","Copy details"),(d,w)->copy(context,report))
                            .setNegativeButton(t("发布页","Releases"),(d,w)->activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(GithubUpdates.PROJECT_URL+"/releases/latest"))))
                            .show();
                });
            } finally {BUSY.set(false);}
        },"package-integrity");
        dialog.setOnCancelListener(d->{cancelled.set(true);worker.interrupt();});
        dialog.show();worker.setDaemon(true);worker.start();
    }
    private static void copy(Context context,String report) {
        ClipboardManager clipboard=(ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText("Dead Space integrity check",report));
    }
}
