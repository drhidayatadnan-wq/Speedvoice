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
import androidx.appcompat.app.AlertDialog;
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
  private TextView number, status, readingHeading, otherSign, laneStatus, familyStatus;
  private SignOverlay marker;
  private TextRecognizer ocr;
  private TextToSpeech tts;
  private ExecutorService executor;
  private boolean voice=true, speechReady=false;
  private volatile boolean showLanes=true, fastMode=true;
  private String latestRoadSign="", checkingRoadSign="", lastSpokenRoadSign="";
  private long lastFamilyCheck=0,lastFamilySeen=0,lastLaneCheck=0,lastNoEntryCheck=0;
  private String checkingFamily="",confirmedFamily="";
  private int familyHits=0;
  private long familyCandidateAt=0,confirmedFamilyAt=0;
  private int roadSignHits=0;
  private long roadSignAt=0, checkingRoadSignAt=0, lastSpokenRoadSignAt=0;
  private int candidate=-1, confirmations=0, announced=-1, confirmed=-1;
  private long candidateAt=0,lastFrameAt=0,confirmedAt=0,announcedAt=0;
  // Only a throttle: actual scan rate depends on the camera and ML Kit processing.
  private static final long FRAME_INTERVAL_MS=120;
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
    layout.addView(label("Speed limits • road signs • experimental lane overlay",12,0xffffffff));
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
    otherSign=label("ROAD SIGN: scanning…",17,0xfff6cb6f);
    layout.addView(otherSign);
    laneStatus=label("LANE MARKINGS: scanning…",12,0xffaedac8);
    layout.addView(laneStatus);
    familyStatus=label("RSA WARNING SIGN FAMILIES: scanning…",12,0xfff6cb6f);
    layout.addView(familyStatus);
    LinearLayout row=new LinearLayout(this);
    Button toggle=new Button(this);toggle.setText("Voice ON");
    toggle.setOnClickListener(v->{voice=!voice;toggle.setText(voice?"Voice ON":"Voice OFF");if(!voice && tts!=null)tts.stop();});
    row.addView(toggle,new LinearLayout.LayoutParams(0,dp(50),1));
    Button demo=new Button(this);demo.setText("DEMO 50");
    demo.setOnClickListener(v->{status.setText("Demo (not a camera detection)");speak("Demo. Possible speed limit 50 kilometres per hour");});
    row.addView(demo,new LinearLayout.LayoutParams(0,dp(50),1));
    Button laneButton=new Button(this);laneButton.setText("LANES ON");
    laneButton.setOnClickListener(v->{
      showLanes=!showLanes;
      laneButton.setText(showLanes?"LANES ON":"LANES OFF");
      marker.enableLanes(showLanes);
      if(!showLanes)laneStatus.setText("LANE OVERLAY: off");
    });
    row.addView(laneButton,new LinearLayout.LayoutParams(0,dp(50),1));
    layout.addView(row);
    Button fastButton=new Button(this);
    fastButton.setText("SPEED PRIORITY: ON");
    fastButton.setOnClickListener(v->{
      fastMode=!fastMode;
      fastButton.setText(fastMode?"SPEED PRIORITY: ON":"SPEED PRIORITY: OFF");
    });
    layout.addView(fastButton,new LinearLayout.LayoutParams(-1,dp(45)));
    Button list=new Button(this);
    list.setText("RSA IRISH SIGN LIST (SUPPORTED / REFERENCE)");
    list.setOnClickListener(v->new AlertDialog.Builder(this)
      .setTitle("Irish road signs — RSA reference")
      .setMessage(IrishSignCatalog.reference())
      .setPositiveButton("Close",(dialog,which)->dialog.dismiss())
      .show());
    layout.addView(list,new LinearLayout.LayoutParams(-1,dp(48)));
    layout.addView(label("Experimental only • possible false alerts. NOT lane guidance. Follow signs and markings. Do not operate while driving.",12,0xffffffff));
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
      // Run all expensive colour, lane and sign computations away from the UI thread.
      // Close the camera frame only after OCR and detection are complete.
      ocr.process(InputImage.fromBitmap(image,0))
        .addOnCompleteListener(executor,task->{
          try{
            if(task.isSuccessful())recognise(task.getResult(),image);
            else runOnUiThread(()->status.setText("Camera text recognition retrying…"));
          }catch(Exception e){
            runOnUiThread(()->status.setText("Recognition retrying…"));
          }finally{
            image.recycle();
            frame.close();
          }
        });
    }catch(Exception error){if(bm!=null && !bm.isRecycled())bm.recycle();frame.close();}
  }
  // Runs on the camera analysis executor, not the Android main thread.
  // Speed is evaluated first; other recognition runs at lower frequency.
  private void recognise(Text result,Bitmap image){
    final long now=SystemClock.elapsedRealtime();
    final int width=image.getWidth(),height=image.getHeight();
    Integer speed=null;
    Rect speedRect=null;
    int largest=0;
    String signText=null;
    Rect signRect=null;
    boolean sawNumeric=false;

    for(Text.TextBlock block:result.getTextBlocks()){
      for(Text.Line line:block.getLines()){
        for(Text.Element e:line.getElements()){
          String txt=e.getText().trim().toUpperCase(Locale.ROOT)
              .replaceAll("\\s+","");
          if(!txt.matches("[0-9OI]{2,3}"))continue;
          txt=txt.replace('O','0').replace('I','1');
          int n;
          try {n=Integer.parseInt(txt);}catch(NumberFormatException ex){continue;}
          if(!LIMITS.contains(n))continue;
          Rect box=e.getBoundingBox();
          if(box==null || box.height()<8)continue;
          sawNumeric=true;
          if(!redRing(image,box))continue;
          int area=box.width()*box.height();
          if(area>largest){largest=area;speed=n;speedRect=new Rect(box);}
        }
        // Occasionally ML Kit groups the digits as "5 0" rather than one element.
        if(speed==null && line.getText().matches("\\s*[0-9]\\s+[0-9]\\s*")){
          String compact=line.getText().replaceAll("\\s+","");
          int n=Integer.parseInt(compact);
          Rect box=line.getBoundingBox();
          if(LIMITS.contains(n) && box!=null && box.height()>=8){
            sawNumeric=true;
            if(redRing(image,box)){speed=n;speedRect=new Rect(box);}
          }
        }
      }
    }
    // Textual road signs are lightweight, but only start after speed OCR.
    for(Text.TextBlock block:result.getTextBlocks()){
      String b=identifyRoadSign(block.getText(),block.getBoundingBox(),image);
      if(b!=null){signText=b;signRect=block.getBoundingBox();}
      for(Text.Line line:block.getLines()){
        String candidateSign=identifyRoadSign(line.getText(),line.getBoundingBox(),image);
        if(candidateSign!=null && (signText==null||"STOP".equals(candidateSign))){
          signText=candidateSign;
          signRect=line.getBoundingBox();
        }
      }
    }

    final boolean runFamily=now-lastFamilyCheck>(fastMode?2200:1100);
    RoadVision.SignGuess family=null;
    if(runFamily){
      lastFamilyCheck=now;
      family=RoadVision.possibleIrishWarningFamily(image);
    }
    final boolean runLane=showLanes && now-lastLaneCheck>(fastMode?1150:650);
    RoadVision.LaneEstimate lane=null;
    if(runLane){
      lastLaneCheck=now;
      lane=RoadVision.lanes(image);
    }
    if(signText==null && now-lastNoEntryCheck>(fastMode?2400:1250)){
      lastNoEntryCheck=now;
      RoadVision.SignGuess noEntry=RoadVision.possibleNoEntry(image);
      if(noEntry!=null){signText=noEntry.label;signRect=noEntry.rect;}
    }
    final Integer detectedSpeed=speed;
    final Rect detectedRect=speedRect;
    final boolean detectedNumeric=sawNumeric;
    final String roadSign=signText;
    final Rect roadSignRect=signRect;
    final RoadVision.SignGuess familyGuess=family;
    final RoadVision.LaneEstimate laneGuess=lane;
    runOnUiThread(()->{
      if(isFinishing()||isDestroyed())return;
      if(runFamily)updateFamily(familyGuess,now,width,height);
      if(laneGuess!=null){
        marker.markLanes(laneGuess);
        laneStatus.setText("LANE: "+laneGuess.summary()+" (experimental)");
      }
      checkRoadSign(roadSign,roadSignRect,now,width,height);
      applySpeed(detectedSpeed,detectedRect,detectedNumeric,now,width,height);
    });
  }

  private void applySpeed(Integer found,Rect bestRect,boolean sawNumeric,long now,int width,int height){
    if(found==null){
      if(candidateAt>0 && now-candidateAt>1600){candidate=-1;confirmations=0;}
      updateStatus(now);
      if(sawNumeric && candidate<0){
        status.setText("Numbers seen; checking for a red speed-limit ring");
      }
      return;
    }
    marker.mark(bestRect,width,height,found);
    if(found!=candidate || now-candidateAt>1600){
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

  private String identifyRoadSign(String raw,Rect rect,Bitmap image){
    if(raw==null)return null;
    String text=java.text.Normalizer.normalize(raw,java.text.Normalizer.Form.NFD)
      .replaceAll("\\p{M}+","").toUpperCase(Locale.ROOT)
      .replaceAll("[^A-Z ]"," ").replaceAll(" +"," ").trim();
    if(text.isEmpty())return null;
    if(text.matches(".*\\bSTOP\\b.*") && redNear(image,rect))return "STOP";
    if((text.contains("YIELD") || text.contains("GEILL SLI")
        || text.contains("GEILL")) && redNear(image,rect))return "YIELD";
    if(text.contains("NO ENTRY"))return "NO ENTRY";
    if(text.contains("END OF DETOUR") || text.contains("DEIREADH AN TIMPILL"))return "END OF DETOUR";
    if(text.contains("NO RIGHT TURN"))return "NO RIGHT TURN";
    if(text.contains("NO LEFT TURN"))return "NO LEFT TURN";
    if(text.contains("NO U TURN"))return "NO U TURN";
    if(text.contains("ROAD CLOSED") || text.contains("BOTHAR DUNTA"))return "ROAD CLOSED";
    if(text.contains("DETOUR") || text.contains("DIVERSION") || text.contains("TIMPILL"))return "DIVERSION";
    if(text.contains("BUS LANE"))return "BUS LANE";
    if(text.contains("CYCLE LANE") || text.contains("CYCLE TRACK"))return "CYCLE LANE";
    if(text.contains("ONE WAY"))return "ONE WAY";
    if(text.contains("KEEP LEFT"))return "KEEP LEFT";
    if(text.contains("KEEP RIGHT"))return "KEEP RIGHT";
    if(text.contains("ROADWORKS") || text.contains("ROAD WORKS"))return "ROADWORKS";
    if(text.contains("ROAD MARKINGS"))return "ROAD MARKINGS";
    if(text.contains("TEMPORARY TRAFFIC") || text.contains("TEMPORARY SIGNALS"))return "TEMPORARY TRAFFIC SIGNALS";
    if(text.contains("LANE CLOSED") || text.contains("LANE CLOSURE"))return "LANE CLOSED";
    if(text.contains("NEW ROAD LAYOUT"))return "NEW ROAD LAYOUT";
    if(text.contains("FLAGMAN") || text.contains("FLAGGER"))return "FLAGMAN AHEAD";
    if(text.contains("SLOW") || text.contains("GO MALL"))return "SLOW";
    if(text.matches("GO") || text.matches("TEIGH")){
      if(greenNear(image,rect))return "ROADWORKS GO";
    }
    if(text.contains("CONCEALED ENTRANCE"))return "CONCEALED ENTRANCE";
    if(text.contains("PEDESTRIAN CROSSING"))return "PEDESTRIAN CROSSING";
    if(text.contains("NO OVERTAKING"))return "NO OVERTAKING";
    if(text.matches(".*\\bSCHOOL\\b.*"))return "SCHOOL";
    return null;
  }
  private boolean greenNear(Bitmap image,Rect rect){
    if(rect==null)return false;
    int pad=Math.max(12,rect.height());
    int x0=Math.max(0,rect.left-pad),x1=Math.min(image.getWidth()-1,rect.right+pad);
    int y0=Math.max(0,rect.top-pad),y1=Math.min(image.getHeight()-1,rect.bottom+pad);
    if(x1<=x0||y1<=y0)return false;
    int count=0;
    for(int y=y0;y<=y1;y+=Math.max(2,(y1-y0)/12)){
      for(int x=x0;x<=x1;x+=Math.max(2,(x1-x0)/12)){
        int p=image.getPixel(x,y),g=Color.green(p);
        if(g>105 && g>Color.red(p)*1.25f && g>Color.blue(p)*1.20f)count++;
      }
    }
    return count>=12;
  }
  private void updateFamily(RoadVision.SignGuess family,long now,int w,int h){
    if(family!=null){
      lastFamilySeen=now;
      marker.markWarningFamily(family.rect,w,h,family.label);
      if(!family.label.equals(checkingFamily) || now-familyCandidateAt>2400){
        checkingFamily=family.label;
        familyHits=1;
      }else familyHits++;
      familyCandidateAt=now;
      if(familyHits>=2){
        confirmedFamily=family.label;
        confirmedFamilyAt=now;
        checkingFamily="";
        familyHits=0;
      }
    } else if(now-familyCandidateAt>2500) {
      checkingFamily="";
      familyHits=0;
    }
    if(!checkingFamily.isEmpty()){
      familyStatus.setText("CHECKING: "+checkingFamily+" (symbol unknown)");
    }else if(!confirmedFamily.isEmpty() && now-confirmedFamilyAt<12000){
      familyStatus.setText("POSSIBLE RSA SIGN: "+confirmedFamily+" (symbol unknown)");
    }else if(now-lastFamilySeen>5000){
      familyStatus.setText("IRISH WARNING / ROADWORKS SIGNS: scanning…");
    }
  }
  private boolean redNear(Bitmap image,Rect rect){
    if(rect==null)return false;
    int pad=Math.max(10,rect.height());
    int x0=Math.max(0,rect.left-pad),x1=Math.min(image.getWidth()-1,rect.right+pad);
    int y0=Math.max(0,rect.top-pad),y1=Math.min(image.getHeight()-1,rect.bottom+pad);
    if(x1<=x0||y1<=y0)return false;
    int red=0;
    for(int y=y0;y<=y1;y+=Math.max(2,(y1-y0)/12)){
      for(int x=x0;x<=x1;x+=Math.max(2,(x1-x0)/12)){
        int p=image.getPixel(x,y);
        if(Color.red(p)>115 && Color.red(p)>Color.green(p)*1.5f
          && Color.red(p)>Color.blue(p)*1.45f)red++;
      }
    }
    return red>=10;
  }
  private void checkRoadSign(String sign,Rect bounds,long now,int imageWidth,int imageHeight){
    if(sign==null){
      if(checkingRoadSignAt>0 && now-checkingRoadSignAt>2000){
        checkingRoadSign="";roadSignHits=0;
      }
    }else{
      if(bounds!=null)marker.markRoadSign(bounds,imageWidth,imageHeight,sign);
      if(!sign.equals(checkingRoadSign) || now-checkingRoadSignAt>2500){
        checkingRoadSign=sign;roadSignHits=1;
      }else roadSignHits++;
      checkingRoadSignAt=now;
      if(roadSignHits>=2){
        latestRoadSign=sign;
        roadSignAt=now;
        // Never give a positive "go" instruction from uncertain camera recognition.
        if(!"ROADWORKS GO".equals(sign) &&
            (!sign.equals(lastSpokenRoadSign)||now-lastSpokenRoadSignAt>90000)){
          lastSpokenRoadSign=sign;
          lastSpokenRoadSignAt=now;
          speak("Possible "+sign.toLowerCase(Locale.UK).replace("yield","give way")+" sign");
        }
        checkingRoadSign="";roadSignHits=0;
      }
    }
    if(!checkingRoadSign.isEmpty()){
      otherSign.setText("CHECKING POSSIBLE: "+checkingRoadSign);
    }else if(latestRoadSign.isEmpty()){
      otherSign.setText("ROAD SIGNS: scanning…");
    }else if(now-roadSignAt>30000){
      otherSign.setText("OLD SIGN: "+latestRoadSign+" (stale)");
    }else{
      otherSign.setText("LAST POSSIBLE SIGN: "+latestRoadSign);
    }
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
    private final Paint familyPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roadPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lanePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Rect speedBox,roadBox,familyBox;
    private String familyLabel="";
    private String roadLabel="";
    private int imgW,imgH,detectedLimit;
    private long speedMarkedAt=0,roadMarkedAt=0,laneMarkedAt=0,familyMarkedAt=0;
    private boolean lanesEnabled=true;
    private RoadVision.LaneEstimate laneEstimate;
    SignOverlay(){
      super(MainActivity.this);
      setClickable(false);
      outline.setColor(0xff75edb9);
      outline.setStyle(Paint.Style.STROKE);
      outline.setStrokeWidth(dp(3));
      roadPaint.setColor(0xffffd36e);
      roadPaint.setStyle(Paint.Style.STROKE);
      roadPaint.setStrokeWidth(dp(3));
      familyPaint.setColor(0xffffab52);
      familyPaint.setStyle(Paint.Style.STROKE);
      familyPaint.setStrokeWidth(dp(3));
      lanePaint.setColor(0xff5ce8ff);
      lanePaint.setStyle(Paint.Style.STROKE);
      lanePaint.setStrokeWidth(dp(3));
      lanePaint.setPathEffect(new DashPathEffect(new float[]{dp(14),dp(8)},0));
      textPaint.setColor(0xff75edb9);
      textPaint.setTypeface(Typeface.DEFAULT_BOLD);
      textPaint.setTextSize(dp(15));
      textPaint.setShadowLayer(dp(2),0,0,0xff000000);
    }
    void markWarningFamily(Rect box,int width,int height,String family){
      familyBox=new Rect(box);
      familyLabel=family;
      imgW=width;imgH=height;
      familyMarkedAt=SystemClock.elapsedRealtime();
      invalidate();postInvalidateDelayed(1500);
    }
    void enableLanes(boolean enabled){lanesEnabled=enabled;invalidate();}
    void markLanes(RoadVision.LaneEstimate estimate) {
      laneEstimate=estimate;
      laneMarkedAt=SystemClock.elapsedRealtime();
      invalidate();
    }
    void mark(Rect box,int width,int height,int speed){
      int pad=Math.max(10,box.height());
      speedBox=new Rect(box.left-pad,box.top-pad,box.right+pad,box.bottom+pad);
      imgW=width;imgH=height;detectedLimit=speed;
      speedMarkedAt=SystemClock.elapsedRealtime();
      invalidate();postInvalidateDelayed(1400);
    }
    void markRoadSign(Rect box,int width,int height,String label){
      int pad=Math.max(8,box.height()/2);
      roadBox=new Rect(box.left-pad,box.top-pad,box.right+pad,box.bottom+pad);
      roadLabel=label;
      imgW=width;imgH=height;
      roadMarkedAt=SystemClock.elapsedRealtime();
      invalidate();postInvalidateDelayed(1400);
    }
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);
      long now=SystemClock.elapsedRealtime();
      if(lanesEnabled && laneEstimate!=null && now-laneMarkedAt<1400){
        RoadVision.LaneEstimate lane=laneEstimate;
        float ratio=Math.max(getWidth()/(float)lane.imageWidth,getHeight()/(float)lane.imageHeight);
        float xoff=(getWidth()-lane.imageWidth*ratio)/2f;
        float yoff=(getHeight()-lane.imageHeight*ratio)/2f;
        if(lane.leftVisible()){
          c.drawLine(lane.leftFar*ratio+xoff,lane.yFar*ratio+yoff,
            lane.leftNear*ratio+xoff,lane.yNear*ratio+yoff,lanePaint);
        }
        if(lane.rightVisible()){
          c.drawLine(lane.rightFar*ratio+xoff,lane.yFar*ratio+yoff,
            lane.rightNear*ratio+xoff,lane.yNear*ratio+yoff,lanePaint);
        }
      }
      if(imgW<=0||imgH<=0)return;
      float ratio=Math.max(getWidth()/(float)imgW,getHeight()/(float)imgH);
      float xoff=(getWidth()-imgW*ratio)/2f;
      float yoff=(getHeight()-imgH*ratio)/2f;
      if(speedBox!=null && now-speedMarkedAt<1300){
        RectF area=new RectF(speedBox.left*ratio+xoff,speedBox.top*ratio+yoff,
          speedBox.right*ratio+xoff,speedBox.bottom*ratio+yoff);
        c.drawRoundRect(area,dp(7),dp(7),outline);
        textPaint.setColor(0xff75edb9);
        c.drawText("CHECK "+detectedLimit,Math.max(dp(4),area.left),
          Math.max(dp(18),area.top-dp(5)),textPaint);
      }
      if(familyBox!=null && now-familyMarkedAt<1400){
        RectF area=new RectF(familyBox.left*ratio+xoff,familyBox.top*ratio+yoff,
            familyBox.right*ratio+xoff,familyBox.bottom*ratio+yoff);
        c.drawRoundRect(area,dp(7),dp(7),familyPaint);
        textPaint.setColor(0xffffab52);
        c.drawText(familyLabel,Math.max(dp(4),area.left),
            Math.max(dp(20),area.top-dp(7)),textPaint);
      }
      if(roadBox!=null && now-roadMarkedAt<1300){
        RectF area=new RectF(roadBox.left*ratio+xoff,roadBox.top*ratio+yoff,
          roadBox.right*ratio+xoff,roadBox.bottom*ratio+yoff);
        c.drawRoundRect(area,dp(7),dp(7),roadPaint);
        textPaint.setColor(0xffffd36e);
        c.drawText("POSSIBLE "+roadLabel,Math.max(dp(4),area.left),
          Math.max(dp(20),area.top-dp(7)),textPaint);
      }
    }
  }
  private boolean redRing(Bitmap image,Rect box){
    // The OCR bounding rectangle is smaller than a circular sign; sample
    // several ring radii around it. Keep red-ring verification to avoid
    // announcing arbitrary numbers as legal speed limits.
    double cx=box.exactCenterX(),cy=box.exactCenterY();
    double scale=Math.max(box.height(),box.width()*.68);
    for(double factor:new double[]{.83,1.05,1.30,1.60,1.9,2.25}){
      double radius=scale*factor;
      int redCount=0,valid=0,quadrants=0;
      for(int q=0;q<4;q++){
        int qRed=0;
        for(int k=0;k<8;k++){
          double theta=(q*8+k)*2.0*Math.PI/32;
          int x=(int)Math.round(cx+radius*Math.cos(theta));
          int y=(int)Math.round(cy+radius*Math.sin(theta));
          if(x<0||y<0||x>=image.getWidth()||y>=image.getHeight())continue;
          valid++;
          int pix=image.getPixel(x,y);
          int r=Color.red(pix),g=Color.green(pix),b=Color.blue(pix);
          if(r>108 && r>g*1.35 && r>b*1.32){redCount++;qRed++;}
        }
        if(qRed>=1)quadrants++;
      }
      if(valid>=24 && redCount>=7 && quadrants>=3)return true;
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