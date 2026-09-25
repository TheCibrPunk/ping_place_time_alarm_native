import 'ping_place_time_alarm_native_platform_interface.dart';

enum PingPlaceTimeAlarmClockBasis {
  absoluteRtc('absolute_rtc'),
  timerElapsedRealtime('timer_elapsed_realtime');

  const PingPlaceTimeAlarmClockBasis(this.wireValue);
  final String wireValue;
}

final class PingPlaceTimeAlarmRequest {
  const PingPlaceTimeAlarmRequest({
    required this.ownerUid,
    required this.taskPath,
    required this.scheduleGeneration,
    required this.notificationId,
    required this.title,
    required this.scheduledAt,
    this.clockBasis = PingPlaceTimeAlarmClockBasis.absoluteRtc,
  });

  final String ownerUid;
  final String taskPath;
  final int scheduleGeneration;
  final int notificationId;
  final String title;
  final DateTime scheduledAt;
  final PingPlaceTimeAlarmClockBasis clockBasis;

  Map<String, Object> toMap() => <String, Object>{
    'ownerUid': ownerUid,
    'taskPath': taskPath,
    'scheduleGeneration': scheduleGeneration,
    'notificationId': notificationId,
    'title': title,
    'scheduledAtEpochMillis': scheduledAt.toUtc().millisecondsSinceEpoch,
    'clockBasis': clockBasis.wireValue,
  };
}

class PingPlaceTimeAlarmNative {
  /// Proves that the platform plugin was resolved and registered.
  ///
  /// This method performs no alarm, audio, notification, navigation, task, or
  /// lifecycle work.
  Future<bool> isNativePluginAvailable() =>
      PingPlaceTimeAlarmNativePlatform.instance.isNativePluginAvailable();

  Future<String> schedule(PingPlaceTimeAlarmRequest request) =>
      PingPlaceTimeAlarmNativePlatform.instance.schedule(request.toMap());

  Future<String> cancel(PingPlaceTimeAlarmRequest request) =>
      PingPlaceTimeAlarmNativePlatform.instance.cancel(request.toMap());

  Future<List<Map<String, Object?>>> pendingAlarms() =>
      PingPlaceTimeAlarmNativePlatform.instance.pendingAlarms();

  /// Returns the exact native session that is currently producing alarm output.
  Future<Map<String, Object?>?> activeAlarmSession() =>
      PingPlaceTimeAlarmNativePlatform.instance.activeAlarmSession();

  /// Emits exact active-session start, stop, and presentation-promotion state.
  Stream<Map<String, Object?>?> activeAlarmSessionEvents() =>
      PingPlaceTimeAlarmNativePlatform.instance.activeAlarmSessionEvents();

  Future<String> activateOwner(String ownerUid) =>
      PingPlaceTimeAlarmNativePlatform.instance.activateOwner(ownerUid);

  Future<String> clearAll() =>
      PingPlaceTimeAlarmNativePlatform.instance.clearAll();

  Future<bool> canUseFullScreenIntent() =>
      PingPlaceTimeAlarmNativePlatform.instance.canUseFullScreenIntent();

  /// Opens Android's app-specific full-screen alarm access screen.
  /// Must only be called from an explicit contextual user action.
  Future<String> openFullScreenIntentSettings() =>
      PingPlaceTimeAlarmNativePlatform.instance.openFullScreenIntentSettings();
}
