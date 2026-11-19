package ie.speedvoice;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.*;
import android.os.*;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.speech.tts.TextToSpeech;
import android.media.AudioAttributes;
import java.util.*;
public class MapsReader extends AccessibilityService {
 private final Handler h=new Handler(Looper.getMainLooper());
 private TextToSpeech tts;
 private SpeechGate gate;
 private VisualReader visual;
 private boolean busy=false;
 private long visualAt=0;
 private int visualValue=-1,visualWindow=-1,generation=0;
 private boolean alive=true,ready=false,everHadLimit=false,lostSpoken=false;
 private int candidate=-1,confirmed=-1,count=0;
 private boolean warned=false;
 private long belowSince=-1;
 private long lastObservation=-1,observation=0,overSince=-1,lastWarn=-100000,missingSince=-1;
 private final Runnable poll=new Runnable(){public void run(){try{tick();}catch(RuntimeException e){invalidate();State.status="Reader unavailable";}finally{h.postDelayed(this,1000);}}};
 @Override protected void onServiceConnected(){super.onServiceConnected();visual=new VisualReader();
 tts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS&&tts!=null){int result=tts.setLanguage(Locale.UK);ready=result>=TextToSpeech.LANG_AVAILABLE;if(ready){if(gate!=null)gate.close();gate=new SpeechGate(this,tts,h);}}});
 h.removeCallbacks(poll);h.post(poll);}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){}
 private void speak(String s){
 if(!ready||gate==null||State.muted)return;
 final int limit=confirmed;
 final boolean warning=s.startsWith("Warning"),unavailable=s.startsWith("Speed limit unavailable"),recovery=s.startsWith("Back within");
 gate.offerDynamic(()->{
  if(!warning)return s;
  float current=State.freshSpeed();
  return Float.isNaN(current)?null:"Warning. Your speed is "+Math.round(current)+" kilometres per hour. Speed limit "+limit+". Please slow down.";
 },warning?2:unavailable?3:1,()->{
  if(!alive||!State.running||!Consent.accepted(MapsReader.this)||State.muted||activeMapsWindow()<0)return false;
  if(unavailable)return confirmed<0&&everHadLimit;
  if(confirmed!=limit||limit<=0||State.limit!=limit)return false;
  if(recovery){float v=State.freshSpeed();return !Float.isNaN(v)&&v<=limit;}
  if(!warning)return true;
  float speed=State.freshSpeed();
  return !Float.isNaN(speed)&&speed>limit+getSharedPreferences("prefs",MODE_PRIVATE).getInt("tolerance",0);
 },()->Math.max(0,(int)Math.floor(State.freshSpeed()-limit)));
 }
 private void invalidate(){warned=false;belowSince=-1;confirmed=candidate=-1;count=0;lastObservation=-1;State.limit=-1;overSince=-1;lastWarn=-100000;}
 private void clearVisual(){visualAt=0;visualValue=-1;visualWindow=-1;generation++;}
 private int activeMapsWindow(){AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return -1;try{return "com.google.android.apps.maps".contentEquals(root.getPackageName())?root.getWindowId():-1;}finally{root.recycle();}}
 private int readLimit(){
 AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null){State.readerDiagnostic="No active window";clearVisual();return -1;}int window;
 try{if(!"com.google.android.apps.maps".contentEquals(root.getPackageName())){State.readerDiagnostic="Google Maps not visible";clearVisual();return -1;}
 window=root.getWindowId();Set<Integer> found=new HashSet<>();walk(root,found,new int[]{0});
 if(found.size()==1){State.readerDiagnostic="Accessibility label";clearVisual();observation=SystemClock.elapsedRealtime();return found.iterator().next();}
 if(found.size()>1){State.readerDiagnostic="Conflicting speed-limit labels";clearVisual();return -1;}
 }finally{root.recycle();}
 if(visualWindow!=window){clearVisual();visualWindow=window;}
 if(!busy&&visual!=null&&alive)capture(window);
 observation=visualAt;int result=SystemClock.elapsedRealtime()-visualAt<=2200?visualValue:-1;State.readerDiagnostic=result>0?"Local OCR sign":"Waiting for readable sign (OCR)";return result;
 }
 private void capture(int window){
 busy=true;final int token=generation;final long started=SystemClock.elapsedRealtime();
 try{takeScreenshotOfWindow(window,getMainExecutor(),new TakeScreenshotCallback(){
 @Override public void onSuccess(ScreenshotResult shot){Bitmap copy=null;HardwareBuffer buffer=shot.getHardwareBuffer();
 try{if(alive&&State.running&&Consent.accepted(MapsReader.this)&&token==generation&&activeMapsWindow()==window){Bitmap hw=Bitmap.wrapHardwareBuffer(buffer,shot.getColorSpace());if(hw!=null){try{copy=hw.copy(Bitmap.Config.ARGB_8888,false);}finally{hw.recycle();}}}}catch(RuntimeException ignored){}finally{buffer.close();}
 if(copy==null){busy=false;visualAt=0;visualValue=-1;return;}
 visual.scan(copy,value->{busy=false;if(alive&&State.running&&Consent.accepted(MapsReader.this)&&token==generation&&activeMapsWindow()==window&&SystemClock.elapsedRealtime()-started<=1800){visualValue=value;visualAt=started;}else{visualAt=0;visualValue=-1;}});
 }
 @Override public void onFailure(int errorCode){busy=false;visualAt=0;visualValue=-1;}
 });}catch(RuntimeException e){busy=false;visualAt=0;visualValue=-1;}
 }
 private void walk(AccessibilityNodeInfo n,Set<Integer> found,int[] seen){
 if(++seen[0]>600)return;if(n.isVisibleToUser()){read(n.getText(),found);read(n.getContentDescription(),found);}
 for(int i=0;i<n.getChildCount()&&seen[0]<=600;i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null){try{walk(c,found,seen);}finally{c.recycle();}}}
 }
 private void read(CharSequence text,Set<Integer> found){if(text!=null){int x=LimitParser.parse(text.toString());if(x>0)found.add(x);}}
 private void tick(){
 if(!State.running||!Consent.accepted(MapsReader.this)){State.readerDiagnostic="Inactive";if(gate!=null)gate.cancel();clearVisual();invalidate();everHadLimit=lostSpoken=false;missingSince=-1;return;}
 long now=SystemClock.elapsedRealtime();int value=readLimit();
 if(value<0){invalidate();State.status="No readable speed limit — use visible portrait Maps navigation";if(missingSince<0)missingSince=now;if(everHadLimit&&!lostSpoken&&now-missingSince>=3000){speak("Speed limit unavailable. Check road signs.");lostSpoken=true;}return;}
 missingSince=-1;lostSpoken=false;
 if(value!=candidate){warned=false;belowSince=-1;candidate=value;count=1;lastObservation=observation;confirmed=-1;State.limit=-1;overSince=-1;State.status="Confirming speed limit";return;}
 if(lastObservation!=observation){count++;lastObservation=observation;}
 if(count>=2&&value!=confirmed){confirmed=value;State.limit=value;everHadLimit=true;overSince=-1;lastWarn=-100000;speak("Speed limit "+value+" kilometres per hour.");}
 if(confirmed<0)return;float speed=State.freshSpeed();State.status=Float.isNaN(speed)?"Limit "+confirmed+"; GPS speed unavailable":"Monitoring • limit "+confirmed+" km/h";
 if(State.muted||Float.isNaN(speed)){warned=false;belowSince=-1;overSince=-1;lastWarn=-100000;return;}int tolerance=getSharedPreferences("prefs",MODE_PRIVATE).getInt("tolerance",0);
 if(speed>confirmed+tolerance){belowSince=-1;if(overSince<0)overSince=now;if(now-overSince>=3000&&now-lastWarn>=1000L*getSharedPreferences("prefs",MODE_PRIVATE).getInt("alert_interval",10)){speak("Warning. Speed limit "+confirmed+". Please slow down.");warned=true;lastWarn=now;}}else{overSince=-1;lastWarn=-100000;if(warned&&speed<=confirmed){if(belowSince<0)belowSince=now;if(now-belowSince>=2000){warned=false;belowSince=-1;if(getSharedPreferences("prefs",MODE_PRIVATE).getBoolean("recovery_voice",true))speak("Back within the detected speed limit.");}}else belowSince=-1;}
 }
 @Override public void onInterrupt(){if(gate!=null)gate.cancel();else if(tts!=null)tts.stop();}
 @Override public void onDestroy(){alive=false;h.removeCallbacks(poll);invalidate();clearVisual();if(visual!=null)visual.close();if(gate!=null)gate.close();if(tts!=null)tts.shutdown();super.onDestroy();}
}