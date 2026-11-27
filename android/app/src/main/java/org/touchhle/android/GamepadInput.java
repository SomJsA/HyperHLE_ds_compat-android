/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.touchhle.android;

import android.view.InputDevice;
import android.view.KeyEvent;

final class GamepadInput {
    private GamepadInput() { }

    static boolean isGamepad(InputDevice device) {
        return device != null && !device.isVirtual()
                && (device.supportsSource(InputDevice.SOURCE_GAMEPAD)
                    || device.supportsSource(InputDevice.SOURCE_JOYSTICK));
    }

    static int button(KeyEvent event) {
        if (!isGamepad(event.getDevice())) return -1;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BUTTON_A: return 0;
            case KeyEvent.KEYCODE_BUTTON_B: return 1;
            case KeyEvent.KEYCODE_BUTTON_X: return 2;
            case KeyEvent.KEYCODE_BUTTON_Y: return 3;
            case KeyEvent.KEYCODE_BUTTON_L1: return 4;
            case KeyEvent.KEYCODE_BUTTON_R1: return 5;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_BUTTON_START: return 6;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_BUTTON_SELECT: return 7;
            case KeyEvent.KEYCODE_BUTTON_THUMBL: return 8;
            case KeyEvent.KEYCODE_BUTTON_THUMBR: return 9;
            default: return -1;
        }
    }
}
