package ie.speedvoice;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.location.*;
import android.os.*;
import android.Manifest;

public class GpsService extends Service implements LocationListener {
 private static final String CHANNEL="monitor";
 private static final int NOTIFICATION_ID=1;
 private LocationManager lm;
 private NotificationManager nm;
 private Bitmap logo;
 private String lastDisplay="";
 private final Handler h=new Handler(Looper.getMainLooper());
 private final Runnable refresh=new Runnable(){public void run(){if(!State.running)return;updateNotification(false);h.postDelayed(this,1000);}};
 @Override public IBinder onBind(Intent i){return null;}
 private PendingIntent action(String name,int code){
  return PendingIntent.getService(this,code,new Intent(this,GpsService.class).setAction(name),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private Bitmap makeLogo(){
  Bitmap bmp=Bitmap.createBitmap(144,144,Bitmap.Config.ARGB_8888);
  Canvas c=new Canvas(bmp);c.drawColor(Color.rgb(19,38,61));
  Drawable d=getDrawable(R.drawable.speedvoice_icon_foreground);
  if(d!=null){d.setBounds(0,0,144,144);d.draw(c);}return bmp;
 }
 private String title(float speed,int limit){
  if(!State.running)return "SpeedVoice • Starting";
  if(Float.isNaN(speed)||limit<=0)return "SpeedVoice • Data unavailable";
  int tolerance=getSharedPreferences("prefs",MODE_PRIVATE).getInt("tolerance",0);
  return speed>limit+tolerance?"SpeedVoice • OVERSPEED":"SpeedVoice • Monitoring";
 }
 private String details(float speed,int limit){
  return "GPS: "+(Float.isNaN(speed)?"unavailable":Math.round(speed)+" km/h")+"  |  Limit: "+(limit<=0?"unavailable":limit+" km/h");
 }
 private Notification notification(float speed,int limit){
  boolean over=State.running&&!Float.isNaN(speed)&&limit>0&&speed>limit+getSharedPreferences("prefs",MODE_PRIVATE).getInt("tolerance",0);
  String line=details(speed,limit);
  String info=(State.muted?"Alerts muted. ":"Alerts enabled. ")+(limit<=0?"Keep Google Maps visible. ":"")+ (Float.isNaN(speed)?"Waiting for reliable GPS. ":"")+"Check road signs and vehicle speedometer.";
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  return new Notification.Builder(this,CHANNEL)
   .setSmallIcon(R.drawable.ic_stat_speedvoice).setLargeIcon(logo)
   .setColor(over?Color.rgb(215,40,40):Color.rgb(30,100,165))
   .setContentTitle(title(speed,limit)+(State.muted?" • Muted":""))
   .setContentText(line).setStyle(new Notification.BigTextStyle().bigText(line+"\n"+info))
   .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false)
   .setVisibility(Notification.VISIBILITY_PRIVATE)
   .addAction(new Notification.Action.Builder((android.graphics.drawable.Icon)null,State.muted?"Unmute":"Mute",action("TOGGLE_MUTE",2)).build())
   .addAction(new Notification.Action.Builder((android.graphics.drawable.Icon)null,"Stop",action("STOP",1)).build())
   .build();
 }
 private void updateNotification(boolean force){
  if(nm==null||!State.running)return;
  float speed=State.freshSpeed();int limit=State.limit;
  String key=title(speed,limit)+details(speed,limit)+State.muted;
  if(!force&&key.equals(lastDisplay))return;
  try{nm.notify(NOTIFICATION_ID,notification(speed,limit));lastDisplay=key;SpeedWidget.refresh(this);}catch(RuntimeException ignored){}
 }
 @Override public int onStartCommand(Intent i,int flags,int id){
  String cmd=i==null?null:i.getAction();
  if("STOP".equals(cmd)){stopSelf();return START_NOT_STICKY;}
  if("TOGGLE_MUTE".equals(cmd)){
   if(State.running){State.muted=!State.muted;updateNotification(true);}else stopSelf();
   return START_NOT_STICKY;
  }
  if(!Consent.accepted(this)){stopSelf();return START_NOT_STICKY;}
  if(State.running){updateNotification(true);return START_NOT_STICKY;}
  if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopSelf();return START_NOT_STICKY;}
  nm=getSystemService(NotificationManager.class);
  nm.createNotificationChannel(new NotificationChannel(CHANNEL,"SpeedVoice monitoring",NotificationManager.IMPORTANCE_LOW));
  logo=makeLogo();State.muted=false;State.limit=-1;State.clearGps();
  try{
   startForeground(NOTIFICATION_ID,notification(Float.NaN,-1),ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
   lm=getSystemService(LocationManager.class);lm.removeUpdates(this);
   State.running=true;State.status="Waiting for Google Maps and GPS";SpeedWidget.refresh(this);
   lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this,Looper.getMainLooper());
   h.removeCallbacks(refresh);h.post(refresh);
  }catch(Exception e){State.status="GPS unavailable: check permission and location";SpeedWidget.refresh(this);stopSelf();}
  return START_NOT_STICKY;
 }
 @Override public void onLocationChanged(Location l){
  long now=SystemClock.elapsedRealtime(),age=now-l.getElapsedRealtimeNanos()/1000000;
  if(!l.hasSpeed()||age<0||age>3000||!l.hasAccuracy()||l.getAccuracy()>30||(l.hasSpeedAccuracy()&&l.getSpeedAccuracyMetersPerSecond()>2)){State.clearGps();return;}
  State.speed=l.getSpeed()*3.6f;State.speedAt=now;
 }
 @Override public void onProviderDisabled(String provider){State.clearGps();}
 @Override public void onDestroy(){
  h.removeCallbacks(refresh);
  if(lm!=null)try{lm.removeUpdates(this);}catch(RuntimeException ignored){}
  State.running=false;State.muted=false;State.limit=-1;State.clearGps();State.status="Stopped";SpeedWidget.refresh(this);
  stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
 }
}
