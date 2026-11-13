package org.touchhle.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.text.util.Linkify;
import android.text.method.LinkMovementMethod;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Dead Space launch settings. Kept separate from SDLActivity so native audio /
 * GL startup does not begin until the player has chosen optional diagnostics.
 */
public class LauncherActivity extends Activity {
    private TextView saveNodeStatus;
    private EditText saveNodeSession;
    static final String PREFS = "dead_space_launch";
    static final String KEY_FPS = "show_fps";
    static final String KEY_LOGS = "debug_logs";
    static final String KEY_DUMPS = "dump_frames";
    static final String KEY_COMPAT = "pc_compat";
    static final String KEY_SCALE = "scale_hack";
    static final String KEY_CAP = "fps_cap";
    static final String KEY_STRETCH = "viewport_stretch";
    static final String KEY_FAST_PRESENT = "fast_present";
    static final String KEY_SKIP_READBACK = "skip_readback";
    static final String KEY_GPU_COPY = "gpu_present_copy";
    static final String KEY_TEXTURES = "bundled_textures";
    static final String KEY_FSR = "present_fsr";
    static final String KEY_AA = "aa_mode";
    static final String KEY_SHARPEN = "present_sharpen";
    static final String KEY_SHADOW_BOOST = "shadow_boost";
    static final String KEY_SAFE_BLACK_FLOOR = "safe_black_floor";
    static final String KEY_SAFE_BLACK_GRAY = "safe_black_gray";
    static final String KEY_TILT_PULSE = "tilt_pulse";
    static final String KEY_TILT_THRESHOLD = "tilt_threshold_mg";
    static final String LEGACY_TILT_THRESHOLD = "tilt_threshold_g100";
    static final String KEY_TILT_COOLDOWN = "tilt_cooldown_ms";
    static final String KEY_FSR3_AHB = "fsr3_ahb_probe";
    static final String KEY_FSR3_FG = "fsr3_frame_generation";
    static final String KEY_FSR3_DEPTH = "fsr3_real_depth";
    static final String KEY_FSR3_GPU_DEPTH = "fsr3_gpu_depth";
    static final String KEY_FSR3_ASYNC = "fsr3_async";
    static final String KEY_FSR3_DIRECT_OUTPUT = "fsr3_direct_output";
    static final String KEY_FSR3_FORCE = "fsr3_force_unsupported";
    static final String KEY_PRESET = "quality_preset";

    static final String PRESET_RECOMMENDED = "recommended";
    static final String PRESET_PERFORMANCE = "performance";
    static final String PRESET_COMPATIBILITY = "compatibility";
    static final String PRESET_FSR3 = "fsr3_experimental";
    static final String PRESET_CUSTOM = "custom";

    private static final String[] PRESET_KEYS = {
            PRESET_RECOMMENDED,
            PRESET_PERFORMANCE,
            PRESET_COMPATIBILITY,
            PRESET_FSR3,
            PRESET_CUSTOM
    };
    private static final String[] PRESET_LABELS = {
            "推荐画质",
            "性能优先",
            "兼容优先",
            "FSR3 实验",
            "自定义"
    };

    private static final int COLOR_BG = Color.rgb(7, 11, 17);
    private static final int COLOR_PANEL = Color.rgb(12, 21, 30);
    private static final int COLOR_PANEL_ALT = Color.rgb(17, 32, 43);
    private static final int COLOR_LINE = Color.rgb(39, 65, 79);
    private static final int COLOR_ACCENT = Color.rgb(102, 224, 232);
    private static final int COLOR_TEXT = Color.rgb(235, 247, 249);
    private static final int COLOR_MUTED = Color.rgb(132, 158, 168);
    private static final int COLOR_WARN = Color.rgb(238, 168, 91);
    private static final int REQUEST_IMPORT_SAVE_ZIP = 41;
    private static final int REQUEST_EXPORT_SAVE_ZIP = 42;

    private SharedPreferences prefs;
    private CheckBox fpsToggle;
    private CheckBox logsToggle;
    private CheckBox dumpsToggle;
    private CheckBox compatToggle;
    private CheckBox fastPresentToggle;
    private CheckBox skipReadbackToggle;
    private CheckBox gpuCopyToggle;
    private CheckBox textureToggle;
    private CheckBox fsr3AhbToggle;
    private CheckBox fsr3ForceToggle;
    private CheckBox fsr3FgToggle;
    private CheckBox fsr3DepthToggle;
    private CheckBox fsr3GpuDepthToggle;
    private CheckBox fsr3AsyncToggle;
    private CheckBox fsr3DirectOutputToggle;
    private CheckBox safeBlackFloorToggle;
    private CheckBox tiltPulseToggle;
    private SeekBar tiltThresholdSeek;
    private SeekBar tiltCooldownSeek;
    private TextView tiltThresholdValue;
    private TextView tiltCooldownValue;
    private SeekBar safeBlackGraySeek;
    private TextView safeBlackGrayValue;
    private TextView fsr3SelectionStatus;
    private TextView presetSummary;
    private TextView saveImportStatus;
    private Button advancedButton;
    private LinearLayout advancedContainer;
    private final Button[] presetButtons = new Button[PRESET_KEYS.length];
    private boolean fsr3Supported;
    private Button scaleButton;
    private Button capButton;
    private Button stretchButton;
    private Button fsrButton;
    private Button aaButton;
    private Button sharpenButton;
    private Button shadowBoostButton;
    private int scaleHack;
    private TextView primaryResolution;
    private int fpsCap;
    private boolean stretchViewport;
    private int fsrMode;
    private boolean aaEnabled;
    private int sharpenTenths;
    private int shadowBoostPercent;
    private boolean safeBlackFloor;
    private int safeBlackGray;
    private boolean tiltPulseEnabled;
    private int tiltPulseThresholdMilliG;
    private int tiltPulseCooldownMs;
    private String qualityPreset;
    private boolean advancedExpanded;
    private boolean applyingPreset;
    private boolean showFps;
    private boolean debugLogs;
    private boolean dumpFrames;
    private boolean pcCompat;
    private boolean fastPresent;
    private boolean skipReadback;
    private boolean gpuPresentCopy;
    private boolean bundledTextures;
    private boolean fsr3AhbEnabled;
    private boolean fsr3ForceEnabled;
    private boolean fsr3FgEnabled;
    private boolean fsr3DepthEnabled;
    private boolean fsr3GpuDepthEnabled;
    private boolean fsr3AsyncEnabled;
    private boolean fsr3DirectOutputEnabled;

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(L("离开启动器？"))
                .setMessage(L("当前没有运行中的游戏。返回桌面后，游戏不会自行退出。"))
                .setPositiveButton(L("返回桌面"), (d, which) -> finish())
                .setNegativeButton(L("取消"), null)
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        fsr3Supported = Fsr3Capabilities.supportsBasicVulkanPath(this);
        boolean freshInstall = prefs.getAll().isEmpty();
        LauncherLanguage.initialize(this);
        try { RuntimeStorage.initialize(this); }
        catch (IOException error) { new AlertDialog.Builder(this).setTitle(L("资料位置无法使用")).setMessage(error.getMessage()).setPositiveButton(L("查看设置"),null).show(); }
        freshInstall = freshInstall && !prefs.contains(KEY_SCALE) && !prefs.contains(KEY_PRESET);
        scaleHack = prefs.getInt(KEY_SCALE, 2);
        fpsCap = prefs.getInt(KEY_CAP, 0);
        stretchViewport = prefs.getBoolean(KEY_STRETCH, false);
        fsrMode = prefs.getInt(KEY_FSR, 0);
        aaEnabled = prefs.getBoolean(KEY_AA, false);
        sharpenTenths = prefs.getInt(KEY_SHARPEN, 0);
        shadowBoostPercent = Math.max(0, Math.min(100, prefs.getInt(KEY_SHADOW_BOOST, 0)));
        safeBlackFloor = prefs.getBoolean(KEY_SAFE_BLACK_FLOOR, false);
        safeBlackGray = Math.max(0, Math.min(160, prefs.getInt(KEY_SAFE_BLACK_GRAY, 75)));
        tiltPulseEnabled = prefs.getBoolean(KEY_TILT_PULSE, false);
        tiltPulseThresholdMilliG = TiltGestureSettings.clamp(prefs.getInt(KEY_TILT_THRESHOLD, TiltGestureSettings.upgradeLegacy(prefs.getInt(LEGACY_TILT_THRESHOLD, 32))));
        tiltPulseCooldownMs = Math.max(100, Math.min(1000, prefs.getInt(KEY_TILT_COOLDOWN, 380)));
        // Match the PC launcher contract: diagnostics are opt-in, while the
        // performance/rendering path remains enabled by the selected preset.
        showFps = prefs.getBoolean(KEY_FPS, false);
        debugLogs = prefs.getBoolean(KEY_LOGS, false);
        dumpFrames = prefs.getBoolean(KEY_DUMPS, false);
        pcCompat = prefs.getBoolean(KEY_COMPAT, true);
        fastPresent = prefs.getBoolean(KEY_FAST_PRESENT, true);
        skipReadback = prefs.getBoolean(KEY_SKIP_READBACK, true);
        gpuPresentCopy = prefs.getBoolean(KEY_GPU_COPY, true);
        bundledTextures = prefs.getBoolean(KEY_TEXTURES, BuildConfig.BUNDLED_TEXTURES);
        fsr3AhbEnabled = prefs.getBoolean(KEY_FSR3_AHB, false);
        fsr3ForceEnabled = prefs.getBoolean(KEY_FSR3_FORCE, false);
        fsr3FgEnabled = false;
        fsr3DepthEnabled = prefs.getBoolean(KEY_FSR3_DEPTH, false);
        fsr3GpuDepthEnabled = prefs.getBoolean(KEY_FSR3_GPU_DEPTH, false);
        fsr3AsyncEnabled = prefs.getBoolean(KEY_FSR3_ASYNC, false);
        fsr3DirectOutputEnabled = prefs.getBoolean(KEY_FSR3_DIRECT_OUTPUT, false);

        if (freshInstall) {
            qualityPreset = PRESET_RECOMMENDED;
            applyPresetValues(PRESET_RECOMMENDED);
            saveCurrentSettings();
        } else {
            qualityPreset = prefs.getString(KEY_PRESET, "");
            if (qualityPreset == null || qualityPreset.isEmpty()) {
                qualityPreset = detectCurrentPreset();
            }
            // Preserve an explicit preset. Unsupported paths are reported as
            // fallback at runtime rather than rewriting the user's settings.
        }

