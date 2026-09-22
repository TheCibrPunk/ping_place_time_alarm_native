import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'ping_place_time_alarm_native_platform_interface.dart';

/// An implementation of [PingPlaceTimeAlarmNativePlatform] that uses method channels.
class MethodChannelPingPlaceTimeAlarmNative extends PingPlaceTimeAlarmNativePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('ping_place_time_alarm_native');

  @override
  Future<bool> isNativePluginAvailable() async =>
      await methodChannel.invokeMethod<bool>('isNativePluginAvailable') ?? false;
}
