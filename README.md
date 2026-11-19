# SpeedVoice v0.2
Experimental Android 14+ screen reader for visible portrait Google Maps in Ireland (km/h). GPS speed and on-device OCR, no screenshot storage or network transmission by this app. Requires accessibility permission. No automatic Bluetooth startup. Not a driving safety system. Map/GPS/recognition may be wrong; check road signs and vehicle speedometer. Setup while parked. Screen recognition and Android build not yet verified. Build debug APK from Actions. Install only if you trust the code; review Android permission warnings.

## v0.3 demo
Open Demo mode to test simulated speed limits, GPS-free speed and spoken warnings indoors. The demo stops live GPS monitoring and does not test Maps screen recognition. Voice requires an Android TTS engine. New CI debug signing keys can require uninstalling the earlier APK before installation; re-enable permissions afterward.

## v0.6 audio priority (experimental)
Live Maps mode now defers speech while Android reports navigation or other speech playback, waits 750 ms of quiet, requests transient audio focus and stops/retries on focus loss. Pending messages expire after 20 seconds and are validated against live recognition/GPS before delivery. Android anonymizes playback information, so overlapping audio cannot be ruled out on every phone, Bluetooth route or app. No microphone permission. Demo voice unchanged. This branch upgrades the original package; keep v0.3 APK for rollback. Test parked before road use.
