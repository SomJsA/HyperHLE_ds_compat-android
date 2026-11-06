using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text;

namespace DeadSpaceBootstrap
{
    // Edit only named unlock bytes in settings.sb. Do not interpret OHDR
    // references or checkpoint vector lengths as DLC bitmasks.
    internal sealed class SaveSettings
    {
        private readonly byte[] original;
        private readonly string path;
        private readonly Dictionary<string, int> fields = new Dictionary<string, int>();
        private readonly Dictionary<string, int[]> sections = new Dictionary<string, int[]>();
        private readonly List<string> strings = new List<string>();
        private int data;
        private int dataLength;
        private int ohdr;
        private int ohdrLength;

        internal SaveSettings(string file)
        {
            path = Path.GetFullPath(file);
            original = File.ReadAllBytes(path);
            Require(original.Length >= 8 && Encoding.ASCII.GetString(original, 0, 4) == "SBIN" && U32(4) == 2, "不是支持的 SBIN v2 存档");
            int cursor = 8;
            while (cursor < original.Length)
            {
                Require(cursor + 12 <= original.Length, "区段头不完整");
                string tag = Encoding.ASCII.GetString(original, cursor, 4);
                uint length = U32(cursor + 4);
                Require(length <= int.MaxValue && length <= original.Length - cursor - 12, "区段长度越界");
                Require(!sections.ContainsKey(tag), "重复区段");
                sections.Add(tag, new[] { cursor, cursor + 12, (int)length });
                Require(Hash(original, cursor + 12, (int)length) == U32(cursor + 8), "区段校验失败：" + tag);
                cursor = checked((cursor + 12 + (int)length + 3) & ~3);
            }
            Require(cursor == original.Length, "文件尾不完整");
            int[] ds = Section("DATA"), os = Section("OHDR"), ss = Section("STRS");
            data = ds[1]; dataLength = ds[2]; ohdr = os[1]; ohdrLength = os[2];
            Require(ohdrLength % 4 == 0 && ss[2] >= 4, "无效对象表");
            int pos = ss[1] + 4, end = ss[1] + ss[2];
            uint count = U32(ss[1]); Require(count < 65536, "字符串表过大");
            for (uint i = 0; i < count; i++)
            {
                Require(pos + 4 <= end, "字符串长度缺失"); uint n = U32(pos); pos += 4;
                Require(n <= end - pos, "字符串越界"); strings.Add(new UTF8Encoding(false, true).GetString(original, pos, (int)n)); pos += (int)n;
            }
            Require(pos == end, "字符串表尾部异常");
            int moduleRef = -1;
            int root = data;
            while (root + 8 <= data + dataLength && (U32(root) & 65535) == 0) root += 4;
            uint h = U32(root); int size = (int)(h >> 16), records = (int)(h & 65535);
            Require(size >= 4 && size <= dataLength && records < 128, "顶层结构不受支持");
            for (int p = root + 4, i = 0; i < records; i++)
            {
                Require(p + 8 <= root + size, "顶层字段越界");
                int type = U16(p + 2);
                int valueAt = data + checked((int)U32(p + 4)) + 4;
                int width = type == 9 || type == 13 ? 2 : 4;
                Require(valueAt >= p + 8 && valueAt <= p + 10 && valueAt + width <= root + size, "顶层偏移不匹配");
                string name = Name(U16(p));
                if (name == "dlc_modules") { Require(type == 15, "dlc_modules 不是对象引用"); moduleRef = checked((int)U32(valueAt)); }
                Require(type == 5 || type == 6 || type == 9 || type == 10 || type == 13 || type == 15, "未知顶层类型");
                p = valueAt + width;
            }
            Require(moduleRef >= 0, "没有 dlc_modules 字段");
            int modules = Object(moduleRef);
            foreach (var record in ObjectFields(modules))
            {
                if (record.Key != "attack" && record.Key != "defense" && record.Key != "loot") continue;
                int p = record.Value; Require(U16(p + 2) == 15, "插件不是对象引用");
                int value = modules + checked((int)U32(p + 4));
                int child = Object(checked((int)U32(value)));
                foreach (var unlocked in ObjectFields(child))
                {
                    if (unlocked.Key != "unlocked") continue;
                    Require(U16(unlocked.Value + 2) == 9, "unlocked 类型异常");
                    int at = child + checked((int)U32(unlocked.Value + 4));
                    Require(at < child + (U32(child) >> 16) && original[at] <= 1, "unlocked 不是布尔值");
                    fields.Add(record.Key, at);
                }
            }
            Require(fields.Count == 3, "存档缺少 attack / defense / loot 插件结构");
        }

