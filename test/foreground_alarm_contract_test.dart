import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  const root = 'android/src/main/kotlin/com/pingplace/timealarm/';

  test('foreground visibility is native, positive, and fail closed', () {
    final plugin = File(
      '${root}PingPlaceTimeAlarmNativePlugin.kt',
    ).readAsStringSync();
    final visibility = File(
      '${root}ForegroundVisibilityAuthority.kt',
    ).readAsStringSync();
    expect(plugin, contains('ActivityAware'));
    expect(plugin, contains('ActivityLifecycleCallbacks'));
    expect(visibility, contains('isInteractive'));
    expect(visibility, contains('isKeyguardLocked == false'));
    expect(visibility, contains('hasWindowFocus()'));
    expect(visibility, contains('currentActivity'));
  });

  test(
    'foreground Timer is quiet without changing native output ownership',
    () {
      final controller = File(
        '${root}TimeAlarmSessionController.kt',
      ).readAsStringSync();
      expect(controller, contains('IMPORTANCE_LOW'));
      expect(controller, contains('FOREGROUND_QUIET'));
      expect(controller, contains('if (!foregroundQuiet)'));
      expect(controller, contains('setFullScreenIntent'));
      expect(controller, contains('startOutputs(targetService)'));
      expect(controller, contains('startForeground('));
    },
  );

  test('active session inspection and events retain exact identity', () {
    final controller = File(
      '${root}TimeAlarmSessionController.kt',
    ).readAsStringSync();
    final plugin = File(
      '${root}PingPlaceTimeAlarmNativePlugin.kt',
    ).readAsStringSync();
    expect(controller, contains('identity.toMap()'));
    expect(controller, contains('presentationMode'));
    expect(plugin, contains('activeAlarmSession'));
    expect(plugin, contains('session_events'));
  });

  test(
    'promotion updates presentation without restarting output or timeout',
    () {
      final controller = File(
        '${root}TimeAlarmSessionController.kt',
      ).readAsStringSync();
      final method = controller.substring(
        controller.indexOf('fun visibilityChanged'),
      );
      expect(method, contains('targetService.startForeground'));
      expect(method, isNot(contains('startOutputs(')));
      expect(method, isNot(contains('scheduleTimeout(')));
    },
  );
}
