package ie.speedvoice;
import android.Manifest;
import android.app.*;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageManager;

public class CarConnectionReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  if(!BluetoothDevice.ACTION_ACL_CONNECTED.equals(i.getAction()))return;
  if(c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED||
     c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED||!Consent.accepted(c)||State.running)return;
  try{
   BluetoothDevice device=i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,BluetoothDevice.class);
   String chosen=c.getSharedPreferences("prefs",Context.MODE_PRIVATE).getString("car_address","");
   if(device==null||chosen.isEmpty()||!chosen.equals(device.getAddress()))return;
   NotificationManager nm=c.getSystemService(NotificationManager.class);
   nm.createNotificationChannel(new NotificationChannel("car_reminder","Car connection reminder",NotificationManager.IMPORTANCE_DEFAULT));
   PendingIntent open=PendingIntent.getActivity(c,81,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   nm.notify(81,new Notification.Builder(c,"car_reminder").setSmallIcon(R.drawable.ic_stat_speedvoice).setContentTitle("SpeedVoice • Car connected").setContentText("When safely parked, tap to open SpeedVoice and start monitoring.").setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build());
  }catch(RuntimeException ignored){}
 }
}
