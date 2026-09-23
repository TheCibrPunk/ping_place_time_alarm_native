import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ping_place_time_alarm_native/ping_place_time_alarm_native_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelPingPlaceTimeAlarmNative platform =
      MethodChannelPingPlaceTimeAlarmNative();
  const MethodChannel channel = MethodChannel('ping_place_time_alarm_native');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
          switch (methodCall.method) {
            case 'isNativePluginAvailable':
              return true;
            case 'schedule':
              return 'scheduled';
            case 'cancel':
              return 'cancelled';
            case 'pendingAlarms':
              return <Map<String, Object?>>[
                {'taskPath': 'tasks/task-a', 'scheduleGeneration': 1},
              ];
            case 'activateOwner':
              return 'owner-active';
            case 'clearAll':
              return 'cleared';
          }
          return null;
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('isNativePluginAvailable', () async {
    expect(await platform.isNativePluginAvailable(), true);
  });

  test(
    'schedule cancel and pending alarms use bounded channel methods',
    () async {
      final request = <String, Object>{
        'ownerUid': 'owner-a',
        'taskPath': 'tasks/task-a',
        'scheduleGeneration': 1,
        'notificationId': 123,
        'title': 'Call Mom',
        'scheduledAtEpochMillis': 2000000000000,
      };
      expect(await platform.schedule(request), 'scheduled');
      expect(await platform.cancel(request), 'cancelled');
      expect(
        (await platform.pendingAlarms()).single['taskPath'],
        'tasks/task-a',
      );
      expect(await platform.activateOwner('owner-a'), 'owner-active');
      expect(await platform.clearAll(), 'cleared');
    },
  );
}
