package org.touchhle.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One-page editor: story-save fields above, shared in-app-purchase fields below. */
final class SaveEditorDialog {
    static void show(Activity activity) {
        LauncherLanguage.initialize(activity);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(18 * activity.getResources().getDisplayMetrics().density);
        panel.setPadding(pad, pad, pad, pad);
        panel.setBackgroundColor(Color.rgb(12, 21, 30));

        TextView status = label(activity, LauncherLanguage.text("读取和保存当前存档；ZIP 导入和导出在启动器存档页顶部。保存前自动备份整个 Documents。"), panel);
        RadioGroup slots = new RadioGroup(activity);
        slots.setOrientation(RadioGroup.HORIZONTAL);
        panel.addView(slots);
        for (int i = 0; i < 3; i++) {
            RadioButton button = new RadioButton(activity);
            button.setId(i + 1); button.setText(LauncherLanguage.text("档案 ") + (i + 1)); button.setTextColor(Color.WHITE);
            slots.addView(button);
        }
        label(activity, LauncherLanguage.text("你想要（上半区）"), panel);
        EditText credits = number(activity, LauncherLanguage.text("金钱 / 信用点（0–9999999）"), panel);
        EditText nodes = number(activity, LauncherLanguage.text("能量节点（0–99999）"), panel);
        label(activity, LauncherLanguage.text("大脉冲"), panel);
        CheckBox burstAvailable = toggle(activity, LauncherLanguage.text("持有大脉冲"), panel);
        EditText burstClip = number(activity, LauncherLanguage.text("大脉冲弹匣 / 能量（0–999）"), panel);
        EditText burstAlt = number(activity, LauncherLanguage.text("大脉冲副模式（0–1）"), panel);
        TextView saveWarning = label(activity, LauncherLanguage.text("修改过的字段会同步到本档案的当前进度和关卡起点；缺少字段的旧档案不会强行合成。请先导出三槽位 ZIP，数值含义仍在核对，错误值可能损坏存档。"), panel);

        label(activity, LauncherLanguage.text("内购插件（上半区）"), panel);
        CheckBox attack = toggle(activity, LauncherLanguage.text("攻击增强（attack）"), panel);
        CheckBox defense = toggle(activity, LauncherLanguage.text("防御增强（defense）"), panel);
        CheckBox loot = toggle(activity, LauncherLanguage.text("资源增强（loot）"), panel);
        TextView settingsWarning = label(activity, LauncherLanguage.text("共享 settings.sb 的三个开关。若旧存档缺少字段，保存时会补齐结构；写入前自动备份。"), panel);
        label(activity, LauncherLanguage.text("全部可编辑项（下半区）"), panel);
        label(activity, LauncherLanguage.text("护甲等级"), panel);
        Spinner armor = armorSpinner(activity, panel);
        LinearLayout fullFields = new LinearLayout(activity); fullFields.setOrientation(LinearLayout.VERTICAL); panel.addView(fullFields);
        TextView fullFieldsStatus = label(activity, LauncherLanguage.text("读取档案后显示本档可编辑的生命、升级、武器、设置和周目字段；不存在的字段不会被合成。"), panel);

        ScrollView scroll = new ScrollView(activity); scroll.addView(panel);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(LauncherLanguage.text("存档编辑与内购编辑"))
                .setView(scroll).setNegativeButton(LauncherLanguage.text("关闭"), null).setPositiveButton(LauncherLanguage.text("备份并保存"), null).create();
        panel.setFocusableInTouchMode(true); panel.requestFocus();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                | android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        final int[] request = {0}, selected = {-1};
        final boolean[] loading = {true}, pluginLoaded = {false};
        final boolean[] settingsLoaded = {false};
        final int[] available = {0};
        final long[] loaded = {0, 0, 0, 0, 0, 0};
        final boolean[] loadedPlugins = {false, false, false};
        final LinkedHashMap<String, EditText> fullInputs = new LinkedHashMap<>();
        final LinkedHashMap<String, String> loadedFull = new LinkedHashMap<>();
        ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "save-editor"));
        dialog.setOnDismissListener(ignored -> worker.shutdown());
        dialog.setOnShowListener(ignored -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE); save.setEnabled(false);
            setEnabled(credits, nodes, armor, burstAvailable, burstClip, burstAlt, attack, defense, loot, false); setFieldsEnabled(fullInputs, false);
            slots.setOnCheckedChangeListener((group, id) -> {
                if (id < 1 || id > 3 || id == selected[0]) return;
                selected[0] = id; final int serial = ++request[0]; loading[0] = true; pluginLoaded[0] = false;
                save.setEnabled(false); setEnabled(credits, nodes, armor, burstAvailable, burstClip, burstAlt, attack, defense, loot, false); setFieldsEnabled(fullInputs, false);
                status.setText(LauncherLanguage.text("正在读取档案 ") + id + LauncherLanguage.text(" 和共享 settings.sb……"));
                worker.execute(() -> {
                    try {
                        SaveEditorSnapshot snapshot = SaveArchiveImporter.readEditor(activity, id - 1);
                        SaveScalarEditor.Slot value = snapshot.slot;
                        SaveSettingsEditor.Snapshot settings = snapshot.settings;
                        LinkedHashMap<String,String> fields = snapshot.fields;
                        activity.runOnUiThread(() -> {
                            if (!dialog.isShowing() || serial != request[0]) return;
                            available[0] = value == null ? 0 : value.availableFields;
                            settingsLoaded[0] = settings != null;
                            loaded[0] = value == null ? 0 : value.credits; loaded[1] = value == null ? 0 : value.nodes;
                            loaded[2] = value == null ? 0 : value.rig; loaded[3] = value != null && value.burstAvailable ? 1 : 0;
                            loaded[4] = value == null ? 0 : value.burstClip; loaded[5] = value == null ? 0 : value.burstAlt;
                            credits.setText(Long.toString(loaded[0])); nodes.setText(Long.toString(loaded[1])); armor.setSelection((int)loaded[2]);
                            burstAvailable.setChecked(loaded[3] != 0); burstClip.setText(Long.toString(loaded[4])); burstAlt.setText(Long.toString(loaded[5]));
                            loadedPlugins[0] = settings != null && settings.attack; loadedPlugins[1] = settings != null && settings.defense; loadedPlugins[2] = settings != null && settings.loot;
                            attack.setChecked(loadedPlugins[0]); defense.setChecked(loadedPlugins[1]); loot.setChecked(loadedPlugins[2]);
                            fullFields.removeAllViews(); fullInputs.clear(); loadedFull.clear(); loadedFull.putAll(fields);
                            for (Map.Entry<String,String> entry : fields.entrySet()) {
                                EditText input = fieldInput(activity, entry.getKey(), fullFields); input.setText(entry.getValue()); fullInputs.put(entry.getKey(), input);
                            }
                            fullFieldsStatus.setText(fields.isEmpty() ? LauncherLanguage.text("本档没有可显示的额外可编辑字段；上方专项字段仍可编辑。") : LauncherLanguage.text("下方已列出 ") + fields.size() + LauncherLanguage.text(" 个可编辑字段；只写入你实际改动的字段。备份覆盖整个 Documents。") );
                            pluginLoaded[0] = settings != null || available[0] != 0 || !fields.isEmpty(); loading[0] = false; save.setEnabled(pluginLoaded[0]);
                            settingsWarning.setText(settings == null ? LauncherLanguage.text("无法读取共享设置：") + snapshot.settingsError : settings.complete ? LauncherLanguage.text("共享 settings.sb：字段完整。") : LauncherLanguage.text("共享 settings.sb：缺少字段：") + settings.missing + LauncherLanguage.text("；保存时会补齐。"));
                            status.setText(LauncherLanguage.text("已读取档案 ") + id + (value == null ? "\n" + LauncherLanguage.text("剧情进度不可用：") + snapshot.storyError : ""));
                            saveWarning.setText(available[0] == 63 ? LauncherLanguage.text("修改过的字段会同步到本档案的当前进度和关卡起点；请先导出三槽位 ZIP，数值含义仍在核对，错误值可能损坏存档。") : LauncherLanguage.text("灰色字段尚未在此存档中生成；继续游戏并保存后可再读取。现有字段和共享内购设置可独立修改，不会合成缺失的剧情字段。"));
                            setAvailable(credits, nodes, armor, burstAvailable, burstClip, burstAlt, attack, defense, loot, available[0], settingsLoaded[0]); setFieldsEnabled(fullInputs, true);
                        });
                    } catch (Exception error) {
                        activity.runOnUiThread(() -> { if (serial == request[0]) { loading[0] = false; save.setEnabled(false); status.setText(LauncherLanguage.text("无法读取编辑数据：") + error.getMessage()); }});
                    }
                });
            });
            save.setOnClickListener(v -> {
                if (loading[0] || !pluginLoaded[0]) return;
                final long money, count, clip, alt;
                try { money = Long.parseLong(credits.getText().toString().trim()); count = Long.parseLong(nodes.getText().toString().trim()); clip = Long.parseLong(burstClip.getText().toString().trim()); alt = Long.parseLong(burstAlt.getText().toString().trim()); }
                catch (NumberFormatException error) { status.setText(LauncherLanguage.text("请输入有效的整数。")); return; }
                if (money < 0 || money > 9_999_999 || count < 0 || count > 99_999 || clip < 0 || clip > 999 || alt < 0 || alt > 1) { status.setText(LauncherLanguage.text("数值超出上方显示的范围。")); return; }
                int slot = slots.getCheckedRadioButtonId() - 1, rig = armor.getSelectedItemPosition();
                LinkedHashMap<String,String> fullChanges = new LinkedHashMap<>();
                for (Map.Entry<String,EditText> entry : fullInputs.entrySet()) {
                    String value = entry.getValue().getText().toString().trim();
                    String old = loadedFull.get(entry.getKey());
                    if (!value.equals(old)) fullChanges.put(entry.getKey(), value);
                }
                int changes = ((money != loaded[0] ? 1 : 0) | (count != loaded[1] ? 2 : 0) | (rig != loaded[2] ? 4 : 0)
                        | ((burstAvailable.isChecked() ? 1 : 0) != loaded[3] ? 8 : 0) | (clip != loaded[4] ? 16 : 0) | (alt != loaded[5] ? 32 : 0)) & available[0];
                boolean pluginChanges = settingsLoaded[0] && (attack.isChecked() != loadedPlugins[0] || defense.isChecked() != loadedPlugins[1] || loot.isChecked() != loadedPlugins[2]);
                if (changes == 0 && !pluginChanges && fullChanges.isEmpty()) { status.setText(LauncherLanguage.text("数值没有变化，无需保存。")); return; }
                save.setEnabled(false); loading[0] = true; setEnabled(credits, nodes, armor, burstAvailable, burstClip, burstAlt, attack, defense, loot, false); setFieldsEnabled(fullInputs, false); status.setText(LauncherLanguage.text("正在备份并写入存档、全部可编辑字段与共享 settings.sb……"));
                final int writeChanges = changes; final boolean writePlugins = pluginChanges; final LinkedHashMap<String,String> writeFull = new LinkedHashMap<>(fullChanges);
                final boolean writeBurstAvailable = burstAvailable.isChecked();
                final boolean writeAttack = attack.isChecked(), writeDefense = defense.isChecked(), writeLoot = loot.isChecked();
                worker.execute(() -> {
                    try {
                        StringBuilder backups = new StringBuilder();
                        if (writeChanges != 0) {
                            java.io.File backup = SaveArchiveImporter.editSlot(activity, slot, money, count, rig, writeBurstAvailable, clip, alt, writeChanges);
                            backups.append(LauncherLanguage.text("存档：")).append(backup.getName());
                        }
                        if (!writeFull.isEmpty()) {
                            java.io.File backup = SaveArchiveImporter.editFields(activity, slot, writeFull);
                            if (backups.length() > 0) backups.append("\n"); backups.append(LauncherLanguage.text("全部字段：")).append(backup.getName());
                        }
                        if (writePlugins) {
                            java.io.File backup = SaveArchiveImporter.editSettings(activity, writeAttack, writeDefense, writeLoot);
                            if (backups.length() > 0) backups.append("\n"); backups.append(LauncherLanguage.text("共享设置：")).append(backup.getName());
                        }
                        activity.runOnUiThread(() -> {
                            status.setText(LauncherLanguage.text("已保存档案 ") + (slot + 1) + LauncherLanguage.text(" 和内购设置。\n备份：") + backups); loading[0] = false; save.setEnabled(true);
                            loaded[0] = money; loaded[1] = count; loaded[2] = rig; loaded[3] = writeBurstAvailable ? 1 : 0; loaded[4] = clip; loaded[5] = alt;
                            loadedPlugins[0] = writeAttack; loadedPlugins[1] = writeDefense; loadedPlugins[2] = writeLoot; loadedFull.putAll(writeFull); setAvailable(credits, nodes, armor, burstAvailable, burstClip, burstAlt, attack, defense, loot, available[0], settingsLoaded[0]); setFieldsEnabled(fullInputs, true);
                        });
                    } catch (Exception error) {
                        activity.runOnUiThread(() -> { status.setText(LauncherLanguage.text("未保存：") + error.getMessage()); loading[0] = false; save.setEnabled(true); setAvailable(credits, nodes, armor, burstAvailable, burstClip, burstAlt, attack, defense, loot, available[0], settingsLoaded[0]); setFieldsEnabled(fullInputs, true); });
                    }
                });
            });
            slots.check(1);
        });
        dialog.show();
    }

    private static void setEnabled(EditText credits, EditText nodes, Spinner armor, CheckBox burstAvailable, EditText burstClip, EditText burstAlt, CheckBox attack, CheckBox defense, CheckBox loot, boolean value) {
        credits.setEnabled(value); nodes.setEnabled(value); armor.setEnabled(value); burstAvailable.setEnabled(value); burstClip.setEnabled(value); burstAlt.setEnabled(value); attack.setEnabled(value); defense.setEnabled(value); loot.setEnabled(value);
    }
    private static void setAvailable(EditText credits, EditText nodes, Spinner armor, CheckBox burstAvailable, EditText burstClip, EditText burstAlt, CheckBox attack, CheckBox defense, CheckBox loot, int mask, boolean settings) {
        View[] controls = {credits,nodes,armor,burstAvailable,burstClip,burstAlt};
        for(int i=0;i<controls.length;i++) controls[i].setEnabled((mask & (1 << i)) != 0);
        attack.setEnabled(settings); defense.setEnabled(settings); loot.setEnabled(settings);
    }
    private static void setFieldsEnabled(Map<String, EditText> fields, boolean value) { for (EditText field : fields.values()) field.setEnabled(value); }
    private static String fieldLabel(String path) {
        int slash = path.lastIndexOf('/'); String leaf = slash < 0 ? path : path.substring(slash + 1);
        String label = leaf;
        if ("hp".equals(leaf)) label = LauncherLanguage.text("生命值"); else if ("upgrade_level_health".equals(leaf)) label = LauncherLanguage.text("生命升级");
        else if ("upgrade_level_o2".equals(leaf)) label = LauncherLanguage.text("氧气升级"); else if ("available".equals(leaf)) label = LauncherLanguage.text("持有");
        else if ("clip".equals(leaf)) label = LauncherLanguage.text("弹匣 / 能量"); else if ("altmode".equals(leaf)) label = LauncherLanguage.text("副模式");
        else if ("gameCompleted".equals(leaf)) label = LauncherLanguage.text("通关标志"); else if ("plus".equals(leaf)) label = LauncherLanguage.text("周目标志");
        else if ("subtitles_enabled".equals(leaf)) label = LauncherLanguage.text("字幕"); else if ("auto_reload".equals(leaf)) label = LauncherLanguage.text("自动装弹");
        return (slash < 0 ? LauncherLanguage.text("共享") : path.substring(0, slash)) + " · " + label;
    }
    private static EditText fieldInput(Activity activity, String path, LinearLayout parent) {
        label(activity, fieldLabel(path), parent);
        EditText view = new EditText(activity); view.setTextColor(Color.WHITE);
        int flags = InputType.TYPE_CLASS_NUMBER;
        if (path.endsWith("sfx_volume") || path.endsWith("music_volume") || path.endsWith("brightness") || path.endsWith("sensitivity")) flags |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
        view.setInputType(flags); view.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI); view.setSingleLine(true); parent.addView(view); return view;
    }
    private static Spinner armorSpinner(Activity activity, LinearLayout parent) {
        Spinner spinner = new Spinner(activity);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, new String[]{LauncherLanguage.text("1级护服"), LauncherLanguage.text("2级护服"), LauncherLanguage.text("3级护服"), LauncherLanguage.text("4级护服"), LauncherLanguage.text("5级护服")}) {
            private View styled(View view) { ((TextView) view).setTextColor(Color.WHITE); view.setBackgroundColor(Color.rgb(18, 32, 43)); return view; }
            @Override public View getView(int position, View view, android.view.ViewGroup parent) { return styled(super.getView(position, view, parent)); }
            @Override public View getDropDownView(int position, View view, android.view.ViewGroup parent) { return styled(super.getDropDownView(position, view, parent)); }
        };
        spinner.setAdapter(adapter); parent.addView(spinner); return spinner;
    }
    private static CheckBox toggle(Activity activity, String text, LinearLayout parent) { CheckBox box = new CheckBox(activity); box.setText(text); box.setTextColor(Color.WHITE); parent.addView(box); return box; }
    private static TextView label(Activity activity, String text, LinearLayout parent) { TextView view = new TextView(activity); view.setText(text); view.setTextColor(Color.rgb(225, 239, 245)); view.setTextSize(14); view.setPadding(0, 10, 0, 6); parent.addView(view); return view; }
    private static EditText number(Activity activity, String text, LinearLayout parent) { label(activity, text, parent); EditText view = new EditText(activity); view.setTextColor(Color.WHITE); view.setInputType(InputType.TYPE_CLASS_NUMBER); view.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI); view.setSingleLine(true); parent.addView(view); return view; }
}
