package org.touchhle.android;

import android.content.Context;
import android.app.ActivityManager;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/** Android storage/filesystem adapter around the pure-Java save importer. */
final class SaveArchiveImporter {
    private static final AtomicBoolean importing = new AtomicBoolean(false);
    private SaveArchiveImporter() {}

    static boolean isBusy() { return importing.get(); }

    static SaveArchiveCore.Result importZip(Context context, Uri uri) throws IOException {
        return stopped(context, base -> {
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IOException("Could not open ZIP");
                return SaveArchiveCore.importZip(input, base);
            }
        });
    }

    static void exportZip(Context context, Uri uri) throws IOException {
        stopped(context, base -> {
            try (OutputStream output = context.getContentResolver().openOutputStream(uri, "w")) {
                if (output == null) throw new IOException("Could not create ZIP");
                SaveArchiveCore.exportZip(base, output);
            }
            return null;
        });
    }

    static SaveScalarEditor.Slot readSlot(Context context, int slot) throws IOException {
        return stopped(context, base -> SaveScalarEditor.read(base, slot));
    }

    static File editSlot(Context context, int slot, long credits, long nodes, int rig, int changedFields) throws IOException {
        return stopped(context, base -> SaveScalarEditor.save(base, slot, credits, nodes, rig, changedFields));
    }

    static File[] backups(Context context) throws IOException {
        return stopped(context, base -> {
            File[] files = new File(base, "save-backups").listFiles((dir, name) -> name.endsWith(".zip"));
            if (files == null) return new File[0];
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            return files;
        });
    }

    static SaveArchiveCore.Result restoreBackup(Context context, String name) throws IOException {
        return stopped(context, base -> {
            File directory = new File(base, "save-backups").getCanonicalFile();
            File file = new File(directory, name);
            if (!name.endsWith(".zip") || !file.getCanonicalFile().equals(new File(directory, file.getName())) || !file.isFile())
                throw new IOException("Invalid backup path");
            try (InputStream input = new FileInputStream(file)) { return SaveArchiveCore.importZip(input, base); }
        });
    }

    private interface Work<T> { T run(File base) throws IOException; }
    private static <T> T stopped(Context context, Work<T> work) throws IOException {
        if (!importing.compareAndSet(false, true)) throw new IOException("Another save operation is running");
        try {
        File base = RuntimeStorage.requireCompatible(context);
        if (new File(base, ".save-lab-adb-lock").exists()) throw new IOException("ADB save operation is running");
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = manager == null ? null : manager.getRunningAppProcesses();
        if (processes == null) throw new IOException("Could not verify game process state");
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if ((context.getPackageName() + ":game").equals(process.processName)) {
                throw new IOException("请先退出游戏，再导入、导出或编辑存档");
            }
        }
        return work.run(base);
        } finally {
            importing.set(false);
        }
    }
}