        // Automated real-device runs launch the exported launcher directly with
        // explicit extras. This avoids screenshot-driven UI control while still
        // exercising the same MainActivity startup path as an interactive run.
        Intent launchIntent = getIntent();
        if (launchIntent.getBooleanExtra("autorun", false)) {
            qualityPreset = PRESET_CUSTOM;
            tiltPulseEnabled = launchIntent.getBooleanExtra("tilt_pulse", tiltPulseEnabled);
            tiltPulseThresholdMilliG = TiltGestureSettings.clamp(launchIntent.getIntExtra("tilt_threshold_mg", launchIntent.hasExtra("tilt_threshold_g100") ? launchIntent.getIntExtra("tilt_threshold_g100", 32) * 10 : tiltPulseThresholdMilliG));
            tiltPulseCooldownMs = Math.max(100, Math.min(1000, launchIntent.getIntExtra("tilt_cooldown_ms", tiltPulseCooldownMs)));
            startGameWithConfig(
                    launchIntent.getBooleanExtra("show_fps", false),
                    launchIntent.getBooleanExtra("debug_logs", false),
                    launchIntent.getBooleanExtra("dump_frames", false),
                    launchIntent.getBooleanExtra("pc_compat", true),
                    launchIntent.getBooleanExtra("fast_present", true),
                    launchIntent.getBooleanExtra("skip_readback", true),
                    launchIntent.getBooleanExtra("gpu_present_copy", false),
                    Math.max(1, Math.min(3, launchIntent.getIntExtra("scale_hack", 2))),
                    launchIntent.getIntExtra("fps_cap", 0),
                    launchIntent.getBooleanExtra("viewport_stretch", false),
                    launchIntent.getIntExtra("present_fsr", 0),
                    launchIntent.getBooleanExtra("aa", false),
                    launchIntent.getIntExtra("present_sharpen", 0),
                    launchIntent.getBooleanExtra("safe_black_floor", false),
                    launchIntent.getIntExtra("safe_black_gray", 75),
                    launchIntent.getBooleanExtra("stats_log", false),
                    launchIntent.getBooleanExtra("fsr3_ahb_probe", false),
                    launchIntent.getBooleanExtra("fsr3_fg", false),
                    launchIntent.getBooleanExtra("fsr3_real_depth", false),
                    launchIntent.getBooleanExtra("fsr3_gpu_depth", false),
                    launchIntent.getBooleanExtra("fsr3_async", false),
                    launchIntent.getBooleanExtra("fsr3_direct_output", false),
                    launchIntent.getBooleanExtra("fsr3_diag_readback", false),
                    launchIntent.getBooleanExtra("fsr3_trace", false),
                    launchIntent.getBooleanExtra("fsr3_fg_synthetic", false),
                    launchIntent.getBooleanExtra("fsr3_fg_probe", false),
                    launchIntent.getStringExtra("ipa_path"));
            return;
        }

