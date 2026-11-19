# SpeedVoice v0.2
Experimental Android 14+ screen reader for visible portrait Google Maps in Ireland (km/h). GPS speed and on-device OCR, no screenshot storage or network transmission by this app. Requires accessibility permission. No automatic Bluetooth startup. Not a driving safety system. Map/GPS/recognition may be wrong; check road signs and vehicle speedometer. Setup while parked. Screen recognition and Android build not yet verified. Build debug APK from Actions. Install only if you trust the code; review Android permission warnings.

## v0.3 demo
Open Demo mode to test simulated speed limits, GPS-free speed and spoken warnings indoors. The demo stops live GPS monitoring and does not test Maps screen recognition. Voice requires an Android TTS engine. New CI debug signing keys can require uninstalling the earlier APK before installation; re-enable permissions afterward.

## v0.4 video OCR test
Choose a portrait navigation video via the new Video OCR test button. It samples frames one second apart, displays recognition results from the same VisualReader used in Maps, and announces confirmed limits. Playback is frame-by-frame, not normal-speed video; GPS and overspeed alerts are not tested here. No video frames are uploaded or saved by this feature. Live Maps package restriction remains unchanged. Requires on-device validation.
