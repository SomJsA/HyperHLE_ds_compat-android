package org.touchhle.android;

/** OrientationEventListener measures clockwise device rotation from natural orientation. */
final class LandscapeRotation {
    private LandscapeRotation() { }

    static int side(int degrees, boolean naturalLandscape) {
        if (degrees < 0 || degrees >= 360) return -1;
        // Ignore portrait, diagonal and flat/unknown readings. Sensor angles run
        // opposite to Surface rotation: 90 degrees is reverse landscape.
        int primary = naturalLandscape ? 0 : 270;
        int reverse = naturalLandscape ? 180 : 90;
        if (distance(degrees, primary) <= 25) return 0;
        if (distance(degrees, reverse) <= 25) return 1;
        return -1;
    }

    private static int distance(int a, int b) {
        int delta = Math.abs(a - b);
        return Math.min(delta, 360 - delta);
    }
}
