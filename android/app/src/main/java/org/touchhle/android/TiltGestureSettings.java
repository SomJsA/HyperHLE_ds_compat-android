package org.touchhle.android;

/** Milligravity keeps the new 60% reduction exact; legacy hundredths remain readable. */
final class TiltGestureSettings {
    static final int DEFAULT_MILLI_G = 128;
    static final int MIN_MILLI_G = 80, MAX_MILLI_G = 800;
    static int clamp(int value) { return Math.max(MIN_MILLI_G, Math.min(MAX_MILLI_G,value)); }
    static int upgradeLegacy(int hundredths) {
        return hundredths == 32 ? DEFAULT_MILLI_G : clamp(hundredths * 10);
    }
}
