package ie.speedvoice;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import android.Manifest;
public class GpsService extends Service implements LocationListener {
 private LocationManager lm;
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent i,int flags,int id){
 if(i!=null&&"STOP".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
 if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopSelf();return START_NOT_STICKY;}
 NotificationManager nm=getSystemService(NotificationManager.class);
 nm.createNotificationChannel(new NotificationChannel("monitor","SpeedVoice monitoring",NotificationManager.IMPORTANCE_LOW));
 PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
 PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,GpsService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE);
 Notification note=new Notification.Builder(this,"monitor").setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle("SpeedVoice monitoring").setContentText("GPS speed • Google Maps must be visible").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder((android.graphics.drawable.Icon)null,"Stop",stop).build()).build();
 try{startForeground(1,note);lm=getSystemService(LocationManager.class);lm.removeUpdates(this);State.clearGps();State.running=true;State.status="Waiting for Google Maps and GPS";lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this,Looper.getMainLooper());}
 catch(Exception e){State.status="GPS unavailable: check permission and location";stopSelf();}
 return START_NOT_STICKY;
 }
 @Override public void onLocationChanged(Location l){
 long now=SystemClock.elapsedRealtime(),age=now-l.getElapsedRealtimeNanos()/1000000;
 if(!l.hasSpeed()||age<0||age>3000||!l.hasAccuracy()||l.getAccuracy()>30||(l.hasSpeedAccuracy()&&l.getSpeedAccuracyMetersPerSecond()>2)){State.clearGps();return;}
 State.speed=l.getSpeed()*3.6f;State.speedAt=now;
 }
 @Override public void onProviderDisabled(String provider){State.clearGps();}
 @Override public void onDestroy(){if(lm!=null)lm.removeUpdates(this);State.running=false;State.limit=-1;State.clearGps();State.status="Stopped";stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}