# ping_place_time_alarm_native

Project-owned native Android Time Alarm delivery for Ping Place.

Version `0.1.0` schedules exact generation-bound alarms, runs one serialized
looping alarm-audio session, provides an idempotent native STOP control, and
opens the exact Time task only after native silence. Concurrent due alarms use
a small persisted FIFO and never create competing audio loops.

The plugin does not read Firestore, complete tasks, or interact with Place,
geofence, Trusted Departure, or Driving Alert systems. The phone's configured
system alarm sound is used; no redistributed audio asset is packaged.

Android namespace: `com.pingplace.timealarm`

Reserved Android resources must use the prefix `ping_place_time_alarm_`.

