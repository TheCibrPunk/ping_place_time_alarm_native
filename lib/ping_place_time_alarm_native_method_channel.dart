import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'ping_place_time_alarm_native_platform_interface.dart';

/// An implementation of [PingPlaceTimeAlarmNativePlatform] that uses method channels.
class MethodChannelPingPlaceTimeAlarmNative
    extends PingPlaceTimeAlarmNativePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('ping_place_time_alarm_native');

  @visibleForTesting
  final sessionEventChannel = const EventChannel(
    'ping_place_time_alarm_native/session_events',
  );

  @override
  Future<bool> isNativePluginAvailable() async =>
      await methodChannel.invokeMethod<bool>('isNativePluginAvailable') ??
      false;

  @override
  Future<String> schedule(Map<String, Object> request) async =>
      await methodChannel.invokeMethod<String>('schedule', request) ?? 'error';

  @override
  Future<String> cancel(Map<String, Object> request) async =>
      await methodChannel.invokeMethod<String>('cancel', request) ?? 'error';

  @override
  Future<List<Map<String, Object?>>> pendingAlarms() async {
    final raw = await methodChannel.invokeListMethod<dynamic>('pendingAlarms');
    return (raw ?? const <dynamic>[])
        .whereType<Map<dynamic, dynamic>>()
        .map(
          (entry) => entry.map((key, value) => MapEntry(key.toString(), value)),
        )
        .toList(growable: false);
  }

  @override
  Future<Map<String, Object?>?> activeAlarmSession() async {
    final raw = await methodChannel.invokeMapMethod<dynamic, dynamic>(
      'activeAlarmSession',
    );
    return raw?.map((key, value) => MapEntry(key.toString(), value));
  }

  @override
  Stream<Map<String, Object?>?> activeAlarmSessionEvents() =>
      sessionEventChannel.receiveBroadcastStream().map((raw) {
        if (raw == null) return null;
        if (raw is! Map) return null;
        return raw.map(
          (key, value) => MapEntry(key.toString(), value as Object?),
        );
      });

  @override
  Future<String> activateOwner(String ownerUid) async =>
      await methodChannel.invokeMethod<String>('activateOwner', ownerUid) ??
      'error';

  @override
  Future<String> clearAll() async =>
      await methodChannel.invokeMethod<String>('clearAll') ?? 'error';

  @override
  Future<bool> canUseFullScreenIntent() async =>
      await methodChannel.invokeMethod<bool>('canUseFullScreenIntent') ?? false;

  @override
  Future<String> openFullScreenIntentSettings() async =>
      await methodChannel.invokeMethod<String>(
        'openFullScreenIntentSettings',
      ) ??
      'error';
}
