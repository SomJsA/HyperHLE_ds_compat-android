using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Reflection;
using System.Windows.Forms;
namespace DeadSpaceBootstrap
{
    internal static class VerifyLauncher
    {
        [STAThread]
        private static void Main(string[] args)
        {
            string fixture = args[0], output = args[1]; Directory.CreateDirectory(output);
            var save = new SaveSettings(fixture); byte[] original = File.ReadAllBytes(fixture);
            for (int mask = 0; mask < 8; mask++)
            {
                byte[] edited = save.EditBytes((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0);
                if (edited.Length != original.Length) throw new Exception("Length changed");
                int changed = 0; for (int i = 0; i < original.Length; i++) if (edited[i] != original[i]) changed++;
                if (changed > 7) throw new Exception("Unexpected edits outside 3 flags and checksum");
                string path = Path.Combine(output, "fixture-" + mask + ".sb"); File.WriteAllBytes(path, edited);
                var reread = new SaveSettings(path);
                if (reread.Enabled("attack") != ((mask & 1) != 0) || reread.Enabled("defense") != ((mask & 2) != 0) || reread.Enabled("loot") != ((mask & 4) != 0)) throw new Exception("Toggle mismatch");
                if (Convert.ToBase64String(reread.EditBytes(save.Enabled("attack"), save.Enabled("defense"), save.Enabled("loot"))) != Convert.ToBase64String(original)) throw new Exception("Round trip lost bytes");
            }
            byte[] corrupt = (byte[])original.Clone(); corrupt[corrupt.Length / 2] ^= 1;
            string bad = Path.Combine(output, "corrupt.sb"); File.WriteAllBytes(bad, corrupt);
            bool rejected = false; try { new SaveSettings(bad); } catch (InvalidDataException) { rejected = true; }
            if (!rejected) throw new Exception("Corruption accepted");
            Application.EnableVisualStyles();
            using (var form = new LauncherForm())
            {
                // Never Show()/Activate(): renders only this constructed form.
                // Set managed visibility only: no ShowWindow and no foreground window.
                typeof(Control).GetMethod("SetState", BindingFlags.Instance | BindingFlags.NonPublic).Invoke(form, new object[] { 2, true });
                form.CreateControl();
                for (int i = 0; i < 6; i++)
                {
                    typeof(LauncherForm).GetMethod("SelectPage", BindingFlags.Instance | BindingFlags.NonPublic).Invoke(form, new object[] { i });
                    using (var bitmap = new Bitmap(form.Width, form.Height))
                    { form.DrawToBitmap(bitmap, new Rectangle(Point.Empty, bitmap.Size)); bitmap.Save(Path.Combine(output, "page-" + i + ".jpg"), ImageFormat.Jpeg); }
                }
            }
            Console.WriteLine("PASS: 8 DLC combinations, byte-exact reversal, corruption rejection, 6 offscreen pages; live saves untouched.");
        }
    }
}
