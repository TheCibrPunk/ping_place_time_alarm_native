import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  const nativeRoot = 'android/src/main/kotlin/com/pingplace/timealarm/';

  test(
    'STOP is an activity PendingIntent, not a blocked broadcast trampoline',
    () {
      final factory = File(
        '${nativeRoot}AlarmIntentFactory.kt',
      ).readAsStringSync();
      final receiver = File(
        '${nativeRoot}TimeAlarmActionReceiver.kt',
      ).readAsStringSync();
      final activity = File(
        '${nativeRoot}TimeAlarmStopActivity.kt',
      ).readAsStringSync();

      expect(factory, contains('fun stopAndOpen'));
      expect(factory, contains('PendingIntent.getActivity'));
      expect(factory, contains('TimeAlarmStopActivity::class.java'));
      expect(receiver, isNot(contains('startActivity(')));
      expect(activity, contains('stopIfMatching(this, identity)'));
      expect(
        activity.indexOf('stopIfMatching(this, identity)'),
        lessThan(activity.indexOf('openHostTask(identity)')),
      );
      expect(activity, contains('Intent.ACTION_VIEW'));
      expect(activity, contains('identity.taskDeepLink'));
      expect(activity, isNot(contains('completed')));
      expect(activity, isNot(contains('Firebase')));
    },
  );

  test('swipe/delete remains silent while STOP opens the exact task', () {
    final controller = File(
      '${nativeRoot}TimeAlarmSessionController.kt',
    ).readAsStringSync();
    expect(
      controller,
      contains(
        '.setDeleteIntent(AlarmIntentFactory.stopSilently(context, identity))',
      ),
    );
    expect(
      controller,
      contains(
        '.addAction(0, "STOP", AlarmIntentFactory.stopAndOpen(context, identity))',
      ),
    );
  });

  test('STOP activity is private, transient, and permission-free', () {
    final manifest = File(
      'android/src/main/AndroidManifest.xml',
    ).readAsStringSync();
    expect(manifest, contains('TimeAlarmStopActivity'));
    expect(manifest, contains('android:exported="false"'));
    expect(manifest, contains('android:noHistory="true"'));
    expect(manifest, contains('android:excludeFromRecents="true"'));
    expect(manifest, isNot(contains('USE_FULL_SCREEN_INTENT')));
  });
}
