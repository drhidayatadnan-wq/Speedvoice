package ie.speedvoice;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.widget.*;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoTestActivity extends Activity {
 private final Handler main = new Handler(Looper.getMainLooper());
 private final ExecutorService worker = Executors.newSingleThreadExecutor();
 private MediaMetadataRetriever retriever;
 private VisualReader reader;
 private TextToSpeech voice;
 private boolean ready, running, closing, decoding, active;
 private int generation, candidate=-1, confirmed=-1, count, missed;
 private long timeMs, durationMs;
 private ImageView frame;
 private Bitmap preview;
 private TextView status, detected;
 private Button play;
 private final Runnable next = () -> sample();
 @Override public void onCreate(Bundle saved) {
  super.onCreate(saved);
  reader=new VisualReader();
  LinearLayout l=new LinearLayout(this);
  l.setOrientation(LinearLayout.VERTICAL);
  int p=(int)(16*getResources().getDisplayMetrics().density);
  l.setPadding(p,p,p,p);
  setContentView(l);
  TextView title=new TextView(this); title.setText("SpeedVoice — Video OCR test"); title.setTextSize(22); l.addView(title);
  status=new TextView(this); status.setText("SIMULATION ONLY. Choose a portrait navigation MP4. Live monitoring remains off."); l.addView(status);
  detected=new TextView(this); detected.setTextSize(27); detected.setText("Limit: unavailable"); l.addView(detected);
  Button choose=new Button(this); choose.setText("Choose navigation video"); l.addView(choose);
  choose.setOnClickListener(v->{pause(); Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("video/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,42);});
  play=new Button(this); play.setText("Start test"); play.setEnabled(false); l.addView(play);
  play.setOnClickListener(v->{if(running){pause();}else{if(timeMs>=durationMs)reset();running=true;play.setText("Pause");sample();}});
  Button restart=new Button(this);restart.setText("Restart from beginning");l.addView(restart);
  restart.setOnClickListener(v->{pause();reset();status.setText("Ready to start");});
  frame=new ImageView(this);frame.setScaleType(ImageView.ScaleType.FIT_CENTER);l.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
  voice=new TextToSpeech(this,r->{if(!closing&&voice!=null&&r==TextToSpeech.SUCCESS){ready=voice.setLanguage(Locale.UK)>=TextToSpeech.LANG_AVAILABLE;}});
 }
 private void reset(){timeMs=0;candidate=confirmed=-1;count=missed=0;detected.setText("Limit: unavailable");}
 private void pause(){running=false;generation++;main.removeCallbacks(next);if(play!=null)play.setText("Start / resume test");if(voice!=null)voice.stop();}
 @Override protected void onActivityResult(int req,int result,Intent data){
  super.onActivityResult(req,result,data);
  if(req!=42||result!=RESULT_OK||data==null||data.getData()==null)return;
  pause();reset();play.setEnabled(false);status.setText("Opening video…");
  Uri uri=data.getData();final int token=generation;
  worker.execute(()->{
   long length=0;String error=null;
   try{if(retriever!=null)retriever.release();retriever=new MediaMetadataRetriever();retriever.setDataSource(this,uri);length=Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));}
   catch(Exception e){error="Could not open selected video";}
   final long d=length;final String err=error;
   main.post(()->{if(closing||token!=generation)return;durationMs=d;play.setEnabled(err==null&&d>0);status.setText(err==null?"Video ready — press Start test":err);});
  });
 }
 private void sample(){
  if(closing||!active||!running||decoding)return;
  if(timeMs>=durationMs){pause();status.setText("Test complete");return;}
  decoding=true;final int token=generation;final long stamp=timeMs;
  status.setText(String.format(Locale.UK,"Video %02d:%02d / %02d:%02d — reading frame",stamp/60000,(stamp/1000)%60,durationMs/60000,(durationMs/1000)%60));
  worker.execute(()->{
   Bitmap bmp=null;
   try{if(retriever!=null)bmp=retriever.getFrameAtTime(stamp*1000,MediaMetadataRetriever.OPTION_CLOSEST);}
   catch(Exception ignored){}
   final Bitmap captured=bmp;
   main.post(()->{
    if(closing||token!=generation||!running){if(captured!=null)captured.recycle();decoding=false;if(running&&!closing)sample();return;}
    if(captured==null){accept(token,stamp,-1);return;}
    Bitmap ui=null;
    try{ui=captured.copy(Bitmap.Config.ARGB_8888,false);}catch(Exception ignored){}
    if(ui!=null){frame.setImageBitmap(ui);if(preview!=null)preview.recycle();preview=ui;}
    reader.scan(captured,v->accept(token,stamp,v));
   });
  });
 }
 private void accept(int token,long stamp,int value){
  decoding=false;
  if(closing||token!=generation||!running){if(running&&!closing)sample();return;}
  if(value<0){candidate=-1;count=0;missed++;if(missed>=2)confirmed=-1;detected.setText("No readable sign (frame "+stamp/1000+"s)");}
  else{missed=0;if(value!=candidate){candidate=value;count=1;}else count++;
   if(count>=2&&confirmed!=value){confirmed=value;if(ready&&voice!=null)voice.speak("Speed limit "+value+" kilometres per hour",TextToSpeech.QUEUE_FLUSH,null,"video-"+stamp);}
   detected.setText("OCR: "+value+" km/h"+(count>=2?" — confirmed":" — confirming"));
  }
  timeMs=stamp+1000;main.postDelayed(next,350);
 }
 @Override protected void onResume(){super.onResume();active=true;}
 @Override protected void onPause(){active=false;if(running)pause();super.onPause();}
 @Override protected void onDestroy(){closing=true;pause();if(voice!=null)voice.shutdown();if(reader!=null)reader.close();frame.setImageDrawable(null);if(preview!=null)preview.recycle();worker.execute(()->{if(retriever!=null){try{retriever.release();}catch(Exception ignored){}}});worker.shutdown();super.onDestroy();}
}
