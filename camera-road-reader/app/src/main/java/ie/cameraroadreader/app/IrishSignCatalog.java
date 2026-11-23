package ie.cameraroadreader.app;

/**
 * RSA Rules of the Road, section 6, catalogued as descriptive names.
 * The RSA sign graphics are not bundled. Detection capability is explicitly limited.
 * Source: https://www.rsa.ie/services/learner-drivers/resources/rules-of-the-road
 * NOTE: An item on this list is NOT a guarantee it can be recognised by a phone camera.
 */
final class IrishSignCatalog {
  private IrishSignCatalog() {}
  static String reference() {
    return "RSA RULES OF THE ROAD — IRISH SIGN REFERENCE\n"
      + "Prototype status, not a certified sign classifier.\n"
      + "✓ basic detector  ◐ partial or category only  ○ reference only\n\n"
      + "REGULATORY SIGNS\n"
      + "✓ 20 / 30 / 40 / 50 / 60 / 80 / 100 / 120 km/h: OCR plus red ring\n"
      + "✓ STOP (red octagon or roadworks STOP): text plus red colour\n"
      + "✓ YIELD / GÉILL SLÍ: text plus red colour\n"
      + "✓ No entry to vehicles: red disc with white horizontal bar\n"
      + "◐ No entry / one-way / keep left / keep right: OCR wording only\n"
      + "◐ No left / right / U-turn: OCR wording only\n"
      + "○ Turn left / right / straight ahead blue-circle symbols\n"
      + "○ No overtaking, no parking, width / height / weight restrictions\n"
      + "○ Alternate rural 60 km/h speed-limit sign with diagonal stripes\n\n"
      + "WARNING SIGNS — YELLOW DIAMONDS\n"
      + "◐ Yellow warning diamond: colour + coarse diamond shape only\n"
      + "○ Roundabout ahead\n"
      + "○ Crossroads / T-junction\n"
      + "○ Dangerous bend or series of bends\n"
      + "○ School ahead (children symbol)\n"
      + "○ Two-way traffic, pedestrians, slippery road\n"
      + "○ Chevron board: sharp change of direction\n"
      + "Exact warning pictograms are NOT identified.\n\n"
      + "ROADWORKS — ORANGE SIGNS\n"
      + "◐ Orange roadworks diamond: colour + coarse diamond shape only\n"
      + "◐ ROADWORKS, ROAD CLOSED, DETOUR / DIVERSION, END OF DETOUR\n"
      + "◐ LANE CLOSED, NEW ROAD LAYOUT and TEMPORARY SIGNALS: OCR wording\n"
      + "◐ ROADWORKS GO / TÉIGH: wording plus green colour\n"
      + "○ Worker, flagman or temporary-signals pictograms\n"
      + "○ Temporary lane-shift arrow pictograms\n\n"
      + "INFORMATION AND LANE CONTROLS\n"
      + "◐ BUS LANE, CYCLE TRACK: OCR wording only\n"
      + "◐ White / yellow lane markings: approximate blue line overlay\n"
      + "○ Motorway lane-open green arrow or closed red X\n"
      + "○ Centre, STOP and YIELD road-line classifications\n"
      + "○ Direction sign arrows and route-number interpretation\n\n"
      + "Critical: sign / lane alerts may be missed or wrong.\n"
      + "Never use this prototype to choose your lane, decide whether to stop, "
      + "or override actual signs or traffic control.\n\n"
      + "Source: RSA Rules of the Road, section 6\n"
      + "rsa.ie/services/learner-drivers/resources/rules-of-the-road";
  }
}
