# SpeedVoice — Development privacy notice

**Updated: 19 November 2026. Development builds only.** This notice describes the current repository design, not a verified production release or a final Google Play privacy policy.

## Who operates the project
SpeedVoice is an independent experimental Android project maintained through the public [GitHub repository](https://github.com/drhidayatadnan-wq/Speedvoice). It is not affiliated with Google. Before public store distribution, the responsible controller's verified identity and private privacy contact must be supplied.

## Data and purposes
- Precise location: the live phone build requests GPS location updates to calculate current vehicle speed and evaluate whether alerts should be issued. The latest speed and timestamp are held temporarily in application memory; the code does not implement route recording.
- Accessibility: when live monitoring is manually started, the service checks the visible Google Maps window for speed-limit text or descriptions. If needed, a screenshot of that window is processed using on-device OCR. The code does not implement saving or uploading these screenshots. Accessibility access is optional and can be disabled in Android Settings.
- Audio: the audio-priority feature checks Android playback configuration metadata and requests audio focus to reduce overlapping announcements. It does not record microphone audio. Speech text is passed to the selected Android text-to-speech engine, which may have its own network and privacy behavior.
- Settings: alert preferences and acknowledgement of the development disclosure are stored locally in Android private application preferences. No account is required by the app.
- Notifications: the live monitoring notification may show GPS speed and detected limit; Android controls its presentation and lock-screen privacy.
- Indoor video test: the separate test build processes frames of a video the user selects through Android's file picker. Its code does not implement video upload or recording.

## Sharing, retention and third parties
The app code does not implement an account, analytics service, advertising, a remote database or a user-data upload feature. The phone build manifest removes the INTERNET permission. Temporary recognition inputs and GPS speed are intended to be discarded or overwritten when no longer needed; settings persist until cleared. Android, Google Maps, the selected text-to-speech engine and GitHub have separate privacy practices. Third-party behavior has not been independently audited.

## Choices and removal
You may decline or withdraw the in-app acknowledgement, stop monitoring, revoke precise location and notification permissions, or disable the accessibility service in Android Settings. Clear application storage or uninstall to remove locally stored settings; uninstall also removes app permissions. No central app account or remotely managed user-data store is implemented.

## Questions and rights
Applicable privacy rights are not waived by using the app. General, non-personal questions may be raised via [GitHub Issues](https://github.com/drhidayatadnan-wq/Speedvoice/issues). **Do not submit personal information, screenshots of private routes, or privacy-rights requests containing identifying information to public issues.** A verified private contact and any applicable controller, legal-basis, rights and complaint information must be confirmed before public production distribution.

## Changes
Revised notices will be published here. Material changes to in-app collection or use require a new disclosure and acknowledgement as appropriate. This is experimental software intended for controlled evaluation only; see the repository safety disclaimer.
