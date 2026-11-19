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
 private VisualReader visual;
 private boolean busy=false;
 private long visualAt=0;
 private int visualValue=-1,visualWindow=-1,generation=0;
 private boolean alive=true,ready=false,everHadLimit=false,lostSpoken=false;
 private int candidate=-1,confirmed=-1,count=0;
 private long lastObservation=-1,observation=0,overSince=-1,lastWarn=-100000,missingSince=-1;
 private final Runnable poll=new Runnable(){public void run(){try{tick();}catch(RuntimeException e){invalidate();State.status="Reader unavailable";}finally{h.postDelayed(this,1000);}}};
 @Override protected void onServiceConnected(){super.onServiceConnected();visual=new VisualReader();
 tts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS&&tts!=null){int result=tts.setLanguage(Locale.UK);ready=result>=TextToSpeech.LANG_AVAILABLE;tts.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());}});
 h.removeCallbacks(poll);h.post(poll);}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){}
 private void speak(String s){if(ready&&tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"speedvoice-"+SystemClock.elapsedRealtime());}
 private void invalidate(){confirmed=candidate=-1;count=0;lastObservation=-1;State.limit=-1;overSince=-1;lastWarn=-100000;}
 private void clearVisual(){visualAt=0;visualValue=-1;visualWindow=-1;generation++;}
 private int activeMapsWindow(){AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return -1;try{return "com.google.android.apps.maps".contentEquals(root.getPackageName())?root.getWindowId():-1;}finally{root.recycle();}}
 private int readLimit(){
 AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null){clearVisual();return -1;}int window;
 try{if(!"com.google.android.apps.maps".contentEquals(root.getPackageName())){clearVisual();return -1;}
 window=root.getWindowId();Set<Integer> found=new HashSet<>();walk(root,found,new int[]{0});
 if(found.size()==1){clearVisual();observation=SystemClock.elapsedRealtime();return found.iterator().next();}
 if(found.size()>1){clearVisual();return -1;}
 }finally{root.recycle();}
 if(visualWindow!=window){clearVisual();visualWindow=window;}
 if(!busy&&visual!=null&&alive)capture(window);
 observation=visualAt;return SystemClock.elapsedRealtime()-visualAt<=2200?visualValue:-1;
 }
 private void capture(int window){
 busy=true;final int token=generation;final long started=SystemClock.elapsedRealtime();
 try{takeScreenshotOfWindow(window,getMainExecutor(),new TakeScreenshotCallback(){
 @Override public void onSuccess(ScreenshotResult shot){Bitmap copy=null;HardwareBuffer buffer=shot.getHardwareBuffer();
 try{if(alive&&State.running&&token==generation&&activeMapsWindow()==window){Bitmap hw=Bitmap.wrapHardwareBuffer(buffer,shot.getColorSpace());if(hw!=null){try{copy=hw.copy(Bitmap.Config.ARGB_8888,false);}finally{hw.recycle();}}}}catch(RuntimeException ignored){}finally{buffer.close();}
 if(copy==null){busy=false;visualAt=0;visualValue=-1;return;}
 visual.scan(copy,value->{busy=false;if(alive&&State.running&&token==generation&&activeMapsWindow()==window&&SystemClock.elapsedRealtime()-started<=1800){visualValue=value;visualAt=started;}else{visualAt=0;visualValue=-1;}});
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
 if(!State.running){clearVisual();invalidate();everHadLimit=lostSpoken=false;missingSince=-1;return;}
 long now=SystemClock.elapsedRealtime();int value=readLimit();
 if(value<0){invalidate();State.status="No readable speed limit — use visible portrait Maps navigation";if(missingSince<0)missingSince=now;if(everHadLimit&&!lostSpoken&&now-missingSince>=3000){speak("Speed limit unavailable. Check road signs.");lostSpoken=true;}return;}
 missingSince=-1;lostSpoken=false;
 if(value!=candidate){candidate=value;count=1;lastObservation=observation;confirmed=-1;State.limit=-1;overSince=-1;State.status="Confirming speed limit";return;}
 if(lastObservation!=observation){count++;lastObservation=observation;}
 if(count>=2&&value!=confirmed){confirmed=value;State.limit=value;everHadLimit=true;overSince=-1;lastWarn=-100000;speak("Speed limit "+value+" kilometres per hour.");}
 if(confirmed<0)return;float speed=State.freshSpeed();State.status=Float.isNaN(speed)?"Limit "+confirmed+"; GPS speed unavailable":"Monitoring • limit "+confirmed+" km/h";
 if(Float.isNaN(speed)){overSince=-1;return;}int tolerance=getSharedPreferences("prefs",MODE_PRIVATE).getInt("tolerance",0);
 if(speed>confirmed+tolerance){if(overSince<0)overSince=now;if(now-overSince>=3000&&now-lastWarn>=10000){speak("Warning. Speed limit "+confirmed+". Please slow down.");lastWarn=now;}}else{overSince=-1;lastWarn=-100000;}
 }
 @Override public void onInterrupt(){if(tts!=null)tts.stop();}
 @Override public void onDestroy(){alive=false;h.removeCallbacks(poll);invalidate();clearVisual();if(visual!=null)visual.close();if(tts!=null)tts.shutdown();super.onDestroy();}
}