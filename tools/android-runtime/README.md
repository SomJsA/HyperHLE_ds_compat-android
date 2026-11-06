# Android device regression

`InputPlan.java` is an optional shell-UID diagnostic helper, outside the APK.
Use only on an authorized device and an isolated test package/profile. It does
not inject Windows input or request root. Device Android versions must expose
UID-targeted input injection; `--probe` reports availability without sending
events. Execution rejects the unscoped fallback. Confirm the test activity is
focused and pass its actual UID; the dispatcher rejects a different target.

Compile against the installed `android.jar`, then run D8 with `--min-api 26`.
Push the resulting dex and a new plan to a task-owned `/data/local/tmp` folder.
Run with `adb -s SERIAL shell env CLASSPATH=DEX app_process /system/bin
org.touchhle.r15diag.InputPlan TARGET_UID PLAN`.

Plan rows use elapsed milliseconds and physical display pixels:

```text
0 down 0 400 900
50 down 1 1300 600
100 move 0 400 800
150 up 1 1300 600
200 up 0 400 800
1000 key N
```

IDs are 0–4. Plans are bounded to 120 seconds / 4096 rows; held pointers receive
CANCEL on failure or completion. Injection waits for UID-targeted dispatch
acceptance, not completion of the app callback; the latter can stretch a short
tap on a busy UI thread. Completion reports maximum scheduling lateness and
dispatch time so delayed plans cannot be mistaken for accurate gesture tests.
N is the runtime's read-only gauge/frame probe;
exclude these probes and screenshots from performance sample windows. A short
50 ms tap entered the original Store at both 1x and 3x on the authorized Adreno 610 device.
Long holds can invoke other original gameplay gestures; do not treat a long
hold as equivalent to a tap.

For exact runtime frame times, set `TOUCHHLE_DS_FRAME_TIMES` to a **new** private
CSV path before launching. It records actual `presentRenderbuffer` intervals,
including loading and suspend gaps, independently of SurfaceFlinger submissions.
Recording is off by default, refuses an existing file, stops after one million
rows or an I/O error, and flushes every 30 rows (up to 29 final rows can be lost
on force-stop). Use explicit steady-scene boundaries for percentiles. The Java
overlay's 1024-frame FPS history remains a rolling statistic, not a whole-run
capture. Neither counter proves generated frames or FSR3 activation.
