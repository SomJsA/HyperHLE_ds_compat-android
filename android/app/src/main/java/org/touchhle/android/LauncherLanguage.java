package org.touchhle.android;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.json.JSONObject;

/** UI language is independent of the game's original language and save data. */
final class LauncherLanguage {
    private static final String KEY = "ui_language";
    private static boolean english;
    private static JSONObject translations = new JSONObject();
    static String defaultLanguage(Locale locale) {
        return "zh".equalsIgnoreCase(locale.getLanguage()) ? "zh" : "en";
    }
    static void initialize(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(LauncherActivity.PREFS, Context.MODE_PRIVATE);
        String selected = prefs.getString(KEY, "");
        if (!"zh".equals(selected) && !"en".equals(selected)) {
            selected = defaultLanguage(context.getResources().getConfiguration().locale);
            prefs.edit().putString(KEY, selected).apply();
        }
        english = "en".equals(selected);
        try (InputStream in = context.getResources().openRawResource(R.raw.launcher_en)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            translations = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception error) { throw new IllegalStateException("Launcher translations unavailable", error); }
    }
    static boolean english() { return english; }
    static void toggle(Context context) {
        english = !english;
        context.getSharedPreferences(LauncherActivity.PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, english ? "en" : "zh").apply();
    }
    static String text(String source) {
        return english ? translations.optString(source, source) : source;
    }
}
