import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'ping_place_time_alarm_native_method_channel.dart';

abstract class PingPlaceTimeAlarmNativePlatform extends PlatformInterface {
  /// Constructs a PingPlaceTimeAlarmNativePlatform.
  PingPlaceTimeAlarmNativePlatform() : super(token: _token);

  static final Object _token = Object();

  static PingPlaceTimeAlarmNativePlatform _instance =
      MethodChannelPingPlaceTimeAlarmNative();

  /// The default instance of [PingPlaceTimeAlarmNativePlatform] to use.
  ///
  /// Defaults to [MethodChannelPingPlaceTimeAlarmNative].
  static PingPlaceTimeAlarmNativePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [PingPlaceTimeAlarmNativePlatform] when
  /// they register themselves.
  static set instance(PingPlaceTimeAlarmNativePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<bool> isNativePluginAvailable() {
    throw UnimplementedError(
      'isNativePluginAvailable() has not been implemented.',
    );
  }

  Future<String> schedule(Map<String, Object> request) {
    throw UnimplementedError('schedule() has not been implemented.');
  }

  Future<String> cancel(Map<String, Object> request) {
    throw UnimplementedError('cancel() has not been implemented.');
  }

  Future<List<Map<String, Object?>>> pendingAlarms() {
    throw UnimplementedError('pendingAlarms() has not been implemented.');
  }

  Future<String> activateOwner(String ownerUid) {
    throw UnimplementedError('activateOwner() has not been implemented.');
  }

  Future<String> clearAll() {
    throw UnimplementedError('clearAll() has not been implemented.');
  }

  Future<bool> canUseFullScreenIntent() {
    throw UnimplementedError(
      'canUseFullScreenIntent() has not been implemented.',
    );
  }

  Future<String> openFullScreenIntentSettings() {
    throw UnimplementedError(
      'openFullScreenIntentSettings() has not been implemented.',
    );
  }
}
