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

## Development branches

The default branch preserves the original v0.3 driving prototype. The latest experimental phone features are developed separately on [v0.7-phone](https://github.com/drhidayatadnan-wq/Speedvoice/tree/v0.7-phone). [v0.6-audio-priority](https://github.com/drhidayatadnan-wq/Speedvoice/tree/v0.6-audio-priority) contains the audio-priority prototype, and [video-test](https://github.com/drhidayatadnan-wq/Speedvoice/tree/video-test) is a separate indoor simulation app. None is a stable public release.

## Existing development notes
Experimental Android 14+ screen reader for visible portrait Google Maps in Ireland (km/h). GPS speed and on-device OCR, no screenshot storage or network transmission by this app. Requires accessibility permission. No automatic Bluetooth startup. Not a driving safety system. Map/GPS/recognition may be wrong; check road signs and vehicle speedometer. Setup while parked. Screen recognition and Android build not yet verified. Build debug APK from Actions. Install only if you trust the code; review Android permission warnings.

## v0.3 demo
Open Demo mode to test simulated speed limits, GPS-free speed and spoken warnings indoors. The demo stops live GPS monitoring and does not test Maps screen recognition. Voice requires an Android TTS engine. New CI debug signing keys can require uninstalling the earlier APK before installation; re-enable permissions afterward.

## Privacy notice
See the [Development privacy notice](PRIVACY.md). The latest experimental phone branch includes an in-app acknowledgement and withdrawal control; the original v0.3 prototype remains unchanged.
