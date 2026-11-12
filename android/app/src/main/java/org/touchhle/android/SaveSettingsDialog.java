package org.touchhle.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shared settings editor for the three paid-plugin unlock booleans. */
final class SaveSettingsDialog {
    static void show(Activity activity) {
        LauncherLanguage.initialize(activity);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(18 * activity.getResources().getDisplayMetrics().density);
        panel.setPadding(pad, pad, pad, pad);
        panel.setBackgroundColor(Color.rgb(12, 21, 30));
        TextView status = label(activity, LauncherLanguage.text("读取共享 settings.sb。保存前自动备份整个 Documents。"), panel);
        CheckBox attack = toggle(activity, LauncherLanguage.text("攻击增强（attack）"), panel);
        CheckBox defense = toggle(activity, LauncherLanguage.text("防御增强（defense）"), panel);
        CheckBox loot = toggle(activity, LauncherLanguage.text("资源增强（loot）"), panel);
        TextView warning = label(activity,
                LauncherLanguage.text("0 / 1 为开关。若旧存档缺少字段，首次保存会补齐结构；写入失败时不会替换原文件。"),
                panel);
        ScrollView scroll = new ScrollView(activity); scroll.addView(panel);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(LauncherLanguage.text("内购插件"))
                .setView(scroll).setNegativeButton(LauncherLanguage.text("关闭"), null)
                .setPositiveButton(LauncherLanguage.text("备份并保存"), null).create();
        ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "save-settings"));
        dialog.setOnDismissListener(ignored -> worker.shutdown());
        dialog.setOnShowListener(ignored -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE); save.setEnabled(false);
            setEnabled(attack, defense, loot, false);
            worker.execute(() -> {
                try {
                    SaveSettingsEditor.Snapshot value = SaveArchiveImporter.readSettings(activity);
                    activity.runOnUiThread(() -> {
                        attack.setChecked(value.attack); defense.setChecked(value.defense); loot.setChecked(value.loot);
                        String suffix = value.complete ? LauncherLanguage.text("字段完整。") : LauncherLanguage.text("缺少字段：") + value.missing + LauncherLanguage.text("；保存时会补齐。");
                        status.setText(LauncherLanguage.text("已读取 settings.sb。") + suffix);
                        save.setEnabled(true); setEnabled(attack, defense, loot, true);
                    });
                } catch (Exception error) {
                    activity.runOnUiThread(() -> status.setText(LauncherLanguage.text("无法读取共享设置：") + error.getMessage()));
                }
            });
            save.setOnClickListener(v -> {
                save.setEnabled(false); setEnabled(attack, defense, loot, false);
                status.setText(LauncherLanguage.text("正在备份并写入 settings.sb……"));
                worker.execute(() -> {
                    try {
                        java.io.File backup = SaveArchiveImporter.editSettings(activity,
                                attack.isChecked(), defense.isChecked(), loot.isChecked());
                        activity.runOnUiThread(() -> {
                            status.setText(LauncherLanguage.text("已保存内购插件状态。\n备份：") + backup.getName());
                            save.setEnabled(true); setEnabled(attack, defense, loot, true);
                        });
                    } catch (Exception error) {
                        activity.runOnUiThread(() -> {
                            status.setText(LauncherLanguage.text("未保存：") + error.getMessage());
                            save.setEnabled(true); setEnabled(attack, defense, loot, true);
                        });
                    }
                });
            });
        });
        dialog.show();
    }

    private static void setEnabled(CheckBox a, CheckBox b, CheckBox c, boolean value) {
        a.setEnabled(value); b.setEnabled(value); c.setEnabled(value);
    }
    private static CheckBox toggle(Activity activity, String text, LinearLayout parent) {
        CheckBox box = new CheckBox(activity); box.setText(text); box.setTextColor(Color.WHITE); parent.addView(box); return box;
    }
    private static TextView label(Activity activity, String text, LinearLayout parent) {
        TextView view = new TextView(activity); view.setText(text); view.setTextColor(Color.rgb(225,239,245)); view.setTextSize(14); view.setPadding(0, 10, 0, 6); parent.addView(view); return view;
    }
}
