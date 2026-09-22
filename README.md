# ping_place_time_alarm_native

Project-owned native foundation for Ping Place Time Alarm delivery.

Version `0.0.1` is intentionally inert. It proves that Flutter can package and
call Ping Place-owned native code while reserving narrowly scoped Android
receiver, service, and resource locations for later implementation.

The only public API is `isNativePluginAvailable()`. It returns a deterministic
availability result and performs no alarm scheduling, audio, vibration,
notification, navigation, task, Firestore, Place, or geofence work.

Android namespace: `com.pingplace.timealarm`

Reserved Android resources must use the prefix `ping_place_time_alarm_`.

