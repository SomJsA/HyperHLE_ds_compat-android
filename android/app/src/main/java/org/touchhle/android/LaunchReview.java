package org.touchhle.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

/** First launch review uses existing preferences; never writes a distribution marker. */
final class LaunchReview {
    static final String LIGHTING = "advanced_lighting";
    static final String REVIEWED = "launch_review_r4_completed";
    interface Confirm { void accept(boolean text, boolean lighting, boolean textures, int scale, String language); }
    private static String t(String value) { return LauncherLanguage.text(value); }
    static boolean lighting(SharedPreferences preferences) {
        return preferences.getBoolean(LIGHTING, BuildConfig.BUNDLED_TEXTURES);
    }
    static boolean textures(SharedPreferences preferences) {
        return preferences.getBoolean("bundled_textures", BuildConfig.BUNDLED_TEXTURES);
    }
    /** Select the largest supported internal scale whose short edge stays within the device. */
    static int recommendedScale(Context context, int maxScale) {
        DisplayMetrics metrics=new DisplayMetrics();
        android.view.WindowManager manager=(android.view.WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        if(manager!=null) manager.getDefaultDisplay().getRealMetrics(metrics);
        else metrics.setTo(context.getResources().getDisplayMetrics());
        int shortEdge=Math.min(metrics.widthPixels,metrics.heightPixels);
        int scale=shortEdge/320;
        return Math.max(1,Math.min(Math.max(1,maxScale),scale));
    }
    static String resolution(Context context, int scale, boolean stretch) {
        DisplayMetrics metrics=new DisplayMetrics();
        android.view.WindowManager manager=(android.view.WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        if(manager!=null) manager.getDefaultDisplay().getRealMetrics(metrics);
        else metrics.setTo(context.getResources().getDisplayMetrics());
        int width=Math.max(metrics.widthPixels,metrics.heightPixels);
        int height=Math.min(metrics.widthPixels,metrics.heightPixels);
        int viewWidth=width,viewHeight=height;
        if (!stretch) {
            if ((long)width*2>(long)height*3) viewWidth=height*3/2;
            else viewHeight=width*2/3;
        }
        return t("内部渲染")+": "+scale+"× · "+(480*scale)+" × "+(320*scale)
                +"\n"+t("输出分辨率")+": "+width+" × "+height+" · "+t("设备屏幕")
                +"\n"+t("游戏视区")+": "+viewWidth+" × "+viewHeight;
    }
    static void show(Activity activity, SharedPreferences preferences, int currentScale,
                     boolean stretch, Confirm confirm) {
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(20*activity.getResources().getDisplayMetrics().density+.5f);
        body.setPadding(pad,pad,pad,pad);
        TextView explanation=new TextView(activity);
        explanation.setText(t("首次启动前，请复核以下画面设置；以后可在启动器中修改。"));
        body.addView(explanation);
        CheckBox text=new CheckBox(activity);text.setText(t("高清文字组件（章节 / 任务 / 字幕）"));
        text.setChecked(preferences.getBoolean("overlay_enabled",true));body.addView(text);
        TextView languageLabel=new TextView(activity);languageLabel.setText(t("高清语言"));body.addView(languageLabel);
        Spinner language=new Spinner(activity);
        language.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,
                HdTextLanguage.labels(activity)));
        language.setSelection(HdTextLanguage.index(activity));body.addView(language);
        CheckBox lighting=new CheckBox(activity);lighting.setText(t("高级光影"));
        lighting.setChecked(lighting(preferences));body.addView(lighting);
        TextView effects=new TextView(activity);
        effects.setText(t(BuildConfig.BUNDLED_TEXTURES
                ? "HD版默认开启高级光影，恢复护甲与环境反光、动态灯光等原生高档效果。"
                : "普通版也支持高级光影，默认关闭。可在画质设置中开启，恢复护甲与环境反光、动态灯光等原生高档效果。"));
        body.addView(effects);
        CheckBox textures=new CheckBox(activity);textures.setText(t("高清纹理"));
        textures.setChecked(textures(preferences));
        textures.setEnabled(BuildConfig.BUNDLED_TEXTURES);
        body.addView(textures);
        TextView textureNote=new TextView(activity);
        textureNote.setText(t(BuildConfig.BUNDLED_TEXTURES
                ? "HD版默认载入内置高清纹理；关闭后使用原版纹理，可降低显存和加载压力。"
                : "普通版仅含原版纹理；高清纹理需使用 HD 安装包。"));
        body.addView(textureNote);
        TextView scaleLabel=new TextView(activity);scaleLabel.setText(t("内部分辨率"));body.addView(scaleLabel);
        Spinner scale=new Spinner(activity);
        scale.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"1× · 480 × 320","2× · 960 × 640","3× · 1440 × 960","4× · 1920 × 1280",
                        "5× · 2400 × 1600","6× · 2880 × 1920","7× · 3360 × 2240","8× · 3840 × 2560"}));
        scale.setSelection(Math.max(0,Math.min(7,currentScale-1)));body.addView(scale);
        TextView output=new TextView(activity);output.setText(resolution(activity,currentScale,stretch));body.addView(output);
        scale.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent,android.view.View view,int position,long id) {
                output.setText(resolution(activity,position+1,stretch));
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        TextView languages=new TextView(activity);languages.setPadding(0,pad,0,0);
        languages.setText(t("可选中文、English、法语、德语、意大利语、韩语和西班牙语；日语原包只有占位文本，使用 English 回退。启动时同步原生语言；关闭高清文字组件仍保留选择。"));
        body.addView(languages);
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);
        new AlertDialog.Builder(activity).setTitle(t("首次启动 · 画面设置复核")).setView(scroll)
                .setNegativeButton(t("返回设置"),null)
                .setPositiveButton(t("确认并启动"),(dialog,which)->confirm.accept(text.isChecked(),lighting.isChecked(),textures.isChecked(),scale.getSelectedItemPosition()+1,
                        HdTextLanguage.codeAt(language.getSelectedItemPosition())))
                .show();
    }
}
