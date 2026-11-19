# SpeedVoice — Work in progress

> [!WARNING]
> **WORK IN PROGRESS — EXPERIMENTAL / PRE-RELEASE SOFTWARE**
>
> This repository contains development builds for controlled testing and evaluation only. SpeedVoice is not a finished, certified or independently validated road-safety product. It is not recommended for reliance during real-world driving. A successful build does not establish correct operation.

## Safety disclaimer

- SpeedVoice is supplementary information only. It does not replace posted road signs, the vehicle speedometer, the driver's judgment or compliance with applicable traffic law. The driver remains responsible for safe driving and observing the actual legal speed limit.
- Speed-limit recognition, GPS speed, audio alerts and notifications may be inaccurate, delayed, unavailable or absent. Incorrect or missed alerts can occur, including when the map is obscured, the phone is locked, GPS is unreliable or Android restricts background activity. Silence does not mean that the vehicle is within the speed limit.
- Configure, install, test and adjust the application only while safely parked. Do not handle the phone or interact with its controls while driving. Use indoor simulation for initial testing.
- Warning thresholds and repeat intervals are alert settings only; they are not legal allowances to exceed a speed limit. Muting alerts disables audible warnings.
- Audio-priority handling is best effort where implemented. Navigation speech, calls, Bluetooth routing and Android audio-focus behavior can delay, interrupt or suppress alerts; overlapping audio remains possible.
- No guarantee is made of accuracy, reliability, availability, fitness for a particular purpose or compatibility with any device, vehicle or jurisdiction. Development builds are provided as is, without warranties to the extent permitted by applicable law. Nothing in this notice excludes or limits rights or liabilities that cannot lawfully be excluded or limited.

## Privacy and permissions (development builds)

The live phone version uses precise location for GPS speed, Android Accessibility Services to read the visible Google Maps interface, and local OCR as a fallback. The app's current design does not save or upload captured screenshots or GPS readings. The separate video-test build processes user-selected video locally and is for indoor simulation only. Android, Google Maps and the installed text-to-speech provider have their own settings and privacy practices. This notice is a development summary, not a completed release privacy policy.

Grant permissions only if you understand and accept their purpose. Accessibility access can be sensitive; it can be disabled in Android Settings whenever it is not needed. Do not post screenshots, routes, precise locations, credentials or other personal information in public issues.

## Compatibility and distribution

Development phone builds are intended for Android 14+ and visible portrait Google Maps navigation in km/h. Android Auto integration, standalone vehicle head units, screen-off recognition and road-limit database integration are not established supported features. SpeedVoice is an independent project and is not affiliated with, sponsored by or endorsed by Google or any vehicle manufacturer. Google Maps and Android are third-party products.

GitHub Actions artifacts are debug builds, not stable Play Store releases. Install only if you trust the source and understand the permissions and limitations. Test results and bug reports are welcome; do not treat this repository as a production release or a guarantee of future support.

## Existing development notes
Experimental Android 14+ screen reader for visible portrait Google Maps in Ireland (km/h). GPS speed and on-device OCR, no screenshot storage or network transmission by this app. Requires accessibility permission. No automatic Bluetooth startup. Not a driving safety system. Map/GPS/recognition may be wrong; check road signs and vehicle speedometer. Setup while parked. Screen recognition and Android build not yet verified. Build debug APK from Actions. Install only if you trust the code; review Android permission warnings.

## v0.3 demo
Open Demo mode to test simulated speed limits, GPS-free speed and spoken warnings indoors. The demo stops live GPS monitoring and does not test Maps screen recognition. Voice requires an Android TTS engine. New CI debug signing keys can require uninstalling the earlier APK before installation; re-enable permissions afterward.

## v0.6 audio priority (experimental)
Live Maps mode now defers speech while Android reports navigation or other speech playback, waits 750 ms of quiet, requests transient audio focus and stops/retries on focus loss. Pending messages expire after 20 seconds and are validated against live recognition/GPS before delivery. Android anonymizes playback information, so overlapping audio cannot be ruled out on every phone, Bluetooth route or app. No microphone permission. Demo voice unchanged. This branch upgrades the original package; keep v0.3 APK for rollback. Test parked before road use.

## v0.7 phone
Default two short 880 Hz overspeed beeps before voice; speed-limit announcements remain voice-only. Voice uses fresh GPS speed at dispatch. Configurable voice-only, single beep + voice, double beep + voice, double beep only; repeat interval 10/20/30 seconds and alert volume 25–100% of system stream volume. Both tone and voice share the audio-priority gate and are revalidated before voice. Demo mode uses the same gate and alert settings. Android 14+ phone version; no automatic Bluetooth startup or Android Auto screen support. Experimental; requires device testing.

## v0.7.2 notification
Ongoing monitoring notification with a monochrome speedometer status icon, large colored logo, current GPS speed and confirmed limit, overspeed text and requested accent, and mute/unmute and stop actions. Updates at most once per second when displayed data changes. Mute affects live alerts including beeps, not monitoring; resets at next start. Opening the notification shade can obscure Maps and invalidate the limit, as before. Android controls icon color, notification presentation, and permission. Set up parked.

## v0.7.3 disclosure
Live monitoring and the app's accessibility setup shortcut require affirmative acknowledgement of the in-app safety and privacy disclosure. The user can decline or withdraw acknowledgement and stop monitoring. The disclosure is not a waiver of legal rights. See [Development privacy notice](PRIVACY.md). This update still requires device testing.

## v0.8 phone (experimental)
Based on known-working v0.7.2. Corrects v0.7.3 callback context compile error and retains the disclosure. Optional car Bluetooth connection reminder uses an explicitly selected paired device and opens the app; the user starts GPS while parked (no unattended background launch or automatic stop). Graded mode uses one beep under 5 km/h above detected limit, two at 5–14, three at 15 or more, subject to configured alert threshold. Optional return-to-detected-limit voice requires sustained reliable speed at/below same limit after warning queued. Reader diagnostics identify source or failure. No Waze or Android Auto support, no journey recording. Test indoors first.
