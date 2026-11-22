package ie.speedvoice;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Locale;

public class DemoActivity extends Activity {
 private final Handler h=new Handler(Looper.getMainLooper());
 private TextToSpeech voice;
 private SpeechGate gate;
 private boolean ready=false, active=false;
 private int limit=50,speed=40,tolerance=0;
 private long overSince=-1,lastWarn=-1,belowSince=-1;
 private boolean warned=false;
 private TextView sign, speedText, message, voiceStatus, lastVoice;
 private final Runnable tick=new Runnable(){public void run(){if(active){evaluate();h.postDelayed(this,200);}}};
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView label(LinearLayout l,String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setPadding(0,dp(6),0,dp(6));l.addView(v);return v;}
 private void button(LinearLayout l,String s,View.OnClickListener cb){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setOnClickListener(cb);l.addView(b);}
 @Override public void onCreate(Bundle saved){super.onCreate(saved);
 tolerance=getSharedPreferences("prefs",MODE_PRIVATE).getInt("tolerance",0);
 ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
 LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(24),dp(44),dp(24),dp(36));scroll.addView(l);setContentView(scroll);
 label(l,"SpeedVoice — Demo",28);
 label(l,"SIMULATED ONLY • No GPS or Google Maps needed. Test while stationary. Live monitoring is stopped.",15);
 LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);l.addView(row);
 sign=new TextView(this);sign.setTextColor(Color.BLACK);sign.setTextSize(39);sign.setGravity(Gravity.CENTER);
 GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(Color.WHITE);circle.setStroke(dp(8),Color.rgb(214,35,35));sign.setBackground(circle);
 row.addView(sign,new LinearLayout.LayoutParams(dp(105),dp(105)));
 speedText=new TextView(this);speedText.setTextSize(31);speedText.setPadding(dp(20),0,0,0);row.addView(speedText);
 message=label(l,"",20);
 label(l,"Simulated road speed limit (km/h)",16);
 Spinner limits=new Spinner(this);Integer[] values={20,30,40,50,60,80,100,120};ArrayAdapter<Integer> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);limits.setAdapter(adapter);limits.setSelection(3);l.addView(limits);
 limits.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){int next=values[pos];if(next!=limit){limit=next;resetWarning();speak("Speed limit "+limit+" kilometres per hour.");}evaluate();}});
 label(l,"Simulated vehicle speed (km/h)",16);
 SeekBar bar=new SeekBar(this);bar.setMax(130);bar.setProgress(speed);l.addView(bar);
 bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int value,boolean fromUser){speed=value;evaluate();}});
 button(l,"Simulate speeding (+10 km/h)",v->bar.setProgress(Math.min(130,limit+Math.max(10,tolerance+1))));
 button(l,"Slow down",v->bar.setProgress(Math.max(0,limit-5)));
 voiceStatus=label(l,"Initialising voice…",14);
 lastVoice=label(l,"Last announcement: none",16);
 button(l,"Repeat speed limit announcement",v->speak("Speed limit "+limit+" kilometres per hour."));
 button(l,"Exit demo",v->finish());
 render();
 voice=new TextToSpeech(this,result->h.post(()->{
  if(isFinishing()||isDestroyed()||voice==null)return;
  if(result==TextToSpeech.SUCCESS){
   int language=voice.setLanguage(Locale.UK);
   if(language<TextToSpeech.LANG_AVAILABLE)language=voice.setLanguage(Locale.US);
   if(language<TextToSpeech.LANG_AVAILABLE)language=voice.setLanguage(Locale.ENGLISH);
   ready=language>=TextToSpeech.LANG_AVAILABLE;
   if(ready)gate=new SpeechGate(this,voice,h);
  }
  voiceStatus.setText(ready?"Voice ready; alerts after 3 seconds":"TTS unavailable; test voice on main screen and check Android settings");
  if(ready&&active)speak("Speed limit "+limit+" kilometres per hour.");
 }));
 }
 private void resetWarning(){overSince=lastWarn=belowSince=-1;warned=false;}
 private void render(){if(voiceStatus!=null&&ready)voiceStatus.setText("Voice: "+State.voiceDiagnostic);if(sign==null||speedText==null||message==null)return;boolean over=speed>limit+tolerance;sign.setText(""+limit);speedText.setText(speed+" km/h");speedText.setTextColor(over?Color.rgb(192,32,32):Color.rgb(30,125,65));message.setText(over?(lastWarn<0?"Simulated overspeed — warning after 3 seconds":"Simulated overspeed — slow down"):"Within selected warning threshold");}
 private void evaluate(){if(!active){render();return;}long now=SystemClock.elapsedRealtime();if(speed>limit+tolerance){belowSince=-1;if(overSince<0)overSince=now;if(now-overSince>=3000&&(lastWarn<0||now-lastWarn>=1000L*getSharedPreferences("prefs",MODE_PRIVATE).getInt("alert_interval",10))){lastWarn=now;warned=true;speak("Warning. Speed limit "+limit+". Please slow down.");}}else{overSince=lastWarn=-1;if(warned&&speed<=limit){if(belowSince<0)belowSince=now;if(now-belowSince>=2000){warned=false;belowSince=-1;if(getSharedPreferences("prefs",MODE_PRIVATE).getBoolean("recovery_voice",true))speak("Back within the detected speed limit.");}}else belowSince=-1;}render();}
 private void speak(String s){
 if(!active)return;
 final int expected=limit;
 final boolean warning=s.startsWith("Warning");
 if(lastVoice!=null)lastVoice.setText("Queued: "+(warning?"Overspeed warning":s));
 if(ready&&gate!=null)gate.offerDynamic(()->{
  String msg=warning?"Warning. Your speed is "+speed+" kilometres per hour. Speed limit "+expected+". Please slow down.":s;
  if(lastVoice!=null)lastVoice.setText("Last announcement: "+msg);
  return msg;
 },warning?2:1,()->active&&limit==expected&&(!warning||speed>limit+tolerance)&&(!s.startsWith("Back within")||speed<=limit),()->Math.max(0,speed-expected));
 }
 @Override protected void onResume(){super.onResume();active=true;resetWarning();h.removeCallbacks(tick);h.post(tick);if(ready)speak("Speed limit "+limit+" kilometres per hour.");}
 @Override protected void onPause(){active=false;h.removeCallbacks(tick);if(gate!=null)gate.cancel();else if(voice!=null)voice.stop();super.onPause();}
 @Override protected void onDestroy(){h.removeCallbacks(tick);if(gate!=null)gate.close();if(voice!=null)voice.shutdown();super.onDestroy();}
}