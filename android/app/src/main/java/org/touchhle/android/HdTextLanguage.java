package org.touchhle.android;

import android.content.Context;
import java.util.Locale;

/** Game text choice is independent of the launcher's Chinese/English UI. */
final class HdTextLanguage {
    static final String KEY="hd_text_language", ZH="zh", EN="en";
    private static final String[] CODES={ZH,EN,"fr","de","it","ko","es","ja"};
    private static final String[] BANKS={"CHS_CN","ENG_US","FRE_FR","GER_DE","ITA_IT","KOR_KR","SPA_ES","ENG_US"};
    private static final String[] LOCALES={"zh-Hans,zh","en-US,en","fr-FR,fr","de-DE,de","it-IT,it","ko-KR,ko","es-ES,es","en-US,en"};
    private static String selected=ZH;
    private HdTextLanguage() { }
    static String normalize(String value) { for(String code:CODES)if(code.equals(value))return code;return ZH; }
    static int indexOf(String value) { for(int i=0;i<CODES.length;i++)if(CODES[i].equals(value))return i;return 0; }
    static void initialize(Context context) { selected=normalize(context.getSharedPreferences(LauncherActivity.PREFS,Context.MODE_PRIVATE).getString(KEY,ZH)); }
    static String get(Context context) { initialize(context);return selected; }
    static void set(Context context,String value) {
        selected=normalize(value);
        context.getSharedPreferences(LauncherActivity.PREFS,Context.MODE_PRIVATE).edit().putString(KEY,selected).apply();
    }
    // The :game process receives an explicit snapshot, without writing its
    // cached preferences over the launcher's newer choices.
    static void forLaunch(String value) { if(value!=null)selected=normalize(value); }
    static String selected() { return selected; }
    static boolean isChinese() { return ZH.equals(selected); }
    static boolean isEnglish() { return EN.equals(selected)||"ja".equals(selected); }
    static boolean isEnglish(Context context) { initialize(context);return isEnglish(); }
    static int index(Context context) { return indexOf(get(context)); }
    static String codeAt(int index) { return CODES[Math.max(0,Math.min(CODES.length-1,index))]; }
    static String nativeLocale(String value) { return BANKS[indexOf(value)]; }
    static String nativeLocale() { return nativeLocale(selected); }
    static String preferredLanguages(Context context) { return preferredLanguages(get(context)); }
    static String preferredLanguages() { return preferredLanguages(selected); }
    static String preferredLanguages(String value) { return LOCALES[indexOf(value)]; }
    static Locale lineLocale() { return new Locale(CODES[indexOf(selected)]); }
    static String label(Context context,int index) {
        String[] labels={"中文 - 贴吧汉化组","English - Original","Français - Original","Deutsch - Original","Italiano - Original","한국어 - Original","Español - Original",
                LauncherLanguage.text("日本語 - 原包为占位文本，使用 English","日本語 - original placeholders; uses English")};
        return labels[Math.max(0,Math.min(labels.length-1,index))];
    }
    static String[] labels(Context context) { String[] result=new String[CODES.length];for(int i=0;i<result.length;i++)result[i]=label(context,i);return result; }
}
