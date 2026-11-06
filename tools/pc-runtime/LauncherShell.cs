using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Windows.Forms;

namespace DeadSpaceBootstrap
{
    internal sealed partial class LauncherForm
    {
        private static readonly Color Shell = Color.FromArgb(10, 17, 24);
        private static readonly Color Surface = Color.FromArgb(19, 30, 40);
        private static readonly Color Ink = Color.FromArgb(221, 232, 237);
        private static readonly Color Muted = Color.FromArgb(142, 163, 176);
        private static readonly Color Accent = Color.FromArgb(98, 218, 232);
        private readonly List<Panel> pages = new List<Panel>();
        private readonly List<Button> navigation = new List<Button>();
        private SaveSettings loadedSave;
        private readonly CheckBox attackPlugin = new CheckBox();
        private readonly CheckBox defensePlugin = new CheckBox();
        private readonly CheckBox lootPlugin = new CheckBox();
        private readonly Label saveStatus = new Label();
        private readonly Button writeSave = new Button();

        private void BuildShell()
        {
            BackColor = Shell;
            ForeColor = Ink;
            Controls.Add(new Label { Text = "DEAD SPACE", Font = new Font("Segoe UI", 27, FontStyle.Bold),
                ForeColor = Ink, Location = new Point(28, 24), AutoSize = true });
            Controls.Add(new Label { Text = "IOS 移植  /  PC", ForeColor = Accent,
                Location = new Point(32, 78), AutoSize = true });
            var main = new Panel { Location = new Point(224, 126), Size = new Size(748, 510),
                Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right };
            Controls.Add(main);
            Panel graphics = Page(main, "01   画面与后处理");
            Panel input = Page(main, "02   键鼠与手柄");
            Panel compatibility = Page(main, "03   兼容与诊断");
            Panel library = Page(main, "04   游戏文件");
            Panel saves = Page(main, "05   存档插件");
            Panel bindings = Page(main, "06   键盘改键");
            BuildKeyBindings(bindings);

            Heading(graphics, "画面与后处理", "渲染分辨率与呈现效果分别设置。", 22);
            AddComboRow(graphics, "内部渲染", 96, scaleHack, new[] { "1x 性能", "2x 平衡", "3x 清晰" });
            AddComboRow(graphics, "帧率上限", 142, fpsCap, new[] { "30 FPS", "60 FPS", "不限" });
            LabelText(graphics, "后处理方案", 30, 203, 135, 24);
            string[] names = { "原始画面", "轻量超分", "超分与锐化" };
            for (int i = 0; i < names.Length; i++)
            {
                int preset = i;
                var button = new Button { Text = names[i], Location = new Point(174 + i * 166, 196), Size = new Size(156, 38) };
                button.Click += delegate { fsrMode.SelectedIndex = preset == 1 ? 2 : preset == 2 ? 1 : 0;
                    sharpen.Checked = false; status.Text = "已选择「" + names[preset] + "」，启动时保存。"; };
                graphics.Controls.Add(button);
            }
            AddComboRow(graphics, "超分算法", 258, fsrMode, new[] { "关闭", "FSR1 EASU + RCAS", "FSR1 EASU（轻量）" });
            AddCheck(graphics, sharpen, 308, "追加 CAS 锐化（可独立于 FSR1 使用）", false);
            AddCheck(graphics, fastPresent, 352, "快速呈现", false);
            LabelText(graphics, "方案按钮只改变后处理；下方设置显示实际生效选项。\r\n原始画面关闭超分和锐化；超分与锐化使用 FSR1 自带 RCAS。", 30, 414, 665, 58);

            Heading(input, "输入方式", "鼠标捕获与灵敏度；手柄保留现有模拟摇杆输入。", 22);
            AddComboRow(input, "鼠标捕获", 96, mouseLook, new[] { "关闭", "仅瞄准时捕获", "始终捕获" });
            AddComboRow(input, "视角方向", 142, lookDirection, new[] { "反转水平+垂直", "标准", "只反水平", "只反垂直" });
            AddTextRow(input, "视角灵敏度", 188, lookGain);
            AddCheck(input, pcMouse, 232, "右键瞄准，左键射击", true);
            AddTextRow(input, "瞄准点 x,y", 278, aimPoint);
            AddTextRow(input, "移动摇杆增益", 324, stickGain);
            LabelText(input, "Alt / F8 切换捕获，Win 释放鼠标。\r\nWASD 移动 · E 交互 / QTE · F 念力 · C 静态能量 · R 装弹\r\nQ 近战 · X 旋转切割器 · 1–5 武器 · Shift+Space 无重力跳\r\nF5 为诊断状态快照，F7 为画面标记；游戏进度由游戏自身保存。", 30, 386, 680, 104);

            Heading(compatibility, "兼容与诊断", "常规游玩保留雾效；实验选项只用于定位问题。", 22);
            AddCheck(compatibility, pcCompat, 96, "启用已验证的 PC 渲染兼容修复", true);
            AddCheck(compatibility, openingMips, 140, "修正开场贴图的远距离采样", true);
            AddCheck(compatibility, globalMips, 184, "实验：重建全部贴图的远距离采样", false);
            AddCheck(compatibility, disableFog, 228, "诊断：关闭场景雾", false);
            AddCheck(compatibility, screenFogMask, 272, "实验：背景层排除雾效", false);
            AddCheck(compatibility, skipFinish, 316, "实验：慢呈现路径跳过同步等待", false);
            AddCheck(compatibility, debugLogs, 360, "详细调试日志", false);
            AddCheck(compatibility, frameDump, 404, "定期保存画面到 frames（影响性能）", false);

            Heading(library, "游戏文件", "设置保存在启动器旁，与当前运行目录的存档对应。", 22);
            LabelText(library, "游戏 IPA", 30, 96, 200, 24);
            ipaPath.Location = new Point(30, 134); ipaPath.Size = new Size(650, 28);
            library.Controls.Add(ipaPath);
            var browse = new Button { Text = "选择游戏文件", Location = new Point(30, 182), Size = new Size(160, 38) };
            browse.Click += delegate { BrowseIpa(); }; library.Controls.Add(browse);
            LabelText(library, "运行目录\r\n" + baseDir + "\r\n\r\n存档目录\r\ntouchHLE_sandbox / com.ea.deadspace.bv / Documents / var", 30, 254, 675, 150);

            Heading(saves, "存档插件", "编辑当前运行目录的 settings.sb，写入前自动备份。", 22);
            AddCheck(saves, attackPlugin, 96, "攻击增强（attack）", false);
            AddCheck(saves, defensePlugin, 140, "防御增强（defense）", false);
            AddCheck(saves, lootPlugin, 184, "资源增强（loot）", false);
            var readSave = new Button { Text = "读取当前存档", Location = new Point(30, 242), Size = new Size(170, 40) };
            readSave.Click += delegate { ReadCurrentSave(); }; saves.Controls.Add(readSave);
            writeSave.Text = "保存插件开关"; writeSave.Location = new Point(218, 242); writeSave.Size = new Size(170, 40); writeSave.Enabled = false;
            writeSave.Click += delegate
            {
                try
                {
                    string backup = loadedSave.Write(attackPlugin.Checked, defensePlugin.Checked, lootPlugin.Checked);
                    ReadCurrentSave(); saveStatus.Text = "保存成功。备份：\r\n" + backup;
                }
                catch (Exception ex) { saveStatus.Text = ex.Message; }
            };
            saves.Controls.Add(writeSave);
            saveStatus.Location = new Point(30, 312); saveStatus.Size = new Size(680, 160);
            saveStatus.Text = "游戏运行时可以读取，保存需先退出游戏。\r\n这里只修改三个插件的解锁状态，不改章节进度或物品。\r\n插件在游戏中的实际效果仍待验证。"; saves.Controls.Add(saveStatus);

            var defaults = new Button { Text = "恢复默认", Location = new Point(224, 662), Size = new Size(122, 42), Anchor = AnchorStyles.Left | AnchorStyles.Bottom };
            defaults.Click += delegate { ApplyDefaults(); status.Text = "已恢复默认；启动时保存。"; }; Controls.Add(defaults);
            var launch = new Button { Text = "启动游戏  →", Location = new Point(792, 656), Size = new Size(180, 48), Anchor = AnchorStyles.Right | AnchorStyles.Bottom };
            launch.Click += delegate { LaunchGame(); }; Controls.Add(launch);
            status.Location = new Point(224, 710); status.Size = new Size(748, 24);
            status.Anchor = AnchorStyles.Left | AnchorStyles.Right | AnchorStyles.Bottom; Controls.Add(status);
            Theme(this);
            launch.BackColor = Accent; launch.ForeColor = Shell;
            SelectPage(0);
        }

