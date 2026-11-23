package ie.cameraroadreader.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.media.*;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {
  private PreviewView preview;
  private TextView number, status;
  private TextRecognizer ocr;
  private TextToSpeech tts;
  private ExecutorService executor;
  private boolean voice=true, speechReady=false;
  private int candidate=-1, confirmations=0, announced=-1;
  private long candidateAt=0,lastFrameAt=0;
  private static final Set<Integer> LIMITS = new HashSet<>(Arrays.asList(20,30,40,50,60,80,100,120));
  private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  private TextView label(String s,int size,int color) {
    TextView t=new TextView(this);t.setText(s);t.setTextColor(color);
    t.setTextSize(size);t.setGravity(Gravity.CENTER);t.setPadding(dp(6),dp(8),dp(6),dp(8));return t;
  }
  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    LinearLayout layout=new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setBackgroundColor(0xff142338);
    layout.setPadding(dp(10),dp(30),dp(10),dp(10));
    layout.addView(label("CAMERA ROAD READER",22,0xff75edb9));
    layout.addView(label("Experimental speed sign recognition",12,0xffffffff));
    preview=new PreviewView(this);
    layout.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
    number=label("— km/h",42,0xffffffff);layout.addView(number);
    status=label("Starting camera…",14,0xff75edb9);layout.addView(status);
    LinearLayout row=new LinearLayout(this);
    Button toggle=new Button(this);toggle.setText("Voice ON");
    toggle.setOnClickListener(v->{voice=!voice;toggle.setText(voice?"Voice ON":"Voice OFF");if(!voice && tts!=null)tts.stop();});
    row.addView(toggle,new LinearLayout.LayoutParams(0,dp(50),1));
    Button demo=new Button(this);demo.setText("DEMO 50");
    demo.setOnClickListener(v->{status.setText("Demo (not a camera detection)");speak("Demo. Possible speed limit 50 kilometres per hour");});
    row.addView(demo,new LinearLayout.LayoutParams(0,dp(50),1));
    layout.addView(row);
    layout.addView(label("Advisory prototype only. Follow actual road signs. Never handle your phone while driving.",12,0xffffffff));
    setContentView(layout);
    executor=Executors.newSingleThreadExecutor();
    ocr=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    tts=new TextToSpeech(this, state->{if(state==TextToSpeech.SUCCESS && tts!=null){tts.setLanguage(Locale.UK);speechReady=true;}});
    if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)startCamera();
    else requestPermissions(new String[]{Manifest.permission.CAMERA},991);
  }
  @Override public void onRequestPermissionsResult(int request,@NonNull String[] permissions,@NonNull int[] results){
    super.onRequestPermissionsResult(request,permissions,results);
    if(request==991 && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED)startCamera();
    else status.setText("Enable camera permission in Android Settings");
  }
  private void startCamera(){
    final var future=ProcessCameraProvider.getInstance(this);
    future.addListener(()->{
      try{
        ProcessCameraProvider provider=future.get();
        Preview p=new Preview.Builder().build();p.setSurfaceProvider(preview.getSurfaceProvider());
        ImageAnalysis a=new ImageAnalysis.Builder()
          .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        a.setAnalyzer(executor,this::analyze);
        provider.unbindAll();
        provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,p,a);
        status.setText("Scanning for red-ring signs");
      }catch(Exception err){status.setText("Camera unavailable: "+err.getClass().getSimpleName());}
    },ContextCompat.getMainExecutor(this));
  }
  private void analyze(@NonNull ImageProxy frame){
    long now=SystemClock.elapsedRealtime();
    if(now-lastFrameAt<800){frame.close();return;} lastFrameAt=now;
    Bitmap bm=null;
    try {
      int w=frame.getWidth(),h=frame.getHeight();
      ImageProxy.PlaneProxy plane=frame.getPlanes()[0];
      ByteBuffer in=plane.getBuffer().duplicate();
      byte[] rgba=new byte[w*h*4];
      for(int y=0;y<h;y++){in.position(y*plane.getRowStride());in.get(rgba,y*w*4,w*4);}
      Bitmap raw=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
      raw.copyPixelsFromBuffer(ByteBuffer.wrap(rgba));
      int angle=frame.getImageInfo().getRotationDegrees();
      if(angle==0)bm=raw;
      else{Matrix m=new Matrix();m.postRotate(angle);bm=Bitmap.createBitmap(raw,0,0,w,h,m,true);raw.recycle();}
      Bitmap image=bm;
      ocr.process(InputImage.fromBitmap(image,0))
        .addOnSuccessListener(result->recognise(result,image))
        .addOnFailureListener(error->status.setText("OCR retrying"))
        .addOnCompleteListener(done->{image.recycle();frame.close();});
    }catch(Exception error){if(bm!=null && !bm.isRecycled())bm.recycle();frame.close();}
  }
  private void recognise(Text result,Bitmap image){
    Integer found=null;
    for(Text.TextBlock b:result.getTextBlocks())for(Text.Line l:b.getLines())for(Text.Element e:l.getElements()){
      String s=e.getText().trim();
      if(!s.matches("[0-9]{2,3}"))continue;
      int n=Integer.parseInt(s);
      if(!LIMITS.contains(n))continue;
      Rect r=e.getBoundingBox();
      if(r!=null&&r.height()>10&&redRing(image,r)){found=n;break;}
    }
    if(found==null){candidate=-1;confirmations=0;return;}
    long now=SystemClock.elapsedRealtime();
    if(found!=candidate||now-candidateAt>4500){candidate=found;candidateAt=now;confirmations=1;return;}
    if(++confirmations<2)return;
    number.setText(found+" km/h");
    status.setText("Possible sign — independently verify");
    if(announced!=found){announced=found;speak("Possible speed limit "+found+" kilometres per hour");}
  }
  private boolean redRing(Bitmap image,Rect rect){
    double cx=rect.exactCenterX(),cy=rect.exactCenterY(),h=rect.height();
    for(double radius:new double[]{.85,1.0,1.2,1.4}){
      int red=0,visible=0;
      for(int i=0;i<32;i++){
        double a=2*Math.PI*i/32;
        int x=(int)(cx+h*radius*Math.cos(a)),y=(int)(cy+h*radius*Math.sin(a));
        if(x<0||y<0||x>=image.getWidth()||y>=image.getHeight())continue;
        visible++;
        int p=image.getPixel(x,y),r=Color.red(p),g=Color.green(p),b=Color.blue(p);
        if(r>100&&r>g*1.4&&r>b*1.4)red++;
      }
      if(visible>=24&&red>=10)return true;
    }
    return false;
  }
  private void speak(String words){
    if(!voice||!speechReady||tts==null)return;
    ToneGenerator beep=null;
    try{beep=new ToneGenerator(AudioManager.STREAM_MUSIC,60);beep.startTone(ToneGenerator.TONE_PROP_BEEP,110);}catch(Exception ignored){}
    ToneGenerator b=beep;
    new Handler(Looper.getMainLooper()).postDelayed(()->{
      if(b!=null)b.release();
      if(voice&&tts!=null)tts.speak(words,TextToSpeech.QUEUE_FLUSH,null,"roadreader-"+SystemClock.elapsedRealtime());
    },230);
  }
  @Override protected void onDestroy(){
    if(tts!=null){tts.stop();tts.shutdown();}
    if(ocr!=null)ocr.close();
    if(executor!=null)executor.shutdown();
    super.onDestroy();
  }
}