        private Dictionary<string, int> ObjectFields(int start)
        {
            uint header = U32(start); int count = (int)(header & 65535), size = (int)(header >> 16);
            Require(count < 128 && size >= 4 && start + size <= data + dataLength, "对象越界");
            var result = new Dictionary<string, int>(); int p = start + 4;
            for (int i = 0; i < count; i++)
            {
                Require(p + 8 <= start + size, "字段头越界");
                int type = U16(p + 2); int value = start + checked((int)U32(p + 4));
                Require(value >= p + 8 && value < start + size, "字段偏移越界");
                result.Add(Name(U16(p)), p);
                int width = type == 9 || type == 13 ? 2 : 4;
                p = value + width;
            }
            return result;
        }

        private int Object(int index)
        {
            Require(index >= 0 && index < ohdrLength / 4, "对象索引越界"); uint entry = U32(ohdr + index * 4);
            Require((entry & 7) == 1 && (entry >> 3) + 4 <= dataLength, "预期为对象"); return data + (int)(entry >> 3);
        }
        private string Name(int index) { Require(index > 0 && index <= strings.Count, "名称索引越界"); return strings[index - 1]; }
        private int[] Section(string tag) { Require(sections.ContainsKey(tag), "缺少区段 " + tag); return sections[tag]; }
        private ushort U16(int at) { Require(at >= 0 && at + 2 <= original.Length, "读取越界"); return BitConverter.ToUInt16(original, at); }
        private uint U32(int at) { Require(at >= 0 && at + 4 <= original.Length, "读取越界"); return BitConverter.ToUInt32(original, at); }
        private static void Require(bool ok, string message) { if (!ok) throw new InvalidDataException(message); }
        private static uint Hash(byte[] bytes, int start, int length) { uint h = 2166136261; for (int i = start; i < start + length; i++) h = unchecked(h * 16777619) ^ bytes[i]; return h; }
        internal bool Enabled(string name) { return original[fields[name]] == 1; }

        internal byte[] EditBytes(bool attack, bool defense, bool loot)
        {
            byte[] edited = (byte[])original.Clone();
            edited[fields["attack"]] = (byte)(attack ? 1 : 0);
            edited[fields["defense"]] = (byte)(defense ? 1 : 0);
            edited[fields["loot"]] = (byte)(loot ? 1 : 0);
            int[] section = Section("DATA");
            Array.Copy(BitConverter.GetBytes(Hash(edited, data, dataLength)), 0, edited, section[0] + 8, 4);
            return edited;
        }

        internal string Write(bool attack, bool defense, bool loot)
        {
            Require(Process.GetProcessesByName("touchHLE").Length == 0, "请先退出游戏，再修改存档");
            byte[] now = File.ReadAllBytes(path);
            Require(Convert.ToBase64String(now) == Convert.ToBase64String(original), "存档已改变，请重新读取");
            byte[] edited = EditBytes(attack, defense, loot);
            string token = DateTime.UtcNow.ToString("yyyyMMdd-HHmmss-fffffff") + "-" + Guid.NewGuid().ToString("N");
            string temp = path + "." + token + ".tmp", backup = path + "." + token + ".bak";
            try
            {
                File.WriteAllBytes(temp, edited);
                var check = new SaveSettings(temp);
                Require(check.Enabled("attack") == attack && check.Enabled("defense") == defense && check.Enabled("loot") == loot, "写入验证失败");
                Require(Process.GetProcessesByName("touchHLE").Length == 0, "游戏已启动，取消修改");
                Require(Convert.ToBase64String(File.ReadAllBytes(path)) == Convert.ToBase64String(original), "存档已改变，取消修改");
                File.Replace(temp, path, backup);
                return backup;
            }
            finally { if (File.Exists(temp)) File.Delete(temp); }
        }
    }
}