        private void ReadCurrentSave()
        {
            try
            {
                string file = Path.Combine(baseDir, "touchHLE_sandbox", "com.ea.deadspace.bv", "Documents", "var", "settings.sb");
                loadedSave = new SaveSettings(file);
                attackPlugin.Checked = loadedSave.Enabled("attack"); defensePlugin.Checked = loadedSave.Enabled("defense"); lootPlugin.Checked = loadedSave.Enabled("loot");
                writeSave.Enabled = true;
                saveStatus.Text = "已读取：\r\n" + file + "\r\n\r\n此设置由该运行目录的存档槽共享；写入需先退出游戏。";
            }
            catch (Exception ex) { loadedSave = null; writeSave.Enabled = false; saveStatus.Text = "无法读取：" + ex.Message; }
        }

        private Panel Page(Panel host, string name)
        {
            int index = pages.Count;
            var page = new Panel { Dock = DockStyle.Fill, AutoScroll = true, Visible = false };
            pages.Add(page); host.Controls.Add(page);
            var button = new Button { Text = name, TextAlign = ContentAlignment.MiddleLeft,
                Location = new Point(24, 136 + index * 56), Size = new Size(180, 44) };
            button.Click += delegate { SelectPage(index); };
            navigation.Add(button); Controls.Add(button); return page;
        }

        private void SelectPage(int index)
        {
            for (int i = 0; i < pages.Count; i++)
            {
                pages[i].Visible = i == index;
                navigation[i].ForeColor = i == index ? Accent : Muted;
                navigation[i].BackColor = i == index ? Surface : Shell;
            }
        }

        private static void LabelText(Control parent, string text, int x, int y, int width, int height)
        { parent.Controls.Add(new Label { Text = text, Location = new Point(x, y), Size = new Size(width, height) }); }

        private static void Heading(Control page, string title, string description, int y)
        {
            page.Controls.Add(new Label { Text = title, Font = new Font("Microsoft YaHei UI", 17, FontStyle.Bold),
                Location = new Point(30, y), Size = new Size(650, 38) });
            LabelText(page, description, 30, y + 42, 680, 26);
        }

        private static void Theme(Control control)
        {
            foreach (Control child in control.Controls)
            {
                child.BackColor = child is Panel ? Surface : control.BackColor;
                child.ForeColor = Ink;
                var button = child as Button;
                if (button != null) { button.FlatStyle = FlatStyle.Flat; button.FlatAppearance.BorderColor = Color.FromArgb(48, 72, 87); }
                if (child is TextBox || child is ComboBox) child.BackColor = Shell;
                Theme(child);
            }
        }
    }
}
