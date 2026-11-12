package org.touchhle.android;
public final class TiltGestureSettingsTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        check(TiltGestureSettings.DEFAULT_MILLI_G==320*40/100);
        check(TiltGestureSettings.upgradeLegacy(32)==128);
        check(TiltGestureSettings.upgradeLegacy(45)==450);
        check(TiltGestureSettings.clamp(128)==128);
        check(TiltGestureSettings.clamp(0)==80);
        System.out.println("PASS: exact 60% default reduction; old default migrates; custom thresholds preserved; new default is not clamped back to 0.20G");
    }
}
