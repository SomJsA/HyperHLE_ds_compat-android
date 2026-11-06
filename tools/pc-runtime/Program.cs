using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Text;
using System.Windows.Forms;

namespace DeadSpaceBootstrap
{
    internal static class Program
    {
        [STAThread]
        private static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new LauncherForm());
        }
    }

    internal sealed partial class LauncherForm : Form
    {
        private readonly string baseDir = AppDomain.CurrentDomain.BaseDirectory;
        private readonly string iniPath;

        private readonly TextBox ipaPath = new TextBox();
        private readonly ComboBox scaleHack = new ComboBox();
        private readonly ComboBox fpsCap = new ComboBox();
        private readonly ComboBox fsrMode = new ComboBox();
        private readonly ComboBox mouseLook = new ComboBox();
        private readonly ComboBox lookDirection = new ComboBox();
        private readonly TextBox aimPoint = new TextBox();
        private readonly TextBox stickGain = new TextBox();
        private readonly TextBox lookGain = new TextBox();

        private readonly CheckBox fastPresent = new CheckBox();
        private readonly CheckBox skipFinish = new CheckBox();
        private readonly CheckBox pcCompat = new CheckBox();
        private readonly CheckBox openingMips = new CheckBox();
        private readonly CheckBox globalMips = new CheckBox();
        private readonly CheckBox disableFog = new CheckBox();
        private readonly CheckBox screenFogMask = new CheckBox();
        private readonly CheckBox sharpen = new CheckBox();
        private readonly CheckBox pcMouse = new CheckBox();
        private readonly CheckBox debugLogs = new CheckBox();
        private readonly CheckBox frameDump = new CheckBox();

        private readonly Label status = new Label();

        public LauncherForm()
        {
            iniPath = Path.Combine(baseDir, "DeadSpace-Launcher.ini");
            Text = "DeadSpace IOS移植 · 启动器";
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(1000, 740);
            MinimumSize = new Size(1016, 779);
            Font = new Font("Microsoft YaHei UI", 9.5f);
            BackColor = Color.FromArgb(236, 241, 245);

            BuildShell();
            LoadSettings();
            LoadKeyBindings();
        }

        private static void AddComboRow(Control parent, string label, int y, ComboBox combo, string[] items)
        {
            parent.Controls.Add(new Label { Text = label, Location = new Point(30, y + 4), Size = new Size(135, 24) });
            combo.DropDownStyle = ComboBoxStyle.DropDownList;
            combo.Items.AddRange(items);
            combo.Location = new Point(174, y);
            combo.Size = new Size(210, 28);
            parent.Controls.Add(combo);
        }

        private static void AddTextRow(Control parent, string label, int y, TextBox textBox)
        {
            parent.Controls.Add(new Label { Text = label, Location = new Point(30, y + 4), Size = new Size(135, 24) });
            textBox.Location = new Point(174, y);
            textBox.Size = new Size(210, 28);
            parent.Controls.Add(textBox);
        }

        private static void AddCheck(Control parent, CheckBox checkBox, int y, string text, bool defaultValue)
        {
            checkBox.Text = text;
            checkBox.Location = new Point(30, y);
            checkBox.Size = new Size(625, 28);
            checkBox.Checked = defaultValue;
            parent.Controls.Add(checkBox);
        }

        private void BrowseIpa()
        {
            using (var dialog = new OpenFileDialog())
            {
                dialog.Title = "选择 Dead Space IPA";
                dialog.Filter = "iOS 应用包 (*.ipa)|*.ipa|所有文件 (*.*)|*.*";
                string current = ipaPath.Text.Trim();
                if (current.Length > 0)
                {
                    string directory = Path.GetDirectoryName(current);
                    if (!string.IsNullOrEmpty(directory) && Directory.Exists(directory))
                    {
                        dialog.InitialDirectory = directory;
                    }
                }
                if (dialog.ShowDialog(this) == DialogResult.OK)
                {
                    ipaPath.Text = dialog.FileName;
                }
            }
        }

        private string FindDefaultIpa()
        {
            string[] candidates =
            {
                Path.Combine(baseDir, "DeadSpace.ipa"),
                Path.Combine(baseDir, "Dead Space-v1.3.34.ipa"),
                Path.Combine(baseDir, "touchHLE_apps", "DeadSpace.ipa"),
                Path.Combine(baseDir, "touchHLE_apps", "DeadSpace-ipad2.ipa"),
                @"D:\BaiduNetdiskDownload\Dead Space-v1.3.34-jack821309.ipa"
            };
            foreach (string candidate in candidates)
            {
                if (File.Exists(candidate))
                {
                    return candidate;
                }
            }
            return "";
        }

        private void ApplyDefaults()
        {
            ResetKeyBindings();
            string existing = ipaPath.Text.Trim();
            scaleHack.SelectedIndex = 1;
            fpsCap.SelectedIndex = 1;
            fsrMode.SelectedIndex = 1;
            mouseLook.SelectedIndex = 2;
            lookDirection.SelectedIndex = 0;
            lookGain.Text = "1.0";
            aimPoint.Text = "360,200";
            stickGain.Text = "2.0";
            fastPresent.Checked = true;
            skipFinish.Checked = false;
            pcCompat.Checked = true;
            openingMips.Checked = true;
            globalMips.Checked = false;
            disableFog.Checked = false;
            screenFogMask.Checked = false;
            sharpen.Checked = true;
            pcMouse.Checked = true;
            debugLogs.Checked = false;
            frameDump.Checked = false;
            ipaPath.Text = existing.Length > 0 ? existing : FindDefaultIpa();
        }

        private void LoadSettings()
        {
            var values = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            if (File.Exists(iniPath))
            {
                foreach (string raw in File.ReadAllLines(iniPath, Encoding.UTF8))
                {
                    int split = raw.IndexOf('=');
                    if (split > 0)
                    {
                        values[raw.Substring(0, split).Trim()] = raw.Substring(split + 1).Trim();
                    }
                }
            }

            ipaPath.Text = Get(values, "ipa", FindDefaultIpa());
            SetCombo(scaleHack, Get(values, "scale", "2x 平衡"));
            SetCombo(fpsCap, Get(values, "fps", "60 FPS"));
            SetCombo(fsrMode, Get(values, "fsr", "FSR1 EASU + RCAS"));
            SetCombo(mouseLook, Get(values, "mouse_look", "始终捕获"));
            SetCombo(lookDirection, Get(values, "look_direction", "反转水平+垂直"));
            lookGain.Text = Get(values, "look_gain", "1.0");
            aimPoint.Text = Get(values, "aim_xy", "360,200");
            stickGain.Text = Get(values, "stick_gain", "2.0");
            fastPresent.Checked = GetBool(values, "fast_present", true);
            skipFinish.Checked = GetBool(values, "skip_finish", false);
            pcCompat.Checked = GetBool(values, "pc_compat", true);
            openingMips.Checked = GetBool(values, "opening_mips", true);
            globalMips.Checked = GetBool(values, "global_mips", false);
            disableFog.Checked = GetBool(values, "disable_fog", false);
            screenFogMask.Checked = GetBool(values, "screen_fog_mask", false);
            sharpen.Checked = GetBool(values, "sharpen", true);
            pcMouse.Checked = GetBool(values, "pc_mouse", true);
            debugLogs.Checked = GetBool(values, "debug_logs", false);
            frameDump.Checked = GetBool(values, "frame_dump", false);
        }

        private static string Get(Dictionary<string, string> values, string key, string fallback)
        {
            string value;
            return values.TryGetValue(key, out value) ? value : fallback;
        }

        private static bool GetBool(Dictionary<string, string> values, string key, bool fallback)
        {
            string value;
            if (!values.TryGetValue(key, out value))
            {
                return fallback;
            }
            return value == "1" || value.Equals("true", StringComparison.OrdinalIgnoreCase);
        }

        private static void SetCombo(ComboBox combo, string value)
        {
            int index = combo.Items.IndexOf(value);
            combo.SelectedIndex = index >= 0 ? index : 0;
        }

        private void SaveSettings()
        {
            var lines = new[]
            {
                "ipa=" + ipaPath.Text.Trim(),
                "scale=" + scaleHack.SelectedItem,
                "fps=" + fpsCap.SelectedItem,
                "fsr=" + fsrMode.SelectedItem,
                "mouse_look=" + mouseLook.SelectedItem,
                "look_direction=" + lookDirection.SelectedItem,
                "look_gain=" + lookGain.Text.Trim(),
                "aim_xy=" + aimPoint.Text.Trim(),
                "stick_gain=" + stickGain.Text.Trim(),
                "fast_present=" + (fastPresent.Checked ? "1" : "0"),
                "skip_finish=" + (skipFinish.Checked ? "1" : "0"),
                "pc_compat=" + (pcCompat.Checked ? "1" : "0"),
                "opening_mips=" + (openingMips.Checked ? "1" : "0"),
                "global_mips=" + (globalMips.Checked ? "1" : "0"),
                "disable_fog=" + (disableFog.Checked ? "1" : "0"),
                "screen_fog_mask=" + (screenFogMask.Checked ? "1" : "0"),
                "sharpen=" + (sharpen.Checked ? "1" : "0"),
                "pc_mouse=" + (pcMouse.Checked ? "1" : "0"),
                "debug_logs=" + (debugLogs.Checked ? "1" : "0"),
                "frame_dump=" + (frameDump.Checked ? "1" : "0")
            };
            File.WriteAllLines(iniPath, lines, Encoding.UTF8);
        }

        private void LaunchGame()
        {
            string ipa = ipaPath.Text.Trim();
            string gameExe = Path.Combine(baseDir, "touchHLE.exe");
            if (!File.Exists(gameExe))
            {
                status.Text = "缺少 touchHLE.exe";
                return;
            }
            if (!File.Exists(ipa))
            {
                status.Text = "请选择有效的 Dead Space IPA";
                return;
            }

            try { SaveKeyBindings(); }
            catch (Exception error) { status.Text = error.Message; return; }
            SaveSettings();
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_KEY_BINDINGS", Path.Combine(baseDir, "DeadSpace-Keys.ini"));
            int renderScale = scaleHack.SelectedIndex >= 1 ? scaleHack.SelectedIndex + 1 : 1;
            int cap = fpsCap.SelectedIndex == 0 ? 30 : (fpsCap.SelectedIndex == 1 ? 60 : 0);

            Environment.SetEnvironmentVariable("TOUCHHLE_DS_SCREEN_SCALE", "2");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_DEPTH_SIZE", "24");
            // The validated PC baseline renders the EAGL drawable at 4x MSAA
            // and clears the multisample depth after each resolve. Keep this
            // deterministic even if a diagnostic process left conflicting
            // environment variables behind.
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_MSAA", "4");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_MSAA_CLEAR_DEPTH", "1");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_MSAA_FBO_DEPTH", null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_MSAA_DRAWABLE_ONLY", null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_HOLD_LAST_ON_BLACK", null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_FORCE_NO_CULL", null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_FORCE_CULL_FACE", null);
            Environment.SetEnvironmentVariable(
                "TOUCHHLE_DS_NO_CULL_HASH_COUNT",
                "02d3486c:405");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_DEPTH_ALWAYS_HASH_COUNT", null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_CLEAR_DEPTH_HASH_COUNT", null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_DEPTH_ALWAYS_TRI_HASH_COUNT", null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_FLIP_WINDING_HASH_COUNT", null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_NO_CULL_TRI_HASH_COUNT", null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_UNLIT_BACKFACE_HASH_COUNT", null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_TEXMAT_IDENTITY_HASH_COUNT", null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_CLEAR_OPENING_DEPTH", null);
            Environment.SetEnvironmentVariable(
                "TOUCHHLE_DS_DST_FILL_HASH_COUNT",
                null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_CG_GLYPH_SCALE", "0.68");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_CG_GLYPH_DY", "0");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_ALPHA_AS_WHITE", "0");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_PRESENT_SHARPEN", sharpen.Checked ? "1.5" : "0");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_PRESENT_FSR",
                fsrMode.SelectedIndex > 0 ? fsrMode.SelectedIndex.ToString() : null);

            if (pcCompat.Checked)
            {
                // The asteroid d1feb687:600 must retain depth writes and GL_LESS.
                // Keep gameplay on the guest's GL_LESS. Only the full-screen
                // opening background atlases need the later-layer tie break.
                Environment.SetEnvironmentVariable("TOUCHHLE_DS_DEPTH_LEQUAL", null);
                string depthLequalHash =
                    "25a08a82,cee43546,36497762,1d5cccb8,8c12a704,87127d0f,e70efc97,2131afd5,18c949fb,f4d178fa";
                if (Environment.GetEnvironmentVariable(
                    "TOUCHHLE_DS_DIAG_PRESERVE_DEPTH_HASH") == "1")
                {
                    string diagnosticDepthHash = Environment.GetEnvironmentVariable(
                        "TOUCHHLE_DS_DEPTH_LEQUAL_HASH");
                    if (!string.IsNullOrWhiteSpace(diagnosticDepthHash))
                    {
                        depthLequalHash = diagnosticDepthHash;
                    }
                }
                Environment.SetEnvironmentVariable(
                    "TOUCHHLE_DS_DEPTH_LEQUAL_HASH",
                    depthLequalHash);
                // The background stack must not write depth after winning a
                // tie, or its black layers cut triangular holes through the
                // cockpit and distant safety posters.
                Environment.SetEnvironmentVariable("TOUCHHLE_DS_DEPTH_LEQUAL_NOWRITE", "1");
                Environment.SetEnvironmentVariable("TOUCHHLE_DS_PROJ_Z_SCALE", null);
                Environment.SetEnvironmentVariable("TOUCHHLE_DS_NO_PROJ_Z_FIX", null);
            }
            else
            {
                Environment.SetEnvironmentVariable("TOUCHHLE_DS_DEPTH_LEQUAL", null);
                Environment.SetEnvironmentVariable("TOUCHHLE_DS_NO_PROJ_Z_FIX", "1");
            }
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_NO_FOG", disableFog.Checked ? "1" : "0");
            // Keep atmospheric fog on 3D perspective scene geometry, but never
            // let eye-space fog leak into orthographic/2D overlays.
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_NO_FOG_ORTHO", "1");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_NO_FOG_SCREEN", screenFogMask.Checked ? "1" : "0");
            // The opening station's lit window/porthole uses this atlas. Its
            // transparent overlay collapses to almost black when eye-space fog
            // is applied, while the rest of the scene still needs the original
            // distance fade.
            Environment.SetEnvironmentVariable(
                "TOUCHHLE_DS_NO_FOG_HASH",
                "7027023c,56c62e8a");
            string texNoDepthHash = null;
            if (Environment.GetEnvironmentVariable(
                "TOUCHHLE_DS_DIAG_PRESERVE_TEXNODEPTH_HASH") == "1")
            {
                texNoDepthHash = Environment.GetEnvironmentVariable(
                    "TOUCHHLE_DS_TEXNODEPTH_HASH");
            }
            Environment.SetEnvironmentVariable(
                "TOUCHHLE_DS_TEXNODEPTH_HASH",
                texNoDepthHash);
            // The opening/porthole/poster atlas is made of overlapping cover
            // quads. During gameplay-camera shots, depth rejects some of those
            // later transparent layers and leaves black walls / a diagonally
            // split poster. Scope by the complete draw count; the runtime
            // scene-signature gate disables this during the opening fly-in.
            Environment.SetEnvironmentVariable(
                "TOUCHHLE_DS_TEXNODEPTH_HASH_COUNT",
                "56c62e8a:6,56c62e8a:36,7027023c:6,7027023c:36");
            // The game uploads textures from one guest thread and renders from
            // another. Complete each upload before the render thread can draw
            // it, otherwise the opening cockpit intermittently samples a black
            // atlas for one or more frames.
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_TEX_UPLOAD_FINISH", "1");
            // The opening station uses this atlas for the small safety-poster
            // quads. Keep linear filtering without changing its wall batches.
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_TEXFILTER_LINEAR_HASH", "25a08a82");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_TEXFILTER_LINEAR_MAX_COUNT", "6");
            // Leave the experimental texture-coordinate pre-transform off until
            // it is scoped to the weapon batch without affecting the porthole.
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_PRETRANSFORM_TEX_HASH", null);
            // The cinematic's character/environment batch contains two bad
            // triangles that cover the right half of the frame with black.
            // Remove only those triangles; skipping the whole batch also erases
            // the corridor behind them.
            // The fog fix removes the former black weapon/corridor block
            // without deleting geometry from these shared batches.
            string skipTriangles = null;
            if (Environment.GetEnvironmentVariable(
                "TOUCHHLE_DS_DIAG_PRESERVE_SKIP_TRI") == "1")
            {
                string diagnosticSkip = Environment.GetEnvironmentVariable(
                    "TOUCHHLE_DS_SKIP_TRI_HASH_COUNT");
                if (!string.IsNullOrWhiteSpace(diagnosticSkip))
                {
                    skipTriangles = diagnosticSkip;
                }
            }
            Environment.SetEnvironmentVariable(
                "TOUCHHLE_DS_SKIP_TRI_HASH_COUNT",
                skipTriangles);

            Environment.SetEnvironmentVariable("TOUCHHLE_DS_AUTO_OPENING_MIPS", openingMips.Checked ? "1" : "0");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_AUTO_MIPS_ALL", globalMips.Checked ? "1" : "0");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_SKIP_READBACK_FINISH", skipFinish.Checked ? "1" : null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_MOUSE_LOOK",
                mouseLook.SelectedIndex == 1 ? "aim" : (mouseLook.SelectedIndex == 2 ? "all" : null));
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_MOUSE_LOOK_INVERT",
                lookDirection.SelectedIndex == 0 ? "both"
                    : lookDirection.SelectedIndex == 1 ? "none"
                    : lookDirection.SelectedIndex == 2 ? "x" : "y");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_MOUSE_LOOK_GAIN", lookGain.Text.Trim());
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_AIM_XY", pcMouse.Checked ? aimPoint.Text.Trim() : null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_KEY_STICK_GAIN", stickGain.Text.Trim());

            string macroPath = Path.Combine(baseDir, "ds-macros.txt");
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_MACROS", File.Exists(macroPath) ? macroPath : null);
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_AUTO_DISMISS_ALERTS", debugLogs.Checked ? "0" : "1");

            string frameDir = Path.Combine(baseDir, "frames");
            if (frameDump.Checked)
            {
                Directory.CreateDirectory(frameDir);
                Environment.SetEnvironmentVariable("TOUCHHLE_DS_DUMP_FRAMES", frameDir);
                Environment.SetEnvironmentVariable("TOUCHHLE_DS_DUMP_FRAME_EVERY", "120");
                Environment.SetEnvironmentVariable("TOUCHHLE_DS_DUMP_AT_PRESENT", "1");
            }
            else
            {
                // Keep the shipped launcher deterministic, but allow a
                // diagnostic run to provide its own tightly scoped frame dump
                // window. This is only honored when explicitly requested.
                if (Environment.GetEnvironmentVariable(
                    "TOUCHHLE_DS_DIAG_PRESERVE_FRAME_DUMP") != "1")
                {
                    Environment.SetEnvironmentVariable("TOUCHHLE_DS_DUMP_FRAMES", null);
                    Environment.SetEnvironmentVariable("TOUCHHLE_DS_DUMP_FRAME_EVERY", null);
                    Environment.SetEnvironmentVariable("TOUCHHLE_DS_DUMP_FRAME_FROM", null);
                    Environment.SetEnvironmentVariable("TOUCHHLE_DS_DUMP_FRAME_TO", null);
                    Environment.SetEnvironmentVariable("TOUCHHLE_DS_DUMP_FRAME_ROTATE", null);
                    Environment.SetEnvironmentVariable("TOUCHHLE_DS_DUMP_FRAME_SCALE", null);
                    Environment.SetEnvironmentVariable("TOUCHHLE_DS_DUMP_AT_PRESENT", null);
                }
            }
            Environment.SetEnvironmentVariable("TOUCHHLE_DS_LOG_LEVEL", debugLogs.Checked ? "debug" : "normal");

            var args = new StringBuilder();
            args.Append("\"").Append(ipa).Append("\"");
            args.Append(" --force-composition --landscape-right");
            args.Append(" --scale-hack=").Append(renderScale);
            args.Append(" --preferred-languages=zh-Hans,zh");
            args.Append(" --stick-to-touch=30,190,120,110");
            if (fastPresent.Checked)
            {
                args.Append(" --fast-present");
            }
            if (cap == 0)
            {
                args.Append(" --fps-limit=off");
            }
            else
            {
                args.Append(" --fps-limit=").Append(cap);
            }
            if (debugLogs.Checked)
            {
                args.Append(" --print-fps");
            }

            try
            {
                var info = new ProcessStartInfo
                {
                    FileName = gameExe,
                    Arguments = args.ToString(),
                    WorkingDirectory = baseDir,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };
                Process.Start(info);
                Close();
            }
            catch (Exception exception)
            {
                status.Text = "启动失败：" + exception.Message;
            }
        }
    }
}