        setContentView(buildUi());
        applyImmersiveMode();
        getWindow().getDecorView().post(this::applyImmersiveMode);
        if (BuildConfig.SAVE_LAB && launchIntent.getBooleanExtra("save_lab_import", false)) {
            runSaveLabImportProbe();
        }
    }

    /** Fixed app-private fixture hook, available only in the isolated lab APK. */
    private void runSaveLabImportProbe() {
        new Thread(() -> {
            File base = new File(getFilesDir(), "save-lab");
            org.json.JSONObject result = new org.json.JSONObject();
            try {
                SaveArchiveCore.Result imported = SaveArchiveImporter.importZip(this,
                        Uri.fromFile(new File(base, "import-test.zip")));
                result.put("status", "ok"); result.put("sbin_files", imported.importedFiles);
                result.put("total_files", imported.totalFiles);
                result.put("backup", imported.backup == null ? org.json.JSONObject.NULL : imported.backup.getName());
            } catch (Exception error) {
                try { result.put("status", "error"); result.put("error", error.toString()); }
                catch (org.json.JSONException ignored) { }
            }
            try (FileOutputStream output = new FileOutputStream(new File(base, "import-test-result.json"))) {
                output.write(result.toString().getBytes(StandardCharsets.UTF_8)); output.getFD().sync();
            } catch (Exception error) { android.util.Log.e("SaveLab", "Could not record import probe", error); }
            String message = result.optString("status").equals("ok") ? L("存档导入测试完成") : L("存档导入被拒绝：") + result.optString("error");
            runOnUiThread(() -> updateSaveImportStatus(message, COLOR_TEXT));
        }, "save-lab-import-probe").start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT_SAVE_ZIP && requestCode != REQUEST_EXPORT_SAVE_ZIP) {
            return;
        }
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            updateSaveImportStatus(requestCode == REQUEST_EXPORT_SAVE_ZIP ? L("已取消 ZIP 导出。") : L("已取消 ZIP 导入。"), COLOR_MUTED);
            return;
        }
        if (requestCode == REQUEST_EXPORT_SAVE_ZIP) exportSaveZip(data.getData());
        else importSaveZip(data.getData());
    }

    private void chooseExportZip() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("application/zip");
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(new java.util.Date());
        intent.putExtra(Intent.EXTRA_TITLE, "DeadSpace-Three-Slots-" + stamp + ".zip");
        try { startActivityForResult(intent, REQUEST_EXPORT_SAVE_ZIP); }
        catch (ActivityNotFoundException error) { updateSaveImportStatus(L("当前设备没有可用的文件选择器。"), COLOR_WARN); }
    }

    private void exportSaveZip(Uri uri) {
        updateSaveImportStatus(L("正在校验并导出三个槽位……"), COLOR_ACCENT);
        new Thread(() -> {
            try { SaveArchiveImporter.exportZip(this, uri);
                runOnUiThread(() -> updateSaveImportStatus(L("已导出一个 ZIP，包含全部现有槽位和公共设置。可用 ZIP 导入恢复。"), COLOR_ACCENT));
            } catch (Exception error) { runOnUiThread(() -> updateSaveImportStatus(L("导出失败：") + error.getMessage(), COLOR_WARN)); }
        }, "save-archive-export").start();
    }

    private void chooseSaveZip() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Some Android file providers classify ZIPs as octet-stream or expose
        // no MIME type at all. Accept any selectable file and validate the ZIP
        // plus its .sb contents in SaveArchiveImporter.
        intent.setType("*/*");
        try {
            startActivityForResult(intent, REQUEST_IMPORT_SAVE_ZIP);
        } catch (ActivityNotFoundException error) {
            updateSaveImportStatus(L("当前设备没有可用的文件选择器。"), COLOR_WARN);
        }
    }

    private void chooseSaveBackup() {
        new Thread(() -> {
            try {
                File[] files = SaveArchiveImporter.backups(this);
                runOnUiThread(() -> {
                    if (files.length == 0) { updateSaveImportStatus(L("尚无自动备份。导入或编辑存档时会自动生成。"), COLOR_MUTED); return; }
                    String[] labels = new String[files.length];
                    java.text.DateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                    for (int i = 0; i < files.length; i++) labels[i] = format.format(new java.util.Date(files[i].lastModified())) + "\n" + files[i].getName();
                    new AlertDialog.Builder(this).setTitle(L("恢复自动备份 · 三个槽位"))
                        .setItems(labels, (dialog, index) -> new AlertDialog.Builder(this).setTitle(L("恢复三个槽位？"))
                            .setMessage(L("将恢复此备份内的全部槽位和公共设置。当前存档会再次自动备份。\n") + files[index].getName())
                            .setNegativeButton(L("取消"), null).setPositiveButton(L("恢复"), (confirm, which) -> {
                                updateSaveImportStatus(L("正在校验并恢复备份……"), COLOR_ACCENT);
                                new Thread(() -> {
                                    try { SaveArchiveImporter.restoreBackup(this, files[index].getName());
                                        runOnUiThread(() -> updateSaveImportStatus(L("已恢复三个槽位；恢复前的存档已另行备份。"), COLOR_ACCENT));
                                    } catch (Exception error) { runOnUiThread(() -> updateSaveImportStatus(L("恢复失败：") + error.getMessage(), COLOR_WARN)); }
                                }, "save-backup-restore").start();
                            }).show()).setNegativeButton(L("关闭"), null).show();
                });
            } catch (Exception error) { runOnUiThread(() -> updateSaveImportStatus(L("无法读取备份：") + error.getMessage(), COLOR_WARN)); }
        }, "save-backup-list").start();
    }

    private void importSaveZip(Uri uri) {
        updateSaveImportStatus(L("正在校验并导入 ZIP，请勿关闭启动器……"), COLOR_ACCENT);
        new Thread(() -> {
            try {
                SaveArchiveCore.Result result = SaveArchiveImporter.importZip(this, uri);
                String message = L("已导入 ") + result.importedFiles + L(" 个 .sb 存档。");
                message += L("\n依赖文件：") + (result.totalFiles - result.importedFiles) + L(" 个。");
                if (result.backup != null) {
                    message += L("\n导入前备份：") + result.backup.getName();
                }
                String finalMessage = message;
                runOnUiThread(() -> updateSaveImportStatus(finalMessage, COLOR_TEXT));
            } catch (Exception error) {
                String message = L("导入失败：")
                        + (error.getMessage() == null ? error.getClass().getSimpleName()
                        : error.getMessage());
                runOnUiThread(() -> {
                    updateSaveImportStatus(message, COLOR_WARN);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
            }
        }, "save-import").start();
    }

    private void updateSaveImportStatus(String message, int color) {
        if (saveImportStatus == null) {
            return;
        }
        saveImportStatus.setText(message);
        saveImportStatus.setTextColor(color);
    }

    private static String L(String value) { return LauncherLanguage.text(value); }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_BG);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(28), dp(24), dp(28), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        content.addView(text(L("00 关于"), 18, COLOR_ACCENT, true));
        TextView eyebrow = text("DEAD SPACE MOBILE  /  IOS HD PORT  /  1.3.34", 11, COLOR_ACCENT, true);
        eyebrow.setLetterSpacing(0.22f);
        eyebrow.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        content.addView(eyebrow);

        TextView title = text(L("死亡空间移动版（IOS端高清移植）"), 30, COLOR_TEXT, true);
        title.setPadding(0, dp(7), 0, dp(3));
        content.addView(title);

        TextView subtitle = text(
                L("快行动，破坏者！\n让我们把泰坦空间站烧成灰！"),
                14, COLOR_MUTED, false);
        subtitle.setPadding(0, 0, 0, dp(16));
        content.addView(subtitle);

        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(panelBackground(COLOR_PANEL, COLOR_LINE));
        LauncherBackdropView backdrop = new LauncherBackdropView(this);
        hero.addView(backdrop, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout heroRow = new LinearLayout(this);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER_VERTICAL);
        heroRow.setPadding(dp(18), dp(16), dp(18), dp(16));

        ImageView gameIcon = new ImageView(this);
        gameIcon.setImageResource(R.drawable.icon);
        gameIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        gameIcon.setBackground(panelBackground(COLOR_BG, COLOR_LINE));
        gameIcon.setClipToOutline(true);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(112), dp(112));
        iconParams.rightMargin = dp(18);
        heroRow.addView(gameIcon, iconParams);

        LinearLayout heroCopy = new LinearLayout(this);
        heroCopy.setOrientation(LinearLayout.VERTICAL);
        TextView heroKicker = text("ISHIMURA / USG / 02", 10, COLOR_ACCENT, true);
        heroKicker.setLetterSpacing(0.2f);
        heroKicker.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        heroCopy.addView(heroKicker);
        TextView heroTitle = text("DEAD SPACE", 27, COLOR_TEXT, true);
        heroTitle.setLetterSpacing(0.08f);
        heroTitle.setPadding(0, dp(2), 0, 0);
        heroCopy.addView(heroTitle);
        TextView heroName = text(L("死亡空间移动版（IOS端高清移植）"), 17, COLOR_TEXT, true);
        heroCopy.addView(heroName);
        TextView heroBody = text(
                L("原版 1.3.34 内容 · PC 主线修复 · Android 触摸与横屏适配"),
                12, COLOR_MUTED, false);
        heroBody.setPadding(0, dp(5), 0, dp(8));
        heroCopy.addView(heroBody);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(chip(L("原生 3×")));
        chips.addView(chip("FXAA + CAS"));
        chips.addView(chip("PC FIX"));
        heroCopy.addView(chips);
        heroRow.addView(heroCopy, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        LinearLayout credits = new LinearLayout(this);
        credits.setOrientation(LinearLayout.VERTICAL);
        credits.setGravity(Gravity.END);
        credits.setPadding(dp(16), 0, 0, 0);
        TextView creditsLabel = text("CREW / CREDITS", 9, COLOR_ACCENT, true);
        creditsLabel.setLetterSpacing(0.18f);
        creditsLabel.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        creditsLabel.setGravity(Gravity.END);
        credits.addView(creditsLabel);
        TextView projectCredit = text(L("死亡空间移动版（IOS端高清移植）") + " · SomJs", 10, COLOR_TEXT, false);
        projectCredit.setGravity(Gravity.END);
        projectCredit.setPadding(0, dp(5), 0, 0);
        credits.addView(projectCredit);
        heroRow.addView(credits, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        hero.addView(heroRow, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        content.addView(hero, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView creditsPanel = text(
                L("死亡空间移动版（IOS端高清移植）") + "\n" +
                        L("基于 touchHLE 与 HyperHLE\n") +
                        L("本项目链接当前可能暂时处于私有状态\n") +
                        L("SomJs：项目主要开发、手柄游玩测试、骁龙 Android 设备游玩测试\n") +
                        L("lxyun_2（腹黑小喵）：超级校对、超级测试\n") +
                        L("MisakaCharlotte（是的呀啊）：超级校对、超级测试\n") +
                        L("特洛克·洛克莱斯：超级检查测试\n") +
                        L("天雷本雷：键鼠游玩测试、天玑 Android 设备游玩测试\n") +
                        L("豆沙包里有磁：键鼠测试（顶着极低的全转储帧率）\n") +
                        L("问题反馈：https://github.com/SomJsA/DeadSpaceMobile-Community/issues\n") +
                        L("项目 / Project：https://github.com/SomJsA/HyperHLE_ds_compat-android\n") +
                        L("上游 / Upstream：https://github.com/touchHLE/touchHLE · https://github.com/HyperHLE/HyperHLE\n") +
                        L("SomJs 渠道：Bilibili https://space.bilibili.com/34237026 · Reddit https://www.reddit.com/user/SomJs/ · GitHub https://github.com/SomJsA\n") +
                        L("死亡空间贴吧汉化组：https://tieba.baidu.com/wxf/341915?kw=%E6%AD%BB%E4%BA%A1%E7%A9%BA%E9%97%B4&fr=sharewise"),
                11, COLOR_TEXT, false);
        creditsPanel.setAutoLinkMask(Linkify.WEB_URLS);
        creditsPanel.setLinksClickable(true);
        creditsPanel.setMovementMethod(LinkMovementMethod.getInstance());
        creditsPanel.setLinkTextColor(COLOR_ACCENT);
        creditsPanel.setBackground(panelBackground(COLOR_PANEL, COLOR_LINE));
        creditsPanel.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams creditsPanelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        creditsPanelParams.topMargin = dp(12);
        creditsPanelParams.bottomMargin = dp(10);
        content.addView(creditsPanel, creditsPanelParams);
        GithubUpdates.add(this, content);

        LinearLayout qualityBlock = new LinearLayout(this);
        qualityBlock.setOrientation(LinearLayout.VERTICAL);
        content.addView(qualityBlock, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        addSection(qualityBlock,L("01  /  主要画面设置"));
        CheckBox overlayToggle = addToggle(qualityBlock, L("高清中文文字组件（章节 / 任务 / 字幕）"), L("章节、任务追踪与字幕的总开关；样式在外部预览器调整。"),
                prefs.getBoolean("overlay_enabled", true));
        overlayToggle.setOnCheckedChangeListener((button, checked) -> {prefs.edit().putBoolean("overlay_enabled", checked).apply();saveCurrentSettings();});

        View displayAnchor=new View(this);qualityBlock.addView(displayAnchor,new LinearLayout.LayoutParams(1,1));
        scaleButton = addCycleButton(qualityBlock, "", v -> {
            scaleHack = scaleHack >= 3 ? 1 : scaleHack + 1;
            updateChoiceLabels();onQualitySettingChanged();
        });
        primaryResolution=text(LaunchReview.resolution(this,scaleHack,stretchViewport),13,COLOR_TEXT,false);
        qualityBlock.addView(primaryResolution);
        CheckBox lightingToggle=addToggle(qualityBlock,L("高级光影"),
                L("恢复护甲与环境反光、动态灯光等原生高档效果；普通版默认关闭，HD版默认开启。"),LaunchReview.lighting(prefs));
        lightingToggle.setOnCheckedChangeListener((button,checked)-> {
            prefs.edit().putBoolean(LaunchReview.LIGHTING,checked).apply();saveCurrentSettings();
        });
        TextView languageNote=text(L("高清文字目前为中文。可关闭高清中文组件，使用游戏原生文字与语言设置。欢迎多语言用户投稿；若暂无投稿，我们计划在R8之前支持其他语言的高清文字组件。"),12,COLOR_MUTED,false);
        languageNote.setPadding(0,dp(8),0,dp(16));qualityBlock.addView(languageNote);
        View presetAnchor=new View(this);qualityBlock.addView(presetAnchor,new LinearLayout.LayoutParams(1,1));
        addSection(qualityBlock, L("02  /  画质模式"));
        TextView presetHint = text(
                L("推荐：原生 3× + FXAA + CAS 0.5，FSR3 关闭。")
                        + L("先选常用预设；需要逐项调整时再进入自定义。"),
                13, COLOR_MUTED, false);
        presetHint.setPadding(0, dp(4), 0, dp(8));
        qualityBlock.addView(presetHint);
        addPresetRow(qualityBlock, 0, 2);
        addPresetRow(qualityBlock, 2, 2);
        addPresetRow(qualityBlock, 4, 1);

        presetSummary = text("", 14, COLOR_TEXT, false);
        presetSummary.setLineSpacing(0f, 1.08f);
        presetSummary.setPadding(dp(16), dp(12), dp(16), dp(12));
        presetSummary.setBackground(
                panelBackground(COLOR_PANEL_ALT, COLOR_LINE));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = dp(2);
        summaryParams.bottomMargin = dp(8);
        qualityBlock.addView(presetSummary, summaryParams);

        shadowBoostButton = addCycleButton(qualityBlock, "", v -> {
            shadowBoostPercent = shadowBoostPercent == 0 ? 30
                    : (shadowBoostPercent <= 30 ? 60 : (shadowBoostPercent <= 60 ? 100 : 0));
            updateChoiceLabels();
            prefs.edit().putInt(KEY_SHADOW_BOOST, shadowBoostPercent).apply();
        });
        TextView shadowHint = text(L("已弃用，默认 0%；会放大暗部色阶和色偏，无法恢复纯黑细节。仅保留作对照。"), 12, COLOR_MUTED, false);
        shadowHint.setPadding(dp(8), 0, dp(8), dp(8));
        qualityBlock.addView(shadowHint);

        addSection(qualityBlock, L("护眼模式（实验性）"));
        safeBlackFloorToggle = addToggle(qualityBlock,
                L("将接近黑色替换为灰色"),
                L("原游戏存在大量整屏明暗跳变；不调节时可能难以忍受，灰度过高会把黑场抬成发白。")
                        + L("默认关闭，不改变深度、排序或输入。"),
                safeBlackFloor);
        safeBlackFloorToggle.setOnCheckedChangeListener((button, checked) -> {
            safeBlackFloor = checked;
            updateSafeBlackGrayUi();
            onQualitySettingChanged();
        });
        LinearLayout grayRow = new LinearLayout(this);
        grayRow.setOrientation(LinearLayout.HORIZONTAL);
        grayRow.setGravity(Gravity.CENTER_VERTICAL);
        grayRow.setPadding(dp(8), dp(2), dp(8), dp(8));
        TextView grayLabel = text(L("黑色替换灰度（推荐 / 默认 75 / 255）"), 13, COLOR_TEXT, true);
        grayRow.addView(grayLabel, new LinearLayout.LayoutParams(0, dp(44), 1.0f));
        safeBlackGrayValue = text(safeBlackGray + " / 255", 13, COLOR_ACCENT, true);
        safeBlackGrayValue.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        grayRow.addView(safeBlackGrayValue, new LinearLayout.LayoutParams(dp(90), dp(44)));
        qualityBlock.addView(grayRow);
        safeBlackGraySeek = new SeekBar(this);
        safeBlackGraySeek.setMax(160);
        safeBlackGraySeek.setProgress(safeBlackGray);
        safeBlackGraySeek.setContentDescription(L("黑色替换灰度"));
        safeBlackGraySeek.setPadding(dp(8), 0, dp(8), dp(8));
        qualityBlock.addView(safeBlackGraySeek, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
        TextView grayHint = text(L("0 = 黑色；160 = 较亮灰色。建议从 75 附近逐步调整。只在最终 GPU 显示层处理，不回读整帧。"), 12, COLOR_MUTED, false);
        grayHint.setPadding(dp(8), 0, dp(8), dp(8));
        qualityBlock.addView(grayHint);
        safeBlackGraySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                safeBlackGray = Math.max(0, Math.min(160, progress));
                if (safeBlackGrayValue != null) safeBlackGrayValue.setText(safeBlackGray + " / 255");
            }
            public void onStartTrackingTouch(SeekBar seekBar) { }
            public void onStopTrackingTouch(SeekBar seekBar) { onQualitySettingChanged(); }
        });
        updateSafeBlackGrayUi();

        addSection(qualityBlock, L("Android 输入"));
        tiltPulseToggle = addToggle(qualityBlock,
                L("侧向切换模式冷却（Android）"),
                L("开启：一次较强侧向偏转只触发一次主/副模式切换，设备回正后才可再次触发；")
                        + L("关闭：回到原生连续加速度输入语义。与护眼模式、画质预设和横屏确认独立。"),
                tiltPulseEnabled);
        tiltPulseToggle.setOnCheckedChangeListener((button, checked) -> {
            tiltPulseEnabled = checked;
            saveCurrentSettings();
        });
        LinearLayout tiltThresholdRow = new LinearLayout(this);
        tiltThresholdRow.setOrientation(LinearLayout.HORIZONTAL);
        tiltThresholdRow.setGravity(Gravity.CENTER_VERTICAL);
        tiltThresholdRow.setPadding(dp(8), dp(2), dp(8), 0);
        tiltThresholdRow.addView(text(L("侧向强度阈值"), 13, COLOR_TEXT, true), new LinearLayout.LayoutParams(0, dp(40), 1.0f));
        tiltThresholdValue = text("", 13, COLOR_ACCENT, true);
        tiltThresholdValue.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        tiltThresholdRow.addView(tiltThresholdValue, new LinearLayout.LayoutParams(dp(105), dp(40)));
        qualityBlock.addView(tiltThresholdRow);
        tiltThresholdSeek = new SeekBar(this);
        tiltThresholdSeek.setMax(720);
        tiltThresholdSeek.setProgress(tiltPulseThresholdMilliG - 80);
        tiltThresholdSeek.setContentDescription(L("侧向切换强度阈值"));
        tiltThresholdSeek.setPadding(dp(8), 0, dp(8), 0);
        qualityBlock.addView(tiltThresholdSeek, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
        LinearLayout tiltCooldownRow = new LinearLayout(this);
        tiltCooldownRow.setOrientation(LinearLayout.HORIZONTAL);
        tiltCooldownRow.setGravity(Gravity.CENTER_VERTICAL);
        tiltCooldownRow.setPadding(dp(8), dp(2), dp(8), 0);
        tiltCooldownRow.addView(text(L("侧向切换冷却时间"), 13, COLOR_TEXT, true), new LinearLayout.LayoutParams(0, dp(40), 1.0f));
        tiltCooldownValue = text("", 13, COLOR_ACCENT, true);
        tiltCooldownValue.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        tiltCooldownRow.addView(tiltCooldownValue, new LinearLayout.LayoutParams(dp(105), dp(40)));
        qualityBlock.addView(tiltCooldownRow);
        tiltCooldownSeek = new SeekBar(this);
        tiltCooldownSeek.setMax(900);
        tiltCooldownSeek.setProgress(tiltPulseCooldownMs - 100);
        tiltCooldownSeek.setContentDescription(L("侧向切换冷却时间"));
        tiltCooldownSeek.setPadding(dp(8), 0, dp(8), 0);
        qualityBlock.addView(tiltCooldownSeek, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
        TextView tiltHint = text(L("强度越高越不易误触；冷却越长，连续侧向偏转越不容易重复切换。默认 0.128G / 380ms。"), 12, COLOR_MUTED, false);
        tiltHint.setPadding(dp(8), 0, dp(8), dp(8));
        qualityBlock.addView(tiltHint);
        updateTiltPulseUi();
        tiltThresholdSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tiltPulseThresholdMilliG = TiltGestureSettings.clamp(progress + 80);
                updateTiltPulseUi();
            }
            public void onStartTrackingTouch(SeekBar seekBar) { }
            public void onStopTrackingTouch(SeekBar seekBar) { saveCurrentSettings(); }
        });
        tiltCooldownSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tiltPulseCooldownMs = Math.max(100, Math.min(1000, progress + 100));
                updateTiltPulseUi();
            }
            public void onStartTrackingTouch(SeekBar seekBar) { }
            public void onStopTrackingTouch(SeekBar seekBar) { saveCurrentSettings(); }
        });

        advancedButton = button(L("高级参数：展开（自定义）   ›"), false);
        advancedButton.setOnClickListener(v -> {
            advancedExpanded = !advancedExpanded;
            updateAdvancedVisibility();
        });
        LinearLayout.LayoutParams advancedButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        advancedButtonParams.bottomMargin = dp(6);
        qualityBlock.addView(advancedButton, advancedButtonParams);

        advancedContainer = new LinearLayout(this);
        advancedContainer.setOrientation(LinearLayout.VERTICAL);
        advancedExpanded = PRESET_CUSTOM.equals(qualityPreset);
        advancedContainer.setVisibility(View.GONE);
        qualityBlock.addView(advancedContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        addSection(advancedContainer, "FSR3 / Vulkan");
        TextView fsr3Status = text(Fsr3Capabilities.summary(this), 13, COLOR_MUTED, false);
        fsr3Status.setPadding(0, dp(4), 0, dp(8));
        advancedContainer.addView(fsr3Status);
        addInfoCard(advancedContainer,
                L("当前是什么状态"),
                L("这是带真实 D16 深度、异步一帧流水线的 FSR3.1 实验路径，仍不是完整时间超分。\n")
                        + L("已接通：RGBA8 颜色、游戏深度、AHB/EGLImage、Vulkan 外部内存和超分输出。\n")
                        + L("仍缺失：真实运动向量、相机 jitter、有效历史帧和 HUD/UI 分层。\n")
                        + L("异步模式会固定增加约 1 帧显示延迟；它不是帧生成，也不能对外宣称完整 FSR3。"),
                COLOR_WARN);
        addInfoCard(advancedContainer,
                L("Adreno 740 实机基线与优化结果"),
                L("固定第 8 章、2376x1080 Surface、录像后段中位数：\n")
                        + L("原生 960x1440：60.2 FPS / 16.56 ms / 1% low 41.8。\n")
                        + L("同步 FSR 640x960：21.1 FPS / 49.26 ms / 1% low 5.7。\n")
                        + L("异步 FSR 640x960：50.5 FPS / 19.84 ms / 1% low 38.1。\n")
                        + L("异步 FSR 960x1440：44.8 FPS / 22.17 ms / 1% low 35.4。\n")
                        + L("PSS 约 2.8 GB；直写 2× 曾异常到 4.37 GB。")
                        + L("仅 Adreno 740 有这组证据，未知 GPU 必须回退原生。"),
                COLOR_WARN);
        addInfoCard(advancedContainer,
                L("兼容门槛"),
                L("要求 Android 12+、arm64-v8a、Vulkan 1.2+ Compute、AHB 外部内存、")
                        + L("EGL 外部图像和 EGL_KHR_gl_renderbuffer_image。\n")
                        + L("当前只有 Adreno 740 有真实深度和 gameplay 证据；未知 GPU 应回退原生。\n")
                        + L("游戏固定横屏，用户旋转不属于验收需求。"),
                COLOR_ACCENT);
        fsr3ForceToggle = addToggle(advancedContainer,
                L("强制启用 FSR3（高风险）"),
                L("即使 Vulkan 1.2 / AHB / EGLImage 预检失败也把开关传给原生层。")
                        + L("用于收集驱动实测证据，不保证可运行；出现黑屏或闪退请关闭。"),
                fsr3ForceEnabled);
        fsr3AhbToggle = addToggle(advancedContainer,
                L("FSR3.1 Vulkan 超分（实验）"),
                L("总开关。把游戏 GLES 颜色通过共享 AHB 送入 FSR3.1 Vulkan。")
                        + L("关闭后立即回到原生呈现；只有同时开启下面的真实深度，")
                        + L("FSR 才会收到 depth=true。"),
                fsr3AhbEnabled);
        fsr3FgToggle = addToggle(advancedContainer,
                L("FSR3 帧生成（暂不可用）"),
                L("没有真实 per-pixel motion、低延迟呈现和 UI 分层。")
                        + L("普通开关会安全拒绝，不能用它冒充帧数提升。"),
                false);
        fsr3DepthToggle = addToggle(advancedContainer,
                L("真实深度输入 D16（实验）"),
                L("通过 EGLImage 读取游戏深度并上传给 FSR3。")
                        + L("该路径每帧有 CPU 读回开销，但缺少它时 depth=false，")
                        + L("只能验证呈现链路，不能算完整超分输入。"),
                fsr3DepthEnabled);
        fsr3GpuDepthToggle = addToggle(advancedContainer,
                L("GPU 深度直通（实验）"),
                L("通过 EGLImage 和 glBlitFramebuffer 将 D16 深度写入 AHardwareBuffer，")
                        + L("避免每帧 CPU glReadPixels。仅 Adreno 740 已验证；失败时回退 CPU。"),
                fsr3GpuDepthEnabled);
        fsr3AsyncToggle = addToggle(advancedContainer,
                L("异步 FSR 流水线（实验）"),
                L("在 CPU 侧不等当前 FSR GPU 完成：提交当前帧后显示上一帧已完成的 FSR 输出，")
                        + L("让当前游戏渲染与上一帧超分重叠。代价是固定增加约 1 帧显示延迟；")
                        + L("首帧和异常时自动回退原生。默认关闭，帧生成/诊断读回开启时也不使用。"),
                fsr3AsyncEnabled);
        fsr3DirectOutputToggle = addToggle(advancedContainer,
                L("FSR 直接写输出 AHB（实验）"),
                L("让 FSR3 直接写入用于 GLES 呈现的 AHardwareBuffer，")
                        + L("省掉 1620×1080 中间纹理到输出 AHB 的全画面复制。")
                        + L("依赖异步流水线；驱动不支持外部存储图像时自动回退。默认关闭。"),
                fsr3DirectOutputEnabled);
        fsr3SelectionStatus = text("", 13, COLOR_WARN, false);
        fsr3SelectionStatus.setLineSpacing(0f, 1.08f);
        fsr3SelectionStatus.setPadding(dp(16), dp(12), dp(16), dp(12));
        fsr3SelectionStatus.setBackground(
                panelBackground(COLOR_PANEL_ALT, COLOR_LINE));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.bottomMargin = dp(8);
        advancedContainer.addView(fsr3SelectionStatus, statusParams);
        updateFsr3ControlEnablement();
        fsr3FgToggle.setEnabled(false);
        fsr3ForceToggle.setOnCheckedChangeListener((button, checked) -> {
            fsr3ForceEnabled = checked;
            if (checked && !fsr3Supported) {
                Toast.makeText(this,
                        L("已允许 FSR3 强开：此路径仅用于实验，可能黑屏或闪退。"),
                        Toast.LENGTH_LONG).show();
            }
            if (!checked && !fsr3Supported) {
                fsr3AhbEnabled = false;
                setChecked(fsr3AhbToggle, false);
            }
            onQualitySettingChanged();
            updateFsr3SelectionStatus();
        });
        fsr3AhbToggle.setOnCheckedChangeListener((button, checked) -> {
            fsr3AhbEnabled = checked;
            onQualitySettingChanged();
            updateFsr3SelectionStatus();
        });
        fsr3DepthToggle.setOnCheckedChangeListener((button, checked) -> {
            fsr3DepthEnabled = checked;
            onQualitySettingChanged();
            updateFsr3SelectionStatus();
        });
        fsr3GpuDepthToggle.setOnCheckedChangeListener((button, checked) -> {
            fsr3GpuDepthEnabled = checked;
            onQualitySettingChanged();
            updateFsr3SelectionStatus();
        });
        fsr3AsyncToggle.setOnCheckedChangeListener((button, checked) -> {
            fsr3AsyncEnabled = checked;
            onQualitySettingChanged();
            updateFsr3SelectionStatus();
        });
        fsr3DirectOutputToggle.setOnCheckedChangeListener((button, checked) -> {
            fsr3DirectOutputEnabled = checked;
            onQualitySettingChanged();
            updateFsr3SelectionStatus();
        });
        updateFsr3SelectionStatus();

        addSection(advancedContainer, L("性能"));
        fpsToggle = addToggle(advancedContainer,
                L("显示实际 FPS"),
                L("基于 presentRenderbuffer 的真实帧计数，显示 FPS、平均帧时间和累计帧号。"),
                showFps);
        fpsToggle.setOnCheckedChangeListener((button, checked) -> {
            showFps = checked;
            saveCurrentSettings();
        });
        capButton = addCycleButton(advancedContainer, "", v -> {
            fpsCap = fpsCap == 0 ? 70 : (fpsCap == 70 ? 60 : (fpsCap == 60 ? 30 : 0));
            updateChoiceLabels();
            onQualitySettingChanged();
        });
        stretchButton = addCycleButton(advancedContainer, "", v -> {
            stretchViewport = !stretchViewport;
            updateChoiceLabels();
            onQualitySettingChanged();
        });
        fastPresentToggle = addToggle(advancedContainer,
                L("快速呈现"),
                L("绕过 glReadPixels 合成路径；当前 OnePlus 设备已验证稳定，若其他设备闪烁请关闭。"),
                fastPresent);
        fastPresentToggle.setOnCheckedChangeListener((button, checked) -> {
            fastPresent = checked;
            onQualitySettingChanged();
        });
        skipReadbackToggle = addToggle(advancedContainer,
                L("跳过读回 Finish"),
                L("省掉每帧 glFinish；当前设备已验证画面正常，若出现黑帧请关闭。"),
                skipReadback);
        skipReadbackToggle.setOnCheckedChangeListener((button, checked) -> {
            skipReadback = checked;
            onQualitySettingChanged();
        });
        gpuCopyToggle = addToggle(advancedContainer,
                L("GPU 复制呈现（实验）"),
                L("用 GPU 复制 renderbuffer 代替每帧 CPU 读回；失败时关闭并回退。"),
                gpuPresentCopy);
        gpuCopyToggle.setOnCheckedChangeListener((button, checked) -> {
            gpuPresentCopy = checked;
            onQualitySettingChanged();
        });
        if (BuildConfig.BUNDLED_TEXTURES) {
            textureToggle = addToggle(advancedContainer,
                    L("内置高清纹理（4×）"),
                    L("当前安装包包含 4× 纹理；会显著增加显存与加载压力。默认开启，低端设备建议关闭并降到性能优先。"),
                    bundledTextures);
            textureToggle.setOnCheckedChangeListener((button, checked) -> {
                bundledTextures = checked;
                saveCurrentSettings();
            });
        }

        addSection(advancedContainer, L("FSR / 抗锯齿"));
        fsrButton = addCycleButton(advancedContainer, "", v -> {
            fsrMode = fsrMode >= 2 ? 0 : fsrMode + 1;
            updateChoiceLabels();
            onQualitySettingChanged();
        });
        aaButton = addCycleButton(advancedContainer, "", v -> {
            aaEnabled = !aaEnabled;
            updateChoiceLabels();
            onQualitySettingChanged();
        });
        sharpenButton = addCycleButton(advancedContainer, "", v -> {
            sharpenTenths = sharpenTenths == 0 ? 5 : (sharpenTenths == 5 ? 10 : 0);
            updateChoiceLabels();
            onQualitySettingChanged();
        });
        TextView qualityHint = text(
                L("FSR、FXAA 与 CAS 会提升清晰度，但会增加 GPU 负载；速度优先时请保持关闭。"),
                13, COLOR_MUTED, false);
        qualityHint.setPadding(0, dp(4), 0, dp(8));
        advancedContainer.addView(qualityHint);

        addSection(advancedContainer, L("诊断"));
        logsToggle = addToggle(advancedContainer,
                L("详细调试日志"),
                L("开启投影、雾、draw、纹理和 FPS 日志；会增加 CPU 与 logcat 负载。"),
                debugLogs);
        logsToggle.setOnCheckedChangeListener((button, checked) -> {
            debugLogs = checked;
            saveCurrentSettings();
        });
        dumpsToggle = addToggle(advancedContainer,
                L("帧转储"),
                L("每 600 帧写入一张 PPM，文件较大且会明显影响帧率。默认关闭。"),
                dumpFrames);
        dumpsToggle.setOnCheckedChangeListener((button, checked) -> {
            dumpFrames = checked;
            saveCurrentSettings();
        });

        addSection(advancedContainer, L("兼容"));
        compatToggle = addToggle(advancedContainer,
                L("PC 渲染兼容修复"),
                L("投影 z、深度 LEQUAL、no-fog、ScreenScale=2 和字体缩放。建议保持开启。"),
                pcCompat);
        compatToggle.setOnCheckedChangeListener((button, checked) -> {
            pcCompat = checked;
            onQualitySettingChanged();
        });

        TextView pathHint = text(
                L("诊断文件目录：") + new File(RuntimeStorage.dataBase(this), "diagnostics").getAbsolutePath(),
                13, COLOR_MUTED, false);
        pathHint.setPadding(0, dp(14), 0, dp(16));
        advancedContainer.addView(pathHint);

        LinearLayout saveBlock = new LinearLayout(this);
        saveBlock.setOrientation(LinearLayout.VERTICAL);
        content.addView(saveBlock, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        addSection(saveBlock, L("03  /  存档与文件"));
        TextView storage=text(L("资料位置：")+RuntimeStorage.dataBase(this).getAbsolutePath()+L("\n布局 v1；配置与存档分区保存。somjs与旧包独立，旧进度可导出三槽ZIP后导入。"),13,COLOR_MUTED,false);
        saveBlock.addView(storage);
        Button storageChoice=button(L("资料位置 · 文档目录 / 旧版位置"),false);
        storageChoice.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(L("选择资料位置"))
            .setMessage(L("新安装默认使用应用专属Documents，可从系统文件选择器的本应用入口访问。卸载前请导出存档。切换只选择该位置，不会搬移或合并进度。"))
            .setPositiveButton(L("文档目录"),(d,w)->selectStorage(RuntimeStorage.DOCUMENTS))
            .setNeutralButton(L("旧版位置"),(d,w)->selectStorage(RuntimeStorage.LEGACY)).setNegativeButton(L("取消"),null).show());
        saveBlock.addView(storageChoice,new LinearLayout.LayoutParams(-1,dp(48)));
        addInfoCard(
                saveBlock,
                L("三个槽位，一个 ZIP"),
                L("导出包含全部现有档案和公共设置；导入可整组恢复。也兼容 Documents/var 和直接包含 .sb 的存档包。")
                        + L("导入前会先把当前 Documents 备份到 save-backups；")
                        + L("请先关闭正在运行的游戏。"),
                COLOR_ACCENT);
        Button exportSave = button(L("导出三槽位 ZIP"), false);
        exportSave.setOnClickListener(v -> chooseExportZip());
        saveBlock.addView(exportSave, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
        Button importSave = button(L("选择 ZIP 存档并导入"), false);
        importSave.setOnClickListener(v -> chooseSaveZip());
        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        importParams.bottomMargin = dp(8);
        saveBlock.addView(importSave, importParams);
        saveImportStatus = text(
                L("尚未导入。支持 SBIN、版本文件和已知存档依赖，ZIP 解压上限 256 MB。"),
                12, COLOR_MUTED, false);
        saveImportStatus.setPadding(dp(14), dp(12), dp(14), dp(12));
        saveImportStatus.setBackground(panelBackground(COLOR_PANEL, COLOR_LINE));
        LinearLayout.LayoutParams saveStatusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        saveStatusParams.bottomMargin = dp(8);
        saveBlock.addView(saveImportStatus, saveStatusParams);
        Button editor = button(L("存档编辑与内购编辑 · 上下合并"), false);
        editor.setOnClickListener(v -> SaveEditorDialog.show(this));
        saveBlock.addView(editor, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
        Button restoreBackup = button(L("恢复自动备份"), false);
        restoreBackup.setOnClickListener(v -> chooseSaveBackup());
        saveBlock.addView(restoreBackup, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
        Button saveAdvanced=button(L("高级 · 自动采集存档节点  ▾"),false);
        saveBlock.addView(saveAdvanced,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(50)));
        LinearLayout nodeOptions=new LinearLayout(this);nodeOptions.setOrientation(LinearLayout.VERTICAL);
        saveBlock.addView(nodeOptions);addSaveNodeControls(nodeOptions);nodeOptions.setVisibility(View.GONE);
        saveAdvanced.setOnClickListener(v->{boolean open=nodeOptions.getVisibility()!=View.VISIBLE;
            nodeOptions.setVisibility(open?View.VISIBLE:View.GONE);saveAdvanced.setText(open?L("高级 · 自动采集存档节点  ▴"):L("高级 · 自动采集存档节点  ▾"));});

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        Button reset = button(L("恢复默认"), false);
        reset.setOnClickListener(v -> resetDefaults());
        actions.addView(reset, new LinearLayout.LayoutParams(0, dp(54), 1.0f));

        View gap = new View(this);
        actions.addView(gap, new LinearLayout.LayoutParams(dp(12), dp(1)));

        Button start = button(L("启动游戏"), true);
        start.setOnClickListener(v -> startGame());
        actions.addView(start, new LinearLayout.LayoutParams(0, dp(54), 1.7f));
        updateChoiceLabels();
        updatePresetUi();
        updateAdvancedVisibility();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.HORIZONTAL);
        page.setBackgroundColor(COLOR_BG);
        content.removeView(qualityBlock);content.addView(qualityBlock,0);
        View rail = buildNavigationRail(scroll, hero, overlayToggle, displayAnchor, presetAnchor, advancedContainer, saveBlock);
        page.addView(rail, new LinearLayout.LayoutParams(dp(190),
                LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(COLOR_BG);
        LinearLayout languageRow = new LinearLayout(this);
        languageRow.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        languageRow.setPadding(dp(28), dp(4), dp(28), 0);
        Button language = button(LauncherLanguage.english() ? "中文" : "English", false);
        language.setContentDescription("中文 / English");
        language.setOnClickListener(view -> {
            saveCurrentSettings();
            LauncherLanguage.toggle(this);
            setContentView(buildUi());
        });
        languageRow.addView(language, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)));
        main.addView(languageRow);
        main.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));
        actions.setPadding(dp(28), dp(8), dp(28), dp(20));
        main.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        page.addView(main, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
        return page;
    }

    private View buildNavigationRail(
            ScrollView scroll, View overview, View quality, View display, View presets, View advanced, View saves) {
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setPadding(dp(18), dp(24), dp(18), dp(18));
        rail.setBackground(panelBackground(COLOR_BG, COLOR_LINE));

        TextView brand = text("DEAD SPACE", 17, COLOR_TEXT, true);
        brand.setLetterSpacing(0.24f);
        brand.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        rail.addView(brand);
        TextView brandSub = text("IOS HD PORT  /  1.3.34", 10, COLOR_ACCENT, true);
        brandSub.setLetterSpacing(0.18f);
        brandSub.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        brandSub.setPadding(0, dp(2), 0, dp(5));
        rail.addView(brandSub);
        TextView brandName = text(L("死亡空间移动版"), 13, COLOR_MUTED, false);
        brandName.setPadding(0, 0, 0, dp(22));
        rail.addView(brandName);

        rail.addView(railButton(L("00   关于"), () -> scrollToSection(scroll, overview)));
        rail.addView(railButton(L("01   高清文字"), () -> scrollToSection(scroll, quality)));
        rail.addView(railButton(L("02   显示与光影"), () -> scrollToSection(scroll, display)));
        rail.addView(railButton(L("03   画质预设"), () -> scrollToSection(scroll, presets)));
        rail.addView(railButton(L("04   操作与参数"), () -> {
            advancedExpanded = true;
            updateAdvancedVisibility();
            scrollToSection(scroll, advanced);
        }));
        rail.addView(railButton(L("05   存档与文件"), () -> scrollToSection(scroll, saves)));

        View spacer = new View(this);
        rail.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1.0f));
        TextView footer = text(
                L(BuildConfig.BUNDLED_TEXTURES ? "HD版 · 内置4×纹理" : "普通版 · 原版纹理") + "\n\n"
                        + BuildConfig.VERSION_NAME,
                10, COLOR_MUTED, false);
        footer.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        footer.setLetterSpacing(0.08f);
        footer.setPadding(0, dp(12), 0, 0);
        rail.addView(footer);
        return rail;
    }

    private Button railButton(String label, Runnable action) {
        Button button = button(label, false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        button.setTextSize(12);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        params.bottomMargin = dp(6);
        button.setLayoutParams(params);
        return button;
    }

    private void scrollToSection(ScrollView scroll, View target) {
        if (target == null) {
            return;
        }
        target.post(() -> {
            int[] targetLocation = new int[2];
            int[] scrollLocation = new int[2];
            target.getLocationInWindow(targetLocation);
            scroll.getLocationInWindow(scrollLocation);
            int y = scroll.getScrollY() + targetLocation[1] - scrollLocation[1] - dp(10);
            scroll.smoothScrollTo(0, Math.max(0, y));
        });
    }

    private TextView chip(String label) {
        TextView chip = text(label, 10, COLOR_ACCENT, true);
        chip.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        chip.setLetterSpacing(0.08f);
        chip.setPadding(dp(8), dp(4), dp(8), dp(4));
        chip.setBackground(panelBackground(COLOR_PANEL_ALT, COLOR_LINE));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dp(6);
        chip.setLayoutParams(params);
        return chip;
    }

    private void updateChoiceLabels() {
        if (primaryResolution!=null) primaryResolution.setText(LaunchReview.resolution(this,scaleHack,stretchViewport));
        String scaleText;
        if (scaleHack <= 1) {
            scaleText = L("内部渲染：1× 性能");
        } else if (scaleHack == 2) {
            scaleText = L("内部渲染：2× 平衡");
        } else {
            scaleText = L("内部渲染：3× 清晰");
        }
        scaleButton.setText(scaleText + "   ›");

        String capText;
        if (fpsCap == 30) {
            capText = L("帧率上限：30 FPS");
        } else if (fpsCap == 70) {
            capText = L("帧率上限：70 FPS");
        } else if (fpsCap == 60) {
            capText = L("帧率上限：60 FPS");
        } else {
            capText = L("帧率上限：不限制（推荐）");
        }
        capButton.setText(capText + "   ›");
        stretchButton.setText((stretchViewport
                ? L("画面比例：拉伸铺满（会变形）")
                : L("画面比例：保持 3:2")) + "   ›");

        String fsrText;
        if (fsrMode == 1) {
            fsrText = "FSR：EASU + RCAS";
        } else if (fsrMode == 2) {
            fsrText = L("FSR：EASU 快速");
        } else {
            fsrText = L("FSR：关闭");
        }
        fsrButton.setText(fsrText + "   ›");

        aaButton.setText((aaEnabled ? L("抗锯齿：FXAA") : L("抗锯齿：关闭")) + "   ›");

        String sharpenText;
        if (sharpenTenths >= 10) {
            sharpenText = L("CAS 锐化：1.0");
        } else if (sharpenTenths >= 5) {
            sharpenText = L("CAS 锐化：0.5");
        } else {
            sharpenText = L("CAS 锐化：关闭");
        }
        sharpenButton.setText(sharpenText + "   ›");
        String shadowText = shadowBoostPercent == 0 ? L("关闭 0%（原始）")
                : (shadowBoostPercent <= 30 ? "30%"
                : (shadowBoostPercent <= 60 ? "60%" : "100%"));
        shadowBoostButton.setText(L("暗部增强（已弃用）：") + shadowText + "   ›");
    }

    private void addPresetRow(LinearLayout root, int start, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (int index = start; index < start + count; index++) {
            final String preset = PRESET_KEYS[index];
            Button presetButton = button(L(PRESET_LABELS[index]), false);
            presetButton.setOnClickListener(v -> applyPreset(preset, true));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, dp(54), 1.0f);
            if (index > start) {
                params.leftMargin = dp(8);
            }
            row.addView(presetButton, params);
            presetButtons[index] = presetButton;
        }
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(8);
        root.addView(row, rowParams);
    }

    private void applyPreset(String preset, boolean userInitiated) {
        if (PRESET_FSR3.equals(preset) && !fsr3Usable()) {
            preset = PRESET_COMPATIBILITY;
        }
        applyingPreset = true;
        applyPresetValues(preset);
        syncPresetControls();
        applyingPreset = false;
        qualityPreset = preset;
        updateChoiceLabels();
        updatePresetUi();
        updateFsr3SelectionStatus();
        saveCurrentSettings();
        if (userInitiated) {
            advancedExpanded = PRESET_CUSTOM.equals(preset);
            updateAdvancedVisibility();
        }
    }

    private void applyPresetValues(String preset) {
        if (PRESET_RECOMMENDED.equals(preset)) {
            scaleHack = 3;
            fpsCap = 0;
            stretchViewport = false;
            fsrMode = 0;
            aaEnabled = true;
            sharpenTenths = 5;
            fastPresent = true;
            skipReadback = true;
            gpuPresentCopy = true;
            pcCompat = true;
            clearFsr3PresetValues();
        } else if (PRESET_PERFORMANCE.equals(preset)) {
            scaleHack = 1;
            fpsCap = 0;
            stretchViewport = false;
            fsrMode = 0;
            aaEnabled = false;
            sharpenTenths = 0;
            fastPresent = true;
            skipReadback = true;
            gpuPresentCopy = true;
            pcCompat = true;
            clearFsr3PresetValues();
        } else if (PRESET_COMPATIBILITY.equals(preset)) {
            scaleHack = 1;
            fpsCap = 0;
            stretchViewport = false;
            fsrMode = 0;
            aaEnabled = false;
            sharpenTenths = 0;
            fastPresent = false;
            skipReadback = false;
            gpuPresentCopy = false;
            pcCompat = true;
            clearFsr3PresetValues();
        } else if (PRESET_FSR3.equals(preset)) {
            scaleHack = 2;
            fpsCap = 0;
            stretchViewport = false;
            fsrMode = 0;
            aaEnabled = false;
            sharpenTenths = 0;
            fastPresent = true;
            skipReadback = true;
            gpuPresentCopy = true;
            pcCompat = true;
            fsr3AhbEnabled = true;
            fsr3FgEnabled = false;
            fsr3DepthEnabled = true;
            fsr3GpuDepthEnabled = true;
            fsr3AsyncEnabled = true;
            fsr3DirectOutputEnabled = false;
        }
    }

    private void clearFsr3PresetValues() {
        fsr3AhbEnabled = false;
        fsr3FgEnabled = false;
        fsr3DepthEnabled = false;
        fsr3GpuDepthEnabled = false;
        fsr3AsyncEnabled = false;
        fsr3DirectOutputEnabled = false;
    }

    private void syncPresetControls() {
        setChecked(fastPresentToggle, fastPresent);
        setChecked(skipReadbackToggle, skipReadback);
        setChecked(gpuCopyToggle, gpuPresentCopy);
        setChecked(compatToggle, pcCompat);
        setChecked(fsr3AhbToggle, fsr3AhbEnabled);
        setChecked(fsr3FgToggle, fsr3FgEnabled);
        setChecked(fsr3DepthToggle, fsr3DepthEnabled);
        setChecked(fsr3GpuDepthToggle, fsr3GpuDepthEnabled);
        setChecked(fsr3AsyncToggle, fsr3AsyncEnabled);
        setChecked(fsr3DirectOutputToggle, fsr3DirectOutputEnabled);
    }

    private void setChecked(CheckBox box, boolean checked) {
        if (box != null && box.isChecked() != checked) {
            box.setChecked(checked);
        }
    }

    private void updateSafeBlackGrayUi() {
        if (safeBlackGraySeek != null) safeBlackGraySeek.setEnabled(safeBlackFloor);
        if (safeBlackGrayValue != null) {
            safeBlackGrayValue.setEnabled(safeBlackFloor);
            safeBlackGrayValue.setText(safeBlackGray + " / 255");
        }
    }

    private void updateTiltPulseUi() {
        if (tiltThresholdValue != null) {
            tiltThresholdValue.setText(String.format(java.util.Locale.US, "%.3f G", tiltPulseThresholdMilliG / 1000.0));
        }
        if (tiltCooldownValue != null) tiltCooldownValue.setText(tiltPulseCooldownMs + " ms");
        if (tiltThresholdSeek != null && tiltThresholdSeek.getProgress() != tiltPulseThresholdMilliG - 80) {
            tiltThresholdSeek.setProgress(tiltPulseThresholdMilliG - 80);
        }
        if (tiltCooldownSeek != null && tiltCooldownSeek.getProgress() != tiltPulseCooldownMs - 100) {
            tiltCooldownSeek.setProgress(tiltPulseCooldownMs - 100);
        }
    }

    private void onQualitySettingChanged() {
        if (applyingPreset) return;
        qualityPreset = PRESET_CUSTOM;
        saveCurrentSettings();
        updatePresetUi();
    }

    private void saveCurrentSettings() {
        try {RuntimeStorage.checkConfiguration(this);}
        catch(IOException error){Toast.makeText(this,L("设置未保存：")+error.getMessage(),Toast.LENGTH_LONG).show();return;}
        prefs.edit()
                .putBoolean(KEY_FPS, showFps)
                .putBoolean(KEY_LOGS, debugLogs)
                .putBoolean(KEY_DUMPS, dumpFrames)
                .putBoolean(KEY_COMPAT, pcCompat)
                .putBoolean(KEY_FAST_PRESENT, fastPresent)
                .putBoolean(KEY_SKIP_READBACK, skipReadback)
                .putBoolean(KEY_GPU_COPY, gpuPresentCopy)
                .putBoolean(KEY_SAFE_BLACK_FLOOR, safeBlackFloor)
                .putInt(KEY_SAFE_BLACK_GRAY, safeBlackGray)
                .putBoolean(KEY_TILT_PULSE, tiltPulseEnabled)
                .putInt(KEY_TILT_THRESHOLD, tiltPulseThresholdMilliG)
                .putInt(KEY_TILT_COOLDOWN, tiltPulseCooldownMs)
                .putBoolean(KEY_TEXTURES, bundledTextures)
                .putInt(KEY_SCALE, scaleHack)
                .putInt(KEY_CAP, fpsCap)
                .putBoolean(KEY_STRETCH, stretchViewport)
                .putInt(KEY_FSR, fsrMode)
                .putBoolean(KEY_AA, aaEnabled)
                .putInt(KEY_SHARPEN, sharpenTenths)
                .putInt(KEY_SHADOW_BOOST, shadowBoostPercent)
                .putBoolean(KEY_FSR3_AHB, fsr3AhbEnabled)
                .putBoolean(KEY_FSR3_FG, fsr3FgEnabled)
                .putBoolean(KEY_FSR3_DEPTH, fsr3DepthEnabled)
                .putBoolean(KEY_FSR3_GPU_DEPTH, fsr3GpuDepthEnabled)
                .putBoolean(KEY_FSR3_ASYNC, fsr3AsyncEnabled)
                .putBoolean(KEY_FSR3_DIRECT_OUTPUT, fsr3DirectOutputEnabled)
                .putBoolean(KEY_FSR3_FORCE, fsr3ForceEnabled)
                .putString(KEY_PRESET, qualityPreset)
                .apply();
        try {RuntimeStorage.saveConfiguration(this);}
        catch(IOException error){Toast.makeText(this,L("配置文件未保存：")+error.getMessage(),Toast.LENGTH_LONG).show();}
    }

    private String detectCurrentPreset() {
        if (matchesRecommendedPreset()) return PRESET_RECOMMENDED;
        if (matchesPerformancePreset()) return PRESET_PERFORMANCE;
        if (matchesCompatibilityPreset()) return PRESET_COMPATIBILITY;
        if (matchesFsr3Preset()) return PRESET_FSR3;
        return PRESET_CUSTOM;
    }

    private boolean matchesRecommendedPreset() {
        return scaleHack == 3
                && fpsCap == 0
                && !stretchViewport
                && fsrMode == 0
                && aaEnabled
                && sharpenTenths == 5
                && fastPresent
                && skipReadback
                && gpuPresentCopy
                && pcCompat
                && noFsr3Preset();
    }

    private boolean matchesPerformancePreset() {
        return scaleHack == 1
                && fpsCap == 0
                && !stretchViewport
                && fsrMode == 0
                && !aaEnabled
                && sharpenTenths == 0
                && fastPresent
                && skipReadback
                && gpuPresentCopy
                && pcCompat
                && noFsr3Preset();
    }

    private boolean matchesCompatibilityPreset() {
        return scaleHack == 1
                && fpsCap == 0
                && !stretchViewport
                && fsrMode == 0
                && !aaEnabled
                && sharpenTenths == 0
                && !fastPresent
                && !skipReadback
                && !gpuPresentCopy
                && pcCompat
                && noFsr3Preset();
    }

    private boolean matchesFsr3Preset() {
        return scaleHack == 2
                && fpsCap == 0
                && !stretchViewport
                && fsrMode == 0
                && !aaEnabled
                && sharpenTenths == 0
                && fastPresent
                && skipReadback
                && gpuPresentCopy
                && pcCompat
                && fsr3AhbEnabled
                && !fsr3FgEnabled
                && fsr3DepthEnabled
                && fsr3GpuDepthEnabled
                && fsr3AsyncEnabled
                && !fsr3DirectOutputEnabled;
    }

    private boolean noFsr3Preset() {
        return !fsr3AhbEnabled
                && !fsr3FgEnabled
                && !fsr3DepthEnabled
                && !fsr3GpuDepthEnabled
                && !fsr3AsyncEnabled
                && !fsr3DirectOutputEnabled;
    }

    private void updatePresetUi() {
        if (presetSummary == null) return;
        for (int index = 0; index < presetButtons.length; index++) {
            Button presetButton = presetButtons[index];
            if (presetButton == null) continue;
            boolean selected = PRESET_KEYS[index].equals(qualityPreset);
            presetButton.setTextColor(selected ? COLOR_BG : COLOR_TEXT);
            presetButton.setBackground(panelBackground(
                    selected ? COLOR_ACCENT : COLOR_PANEL_ALT,
                    selected ? Color.rgb(169, 245, 247) : COLOR_LINE));
            if (PRESET_FSR3.equals(PRESET_KEYS[index])) {
                presetButton.setEnabled(fsr3Usable());
                presetButton.setAlpha(fsr3Usable() ? 1.0f : 0.45f);
                if (!fsr3Usable()) {
                    presetButton.setText(L("FSR3 实验（不可用）"));
                } else {
                    presetButton.setText(L(PRESET_LABELS[index]));
                }
            }
        }

        String scale = scaleHack + "×";
        String fxaa = aaEnabled ? L("FXAA 开") : L("FXAA 关");
        String cas = sharpenTenths == 0 ? L("CAS 关")
                : String.format(java.util.Locale.US, "CAS %.1f", sharpenTenths / 10.0);
        if (PRESET_RECOMMENDED.equals(qualityPreset)) {
            presetSummary.setTextColor(COLOR_TEXT);
            presetSummary.setText(
                    L("推荐配置总览\n")
                            + L("原生 3× · FSR 关闭 · FXAA 开 · CAS 0.5 · 保持 3:2\n")
                            + L("3× 基线约 60 FPS；FXAA/CAS 组合尚需分设备复测。"));
            presetSummary.setBackground(
                    panelBackground(COLOR_PANEL_ALT, COLOR_LINE));
        } else if (PRESET_PERFORMANCE.equals(qualityPreset)) {
            presetSummary.setTextColor(COLOR_TEXT);
            presetSummary.setText(
                    L("性能优先\n")
                            + L("原生 1× · FSR/FXAA/CAS 关闭 · 快速呈现开启\n")
                            + L("画质更软，优先保证帧时间和发热。"));
            presetSummary.setBackground(
                    panelBackground(COLOR_PANEL_ALT, COLOR_LINE));
        } else if (PRESET_COMPATIBILITY.equals(qualityPreset)) {
            presetSummary.setTextColor(COLOR_TEXT);
            presetSummary.setText(
                    L("兼容优先\n")
                            + L("原生 1× · 关闭快速呈现/跳读回/GPU 复制 · PC 修复开启\n")
                            + L("用于未知 GPU 或出现闪烁、黑帧的设备。"));
            presetSummary.setBackground(
                    panelBackground(COLOR_PANEL_ALT, COLOR_LINE));
        } else if (PRESET_FSR3.equals(qualityPreset)) {
            presetSummary.setTextColor(COLOR_WARN);
            presetSummary.setText(
                    L("FSR3 实验\n")
                            + L("内部 2× · FSR3.1 + 真实 D16 + 异步一帧 · 直写关闭\n")
                            + L("多约一帧延迟；缺 motion/jitter/history，不是完整时间超分或帧生成。"));
            presetSummary.setBackground(
                    panelBackground(COLOR_PANEL, COLOR_WARN));
        } else {
            presetSummary.setTextColor(COLOR_TEXT);
            presetSummary.setText(
                    L("自定义配置\n")
                            + L("内部 ") + scale + " · " + fxaa + " · " + cas
                            + " · " + (fsr3AhbEnabled ? L("FSR3 开") : L("FSR3 关"))
                            + L("\n展开高级参数可继续逐项调整。"));
            presetSummary.setBackground(
                    panelBackground(COLOR_PANEL_ALT, COLOR_LINE));
        }
    }

    private void updateAdvancedVisibility() {
        if (advancedContainer == null || advancedButton == null) return;
        advancedContainer.setVisibility(advancedExpanded ? View.VISIBLE : View.GONE);
        advancedButton.setText(advancedExpanded
                ? L("高级参数：收起   ›")
                : L("高级参数：展开（自定义）   ›"));
    }

    private void resetDefaults() {
        shadowBoostPercent = 0;
        tiltPulseEnabled = false;
        tiltPulseThresholdMilliG = TiltGestureSettings.DEFAULT_MILLI_G;
        tiltPulseCooldownMs = 380;
        showFps = false;
        debugLogs = false;
        dumpFrames = false;
        setChecked(fpsToggle, showFps);
        setChecked(logsToggle, debugLogs);
        setChecked(dumpsToggle, dumpFrames);
        bundledTextures = BuildConfig.BUNDLED_TEXTURES;
        setChecked(textureToggle, bundledTextures);
        setChecked(tiltPulseToggle, tiltPulseEnabled);
        updateTiltPulseUi();
        applyPreset(PRESET_RECOMMENDED, true);
    }

    private void selectStorage(String mode) {
        try {RuntimeStorage.select(this,mode);recreate();}
        catch(IOException error){new AlertDialog.Builder(this).setTitle(L("未切换资料位置")).setMessage(error.getMessage()).setPositiveButton(L("确定"),null).show();}
    }

    private void startGame() {
        if (!prefs.getBoolean(LaunchReview.REVIEWED,false)) {
            LaunchReview.show(this,prefs,scaleHack,stretchViewport,(text,lighting,scale)-> {
                scaleHack=scale;
                prefs.edit().putBoolean("overlay_enabled",text).putBoolean(LaunchReview.LIGHTING,lighting)
                        .putBoolean(LaunchReview.REVIEWED,true).apply();
                saveCurrentSettings();
                startGameConfirmed();
            });
            return;
        }
        startGameConfirmed();
    }
    private void startGameConfirmed() {
        startGameWithConfig(
                showFps,
                debugLogs,
                dumpFrames,
                pcCompat,
                fastPresent,
                skipReadback,
                gpuPresentCopy,
                scaleHack,
                fpsCap,
                stretchViewport,
                fsrMode,
                aaEnabled,
                sharpenTenths,
                safeBlackFloor,
                safeBlackGray,
                false,
                fsr3AhbEnabled,
                fsr3FgEnabled && fsr3AhbEnabled,
                fsr3DepthEnabled && fsr3AhbEnabled,
                fsr3GpuDepthEnabled && fsr3AhbEnabled && fsr3DepthEnabled,
                fsr3AsyncEnabled && fsr3AhbEnabled,
                fsr3DirectOutputEnabled && fsr3AhbEnabled && fsr3AsyncEnabled,
                false,
                false,
                false,
                false,
                null);
    }

    private void startGameWithConfig(
            boolean showFps,
            boolean debugLogs,
            boolean dumpFrames,
            boolean pcCompat,
            boolean fastPresent,
            boolean skipReadback,
            boolean gpuPresentCopy,
            int scale,
            int cap,
            boolean stretch,
            int fsr,
            boolean aa,
            int sharpen,
            boolean safeBlackFloor,
            int safeBlackGray,
            boolean statsLog,
            boolean fsr3AhbProbe,
            boolean fsr3FrameGeneration,
            boolean fsr3RealDepth,
            boolean fsr3GpuDepth,
            boolean fsr3Async,
            boolean fsr3DirectOutput,
            boolean fsr3DiagReadback,
            boolean fsr3Trace,
            boolean fsr3FgSynthetic,
            boolean fsr3FgProbe,
            String ipaPath) {
        File dataBase;
        try {RuntimeStorage.requireGameStopped(this);RuntimeStorage.checkConfiguration(this);dataBase=RuntimeStorage.requireCompatible(this);}
        catch(IOException error){new AlertDialog.Builder(this).setTitle(L("无法启动游戏")).setMessage(error.getMessage()).setPositiveButton(L("确定"),null).show();return;}
        if (new File(dataBase, ".save-lab-adb-lock").exists() || SaveArchiveImporter.isBusy()) {
            Toast.makeText(this, L("正在切换存档，请完成后再启动游戏。"), Toast.LENGTH_LONG).show();
            return;
        }
        prefs.edit()
                .putBoolean(KEY_FPS, showFps)
                .putBoolean(KEY_LOGS, debugLogs)
                .putBoolean(KEY_DUMPS, dumpFrames)
                .putBoolean(KEY_COMPAT, pcCompat)
                .putBoolean(KEY_FAST_PRESENT, fastPresent)
                .putBoolean(KEY_SKIP_READBACK, skipReadback)
                .putBoolean(KEY_GPU_COPY, gpuPresentCopy)
                .putBoolean(KEY_SAFE_BLACK_FLOOR, safeBlackFloor)
                .putInt(KEY_SAFE_BLACK_GRAY, safeBlackGray)
                .putBoolean(KEY_TILT_PULSE, tiltPulseEnabled)
                .putInt(KEY_TILT_THRESHOLD, tiltPulseThresholdMilliG)
                .putInt(KEY_TILT_COOLDOWN, tiltPulseCooldownMs)
                .putInt(KEY_SCALE, scale)
                .putInt(KEY_CAP, cap)
                .putBoolean(KEY_STRETCH, stretch)
                .putInt(KEY_FSR, fsr)
                .putBoolean(KEY_AA, aa)
                .putInt(KEY_SHARPEN, sharpen)
                .putInt(KEY_SHADOW_BOOST, shadowBoostPercent)
                .putBoolean(KEY_FSR3_AHB, fsr3AhbProbe)
                .putBoolean(KEY_FSR3_FG, fsr3FrameGeneration)
                .putBoolean(KEY_FSR3_DEPTH, fsr3RealDepth)
                .putBoolean(KEY_FSR3_GPU_DEPTH, fsr3GpuDepth)
                .putBoolean(KEY_FSR3_ASYNC, fsr3Async)
                .putBoolean(KEY_FSR3_DIRECT_OUTPUT, fsr3DirectOutput)
                .putBoolean(KEY_FSR3_FORCE, fsr3ForceEnabled)
                .putString(KEY_PRESET, qualityPreset)
                .apply();

        try {RuntimeStorage.saveConfiguration(this);}
        catch(IOException error){new AlertDialog.Builder(this).setTitle(L("配置未保存")).setMessage(error.getMessage()).setPositiveButton(L("确定"),null).show();return;}
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("overlay_enabled", getIntent().getBooleanExtra("overlay_enabled",prefs.getBoolean("overlay_enabled", true)));
        intent.putExtra(SaveNodeMonitor.KEY_ENABLED, prefs.getBoolean(SaveNodeMonitor.KEY_ENABLED, false));
        intent.putExtra(SaveNodeMonitor.KEY_IMAGES, prefs.getBoolean(SaveNodeMonitor.KEY_IMAGES, true));
        String nodeSession = saveNodeSession == null ? prefs.getString(SaveNodeMonitor.KEY_SESSION, L("一周目采集"))
                : saveNodeSession.getText().toString().trim();
        if (nodeSession == null || nodeSession.isEmpty()) nodeSession = L("一周目采集");
        prefs.edit().putString(SaveNodeMonitor.KEY_SESSION, nodeSession).apply();
        intent.putExtra(SaveNodeMonitor.KEY_SESSION, nodeSession);
        intent.putExtra("show_fps", showFps);
        intent.putExtra("stats_log", statsLog);
        intent.putExtra("debug_logs", debugLogs);
        intent.putExtra("dump_frames", dumpFrames);
        intent.putExtra("pc_compat", pcCompat);
        intent.putExtra("fast_present", fastPresent);
        intent.putExtra("skip_readback", skipReadback);
        intent.putExtra("gpu_present_copy", gpuPresentCopy);
        intent.putExtra("bundled_textures", bundledTextures);
        intent.putExtra("advanced_lighting",getIntent().getBooleanExtra("advanced_lighting",LaunchReview.lighting(prefs)));
        intent.putExtra("scale_hack", scale);
        intent.putExtra("fps_cap", cap);
        intent.putExtra("viewport_stretch", stretch);
        intent.putExtra("present_fsr", fsr);
        intent.putExtra("aa", aa);
        intent.putExtra("present_sharpen", sharpen);
        intent.putExtra("safe_black_floor", safeBlackFloor);
        intent.putExtra("safe_black_gray", safeBlackGray);
        intent.putExtra("tilt_pulse", tiltPulseEnabled);
        intent.putExtra("tilt_threshold_mg", tiltPulseThresholdMilliG);
        intent.putExtra("tilt_cooldown_ms", tiltPulseCooldownMs);
        intent.putExtra("shadow_boost", shadowBoostPercent);
        intent.putExtra("fsr3_ahb_probe", fsr3AhbProbe);
        intent.putExtra("fsr3_force", fsr3ForceEnabled);
        intent.putExtra("fsr3_fg", fsr3FrameGeneration);
        intent.putExtra("fsr3_real_depth", fsr3RealDepth);
        intent.putExtra("fsr3_gpu_depth", fsr3GpuDepth);
        intent.putExtra("fsr3_async", fsr3Async);
        intent.putExtra("fsr3_direct_output", fsr3DirectOutput);
        intent.putExtra("fsr3_diag_readback", fsr3DiagReadback);
        intent.putExtra("fsr3_trace", fsr3Trace);
        intent.putExtra("fsr3_fg_synthetic", fsr3FgSynthetic);
        intent.putExtra("fsr3_fg_probe", fsr3FgProbe);
        String diagnosticEnv = getIntent().getStringExtra("diagnostic_env");
        if (diagnosticEnv != null && !diagnosticEnv.trim().isEmpty()) {
            intent.putExtra("diagnostic_env", diagnosticEnv);
        }
        if (ipaPath != null && !ipaPath.trim().isEmpty()) {
            intent.putExtra("ipa_path", ipaPath);
        }
        startActivity(intent);
        finish();
    }

    private void addSaveNodeControls(LinearLayout parent) {
        CheckBox enabled = addToggle(parent, L("自动采集存档节点（可选）"),
                L("默认关闭，下次启动游戏生效。开启后归档游戏写出的存档，便于整理一周目节点。"),
                prefs.getBoolean(SaveNodeMonitor.KEY_ENABLED, false));
        enabled.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(SaveNodeMonitor.KEY_ENABLED, checked).apply(); refreshSaveNodeStatus();
        });
        saveNodeSession = new EditText(this); saveNodeSession.setSingleLine(true);
        saveNodeSession.setTextColor(COLOR_TEXT); saveNodeSession.setHintTextColor(COLOR_MUTED);
        saveNodeSession.setHint(L("本轮名称，例如：一周目 · 玩家A"));
        saveNodeSession.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        saveNodeSession.setText(prefs.getString(SaveNodeMonitor.KEY_SESSION, L("一周目采集")));
        parent.addView(saveNodeSession, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
        CheckBox images = addToggle(parent, L("附带节点画面"), L("仅在节点采集开启时拍摄游戏画面。"), prefs.getBoolean(SaveNodeMonitor.KEY_IMAGES, true));
        images.setOnCheckedChangeListener((button, checked) -> prefs.edit().putBoolean(SaveNodeMonitor.KEY_IMAGES, checked).apply());
        saveNodeStatus = text("", 12, COLOR_MUTED, false); saveNodeStatus.setPadding(dp(14), dp(8), dp(14), dp(8));
        parent.addView(saveNodeStatus); refreshSaveNodeStatus();
        Button export = button(L("导出采集节点库 ZIP"), false);
        export.setOnClickListener(v -> {
            export.setEnabled(false);
            new Thread(() -> {
                try {
                    File file = SaveNodeMonitor.export(this);
                    runOnUiThread(() -> new AlertDialog.Builder(this).setTitle(L("节点库已导出"))
                            .setMessage(L("文件：") + file.getName() + L("\n可在应用文件目录的 save-node-exports 中取出。"))
                            .setPositiveButton(L("确定"), null).show());
                } catch (Exception error) { runOnUiThread(() -> Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show()); }
                finally { runOnUiThread(() -> { export.setEnabled(true); refreshSaveNodeStatus(); }); }
            }, "save-node-export").start();
        });
        parent.addView(export, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
    }

    private void refreshSaveNodeStatus() {
        if (saveNodeStatus == null) return;
        String state = prefs.getBoolean(SaveNodeMonitor.KEY_ENABLED, false) ? L("采集已开启（下次启动生效）") : L("采集关闭");
        try {
            if (new File(SaveNodeMonitor.bank(this), "initialization-error.txt").isFile()) {
                saveNodeStatus.setText(state + L(" · 上次采集初始化失败，详见导出记录。")); return;
            }
            File file = new File(SaveNodeMonitor.bank(this), "status.json");
            if (!file.isFile() || file.length() > 65536) { saveNodeStatus.setText(state + L(" · 尚无节点")); return; }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (FileInputStream input = new FileInputStream(file)) { byte[] buffer = new byte[4096]; int count; while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count); }
            org.json.JSONObject status = new org.json.JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            String message = state + L(" · 已归档 ") + status.optLong("nodes") + L(" 个节点 / ") + status.optLong("versions") + L(" 个文件版本");
            message += String.format(java.util.Locale.US, L("\n约 %.1f MB · 最近 %s"), status.optLong("estimated_bytes") / 1048576.0, status.optString("last_level", ""));
            if (!status.optString("last_error", "").isEmpty()) message += L("\n最近采集暂缓，详见导出记录。");
            saveNodeStatus.setText(message);
        } catch (Exception error) { saveNodeStatus.setText(state + L(" · 状态更新中")); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSaveNodeStatus();
        if (saveNodeStatus != null) saveNodeStatus.postDelayed(this::refreshSaveNodeStatus, 3500);
    }

    private void updateFsr3SelectionStatus() {
        if (fsr3SelectionStatus == null) return;
        updateFsr3ControlEnablement();
        if (!fsr3Supported && !fsr3ForceEnabled) {
            fsr3SelectionStatus.setTextColor(COLOR_WARN);
            fsr3SelectionStatus.setText(
                    L("当前选择：不可用。设备未满足基础 Vulkan/ABI/API 条件，将使用原生路径。"));
            if (fsr3DepthToggle != null) fsr3DepthToggle.setEnabled(false);
            return;
        }

        boolean depthEnabled = fsr3AhbToggle.isChecked() && fsr3DepthToggle.isChecked();
        fsr3DepthToggle.setEnabled(fsr3AhbToggle.isChecked());
        fsr3GpuDepthToggle.setEnabled(depthEnabled);
        fsr3AsyncToggle.setEnabled(fsr3AhbToggle.isChecked());
        fsr3DirectOutputToggle.setEnabled(
                fsr3AhbToggle.isChecked() && fsr3AsyncToggle.isChecked());
        if (!fsr3AhbToggle.isChecked()) {
            fsr3SelectionStatus.setTextColor(COLOR_MUTED);
            fsr3SelectionStatus.setText(
                    L("当前选择：原生路径。FSR3 超分、真实深度和帧生成均不会运行。"));
        } else if (!depthEnabled) {
            fsr3SelectionStatus.setTextColor(COLOR_WARN);
            fsr3SelectionStatus.setText(
                    L("当前选择：FSR3.1 呈现/超分探针，depth=false。")
                            + L("请同时开启“真实深度输入 D16”才能形成当前可验证的最小输入组合。"));
        } else {
            fsr3SelectionStatus.setTextColor(COLOR_ACCENT);
            fsr3SelectionStatus.setText(
                    L("当前选择：FSR3.1 + 真实 D16 深度（")
                            + (fsr3GpuDepthToggle.isChecked() ? L("GPU 直通") : L("CPU 读回"))
                            + "，" + (fsr3AsyncToggle.isChecked() ? L("异步一帧流水线") : L("同步等待"))
                            + (fsr3DirectOutputToggle.isChecked() ? L(" + 直接输出") : "")
                            + "）。"
                            + L("仍缺 motion/jitter/history/HUD 分层；帧生成保持关闭。"));
        }
    }

    private boolean fsr3Usable() {
        return fsr3Supported || fsr3ForceEnabled;
    }

    private void updateFsr3ControlEnablement() {
        boolean usable = fsr3Usable();
        boolean ahb = usable && fsr3AhbToggle != null && fsr3AhbToggle.isChecked();
        boolean depth = ahb && fsr3DepthToggle != null && fsr3DepthToggle.isChecked();
        if (fsr3AhbToggle != null) fsr3AhbToggle.setEnabled(usable);
        if (fsr3DepthToggle != null) fsr3DepthToggle.setEnabled(ahb);
        if (fsr3GpuDepthToggle != null) fsr3GpuDepthToggle.setEnabled(depth);
        if (fsr3AsyncToggle != null) fsr3AsyncToggle.setEnabled(ahb);
        if (fsr3DirectOutputToggle != null) {
            fsr3DirectOutputToggle.setEnabled(
                    ahb && fsr3AsyncToggle != null && fsr3AsyncToggle.isChecked());
        }
    }

    private void addInfoCard(LinearLayout root, String title, String body, int accent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.setBackground(panelBackground(COLOR_PANEL, COLOR_LINE));

        TextView heading = text(title, 15, accent, true);
        card.addView(heading);

        TextView detail = text(body, 13, COLOR_MUTED, false);
        detail.setLineSpacing(0f, 1.08f);
        detail.setPadding(0, dp(5), 0, 0);
        card.addView(detail);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        root.addView(card, params);
    }

    private void addSection(LinearLayout root, String label) {
        TextView section = text(label.toUpperCase(), 12, COLOR_ACCENT, true);
        section.setLetterSpacing(0.16f);
        section.setPadding(0, dp(19), 0, dp(7));
        root.addView(section);
    }

    private CheckBox addToggle(LinearLayout root, String title, String detail, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(title + "\n" + detail);
        box.setTextSize(15);
        box.setTextColor(COLOR_TEXT);
        box.setButtonTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT));
        box.setLineSpacing(0f, 1.08f);
        box.setGravity(Gravity.TOP | Gravity.START);
        box.setPadding(dp(16), dp(12), dp(16), dp(12));
        box.setBackground(panelBackground(COLOR_PANEL, COLOR_LINE));
        box.setChecked(checked);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        root.addView(box, params);
        return box;
    }

    private Button addCycleButton(LinearLayout root, String unused, View.OnClickListener listener) {
        Button button = button("", false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        params.bottomMargin = dp(8);
        root.addView(button, params);
        return button;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTextColor(primary ? COLOR_BG : COLOR_TEXT);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setBackground(panelBackground(primary ? COLOR_ACCENT : COLOR_PANEL_ALT,
                primary ? Color.rgb(169, 245, 247) : COLOR_LINE));
        button.setPadding(dp(15), 0, dp(15), 0);
        return button;
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        return view;
    }

    private GradientDrawable panelBackground(int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(4));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @SuppressWarnings("deprecation")
    private void applyImmersiveMode() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attributes);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
