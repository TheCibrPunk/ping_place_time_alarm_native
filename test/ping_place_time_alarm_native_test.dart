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

  @override
  Future<String> schedule(Map<String, Object> request) async => 'scheduled';

  @override
  Future<String> cancel(Map<String, Object> request) async => 'cancelled';

  @override
  Future<List<Map<String, Object?>>> pendingAlarms() async => const [];

  @override
  Future<String> activateOwner(String ownerUid) async => 'owner-active';

  @override
  Future<String> clearAll() async => 'cleared';
}

void main() {
  final PingPlaceTimeAlarmNativePlatform initialPlatform =
      PingPlaceTimeAlarmNativePlatform.instance;

  test('$MethodChannelPingPlaceTimeAlarmNative is the default instance', () {
    expect(
      initialPlatform,
      isInstanceOf<MethodChannelPingPlaceTimeAlarmNative>(),
    );
  });

  test(
    'isNativePluginAvailable is deterministic and side-effect free',
    () async {
      PingPlaceTimeAlarmNative pingPlaceTimeAlarmNativePlugin =
          PingPlaceTimeAlarmNative();
      MockPingPlaceTimeAlarmNativePlatform fakePlatform =
          MockPingPlaceTimeAlarmNativePlatform();
      PingPlaceTimeAlarmNativePlatform.instance = fakePlatform;

      expect(
        await pingPlaceTimeAlarmNativePlugin.isNativePluginAvailable(),
        isTrue,
      );
    },
  );

  test('typed schedule request preserves generation-bound authority', () async {
    final plugin = PingPlaceTimeAlarmNative();
    final request = PingPlaceTimeAlarmRequest(
      ownerUid: 'owner-a',
      taskPath: 'tasks/task-a',
      scheduleGeneration: 2,
      notificationId: 123,
      title: 'Call Mom',
      scheduledAt: DateTime.utc(2030, 1, 1, 12),
    );
    expect(await plugin.schedule(request), 'scheduled');
    expect(await plugin.cancel(request), 'cancelled');
    expect(await plugin.activateOwner('owner-a'), 'owner-active');
    expect(await plugin.clearAll(), 'cleared');
  });
}
