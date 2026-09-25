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
      final coordinator = File(
        '${nativeRoot}TimeAlarmStopCoordinator.kt',
      ).readAsStringSync();

      expect(factory, contains('fun stopAndOpen'));
      expect(factory, contains('PendingIntent.getActivity'));
      expect(factory, contains('TimeAlarmStopActivity::class.java'));
      expect(receiver, isNot(contains('startActivity(')));
      expect(activity, contains('TimeAlarmStopCoordinator.stopAndOpen'));
      expect(
        coordinator.indexOf('stopIfMatching(activity, identity)'),
        lessThan(coordinator.indexOf('openHostTask(activity, deepLink)')),
      );
      expect(coordinator, contains('Intent.ACTION_VIEW'));
      expect(coordinator, contains('silenceAndOpen'));
      expect(coordinator, isNot(contains('completed')));
      expect(coordinator, isNot(contains('Firebase')));
    },
  );

  test('Timer ringing surfaces expose generation-bound RESTART only', () {
    final identity = File('${nativeRoot}AlarmIdentity.kt').readAsStringSync();
    final factory = File(
      '${nativeRoot}AlarmIntentFactory.kt',
    ).readAsStringSync();
    final activity = File(
      '${nativeRoot}TimeAlarmRingingActivity.kt',
    ).readAsStringSync();
    final controller = File(
      '${nativeRoot}TimeAlarmSessionController.kt',
    ).readAsStringSync();

    expect(identity, contains('&timerRestart=true'));
    expect(identity, contains('scheduleGeneration'));
    expect(identity, contains('notificationId'));
    expect(factory, contains('fun restartAndOpen'));
    expect(factory, contains('ACTION_RESTART'));
    expect(activity, contains('text = "RESTART"'));
    expect(
      activity,
      contains('alarm.clockBasis == AlarmClockBasis.TIMER_ELAPSED_REALTIME'),
    );
    expect(controller, contains('.addAction(0, "RESTART", restart)'));
    expect(
      controller,
      contains('.setContentIntent(AlarmIntentFactory.openTask'),
    );
    expect(
      factory,
      contains('Intent(Intent.ACTION_VIEW, identity.taskDeepLink)'),
    );
    expect(
      factory,
      isNot(contains('Intent(Intent.ACTION_VIEW, identity.restartDeepLink')),
    );
    expect(identity, isNot(contains('title == "Timer"')));
  });

  test(
    'RESTART shares silence-first teardown and stale actions fail closed',
    () {
      final coordinator = File(
        '${nativeRoot}TimeAlarmStopCoordinator.kt',
      ).readAsStringSync();
      final activity = File(
        '${nativeRoot}TimeAlarmStopActivity.kt',
      ).readAsStringSync();

      expect(
        coordinator,
        contains(
          'val restartDeepLink = identity.restartDeepLink ?: return false',
        ),
      );
      expect(
        coordinator,
        contains('return silenceAndOpen(activity, identity, restartDeepLink)'),
      );
      expect(
        coordinator.indexOf('stopIfMatching(activity, identity)'),
        lessThan(coordinator.indexOf('activity.stopService')),
      );
      expect(
        coordinator.indexOf('activity.stopService'),
        lessThan(coordinator.indexOf('openHostTask(activity, deepLink)')),
      );
      expect(activity, contains('ACTION_RESTART'));
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

  test(
    'Timer STOP carries an explicit subtype while absolute Time does not',
    () {
      final identity = File('${nativeRoot}AlarmIdentity.kt').readAsStringSync();
      expect(
        identity,
        contains('clockBasis == AlarmClockBasis.TIMER_ELAPSED_REALTIME'),
      );
      expect(identity, contains('&timerStop=true'));
      expect(identity, isNot(contains("title == \"Timer\"")));
    },
  );

  test('STOP and lock-screen alarm activities are private and transient', () {
    final manifest = File(
      'android/src/main/AndroidManifest.xml',
    ).readAsStringSync();
    expect(manifest, contains('TimeAlarmStopActivity'));
    expect(manifest, contains('TimeAlarmRingingActivity'));
    expect(manifest, contains('android:exported="false"'));
    expect(manifest, contains('android:noHistory="true"'));
    expect(manifest, contains('android:excludeFromRecents="true"'));
    expect(manifest, contains('USE_FULL_SCREEN_INTENT'));
  });

  test(
    'lock-screen surface is minimal and preserves silence-first authority',
    () {
      final ringing = File(
        '${nativeRoot}TimeAlarmRingingActivity.kt',
      ).readAsStringSync();
      final controller = File(
        '${nativeRoot}TimeAlarmSessionController.kt',
      ).readAsStringSync();
      final factory = File(
        '${nativeRoot}AlarmIntentFactory.kt',
      ).readAsStringSync();
      expect(ringing, contains('setShowWhenLocked(true)'));
      expect(ringing, contains('setTurnScreenOn(true)'));
      expect(ringing, contains('text = "STOP"'));
      expect(ringing, contains('TimeAlarmStopCoordinator.stopAndOpen'));
      expect(ringing, isNot(contains('Done')));
      expect(ringing, isNot(contains('completed')));
      expect(controller, contains('.setFullScreenIntent('));
      expect(factory, contains('fun ringFullScreen'));
      expect(factory, contains('TimeAlarmRingingActivity::class.java'));
    },
  );

  test(
    'ringer mode owns audio and native vibration without channel output',
    () {
      final policy = File(
        '${nativeRoot}AlarmOutputPolicy.kt',
      ).readAsStringSync();
      final controller = File(
        '${nativeRoot}TimeAlarmSessionController.kt',
      ).readAsStringSync();
      final manifest = File(
        'android/src/main/AndroidManifest.xml',
      ).readAsStringSync();

      expect(policy, contains('RINGER_MODE_NORMAL'));
      expect(policy, contains('RINGER_MODE_VIBRATE'));
      expect(policy, contains('playAudio = false, vibrate = false'));
      expect(controller, contains('manager?.ringerMode'));
      expect(controller, contains('VibrationEffect.createWaveform'));
      expect(
        controller,
        contains('target.vibrate(VibrationEffect.createWaveform'),
      );
      expect(controller, contains('target.vibrate(longArrayOf'));
      expect(controller, contains('vibrator?.cancel()'));
      expect(controller, contains('enableVibration(false)'));
      expect(controller, contains('setSound(null, null)'));
      expect(manifest, contains('android.permission.VIBRATE'));
    },
  );

  test(
    'all session exits share output cleanup before navigation or promotion',
    () {
      final controller = File(
        '${nativeRoot}TimeAlarmSessionController.kt',
      ).readAsStringSync();
      final coordinator = File(
        '${nativeRoot}TimeAlarmStopCoordinator.kt',
      ).readAsStringSync();

      expect(controller, contains('scheduleTimeout'));
      expect(controller, contains('stopIfMatching(context, identity)'));
      expect(controller, contains('serviceDestroyed'));
      expect(controller, contains('stopLocked(targetService, it)'));
      expect(
        controller.indexOf('mediaPlayer?.stop()'),
        lessThan(controller.indexOf('vibrator?.cancel()')),
      );
      expect(
        coordinator.indexOf('stopIfMatching(activity, identity)'),
        lessThan(coordinator.indexOf('openHostTask(activity, deepLink)')),
      );
    },
  );
}
