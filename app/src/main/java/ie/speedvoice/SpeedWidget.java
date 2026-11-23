package ie.speedvoice;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * User-initiated control surface; never starts location monitoring silently.
 * Starting uses a visible Activity so permissions/consent can be requested
 * and Android's foreground-service start rules are respected.
 */
public final class SpeedWidget extends AppWidgetProvider {
 static void refresh(Context context) {
  AppWidgetManager manager=AppWidgetManager.getInstance(context);
  ComponentName provider=new ComponentName(context,SpeedWidget.class);
  int[] ids=manager.getAppWidgetIds(provider);
  if(ids!=null&&ids.length>0)update(context,manager,ids);
 }
 private static void update(Context context,AppWidgetManager manager,int[] ids) {
  boolean running=State.running;
  String text=running?"Monitoring • "+(State.limit>0?State.limit+" km/h limit":"limit unavailable"):"Stopped • tap Start";
  Intent start=new Intent(context,MainActivity.class);
  start.setAction(MainActivity.WIDGET_START);
  start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent startIntent=PendingIntent.getActivity(context,101,start,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Intent stop=new Intent(context,SpeedWidget.class).setAction("ie.speedvoice.WIDGET_STOP");
  PendingIntent stopIntent=PendingIntent.getBroadcast(context,102,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  for(int id:ids) {
   RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.speed_widget);
   v.setTextViewText(R.id.widget_status,text);
   v.setOnClickPendingIntent(R.id.widget_start,startIntent);
   v.setOnClickPendingIntent(R.id.widget_stop,stopIntent);
   manager.updateAppWidget(id,v);
  }
 }
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids) {
  super.onUpdate(context,manager,ids);update(context,manager,ids);
 }
 @Override public void onReceive(Context context,Intent intent) {
  super.onReceive(context,intent);
  if("ie.speedvoice.WIDGET_STOP".equals(intent.getAction())) {
   context.stopService(new Intent(context,GpsService.class));
   State.running=false;
   State.status="Stopped";
   State.limit=-1;
   State.clearGps();
   refresh(context);
  }
 }
}
