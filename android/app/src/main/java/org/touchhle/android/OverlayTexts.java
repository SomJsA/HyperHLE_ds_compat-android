package org.touchhle.android;
import android.content.Context;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import org.json.*;
final class OverlayTexts {
    static String[] ids, text, english;
    private static JSONObject languages;
    static void load(Context context) throws Exception {
        if (ids != null) { return; }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream in = context.getAssets().open("overlay/subtitles-original.json")) {
            byte[] buffer = new byte[16384]; int n;
            while ((n = in.read(buffer)) != -1) { bytes.write(buffer, 0, n); }
        }
        JSONArray entries = new JSONObject(new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_8)).getJSONArray("entries");
        try(InputStream in=context.getAssets().open("overlay/native-languages.json")) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] block=new byte[8192];int n;
            while((n=in.read(block))!=-1)out.write(block,0,n);
            languages=new JSONObject(new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8)).getJSONObject("languages");
        }
        ids = new String[entries.length()+1]; text = new String[ids.length]; english = new String[ids.length];
        for (int i=0;i<entries.length();i++) { ids[i+1]=entries.getJSONObject(i).getString("id"); text[i+1]=entries.getJSONObject(i).getString("text"); english[i+1]=entries.getJSONObject(i).optString("english",""); }
    }
    static String value(String id,boolean en) { for(int i=1;i<ids.length;i++){if(id.equals(ids[i]))return en && english[i] != null && !english[i].isEmpty() ? english[i] : text[i];}return id; }
    static String selected(String id) {
        if(HdTextLanguage.isChinese())return value(id,false);
        JSONObject bank=languages==null?null:languages.optJSONObject(HdTextLanguage.nativeLocale());
        return bank==null?value(id,true):bank.optString(id,value(id,true));
    }
    static String styled(String id) {
        String fallback=selected(id);
        return HdTextLanguage.isChinese()||HdTextLanguage.isEnglish()
                ?OverlayStyle.text(id,fallback,!HdTextLanguage.isChinese()):fallback;
    }
    static String value(android.content.Context context,String id) { return selected(id); }
}
