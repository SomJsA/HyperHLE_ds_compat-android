package org.touchhle.android;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import java.util.IdentityHashMap;

/** The user's exact faces; logical heights use the actual platform metrics. */
final class RoleFonts {
    final Typeface title;
    final Typeface story;
    private static RoleFonts shared;
    private static final IdentityHashMap<Typeface, Float> scales = new IdentityHashMap<>();

    private RoleFonts(Context context) {
        title = Typeface.createFromAsset(context.getAssets(), "overlay/TitleChinese.ttf");
        story = Typeface.createFromAsset(context.getAssets(), "overlay/StoryChinese.ttf");
        Log.i("touchHLE", "[DSfont-role] title=MStiffHei heightToEm=" + heightToEm(title)
                + " story=SweiShearSansCJKsc-Regular heightToEm=" + heightToEm(story));
    }

    static synchronized RoleFonts get(Context context) {
        if (shared == null) { shared = new RoleFonts(context); }
        return shared;
    }

    static synchronized float heightToEm(Typeface face) {
        Float cached = scales.get(face);
        if (cached != null) { return cached; }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(face);
        paint.setTextSize(1000f);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float extent = metrics.descent - metrics.ascent;
        if (Float.isNaN(extent) || Float.isInfinite(extent) || extent <= 0) {
            throw new IllegalStateException("Invalid role font height");
        }
        float scale = 1000f / extent;
        if (scales.size() >= 16) { scales.clear(); }
        scales.put(face, scale);
        return scale;
    }

    static void setTextHeight(Paint paint, float height) {
        paint.setTextSize(height * heightToEm(paint.getTypeface()));
    }
}
