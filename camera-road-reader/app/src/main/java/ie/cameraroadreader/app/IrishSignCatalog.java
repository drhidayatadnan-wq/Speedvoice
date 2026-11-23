package ie.cameraroadreader.app;

/**
 * Road sign names selected from the Road Safety Authority Rules of the Road,
 * especially section 6. Status states actual prototype capability, not
 * whether the sign exists in Ireland.
 *
 * Source: https://www.rsa.ie/services/learner-drivers/resources/rules-of-the-road
 */
final class IrishSignCatalog {
  private IrishSignCatalog() {}
  static String reference() {
    return "BASED ON THE RSA RULES OF THE ROAD\n"
      + "The list below is a reference. A sign being listed does NOT mean the phone can recognise its symbol.\n\n"
      + "REGULATORY\n"
      + "✓ Speed limit 20/30/40/50/60/80/100/120 — numeric OCR + red-ring check\n"
      + "✓ STOP — text with red-colour check\n"
      + "✓ YIELD / GÉILL SLÍ — text with red-colour check\n"
      + "✓ No entry to vehicles — red disc with white bar (basic pattern check)\n"
      + "◐ No entry — only if printed text is readable\n"
      + "◐ One way / Keep left / Keep right — only if readable text is present\n"
      + "○ Turn left / Turn right / Straight ahead — symbol-only detection not installed\n"
      + "○ Parking prohibited / No U-turn / No overtaking — symbol-only detection not installed\n"
      + "○ Alternate rural 60 km/h striped sign — not recognised\n\n"
      + "WARNING SIGNS (YELLOW DIAMONDS)\n"
      + "◐ Yellow warning sign category — experimental colour/shape check\n"
      + "○ Roundabout ahead\n"
      + "○ Series of dangerous bends / dangerous corner\n"
      + "○ School ahead (symbol)\n"
      + "○ Crossroads / T-junction\n"
      + "○ Two-way traffic / pedestrian crossing ahead\n"
      + "The exact black symbol is NOT recognised in this prototype.\n\n"
      + "ROADWORKS (ORANGE DIAMONDS)\n"
      + "◐ Orange roadworks sign category — experimental colour/shape check\n"
      + "◐ Roadworks / Road markings / temporary traffic signals — only with readable text\n"
      + "○ Roadworks ahead (worker symbol)\n"
      + "○ Flagman ahead / temporary signals (symbol)\n"
      + "○ Temporary lane closures and lane shifts (symbols)\n\n"
      + "LANES AND ROAD MARKINGS\n"
      + "◐ Possible white/yellow painted lines — experimental camera overlay\n"
      + "○ Stop line / Yield line / centre line classification — not installed\n"
      + "○ Motorway green arrow / red X lane controls — not installed\n"
      + "○ Bus and cycle lane icons — not installed\n\n"
      + "KEY: ✓ basic detection; ◐ partial or unverified category; ○ reference only.\n\n"
      + "Visual alerts are NOT legal road-speed, road-sign, or lane guidance. "
      + "Do not operate or look at the phone while driving.";
  }
}
