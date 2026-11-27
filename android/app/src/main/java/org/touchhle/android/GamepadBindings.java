/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.touchhle.android;

import java.util.Locale;

/** The action IDs and button tokens are shared with the PC binding file. */
final class GamepadBindings {
    static final String KEY = "gamepad_bindings";
    static final String[] ACTIONS = {
            "interact", "kinesis", "reload", "stasis", "melee", "primary", "cursor", "pause"
    };
    static final String[] BUTTONS = {
            "A", "B", "X", "Y", "LB", "RB", "START", "BACK", "LS", "RS"
    };

    private GamepadBindings() { }

    static int[] defaults() {
        int[] mapping = new int[ACTIONS.length];
        for (int i = 0; i < mapping.length; i++) mapping[i] = i;
        return mapping;
    }

    static int[] parse(String text) {
        int[] mapping = defaults();
        if (text == null) return mapping;
        int actionsSeen = 0;
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int equals = line.indexOf('=');
            if (equals < 1) return defaults();
            int action = indexOf(ACTIONS, line.substring(0, equals).trim());
            int button = indexOf(BUTTONS, line.substring(equals + 1).trim().toUpperCase(Locale.ROOT));
            if (action < 0 || button < 0 || (actionsSeen & (1 << action)) != 0) return defaults();
            actionsSeen |= 1 << action;
            mapping[action] = button;
        }
        int buttonsSeen = 0;
        for (int button : mapping) {
            if ((buttonsSeen & (1 << button)) != 0) return defaults();
            buttonsSeen |= 1 << button;
        }
        return mapping;
    }

    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i;
        return -1;
    }

    static String serialize(int[] mapping) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < ACTIONS.length; i++) {
            text.append(ACTIONS[i]).append('=').append(BUTTONS[mapping[i]]).append('\n');
        }
        return text.toString();
    }

    /** Reassigning an occupied button swaps its previous action in one step. */
    static void assign(int[] mapping, int action, int button) {
        for (int other = 0; other < mapping.length; other++) {
            if (other != action && mapping[other] == button) {
                mapping[other] = mapping[action];
                break;
            }
        }
        mapping[action] = button;
    }
}
