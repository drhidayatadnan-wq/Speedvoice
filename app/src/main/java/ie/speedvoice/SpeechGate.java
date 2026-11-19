package ie.speedvoice;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.function.BooleanSupplier;

// Best-effort, local playback metadata only. No microphone or recording.
final class SpeechGate {
 private static final long QUIET_MS=750, TTL_MS=20000, POLL_MS=200;
 private final AudioManager audio;
 private final TextToSpeech tts;
 private final Handler h;
 private final AudioAttributes attrs=new AudioAttributes.Builder()
  .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
  .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
 private final AudioFocusRequest focus;
 private boolean closed=false,holding=false;
 private long quietSince=-1,retryAfter=0,started=0,seq=0;
 private Item pending,active;
 private String activeId;
 private static final class Item {
  final String text; final int priority; final BooleanSupplier valid; final long created;
  Item(String t,int p,BooleanSupplier v){text=t;priority=p;valid=v;created=SystemClock.elapsedRealtime();}
  boolean usable(long now){try{return now-created<=TTL_MS&&valid.getAsBoolean();}catch(RuntimeException e){return false;}}
 }
 private final Runnable poll=new Runnable(){public void run(){if(closed)return;try{tick();}catch(RuntimeException e){cancel();}finally{if(!closed)h.postDelayed(this,POLL_MS);}}};
 SpeechGate(Context c,TextToSpeech voice,Handler handler){
  audio=c.getSystemService(AudioManager.class);tts=voice;h=handler;
  tts.setAudioAttributes(attrs);
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
   .setAudioAttributes(attrs).setWillPauseWhenDucked(true)
   .setOnAudioFocusChangeListener(change->{if(change<0){interrupt(true);quietSince=-1;retryAfter=SystemClock.elapsedRealtime()+1000;}},h)
   .build();
  tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
   @Override public void onStart(String id){}
   @Override public void onDone(String id){h.post(()->finished(id));}
   @Override public void onError(String id){h.post(()->finished(id));}
   @Override public void onStop(String id,boolean interrupted){h.post(()->finished(id));}
  });
  h.post(poll);
 }
 void offer(String text,int priority,BooleanSupplier valid){
  if(closed)return;
  Item item=new Item(text,priority,valid);
  if(pending==null||!pending.usable(SystemClock.elapsedRealtime())||priority>=pending.priority)pending=item;
 }
 private boolean otherSpeech(){
  if(audio==null)return true;
  try{
   if(audio.getMode()==AudioManager.MODE_IN_CALL||audio.getMode()==AudioManager.MODE_IN_COMMUNICATION)return true;
   for(AudioPlaybackConfiguration p:audio.getActivePlaybackConfigurations()){
    AudioAttributes a=p.getAudioAttributes();if(a==null)continue;
    int usage=a.getUsage(),content=a.getContentType();
    if(usage==AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE||
       usage==AudioAttributes.USAGE_VOICE_COMMUNICATION||
       (content==AudioAttributes.CONTENT_TYPE_SPEECH&&
        !(active!=null&&usage==AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)))return true;
   }
   return false;
  }catch(RuntimeException e){return true;}
 }
 private void tick(){
  long now=SystemClock.elapsedRealtime();
  if(pending!=null&&!pending.usable(now))pending=null;
  if(active!=null&&!active.usable(now)){interrupt(false);}
  if(active!=null&&now-started>10000){interrupt(false);}
  if(pending==null&&active==null){quietSince=-1;return;}
  if(otherSpeech()){quietSince=-1;if(active!=null)interrupt(true);return;}
  if(quietSince<0)quietSince=now;
  if(active!=null||pending==null||now<retryAfter||now-quietSince<QUIET_MS)return;
  if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){retryAfter=now+1000;quietSince=-1;return;}
  holding=true;
  if(otherSpeech()||!pending.usable(now)){releaseFocus();quietSince=-1;return;}
  active=pending;pending=null;activeId="speedvoice-gate-"+(++seq);started=now;
  if(tts.speak(active.text,TextToSpeech.QUEUE_FLUSH,null,activeId)!=TextToSpeech.SUCCESS)interrupt(false);
 }
 private void finished(String id){
  if(activeId==null||!activeId.equals(id))return;
  active=null;activeId=null;releaseFocus();quietSince=-1;
 }
 private void interrupt(boolean retry){
  Item old=active;active=null;activeId=null;
  if(old!=null)tts.stop();
  releaseFocus();quietSince=-1;
  if(retry&&old!=null&&old.usable(SystemClock.elapsedRealtime())&&(pending==null||pending.priority<old.priority))pending=old;
 }
 private void releaseFocus(){if(holding){holding=false;try{audio.abandonAudioFocusRequest(focus);}catch(RuntimeException ignored){}}}
 void cancel(){pending=null;interrupt(false);}
 void close(){closed=true;h.removeCallbacks(poll);cancel();}
}
