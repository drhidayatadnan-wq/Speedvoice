# Camera Road Reader v0.4

Source for the sign names and families: official RSA *Rules of the Road*, particularly Section 6 on traffic signs and road markings: https://www.rsa.ie/services/learner-drivers/resources/rules-of-the-road

This experimental Android app uses ML Kit OCR and basic camera heuristics. The **RSA IRISH SIGN LIST** screen distinguishes signs with basic OCR detection from those it **cannot** recognise. It is not a trained traffic sign classification model.

### Available tentative alerts
- Numeric red-ring speed limits: OCR + red-colour verification; last value remains visible, becomes stale after 45 seconds.
- STOP: OCR and red-colour check.
- YIELD or GÉILL SLÍ: OCR and nearby red-colour check.
- No entry to vehicles: basic red-disc-and-white-bar pattern.
- Some bilingual/textual signs including bus lane, cycle lane, one way, keep left/right, school, roadworks and road markings if text is readable.
- **Yellow diamond**: colour/geometry classification only, cannot distinguish school/roundabout/bend etc.
- **Orange diamond**: colour/geometry classification only, cannot distinguish the roadworks symbol.
- Painted lane markings: experimental cyan overlay, not navigation or lane selection.

### Important limitations
- Most Irish **symbol-only signs**, e.g. roundabout, school symbol, pedestrian crossing, regulatory blue arrows, and motorway lane control red X/green arrow, are **reference only** and not recognised.
- Ireland's special alternative black-stripe 60 km/h sign is also **not recognised**.
- Yellow/orange diamond detections are visual-category guesses, with no audio announcement.
- Signs may be misread or missed and lane overlay may be wrong. Never rely on the app to decide road legality, driving speed, lane selection or safe manoeuvres. Test while stationary. Do not view or operate the phone while driving.

Development project remains on the public `camera-road-reader-v0.1` branch of the Speedvoice repository, without modifying SpeedVoice main.
