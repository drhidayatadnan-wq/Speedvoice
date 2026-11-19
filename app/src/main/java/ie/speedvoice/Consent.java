package ie.speedvoice;
import android.content.Context;

final class Consent {
 private static final String KEY="dev_disclosure_v1";
 static final String DISCLOSURE="WORK IN PROGRESS. SpeedVoice is experimental, not a validated driving safety system. Alerts may be wrong, delayed or absent. Silence does not mean you are within the legal limit. Follow road signs and your vehicle speedometer; set up and test only while safely parked. Do not interact with your phone while driving.\n\nWhen you start live monitoring, SpeedVoice uses precise GPS location to estimate vehicle speed and Android Accessibility access to read the visible Google Maps window. It may capture and locally process a screenshot for speed-limit OCR. The app does not implement saving or uploading GPS readings or screenshots. It uses Android audio playback metadata for voice priority, not microphone recording. Speech is passed to your selected text-to-speech engine. You can withdraw acknowledgement, stop monitoring and disable permissions in Settings.\n\nDevelopment builds are provided as is, to the extent permitted by law. This acknowledgement does not waive your statutory rights.";
 static boolean accepted(Context c){return c.getSharedPreferences("prefs",Context.MODE_PRIVATE).getBoolean(KEY,false);}
 static void accept(Context c){c.getSharedPreferences("prefs",Context.MODE_PRIVATE).edit().putBoolean(KEY,true).apply();}
 static void revoke(Context c){c.getSharedPreferences("prefs",Context.MODE_PRIVATE).edit().remove(KEY).apply();}
}
