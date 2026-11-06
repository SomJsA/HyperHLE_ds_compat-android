package org.touchhle.android;
import android.content.Context;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import org.json.*;
final class OverlayTexts {
    static String[] ids, text, english;
    static void load(Context context) throws Exception {
        if (ids != null) { return; }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream in = context.getAssets().open("overlay/subtitles-original.json")) {
            byte[] buffer = new byte[16384]; int n;
            while ((n = in.read(buffer)) != -1) { bytes.write(buffer, 0, n); }
        }
        JSONArray entries = new JSONObject(new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_8)).getJSONArray("entries");
        ids = new String[entries.length()+1]; text = new String[ids.length]; english = new String[ids.length];
        for (int i=0;i<entries.length();i++) { ids[i+1]=entries.getJSONObject(i).getString("id"); text[i+1]=entries.getJSONObject(i).getString("text"); english[i+1]=entries.getJSONObject(i).optString("english",""); }
    }
    static String value(String id,boolean en) { for(int i=1;i<ids.length;i++){if(id.equals(ids[i]))return en?english[i]:text[i];}return id; }
}
