package ie.cameraroadreader.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.media.*;
import android.util.Size;
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
  private TextView number, status, readingHeading;
  private SignOverlay marker;
  private TextRecognizer ocr;
  private TextToSpeech tts;
  private ExecutorService executor;
  private boolean voice=true, speechReady=false;
  private int candidate=-1, confirmations=0, announced=-1, confirmed=-1;
  private long candidateAt=0,lastFrameAt=0,confirmedAt=0,announcedAt=0;
  private static final long FRAME_INTERVAL_MS=220;
  private static final long STALE_AFTER_MS=45000;
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
    FrameLayout cameraPanel=new FrameLayout(this);
    preview=new PreviewView(this);
    preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);
    cameraPanel.addView(preview,new FrameLayout.LayoutParams(-1,-1));
    marker=new SignOverlay();
    cameraPanel.addView(marker,new FrameLayout.LayoutParams(-1,-1));
    layout.addView(cameraPanel,new LinearLayout.LayoutParams(-1,0,1));
    readingHeading=label("WAITING FOR FIRST SIGN",12,0xfff6cb6f);layout.addView(readingHeading);
    number=label("— km/h",46,0xffffffff);layout.addView(number);
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
          .setTargetResolution(new Size(960,540))
          .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        a.setAnalyzer(executor,this::analyze);
        provider.unbindAll();
        provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,p,a);
        updateStatus(SystemClock.elapsedRealtime());
      }catch(Exception err){status.setText("Camera unavailable: "+err.getClass().getSimpleName());}
    },ContextCompat.getMainExecutor(this));
  }
  private void analyze(@NonNull ImageProxy frame){
    long now=SystemClock.elapsedRealtime();
    if(now-lastFrameAt<FRAME_INTERVAL_MS){frame.close();return;} lastFrameAt=now;
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
    Rect bestRect=null;
    int biggest=0;
    for(Text.TextBlock block:result.getTextBlocks()){
      for(Text.Line line:block.getLines()){
        for(Text.Element element:line.getElements()){
          String text=element.getText().trim();
          if(!text.matches("[0-9]{2,3}"))continue;
          int value=Integer.parseInt(text);
          if(!LIMITS.contains(value))continue;
          Rect box=element.getBoundingBox();
          if(box==null || box.height()<8 || !redRing(image,box))continue;
          int area=box.width()*box.height();
          if(area>biggest){biggest=area;found=value;bestRect=box;}
        }
      }
    }
    long now=SystemClock.elapsedRealtime();
    if(found==null){
      if(candidateAt>0 && now-candidateAt>2500){candidate=-1;confirmations=0;}
      updateStatus(now);
      return;
    }
    marker.mark(bestRect,image.getWidth(),image.getHeight(),found);
    // Show the possible new sign immediately, without replacing the last verified reading.
    if(found!=candidate || now-candidateAt>2500){
      candidate=found;
      candidateAt=now;
      confirmations=1;
      updateStatus(now);
      return;
    }
    confirmations++;
    candidateAt=now;
    if(confirmations>=2){
      confirmed=found;
      confirmedAt=now;
      number.setText(found+" km/h");
      if(announced!=found || now-announcedAt>90000){
        announced=found;
        announcedAt=now;
        speak("Possible speed limit "+found+" kilometres per hour");
      }
      candidate=-1;
      confirmations=0;
      candidateAt=0;
    }
    updateStatus(now);
  }
  private void updateStatus(long now){
    if(confirmed<0){
      readingHeading.setText("WAITING FOR FIRST SIGN");
      number.setTextColor(0xffffffff);
    }else if(now-confirmedAt>STALE_AFTER_MS){
      readingHeading.setText("PREVIOUS POSSIBLE SIGN — STALE");
      number.setTextColor(0xffaeb7c7);
    }else{
      readingHeading.setText("LAST POSSIBLE SPEED LIMIT");
      number.setTextColor(0xffffffff);
    }
    if(candidate>=0 && candidateAt>0 && now-candidateAt<2500){
      status.setText("Checking possible "+candidate+" km/h • still scanning");
    }else if(confirmed<0){
      status.setText("Scanning for speed-limit signs…");
    }else if(now-confirmedAt>STALE_AFTER_MS){
      status.setText("Old reading ("+(now-confirmedAt)/1000+"s ago) — not a current limit");
    }else{
      status.setText("Scanning • last possible sign "+(now-confirmedAt)/1000+"s ago");
    }
  }
  private class SignOverlay extends View{
    private final Paint outline=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Rect detectedBox;
    private int imgW,imgH,detectedLimit;
    private long markedAt=0;
    SignOverlay(){
      super(MainActivity.this);
      setClickable(false);
      outline.setColor(0xff75edb9);
      outline.setStyle(Paint.Style.STROKE);
      outline.setStrokeWidth(dp(3));
      textPaint.setColor(0xff75edb9);
      textPaint.setTypeface(Typeface.DEFAULT_BOLD);
      textPaint.setTextSize(dp(17));
      textPaint.setShadowLayer(dp(2),0,0,0xff000000);
    }
    void mark(Rect box,int width,int height,int speed){
      int pad=Math.max(10,box.height());
      detectedBox=new Rect(box.left-pad,box.top-pad,box.right+pad,box.bottom+pad);
      imgW=width;imgH=height;detectedLimit=speed;
      markedAt=SystemClock.elapsedRealtime();
      invalidate();
      postInvalidateDelayed(1400);
    }
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);
      if(detectedBox==null || imgW==0 || imgH==0 ||
          SystemClock.elapsedRealtime()-markedAt>1300)return;
      float factor=Math.max(getWidth()/(float)imgW,getHeight()/(float)imgH);
      float dx=(getWidth()-imgW*factor)/2f;
      float dy=(getHeight()-imgH*factor)/2f;
      RectF area=new RectF(detectedBox.left*factor+dx,detectedBox.top*factor+dy,
          detectedBox.right*factor+dx,detectedBox.bottom*factor+dy);
      c.drawRoundRect(area,dp(7),dp(7),outline);
      c.drawText("CHECKING "+detectedLimit,Math.max(dp(4),area.left),
          Math.max(dp(22),area.top-dp(7)),textPaint);
    }
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