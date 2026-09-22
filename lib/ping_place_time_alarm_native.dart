
import 'ping_place_time_alarm_native_platform_interface.dart';

class PingPlaceTimeAlarmNative {
  /// Proves that the platform plugin was resolved and registered.
  ///
  /// This method performs no alarm, audio, notification, navigation, task, or
  /// lifecycle work.
  Future<bool> isNativePluginAvailable() =>
      PingPlaceTimeAlarmNativePlatform.instance.isNativePluginAvailable();
}
