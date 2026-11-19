package ie.speedvoice;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import java.util.ArrayList;
import android.net.Uri;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.*;
import java.util.Locale;
public class MainActivity extends Activity {
 private TextView status;
 private final Handler h=new Handler(Looper.getMainLooper());
 private TextToSpeech testVoice;
 private final Runnable refresh=new Runnable(){public void run(){float s=State.freshSpeed();status.setText(State.status+"\nSpeed: "+(Float.isNaN(s)?"Unavailable":Math.round(s)+" km/h")+"\nLimit: "+(State.limit<0?"Unavailable":State.limit+" km/h")+"\nReader: "+State.readerDiagnostic);h.postDelayed(this,1000);}};
 @Override public void onCreate(Bundle b){super.onCreate(b);
 ScrollView sc=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);int p=(int)(24*getResources().getDisplayMetrics().density);l.setPadding(p,p,p,p);sc.addView(l);setContentView(sc);
 TextView title=new TextView(this);title.setText("SpeedVoice v0.8");title.setTextSize(32);l.addView(title);
 TextView intro=new TextView(this);intro.setText("Experimental • Ireland / km/h\nReads visible Google Maps labels or attempts local recognition of red circular signs. If recognition fails, alerts stop. GPS and map data may be wrong. Use road signs and your vehicle speedometer. Set up parked. No screenshots saved or transmitted.");l.addView(intro);
 status=new TextView(this);status.setTextSize(20);status.setPadding(0,p,0,p);l.addView(status);
 add(l,"Demo mode — test indoors",v->{stopService(new Intent(this,GpsService.class));startActivity(new Intent(this,DemoActivity.class));});
 add(l,"1. Enable Maps reader (Accessibility)",v->requireConsent(()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
 add(l,"2. Start monitoring (allow precise GPS)",v->startMonitor());
 add(l,"3. Open Google Maps",v->{Intent i=getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");if(i!=null)startActivity(i);else Toast.makeText(this,"Google Maps not installed",Toast.LENGTH_SHORT).show();});
 add(l,"Stop monitoring",v->stopService(new Intent(this,GpsService.class)));
 add(l,"Safety and privacy notice",v->showDisclosure(null));
 add(l,"Read privacy notice on GitHub",v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/drhidayatadnan-wq/Speedvoice/blob/main/PRIVACY.md")));}catch(RuntimeException e){Toast.makeText(this,"No browser available",Toast.LENGTH_LONG).show();}});
 add(l,"Withdraw acknowledgement / stop monitoring",v->{Consent.revoke(this);getSharedPreferences("prefs",MODE_PRIVATE).edit().remove("car_address").apply();getSystemService(android.app.NotificationManager.class).cancel(81);stopService(new Intent(this,GpsService.class));State.running=false;State.limit=-1;State.clearGps();Toast.makeText(this,"Monitoring stopped. Disable accessibility access in Android Settings if no longer needed.",Toast.LENGTH_LONG).show();});
 add(l,"Test voice",v->{if(testVoice!=null)testVoice.shutdown();testVoice=new TextToSpeech(this,result->{if(result==TextToSpeech.SUCCESS&&testVoice!=null){testVoice.setLanguage(Locale.UK);testVoice.speak("SpeedVoice test. Speed limit fifty kilometres per hour.",TextToSpeech.QUEUE_FLUSH,null,"test");}});});
 TextView t=new TextView(this);t.setText("Warning threshold (not permission to exceed the limit)");l.addView(t);
 RadioGroup g=new RadioGroup(this);for(int n:new int[]{0,3,5}){RadioButton r=new RadioButton(this);r.setId(100+n);r.setText("+"+n+" km/h");g.addView(r);}
 g.check(100+getSharedPreferences("prefs",MODE_PRIVATE).getInt("tolerance",0));g.setOnCheckedChangeListener((group,id)->getSharedPreferences("prefs",MODE_PRIVATE).edit().putInt("tolerance",id-100).apply());l.addView(g);
 final SharedPreferences prefs=getSharedPreferences("prefs",MODE_PRIVATE);
 CheckBox recovery=new CheckBox(this);recovery.setText("Announce return to detected limit");recovery.setChecked(prefs.getBoolean("recovery_voice",true));recovery.setOnCheckedChangeListener((b1,checked)->prefs.edit().putBoolean("recovery_voice",checked).apply());l.addView(recovery);
 add(l,"Choose car Bluetooth device",v->requireConsent(()->chooseCar()));
 add(l,"Disable car connection reminders",v->{prefs.edit().remove("car_address").apply();getSystemService(android.app.NotificationManager.class).cancel(81);Toast.makeText(this,"Car reminders disabled",Toast.LENGTH_SHORT).show();});
 TextView bt=new TextView(this);bt.setText("Car connection reminder: tap the notification while parked to open SpeedVoice and start monitoring. Android restricts unattended GPS starts.");l.addView(bt);
 TextView a=new TextView(this);a.setText("Overspeed alert style");l.addView(a);
 RadioGroup styles=new RadioGroup(this);String[] names={"Voice only","Single beep + voice","Double beep + voice","Double beep only","Graded beeps + voice"};
 for(int i=0;i<names.length;i++){RadioButton option=new RadioButton(this);option.setId(200+i);option.setText(names[i]);styles.addView(option);}
 styles.check(200+prefs.getInt("alert_mode",2));styles.setOnCheckedChangeListener((group,id)->prefs.edit().putInt("alert_mode",id-200).apply());l.addView(styles);
 TextView repeat=new TextView(this);repeat.setText("Repeat warning while overspeeding");l.addView(repeat);
 RadioGroup intervals=new RadioGroup(this);for(int n:new int[]{10,20,30}){RadioButton option=new RadioButton(this);option.setId(300+n);option.setText(n+" seconds");intervals.addView(option);}
 intervals.check(300+prefs.getInt("alert_interval",10));intervals.setOnCheckedChangeListener((group,id)->prefs.edit().putInt("alert_interval",id-300).apply());l.addView(intervals);
 TextView vol=new TextView(this);vol.setText("Alert volume: "+prefs.getInt("alert_volume",100)+"% of system volume");l.addView(vol);
 SeekBar volume=new SeekBar(this);volume.setMax(75);volume.setProgress(prefs.getInt("alert_volume",100)-25);l.addView(volume);
 volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int value,boolean fromUser){int percent=value+25;vol.setText("Alert volume: "+percent+"% of system volume");if(fromUser)prefs.edit().putInt("alert_volume",percent).apply();}});

 }
 private void chooseCar(){
  if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},23);return;}
  if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},24);return;}
  try{
   BluetoothManager bm=getSystemService(BluetoothManager.class);BluetoothAdapter adapter=bm==null?null:bm.getAdapter();
   if(adapter==null){Toast.makeText(this,"Bluetooth unavailable",Toast.LENGTH_LONG).show();return;}
   ArrayList<BluetoothDevice> devices=new ArrayList<>(adapter.getBondedDevices());
   if(devices.isEmpty()){Toast.makeText(this,"Pair your car in Android Bluetooth settings first",Toast.LENGTH_LONG).show();return;}
   String[] names=new String[devices.size()];for(int i=0;i<names.length;i++){BluetoothDevice d=devices.get(i);names[i]=(d.getName()==null?"Paired device":d.getName())+" • "+d.getAddress();}
   new AlertDialog.Builder(this).setTitle("Choose your car").setItems(names,(dialog,which)->{getSharedPreferences("prefs",MODE_PRIVATE).edit().putString("car_address",devices.get(which).getAddress()).apply();Toast.makeText(this,"Car connection reminders enabled",Toast.LENGTH_SHORT).show();}).setNegativeButton("Cancel",null).show();
  }catch(RuntimeException e){Toast.makeText(this,"Cannot access paired devices",Toast.LENGTH_LONG).show();}
 }
 private void requireConsent(Runnable action){if(Consent.accepted(this)){action.run();return;}showDisclosure(action);}
 private void showDisclosure(Runnable action){
  new AlertDialog.Builder(this).setTitle("Experimental — safety and privacy")
   .setMessage(Consent.DISCLOSURE)
   .setPositiveButton("I understand and agree",(d,w)->{Consent.accept(this);if(action!=null)action.run();})
   .setNegativeButton("Decline / withdraw",(d,w)->{Consent.revoke(this);stopService(new Intent(this,GpsService.class));State.running=false;State.limit=-1;State.clearGps();})
   .setCancelable(true).show();
 }
 private void add(LinearLayout l,String label,View.OnClickListener listener){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(listener);l.addView(b);}
 private void startMonitor(){if(!Consent.accepted(this)){requireConsent(()->startMonitor());return;}if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},10);return;}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);return;}launch();}
 private void launch(){if(!Consent.accepted(this))return;try{startForegroundService(new Intent(this,GpsService.class));}catch(Exception e){Toast.makeText(this,"Unable to start monitoring",Toast.LENGTH_LONG).show();}}
 @Override public void onRequestPermissionsResult(int code,String[] perms,int[] result){super.onRequestPermissionsResult(code,perms,result);if(code==10&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)startMonitor();else if(code==11)launch();else if((code==23||code==24)&&result.length>0&&result[0]==PackageManager.PERMISSION_GRANTED)chooseCar();}
 @Override protected void onResume(){super.onResume();h.removeCallbacks(refresh);h.post(refresh);}
 @Override protected void onPause(){h.removeCallbacks(refresh);super.onPause();}
 @Override protected void onDestroy(){if(testVoice!=null)testVoice.shutdown();super.onDestroy();}
}