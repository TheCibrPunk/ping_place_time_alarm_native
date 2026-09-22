import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ping_place_time_alarm_native/ping_place_time_alarm_native_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelPingPlaceTimeAlarmNative platform = MethodChannelPingPlaceTimeAlarmNative();
  const MethodChannel channel = MethodChannel('ping_place_time_alarm_native');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return true;
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('isNativePluginAvailable', () async {
    expect(await platform.isNativePluginAvailable(), true);
  });
}
