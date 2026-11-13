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
    interface Confirm { void accept(boolean text, boolean lighting, int scale); }
    private static String t(String value) { return LauncherLanguage.text(value); }
    static boolean lighting(SharedPreferences preferences) {
        return preferences.getBoolean(LIGHTING, BuildConfig.BUNDLED_TEXTURES);
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
        CheckBox text=new CheckBox(activity);text.setText(t("高清中文文字组件（章节 / 任务 / 字幕）"));
        text.setChecked(preferences.getBoolean("overlay_enabled",true));body.addView(text);
        CheckBox lighting=new CheckBox(activity);lighting.setText(t("高级光影"));
        lighting.setChecked(lighting(preferences));body.addView(lighting);
        TextView effects=new TextView(activity);
        effects.setText(t(BuildConfig.BUNDLED_TEXTURES
                ? "HD版默认开启高级光影，恢复护甲与环境反光、动态灯光等原生高档效果。"
                : "普通版也支持高级光影，默认关闭。可在画质设置中开启，恢复护甲与环境反光、动态灯光等原生高档效果。"));
        body.addView(effects);
        TextView scaleLabel=new TextView(activity);scaleLabel.setText(t("内部分辨率"));body.addView(scaleLabel);
        Spinner scale=new Spinner(activity);
        scale.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"1× · 480 × 320","2× · 960 × 640","3× · 1440 × 960"}));
        scale.setSelection(Math.max(0,Math.min(2,currentScale-1)));body.addView(scale);
        TextView output=new TextView(activity);output.setText(resolution(activity,currentScale,stretch));body.addView(output);
        scale.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent,android.view.View view,int position,long id) {
                output.setText(resolution(activity,position+1,stretch));
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        TextView languages=new TextView(activity);languages.setPadding(0,pad,0,0);
        languages.setText(t("高清文字目前为中文。可关闭高清中文组件，使用游戏原生文字与语言设置。欢迎多语言用户投稿；若暂无投稿，我们计划在R8之前支持其他语言的高清文字组件。"));
        body.addView(languages);
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);
        new AlertDialog.Builder(activity).setTitle(t("首次启动 · 画面设置复核")).setView(scroll)
                .setNegativeButton(t("返回设置"),null)
                .setPositiveButton(t("确认并启动"),(dialog,which)->confirm.accept(text.isChecked(),lighting.isChecked(),scale.getSelectedItemPosition()+1))
                .show();
    }
}
