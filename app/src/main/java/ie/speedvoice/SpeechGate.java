package ie.speedvoice;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.os.Bundle;
import java.util.function.Supplier;
import java.util.function.IntSupplier;
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
 private final SharedPreferences prefs;
 private AudioTrack tone;
 private Runnable afterTone;
 private final TextToSpeech tts;
 private final Handler h;
 private final AudioAttributes attrs=new AudioAttributes.Builder()
  .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
  .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
 private final AudioFocusRequest focus;
 private boolean closed=false,holding=false;
 private long quietSince=-1,retryAfter=0,started=0,seq=0;
 private Item pending,active;
 private String activeId;
 private static final class Item {
  final Supplier<String> text; final IntSupplier excess; final int priority; final BooleanSupplier valid; final long created;
  Item(Supplier<String> t,int p,BooleanSupplier v,IntSupplier e){text=t;excess=e;priority=p;valid=v;created=SystemClock.elapsedRealtime();}
  boolean usable(long now){try{return now-created<=TTL_MS&&valid.getAsBoolean();}catch(RuntimeException e){return false;}}
 }
 private final Runnable poll=new Runnable(){public void run(){if(closed)return;try{tick();}catch(RuntimeException e){cancel();}finally{if(!closed)h.postDelayed(this,POLL_MS);}}};
 SpeechGate(Context c,TextToSpeech voice,Handler handler){
  audio=c.getSystemService(AudioManager.class);prefs=c.getSharedPreferences("prefs",Context.MODE_PRIVATE);tts=voice;h=handler;
  tts.setAudioAttributes(attrs);
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
   .setAudioAttributes(attrs).setWillPauseWhenDucked(true)
   .setOnAudioFocusChangeListener(change->{if(change<0){interrupt(true);quietSince=-1;retryAfter=SystemClock.elapsedRealtime()+1000;}},h)
   .build();
  tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
   @Override public void onStart(String id){h.post(()->{if(id.equals(activeId))State.voiceDiagnostic="Speech engine started output (navigation audio)";});}
   @Override public void onDone(String id){h.post(()->{if(id.equals(activeId))State.voiceDiagnostic="Speech engine completed utterance";finished(id);});}
   @Override public void onError(String id){h.post(()->{if(id.equals(activeId))State.voiceDiagnostic="TTS playback error";finished(id);});}
   @Override public void onError(String id,int errorCode){h.post(()->{if(id.equals(activeId))State.voiceDiagnostic="TTS error code "+errorCode;finished(id);});}
   @Override public void onStop(String id,boolean interrupted){h.post(()->{if(id.equals(activeId))State.voiceDiagnostic="TTS stopped";finished(id);});}
  });
  h.post(poll);
 }
 void offer(String text,int priority,BooleanSupplier valid){
  offerDynamic(()->text,priority,valid);
 }
 void offerDynamic(Supplier<String> text,int priority,BooleanSupplier valid){
  offerDynamic(text,priority,valid,()->0);
 }
 void offerDynamic(Supplier<String> text,int priority,BooleanSupplier valid,IntSupplier excess){
  if(closed)return;
  Item item=new Item(text,priority,valid,excess);
  if(pending==null||!pending.usable(SystemClock.elapsedRealtime())||priority>=pending.priority){pending=item;State.voiceDiagnostic="Alert queued for speech";}
 }
 private boolean otherSpeech(){
  if(audio==null)return true;
  try{
   if(audio.getMode()==AudioManager.MODE_IN_CALL||audio.getMode()==AudioManager.MODE_IN_COMMUNICATION)return true;
   for(AudioPlaybackConfiguration p:audio.getActivePlaybackConfigurations()){
    AudioAttributes a=p.getAudioAttributes();if(a==null)continue;
    int usage=a.getUsage(),content=a.getContentType();
    if(usage==AudioAttributes.USAGE_VOICE_COMMUNICATION)return true;
    // During our own utterance the navigation stream is active as well.
    // Treat it as external speech only before dispatch; audio focus handles interruption.
    if(usage==AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE&&active==null)return true;
    if(content==AudioAttributes.CONTENT_TYPE_SPEECH&&
       !(active!=null&&(usage==AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE||
                         usage==AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)))return true;
   }
   return false;
  }catch(RuntimeException e){return true;}
 }
 private void tick(){
  long now=SystemClock.elapsedRealtime();
  if(pending!=null&&!pending.usable(now)){pending=null;State.voiceDiagnostic="Alert expired or recognition no longer valid";}
  if(active!=null&&!active.usable(now)){interrupt(false);}
  if(active!=null&&now-started>10000){interrupt(false);}
  if(pending==null&&active==null){quietSince=-1;return;}
  if(otherSpeech()){quietSince=-1;State.voiceDiagnostic="Waiting for other speech/navigation audio";if(active!=null)interrupt(true);return;}
  if(quietSince<0)quietSince=now;
  if(active!=null||pending==null||now<retryAfter||now-quietSince<QUIET_MS)return;
  if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){State.voiceDiagnostic="Audio focus denied; retrying";retryAfter=now+1000;quietSince=-1;return;}
  holding=true;
  if(otherSpeech()||!pending.usable(now)){releaseFocus();quietSince=-1;return;}
  active=pending;pending=null;activeId="speedvoice-gate-"+(++seq);started=now;
  int mode=prefs.getInt("alert_mode",2);
  if(active.priority==2&&mode!=0){
   int excess=active.excess.getAsInt(); int count=mode==4?(excess>=15?3:excess>=5?2:1):(mode==1?1:2);
   if(startTone(count)){
    final String id=activeId;
    afterTone=()->{
     afterTone=null;stopTone();
     if(active==null||!id.equals(activeId))return;
     if(!active.usable(SystemClock.elapsedRealtime())){interrupt(false);return;}
     if(otherSpeech()){interrupt(true);return;}
     if(mode==3)finished(id);else speakActive();
    };
    h.postDelayed(afterTone,300+240L*(count-1));
    return;
   }
   if(mode==3){finished(activeId);return;}
  }
  speakActive();
 }
 private float volume(){return Math.max(.25f,Math.min(1f,prefs.getInt("alert_volume",100)/100f));}
 private void speakActive(){
  if(active==null)return;
  String text=active.text.get();
  if(text==null||text.isEmpty()){State.voiceDiagnostic="Alert no longer applicable";interrupt(false);return;}
  Bundle params=new Bundle();params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,volume());
  int queued=tts.speak(text,TextToSpeech.QUEUE_FLUSH,params,activeId);
  if(queued!=TextToSpeech.SUCCESS){State.voiceDiagnostic="TTS rejected speech request";interrupt(false);}else State.voiceDiagnostic="TTS speech request accepted";
 }
 private boolean startTone(int count){
  stopTone();
  final int rate=24000,on=rate*150/1000,gap=rate*80/1000,end=rate*20/1000;
  short[] pcm=new short[on*count+gap*(count-1)+end];
  for(int b=0;b<count;b++)for(int i=0;i<on;i++){
   double env=Math.min(1.0,Math.min(i,on-1-i)/(rate*.012));
   pcm[b*(on+gap)+i]=(short)(11000*env*Math.sin(2*Math.PI*880*i/rate));
  }
  try{
   tone=new AudioTrack.Builder()
    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
    .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
    .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.length*2).build();
   if(tone.getState()!=AudioTrack.STATE_INITIALIZED||tone.write(pcm,0,pcm.length)!=pcm.length){stopTone();return false;}
   tone.setVolume(volume());tone.play();return true;
  }catch(RuntimeException e){stopTone();return false;}
 }
 private void stopTone(){
  if(afterTone!=null){h.removeCallbacks(afterTone);afterTone=null;}
  if(tone!=null){try{tone.stop();}catch(RuntimeException ignored){}try{tone.release();}catch(RuntimeException ignored){}tone=null;}
 }
 private void finished(String id){
  if(activeId==null||!activeId.equals(id))return;
  stopTone();active=null;activeId=null;releaseFocus();quietSince=-1;
 }
 private void interrupt(boolean retry){
  Item old=active;active=null;activeId=null;stopTone();
  if(old!=null)tts.stop();
  releaseFocus();quietSince=-1;
  if(retry&&old!=null&&old.usable(SystemClock.elapsedRealtime())&&(pending==null||pending.priority<old.priority))pending=old;
 }
 private void releaseFocus(){if(holding){holding=false;try{audio.abandonAudioFocusRequest(focus);}catch(RuntimeException ignored){}}}
 void cancel(){pending=null;interrupt(false);}
 void close(){closed=true;h.removeCallbacks(poll);cancel();}
}
