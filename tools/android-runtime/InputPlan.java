package org.touchhle.r15diag;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Finite, shell-UID touch plans for isolated Android device regression. */
public final class InputPlan {
    private final Object manager;
    private final Method inject;
    private final boolean targeted;
    private final int uid;
    private final LinkedHashMap<Integer, float[]> pointers = new LinkedHashMap<>();
    private long downTime;
    private long maxDispatchMillis;

    private InputPlan(int targetUid) throws Exception {
        uid = targetUid;
        Object selected = null; Method method = null; boolean target = false;
        for (String name : new String[] { "android.hardware.input.InputManagerGlobal", "android.hardware.input.InputManager" }) {
            try {
                Class<?> type = Class.forName(name);
                Object instance = type.getDeclaredMethod("getInstance").invoke(null);
                for (Method candidate : type.getDeclaredMethods()) {
                    Class<?>[] args = candidate.getParameterTypes();
                    if (candidate.getName().contains("injectInputEvent") && args.length == 3
                            && args[0] == InputEvent.class && args[1] == int.class && args[2] == int.class) {
                        selected = instance; method = candidate; target = true; break;
                    }
                }
                if (method != null) break;
                if (selected == null) {
                    for (Method candidate : type.getDeclaredMethods()) {
                        Class<?>[] args = candidate.getParameterTypes();
                        if (candidate.getName().equals("injectInputEvent") && args.length == 2
                                && args[0] == InputEvent.class && args[1] == int.class) {
                            selected = instance; method = candidate;
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) { }
        }
        if (method == null) throw new IllegalStateException("No shell input injection API");
        if (targetUid >= 0 && !target) throw new IllegalStateException("UID-targeted injection is required");
        manager = selected; inject = method; targeted = target; inject.setAccessible(true);
        System.out.println("injector=" + inject + " targeted=" + targeted + " uid=" + uid);
    }

    private void send(InputEvent event) throws Exception {
        // WAIT_FOR_RESULT acknowledges the targeted dispatch. WAIT_FOR_FINISH
        // can stretch a short tap while waiting for a busy app's callback.
        long start = SystemClock.uptimeMillis();
        Object result = targeted ? inject.invoke(manager, event, 1, uid) : inject.invoke(manager, event, 1);
        maxDispatchMillis = Math.max(maxDispatchMillis, SystemClock.uptimeMillis() - start);
        if (!Boolean.TRUE.equals(result)) throw new IllegalStateException("Input rejected by dispatcher");
    }

    private void touch(int action, int index) throws Exception {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[pointers.size()];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[pointers.size()];
        int i = 0;
        for (Integer id : pointers.keySet()) {
            properties[i] = new MotionEvent.PointerProperties(); properties[i].id = id;
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords(); coords[i].x = pointers.get(id)[0]; coords[i].y = pointers.get(id)[1];
            coords[i].pressure = 1; coords[i].size = .05f; i++;
        }
        MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action | (index << 8),
                pointers.size(), properties, coords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        try { send(event); } finally { event.recycle(); }
    }

    private void execute(String[] fields) throws Exception {
        if (fields[1].equals("key")) {
            if (fields.length != 3 || !fields[2].equals("N")) throw new IllegalArgumentException("Only read-only N probe key is supported");
            long now = SystemClock.uptimeMillis();
            send(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_N, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD));
            send(new KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_N, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD));
            return;
        }
        if (fields.length != 5) throw new IllegalArgumentException("Expected ms down/move/up id x y");
        int id = Integer.parseInt(fields[2]); float x = Float.parseFloat(fields[3]), y = Float.parseFloat(fields[4]);
        if (id < 0 || id > 4 || !Float.isFinite(x) || !Float.isFinite(y) || x < 0 || x > 3840 || y < 0 || y > 3840)
            throw new IllegalArgumentException("Touch outside bounded device coordinates");
        switch (fields[1]) {
            case "down":
                if (pointers.containsKey(id)) throw new IllegalArgumentException("Pointer already down");
                if (pointers.isEmpty()) downTime = SystemClock.uptimeMillis();
                pointers.put(id, new float[] {x,y}); touch(pointers.size() == 1 ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_POINTER_DOWN, pointers.size()-1); break;
            case "move":
                if (!pointers.containsKey(id)) throw new IllegalArgumentException("Pointer not down");
                pointers.put(id, new float[] {x,y}); touch(MotionEvent.ACTION_MOVE, 0); break;
            case "up":
                if (!pointers.containsKey(id)) throw new IllegalArgumentException("Pointer not down");
                pointers.put(id, new float[] {x,y}); int index = new ArrayList<>(pointers.keySet()).indexOf(id);
                touch(pointers.size() == 1 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_POINTER_UP, index); pointers.remove(id); break;
            default: throw new IllegalArgumentException("Unsupported touch action");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--probe")) { new InputPlan(-1); return; }
        if (args.length != 2) throw new IllegalArgumentException("InputPlan targetUid plan.txt");
        InputPlan instance = new InputPlan(Integer.parseInt(args[0]));
        List<String[]> rows = new ArrayList<>(); long previous = -1;
        try (BufferedReader reader = new BufferedReader(new FileReader(args[1]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
                String[] fields = line.trim().split("\\s+"); long at = Long.parseLong(fields[0]);
                if (at < previous || at > 120000 || rows.size() >= 4096) throw new IllegalArgumentException("Unbounded/unordered plan");
                rows.add(fields); previous = at;
            }
        }
        long start = SystemClock.uptimeMillis(), maxLateness = 0;
        try {
            for (String[] row : rows) {
                long delay = start + Long.parseLong(row[0]) - SystemClock.uptimeMillis();
                if (delay > 0) SystemClock.sleep(delay);
                maxLateness = Math.max(maxLateness, SystemClock.uptimeMillis() - start - Long.parseLong(row[0]));
                instance.execute(row);
            }
        } finally {
            if (!instance.pointers.isEmpty()) { instance.touch(MotionEvent.ACTION_CANCEL, 0); instance.pointers.clear(); }
        }
        System.out.println("completed events=" + rows.size() + " max_lateness_ms=" + maxLateness
                + " max_dispatch_ms=" + instance.maxDispatchMillis);
    }
}
