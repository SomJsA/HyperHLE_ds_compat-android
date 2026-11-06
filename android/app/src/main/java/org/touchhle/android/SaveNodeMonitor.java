package org.touchhle.android;

import android.app.ActivityManager;
import android.content.Context;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Explicitly opted-in, on-device capture. Never changes or restores game saves. */
final class SaveNodeMonitor {
    static final String KEY_ENABLED = "save_node_capture_enabled";
    static final String KEY_SESSION = "save_node_capture_session";
    static final String KEY_IMAGES = "save_node_capture_images";
    interface Picture { void request(File node, SaveNodeArchive archive); }
    interface Completion { void done(File node, Exception error); }
    private final File documents, bank;
    private final String session;
    private final Picture picture;
    private final Context context;
    private final HandlerThread thread = new HandlerThread("save-node-capture");
    private Handler worker;
    private SaveNodeArchive archive;
    private FileObserver rootObserver, varObserver;
    private int attempts;
    private volatile boolean closing;

    SaveNodeMonitor(Context context, String session, Picture picture) {
        documents = new File(dataBase(context), "touchHLE_sandbox/com.ea.deadspace.bv/Documents");
        bank = bank(context); this.session = session; this.picture = picture;
        this.context = context.getApplicationContext();
    }

    static File dataBase(Context context) {
        return RuntimeStorage.dataBase(context);
    }
    static File bank(Context context) { return new File(context.getFilesDir(), "save-node-bank"); }

    void start() {
        thread.start(); worker = new Handler(thread.getLooper());
        worker.post(() -> {
            try {
                File var = new File(documents, "var");
                if (!var.isDirectory() && !var.mkdirs()) throw new IOException("Cannot create save observation directory");
                archive = new SaveNodeArchive(documents, bank, session);
                File oldError = new File(bank, "initialization-error.txt");
                if (oldError.exists() && !oldError.delete()) throw new IOException("Could not reset capture initialization status");
                rootObserver = observe(documents, ""); varObserver = observe(var, "var/");
                rootObserver.startWatching(); varObserver.startWatching();
                if (hasPrimary(var)) schedule();
                worker.postDelayed(heartbeat, 7000);
            } catch (Exception error) {
                Log.e("SaveNodeMonitor", "Capture initialization failed", error);
                try {
                    if (bank.isDirectory() || bank.mkdirs()) try (FileOutputStream out = new FileOutputStream(new File(bank, "initialization-error.txt"))) {
                        out.write(error.toString().getBytes(StandardCharsets.UTF_8)); out.getFD().sync();
                    }
                } catch (IOException ignored) { }
                new Handler(android.os.Looper.getMainLooper()).post(() -> android.widget.Toast.makeText(this.context,
                        "存档节点采集未启动，请返回启动器查看记录。", android.widget.Toast.LENGTH_LONG).show());
            }
        });
    }

    private FileObserver observe(File directory, String prefix) {
        return new FileObserver(directory.getAbsolutePath(), FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO | FileObserver.DELETE) {
            @Override public void onEvent(int event, String path) {
                if (closing || path == null || path.contains("/") || path.contains("\\") || !path.toLowerCase(java.util.Locale.US).endsWith(".sb")) return;
                String relative = prefix + path;
                if ((event & (FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO)) != 0) {
                    try { archive.recordVersion(relative, (event & FileObserver.MOVED_TO) != 0 ? "moved_to" : "close_write"); }
                    catch (IOException error) { Log.w("SaveNodeMonitor", "Version temporarily unavailable", error); }
                }
                if (SaveNodeArchive.primary(relative)) schedule();
            }
        };
    }

    private synchronized void schedule() {
        if (worker == null || closing) return;
        attempts = 0; worker.removeCallbacks(capture); worker.postDelayed(capture, 750);
    }
    private final Runnable capture = new Runnable() {
        @Override public void run() {
            if (archive == null || closing) return;
            try {
                File node = archive.capture("automatic", "", false);
                if (node != null && picture != null) picture.request(node, archive);
                attempts = 0;
            } catch (IOException error) {
                try { archive.failure(error); } catch (IOException ignored) { }
                if (++attempts < 8 && !closing) worker.postDelayed(this, 600);
                Log.w("SaveNodeMonitor", "Capture deferred, attempt=" + attempts, error);
            }
        }
    };

    private final Runnable heartbeat = new Runnable() {
        @Override public void run() {
            if (closing || archive == null) return;
            try {
                if (hasPrimary(new File(documents, "var"))) {
                    File node = archive.capture("poll-fallback", "", false);
                    if (node != null && picture != null) picture.request(node, archive);
                }
            } catch (IOException ignored) { /* The event path owns retry/error accounting. */ }
            if (!closing) worker.postDelayed(this, 7000);
        }
    };

    void mark(String label, Completion completion) {
        if (worker == null || closing) { completion.done(null, new IOException("Capture is not running")); return; }
        worker.post(() -> {
            try {
                if (archive == null) throw new IOException("Capture initialization is not complete");
                File node = archive.capture("manual-label-on-saved-files", label, true);
                if (picture != null) picture.request(node, archive);
                completion.done(node, null);
            } catch (Exception error) { completion.done(null, error); }
        });
    }

    void finish() {
        if (worker == null) return;
        // Called on the dedicated game-exit helper, never on the UI thread.
        java.util.concurrent.CountDownLatch finished = new java.util.concurrent.CountDownLatch(1);
        worker.post(() -> {
            try {
                if (archive != null && hasPrimary(new File(documents, "var"))) archive.capture("game-exit", "", false);
            } catch (IOException error) { Log.w("SaveNodeMonitor", "Exit capture deferred", error); }
            finally { finished.countDown(); }
        });
        try { finished.await(1200, java.util.concurrent.TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        closing = true;
        if (rootObserver != null) rootObserver.stopWatching();
        if (varObserver != null) varObserver.stopWatching();
        thread.quitSafely();
    }

    static File export(Context context) throws IOException {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = manager == null ? null : manager.getRunningAppProcesses();
        if (processes == null) throw new IOException("无法确认游戏是否已退出");
        for (ActivityManager.RunningAppProcessInfo process : processes)
            if ((context.getPackageName() + ":game").equals(process.processName)) throw new IOException("请在游戏退出完成后导出节点库");
        File bank = bank(context);
        if (!new File(bank, "nodes").isDirectory()) throw new IOException("尚无节点记录，请先开启采集并正常游玩");
        File external = RuntimeStorage.requireCompatible(context);
        return SaveNodeArchive.exportZip(bank, new File(external, "save-node-exports"));
    }

    private static boolean hasPrimary(File var) {
        File[] files = var.listFiles();
        if (files != null) for (File file : files) if (SaveNodeArchive.primary(file.getName())) return true;
        return false;
    }
}
