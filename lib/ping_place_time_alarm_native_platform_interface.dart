import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'ping_place_time_alarm_native_method_channel.dart';

abstract class PingPlaceTimeAlarmNativePlatform extends PlatformInterface {
  /// Constructs a PingPlaceTimeAlarmNativePlatform.
  PingPlaceTimeAlarmNativePlatform() : super(token: _token);

  static final Object _token = Object();

  static PingPlaceTimeAlarmNativePlatform _instance = MethodChannelPingPlaceTimeAlarmNative();

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
}
