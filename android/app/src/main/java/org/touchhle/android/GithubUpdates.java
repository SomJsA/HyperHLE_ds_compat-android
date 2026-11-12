package org.touchhle.android;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

/** Release metadata only. Never downloads binaries or embeds credentials. */
final class GithubUpdates {
    static final String REPOSITORY="SomJsA/HyperHLE_ds_compat-android";
    static final String PROJECT_URL="https://github.com/"+REPOSITORY;
    static final String API_URL="https://api.github.com/repos/"+REPOSITORY+"/releases/latest";
    static final String CURRENT="20261112-r4";
    static final class Result { final String state,tag; Result(String s,String t){state=s;tag=t;} }
    private static final ArrayList<Binding> bindings=new ArrayList<>();
    private static boolean started,checking;
    private static Result result;
    private static final class Binding {
        final WeakReference<TextView> status; final WeakReference<Button> button;
        Binding(TextView s,Button b){status=new WeakReference<>(s);button=new WeakReference<>(b);}
    }
    static Result interpret(int code,String body){
        if(code==401||code==403||code==404)return new Result("unavailable","");
        if(code!=200)return new Result("network","");
        try {
            JSONObject json=new JSONObject(body);Object value=json.opt("tag_name");
            if(!(value instanceof String)||json.optBoolean("draft",false))return new Result("invalid","");
            String tag=(String)value;
            if(tag.isEmpty()||tag.length()>100||tag.indexOf('\n')>=0||tag.indexOf('\r')>=0)return new Result("invalid","");
            String normalized=tag.replaceFirst("^[vV]","");
            if(normalized.equalsIgnoreCase(CURRENT))return new Result("current",tag);
            ArrayList<String>a=numbers(CURRENT),b=numbers(tag);
            if(a.size()==b.size()&&!a.isEmpty()&&a.get(0).length()==b.get(0).length()){
                for(int i=0;i<a.size();i++){
                    long first=Long.parseLong(a.get(i)),next=Long.parseLong(b.get(i));
                    if(next>first)return new Result("new",tag);
                    if(next<first)return new Result("older",tag);
                }
            }
            return new Result("available",tag);
        }catch(Exception e){return new Result("invalid","");}
    }
    private static ArrayList<String> numbers(String text){
        ArrayList<String> values=new ArrayList<>();Matcher m=Pattern.compile("\\d+").matcher(text);
        while(m.find())values.add(m.group());return values;
    }
    private static Result fetch(){
        HttpURLConnection connection=null;
        try{
            connection=(HttpURLConnection)new URL(API_URL).openConnection();
            connection.setConnectTimeout(7000);connection.setReadTimeout(7000);connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept","application/vnd.github+json");
            connection.setRequestProperty("User-Agent","DeadSpace-Mobile-Android/"+CURRENT);
            int code=connection.getResponseCode();if(code!=200)return interpret(code,"");
            try(InputStream input=connection.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int count;
                while((count=input.read(buffer))!=-1){if(bytes.size()+count>512*1024)return new Result("invalid","");bytes.write(buffer,0,count);}
                return interpret(code,new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            }
        }catch(Exception e){return new Result("network","");}
        finally{if(connection!=null)connection.disconnect();}
    }
    static void add(Activity activity,LinearLayout parent){
        TextView title=new TextView(activity);title.setText(LauncherLanguage.english()?"GitHub updates · "+CURRENT:"GitHub 更新 · "+CURRENT);title.setTextColor(Color.rgb(102,224,232));title.setTextSize(15);parent.addView(title);
        TextView status=new TextView(activity);status.setTextSize(13);status.setTextColor(Color.rgb(210,230,235));status.setPadding(0,8,0,8);parent.addView(status);
        Button button=new Button(activity);button.setAllCaps(false);button.setText(LauncherLanguage.english()?"Check for updates":"检查更新");parent.addView(button);
        TextView link=new TextView(activity);link.setText(PROJECT_URL+"/releases/latest");link.setTextColor(Color.rgb(102,224,232));link.setAutoLinkMask(android.text.util.Linkify.WEB_URLS);link.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());link.setPadding(0,6,0,20);parent.addView(link);
        bindings.add(new Binding(status,button));render();button.setOnClickListener(v->check());
        if(!started){started=true;check();}
    }
    private static void check(){
        if(checking)return;checking=true;render();
        new Thread(()->{Result next=fetch();new Handler(Looper.getMainLooper()).post(()->{result=next;checking=false;render();});},"github-release-check").start();
    }
    private static void render(){
        boolean en=LauncherLanguage.english();String state=checking?"checking":result==null?"idle":result.state,tag=result==null?"":result.tag;
        String text;
        switch(state){
            case "checking":text=en?"Checking GitHub…":"正在检查 GitHub…";break;
            case "unavailable":text=en?"Cannot check: repository is private, access is missing, or no Release is published.":"无法检查：仓库未公开、缺少权限或尚无 Release。";break;
            case "current":text=en?"This build matches the latest Release.":"当前版本与最新 Release 一致。";break;
            case "new":text=en?"Update available: "+tag+". Open the release page.":"发现更新："+tag+"。可打开发布页查看。";break;
            case "available":text=en?"GitHub release found: "+tag+". Check the version on the release page.":"发现 GitHub 发布："+tag+"；请在发布页核对版本。";break;
            case "older":text=en?"The latest GitHub release is older than this build: "+tag:"GitHub 最新发布早于当前构建："+tag;break;
            case "network":text=en?"Cannot connect to GitHub. Try again later.":"暂时无法连接 GitHub，请稍后重试。";break;
            case "invalid":text=en?"Release details unavailable. Open the releases page.":"发布信息不可用，请打开发布页查看。";break;
            default:text=en?"Updates have not been checked.":"尚未检查更新。";
        }
        for(int i=bindings.size()-1;i>=0;i--){Binding b=bindings.get(i);TextView s=b.status.get();Button button=b.button.get();if(s==null||button==null){bindings.remove(i);continue;}s.setText(text);button.setEnabled(!checking);}
    }
}
