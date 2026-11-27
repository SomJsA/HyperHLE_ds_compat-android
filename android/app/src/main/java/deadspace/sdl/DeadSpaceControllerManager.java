/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.libsdl.app;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Presents Android controller ranges in SDL's fixed order, for both system transports. */
public final class DeadSpaceControllerManager extends SDLControllerManager {
    private static final int LT = 4;
    private static final int RT = 5;
    private static final int FACE_BUTTONS = 0x0f;

    private DeadSpaceControllerManager() { }

    public static void install() {
        mJoystickHandler = new FixedAxes();
    }

    public static boolean handleTriggerKey(KeyEvent event) {
        int key = event.getKeyCode();
        if ((key != KeyEvent.KEYCODE_BUTTON_L2 && key != KeyEvent.KEYCODE_BUTTON_R2)
                || !(mJoystickHandler instanceof FixedAxes)) return false;
        Pad pad = ((FixedAxes) mJoystickHandler).pads.get(event.getDeviceId());
        if (pad == null) return false;
        int axis = key == KeyEvent.KEYCODE_BUTTON_L2 ? LT : RT;
        if (pad.axes[axis] == null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                pad.prime();
                onNativeJoy(pad.id, axis, 1.0f);
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                pad.prime();
                onNativeJoy(pad.id, axis, -1.0f);
            }
        }
        // Analogue controllers may also report a digital event for the same trigger.
        return true;
    }

    private static final class FixedAxes extends SDLJoystickHandler_API19 {
        private final Map<Integer, Pad> pads = new ConcurrentHashMap<>();

        @Override public void pollInputDevices() {
            for (int id : InputDevice.getDeviceIds()) {
                InputDevice device = InputDevice.getDevice(id);
                if (!isGamepad(device) || pads.containsKey(id)) continue;
                Pad pad = new Pad(device);
                int buttons = getButtonMask(device);
                if (device.supportsSource(InputDevice.SOURCE_GAMEPAD)) buttons |= FACE_BUTTONS;
                if (nativeAddJoystick(id, device.getName(), device.getDescriptor(),
                        device.getVendorId(), device.getProductId(), false, buttons,
                        pad.axes.length, pad.hasHat() ? 1 : 0, 0) >= 0) {
                    pads.put(id, pad);
                }
            }
            for (int id : pads.keySet()) {
                if (!isGamepad(InputDevice.getDevice(id))) {
                    pads.remove(id);
                    nativeRemoveJoystick(id);
                }
            }
        }

        @Override public boolean handleMotionEvent(MotionEvent event) {
            Pad pad = pads.get(event.getDeviceId());
            if (pad == null || event.getActionMasked() != MotionEvent.ACTION_MOVE) return false;
            pad.prime();
            for (int axis = 0; axis < pad.axes.length; axis++) {
                InputDevice.MotionRange range = pad.axes[axis];
                if (range == null) continue;
                float unit = (event.getAxisValue(range.getAxis()) - range.getMin()) / range.getRange();
                onNativeJoy(pad.id, axis, Math.max(-1.0f, Math.min(1.0f, unit * 2.0f - 1.0f)));
            }
            if (pad.hasHat()) {
                int x = pad.hatX == null ? 0 : Math.round(event.getAxisValue(MotionEvent.AXIS_HAT_X));
                int y = pad.hatY == null ? 0 : Math.round(event.getAxisValue(MotionEvent.AXIS_HAT_Y));
                onNativeHat(pad.id, 0, x, y);
            }
            return true;
        }

        private static boolean isGamepad(InputDevice device) {
            return device != null && !device.isVirtual()
                    && (device.supportsSource(InputDevice.SOURCE_GAMEPAD)
                        || device.supportsSource(InputDevice.SOURCE_JOYSTICK));
        }
    }

    private static final class Pad {
        private final int id;
        private final InputDevice.MotionRange[] axes;
        private final InputDevice.MotionRange hatX;
        private final InputDevice.MotionRange hatY;
        private boolean primed;

        Pad(InputDevice device) {
            id = device.getId();
            InputDevice.MotionRange rightX = range(device, MotionEvent.AXIS_Z);
            InputDevice.MotionRange rightY = range(device, MotionEvent.AXIS_RZ);
            if (rightX == null || rightY == null) {
                rightX = range(device, MotionEvent.AXIS_RX);
                rightY = range(device, MotionEvent.AXIS_RY);
            }
            axes = new InputDevice.MotionRange[]{
                    range(device, MotionEvent.AXIS_X), range(device, MotionEvent.AXIS_Y),
                    rightX, rightY,
                    triggerRange(device, MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE),
                    triggerRange(device, MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS)
            };
            hatX = range(device, MotionEvent.AXIS_HAT_X);
            hatY = range(device, MotionEvent.AXIS_HAT_Y);
        }

        boolean hasHat() {
            return hatX != null || hatY != null;
        }

        // SDL calibrates axes using the first sample. Establish the released
        // values before forwarding the first press or stick movement.
        void prime() {
            if (primed) return;
            for (int axis = 0; axis < axes.length; axis++) {
                onNativeJoy(id, axis, axis < LT ? 0.0f : -1.0f);
            }
            primed = true;
        }

        private static InputDevice.MotionRange range(InputDevice device, int axis) {
            InputDevice.MotionRange range = device.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK);
            if (range != null && range.getRange() > 0.0f) return range;
            for (InputDevice.MotionRange candidate : device.getMotionRanges()) {
                if (candidate.getAxis() == axis && candidate.getRange() > 0.0f
                        && (candidate.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                    return candidate;
                }
            }
            return null;
        }

        private static InputDevice.MotionRange triggerRange(InputDevice device, int axis, int fallback) {
            InputDevice.MotionRange range = range(device, axis);
            return range != null ? range : range(device, fallback);
        }
    }
}
