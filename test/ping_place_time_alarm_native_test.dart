import 'package:flutter_test/flutter_test.dart';
import 'package:ping_place_time_alarm_native/ping_place_time_alarm_native.dart';
import 'package:ping_place_time_alarm_native/ping_place_time_alarm_native_platform_interface.dart';
import 'package:ping_place_time_alarm_native/ping_place_time_alarm_native_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockPingPlaceTimeAlarmNativePlatform
    with MockPlatformInterfaceMixin
    implements PingPlaceTimeAlarmNativePlatform {

  @override
  Future<bool> isNativePluginAvailable() => Future.value(true);
}

void main() {
  final PingPlaceTimeAlarmNativePlatform initialPlatform = PingPlaceTimeAlarmNativePlatform.instance;

  test('$MethodChannelPingPlaceTimeAlarmNative is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelPingPlaceTimeAlarmNative>());
  });

  test('isNativePluginAvailable is deterministic and side-effect free', () async {
    PingPlaceTimeAlarmNative pingPlaceTimeAlarmNativePlugin = PingPlaceTimeAlarmNative();
    MockPingPlaceTimeAlarmNativePlatform fakePlatform = MockPingPlaceTimeAlarmNativePlatform();
    PingPlaceTimeAlarmNativePlatform.instance = fakePlatform;

    expect(
      await pingPlaceTimeAlarmNativePlugin.isNativePluginAvailable(),
      isTrue,
    );
  });
}
