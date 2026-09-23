import 'ping_place_time_alarm_native_platform_interface.dart';

final class PingPlaceTimeAlarmRequest {
  const PingPlaceTimeAlarmRequest({
    required this.ownerUid,
    required this.taskPath,
    required this.scheduleGeneration,
    required this.notificationId,
    required this.title,
    required this.scheduledAt,
  });

  final String ownerUid;
  final String taskPath;
  final int scheduleGeneration;
  final int notificationId;
  final String title;
  final DateTime scheduledAt;

  Map<String, Object> toMap() => <String, Object>{
    'ownerUid': ownerUid,
    'taskPath': taskPath,
    'scheduleGeneration': scheduleGeneration,
    'notificationId': notificationId,
    'title': title,
    'scheduledAtEpochMillis': scheduledAt.toUtc().millisecondsSinceEpoch,
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

  Future<String> activateOwner(String ownerUid) =>
      PingPlaceTimeAlarmNativePlatform.instance.activateOwner(ownerUid);

  Future<String> clearAll() =>
      PingPlaceTimeAlarmNativePlatform.instance.clearAll();
}
