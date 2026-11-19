package ie.speedvoice;
import android.Manifest;
import android.app.Activity;
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
 private final Runnable refresh=new Runnable(){public void run(){float s=State.freshSpeed();status.setText(State.status+"\nSpeed: "+(Float.isNaN(s)?"Unavailable":Math.round(s)+" km/h")+"\nLimit: "+(State.limit<0?"Unavailable":State.limit+" km/h"));h.postDelayed(this,1000);}};
 @Override public void onCreate(Bundle b){super.onCreate(b);
 ScrollView sc=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);int p=(int)(24*getResources().getDisplayMetrics().density);l.setPadding(p,p,p,p);sc.addView(l);setContentView(sc);
 TextView title=new TextView(this);title.setText("SpeedVoice");title.setTextSize(32);l.addView(title);
 TextView intro=new TextView(this);intro.setText("Experimental • Ireland / km/h\nReads visible Google Maps labels or attempts local recognition of red circular signs. If recognition fails, alerts stop. GPS and map data may be wrong. Use road signs and your vehicle speedometer. Set up parked. No screenshots saved or transmitted.");l.addView(intro);
 status=new TextView(this);status.setTextSize(20);status.setPadding(0,p,0,p);l.addView(status);
 add(l,"1. Enable Maps reader (Accessibility)",v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
 add(l,"2. Start monitoring (allow precise GPS)",v->startMonitor());
 add(l,"3. Open Google Maps",v->{Intent i=getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");if(i!=null)startActivity(i);else Toast.makeText(this,"Google Maps not installed",Toast.LENGTH_SHORT).show();});
 add(l,"Stop monitoring",v->stopService(new Intent(this,GpsService.class)));
 add(l,"Test voice",v->{if(testVoice!=null)testVoice.shutdown();testVoice=new TextToSpeech(this,result->{if(result==TextToSpeech.SUCCESS&&testVoice!=null){testVoice.setLanguage(Locale.UK);testVoice.speak("SpeedVoice test. Speed limit fifty kilometres per hour.",TextToSpeech.QUEUE_FLUSH,null,"test");}});});
 TextView t=new TextView(this);t.setText("Warning threshold (not permission to exceed the limit)");l.addView(t);
 RadioGroup g=new RadioGroup(this);for(int n:new int[]{0,3,5}){RadioButton r=new RadioButton(this);r.setId(100+n);r.setText("+"+n+" km/h");g.addView(r);}
 g.check(100+getSharedPreferences("prefs",MODE_PRIVATE).getInt("tolerance",0));g.setOnCheckedChangeListener((group,id)->getSharedPreferences("prefs",MODE_PRIVATE).edit().putInt("tolerance",id-100).apply());l.addView(g);
 }
 private void add(LinearLayout l,String label,View.OnClickListener listener){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(listener);l.addView(b);}
 private void startMonitor(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},10);return;}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);return;}launch();}
 private void launch(){try{startForegroundService(new Intent(this,GpsService.class));}catch(Exception e){Toast.makeText(this,"Unable to start monitoring",Toast.LENGTH_LONG).show();}}
 @Override public void onRequestPermissionsResult(int code,String[] perms,int[] result){super.onRequestPermissionsResult(code,perms,result);if(code==10&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)startMonitor();else if(code==11)launch();}
 @Override protected void onResume(){super.onResume();h.removeCallbacks(refresh);h.post(refresh);}
 @Override protected void onPause(){h.removeCallbacks(refresh);super.onPause();}
 @Override protected void onDestroy(){if(testVoice!=null)testVoice.shutdown();super.onDestroy();}
}