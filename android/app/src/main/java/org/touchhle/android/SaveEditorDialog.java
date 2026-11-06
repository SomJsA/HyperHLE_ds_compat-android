package org.touchhle.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.text.InputType;
import android.widget.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small, backed-up story-slot editor. Does not synthesize missing save fields. */
final class SaveEditorDialog {
    static void show(Activity activity) {
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);
        int pad=Math.round(18*activity.getResources().getDisplayMetrics().density);panel.setPadding(pad,pad,pad,pad);
        panel.setBackgroundColor(Color.rgb(12,21,30));
        TextView status=label(activity,"选择剧情档案。保存前自动备份全部三个槽位。",panel);
        RadioGroup slots=new RadioGroup(activity);slots.setOrientation(RadioGroup.HORIZONTAL);panel.addView(slots);
        for(int i=0;i<3;i++){RadioButton button=new RadioButton(activity);button.setId(i+1);button.setText("档案 "+(i+1));button.setTextColor(Color.WHITE);slots.addView(button);}
        EditText credits=number(activity,"信用点（0–9999999）",panel);
        EditText nodes=number(activity,"能量节点（0–99999）",panel);
        label(activity,"护甲等级",panel);
        Spinner armor=new Spinner(activity);ArrayAdapter<String> adapter=new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"1级护服","2级护服","3级护服","4级护服","5级护服"}) {
            private android.view.View styled(android.view.View view){((TextView)view).setTextColor(Color.WHITE);view.setBackgroundColor(Color.rgb(18,32,43));return view;}
            @Override public android.view.View getView(int position,android.view.View view,android.view.ViewGroup parent){return styled(super.getView(position,view,parent));}
            @Override public android.view.View getDropDownView(int position,android.view.View view,android.view.ViewGroup parent){return styled(super.getDropDownView(position,view,parent));}
        };armor.setAdapter(adapter);panel.addView(armor);
        label(activity,"修改过的字段会同步到本档案的当前进度和关卡起点。剧情、任务及另外两个槽位保持原样。",panel);
        ScrollView scroll=new ScrollView(activity);scroll.addView(panel);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("存档编辑器").setView(scroll)
                .setNegativeButton("关闭",null).setPositiveButton("备份并保存",null).create();
        panel.setFocusableInTouchMode(true);panel.requestFocus();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                |android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        final int[] request={0},selected={-1};final boolean[] loading={true};
        final long[] loaded={0,0,0};
        ExecutorService worker=Executors.newSingleThreadExecutor(r -> new Thread(r,"save-editor"));
        dialog.setOnDismissListener(ignored -> worker.shutdown());
        dialog.setOnShowListener(ignored->{
            Button save=dialog.getButton(AlertDialog.BUTTON_POSITIVE);save.setEnabled(false);
            slots.setOnCheckedChangeListener((group,id)->{
                if(id<1||id>3||id==selected[0])return;selected[0]=id;
                final int serial=++request[0];loading[0]=true;save.setEnabled(false);status.setText("正在读取档案 "+id+"……");
                worker.execute(()->{
                    try {
                        SaveScalarEditor.Slot value=SaveArchiveImporter.readSlot(activity,id-1);
                        activity.runOnUiThread(()->{if(!dialog.isShowing()||serial!=request[0])return;
                            credits.setText(Long.toString(value.credits));nodes.setText(Long.toString(value.nodes));armor.setSelection(Math.max(0,Math.min(4,value.rig)));
                            loaded[0]=value.credits;loaded[1]=value.nodes;loaded[2]=value.rig;
                            status.setText("档案 "+id+" · 保存时自动备份三个槽位");loading[0]=false;save.setEnabled(true);});
                    } catch(Exception error){activity.runOnUiThread(()->{if(serial==request[0]){credits.setText("");nodes.setText("");status.setText("无法编辑此档案："+error.getMessage());}});}
                });
            });
            save.setOnClickListener(v->{
                if(loading[0])return;
                final long money,count;try {money=Long.parseLong(credits.getText().toString().trim());count=Long.parseLong(nodes.getText().toString().trim());}
                catch(NumberFormatException error){status.setText("请输入有效的整数。");return;}
                if(money<0||money>9_999_999||count<0||count>99_999){status.setText("数值超出上方显示的范围。");return;}
                int slot=slots.getCheckedRadioButtonId()-1,rig=armor.getSelectedItemPosition();
                int changes=(money!=loaded[0]?1:0)|(count!=loaded[1]?2:0)|(rig!=loaded[2]?4:0);
                if(changes==0){status.setText("数值没有变化，无需保存。");return;}
                save.setEnabled(false);loading[0]=true;
                for(int i=0;i<slots.getChildCount();i++)slots.getChildAt(i).setEnabled(false);
                credits.setEnabled(false);nodes.setEnabled(false);armor.setEnabled(false);
                status.setText("正在备份并保存，请稍候……");
                worker.execute(()->{
                    try {
                        java.io.File backup=SaveArchiveImporter.editSlot(activity,slot,money,count,rig,changes);
                        activity.runOnUiThread(()->{status.setText("已保存档案 "+(slot+1)+"。\n备份："+backup.getName());loading[0]=false;save.setEnabled(true);
                            loaded[0]=money;loaded[1]=count;loaded[2]=rig;
                            for(int i=0;i<slots.getChildCount();i++)slots.getChildAt(i).setEnabled(true);credits.setEnabled(true);nodes.setEnabled(true);armor.setEnabled(true);});
                    } catch(Exception error){activity.runOnUiThread(()->{status.setText("未保存："+error.getMessage());loading[0]=false;save.setEnabled(true);
                        for(int i=0;i<slots.getChildCount();i++)slots.getChildAt(i).setEnabled(true);credits.setEnabled(true);nodes.setEnabled(true);armor.setEnabled(true);});}
                });
            });
            slots.check(1);
        });
        dialog.show();
    }
    private static TextView label(Activity activity,String text,LinearLayout parent){
        TextView view=new TextView(activity);view.setText(text);view.setTextColor(Color.rgb(225,239,245));view.setTextSize(14);view.setPadding(0,10,0,6);parent.addView(view);return view;
    }
    private static EditText number(Activity activity,String text,LinearLayout parent){
        label(activity,text,parent);EditText view=new EditText(activity);view.setTextColor(Color.WHITE);view.setInputType(InputType.TYPE_CLASS_NUMBER);
        view.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        view.setSingleLine(true);parent.addView(view);return view;
    }
}
