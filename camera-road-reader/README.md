# Camera Road Reader v0.3

Experimental Android rear-camera prototype, offline and on-device. Uses ML Kit OCR for possible speed limits, STOP, YIELD / Géill Slí, and text signs including NO ENTRY, BUS LANE, CYCLE LANE, ONE WAY, KEEP LEFT, KEEP RIGHT, SCHOOL, ROADWORKS, PEDESTRIAN CROSSING, and NO OVERTAKING. Also includes a conservative *heuristic* visual check for NO ENTRY (red disc with white bar). Two consecutive detections are required to confirm an alert. It keeps the last possible speed-limit number visible while scanning, warns when stale, and offers beep/voice announcements.

**Lane overlay:** Optional, lightweight estimates of nearby white/yellow road markings. Dashed cyan lines are only *possible markings* and are not validated lane boundaries or navigation/steering guidance. No directional steering or lane selection advice is provided.

**Limitations:** Not a trained traffic sign detector. Many symbol-only signs are unsupported. Could miss, misread, or falsely flag signs and lane paint. Do not treat readings as authoritative, do not look at or operate the phone while driving, and always obey physical signs, lights, and road markings. Test while parked. No cloud or camera upload.
