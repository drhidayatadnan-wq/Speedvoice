package ie.speedvoice;
import android.os.SystemClock;
final class State {
 static volatile boolean running=false;
 static volatile boolean muted=false;
 static volatile String status="Idle";
 static volatile int limit=-1;
 static volatile float speed=Float.NaN;
 static volatile long speedAt=0;
 static float freshSpeed(){return SystemClock.elapsedRealtime()-speedAt<=3500?speed:Float.NaN;}
 static void clearGps(){speed=Float.NaN;speedAt=0;}
}