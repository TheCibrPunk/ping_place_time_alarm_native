package com.pingplace.timealarm

import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmPresentationPolicyTest {
    private fun identity(clockBasis: AlarmClockBasis) = AlarmIdentity(
        ownerUid = "owner-a",
        taskPath = "tasks/task-a",
        scheduleGeneration = 3,
        notificationId = 42,
        title = "Timer",
        scheduledAtEpochMillis = 2_000_000_000_000L,
        clockBasis = clockBasis,
    )

    @Test fun visibleTimerStartsQuiet() = assertEquals(
        AlarmPresentation.FOREGROUND_QUIET,
        AlarmPresentationPolicy.initial(
            identity(AlarmClockBasis.TIMER_ELAPSED_REALTIME),
            confidentlyVisible = true,
        ),
    )

    @Test fun backgroundTimerUsesNativeAlarm() = assertEquals(
        AlarmPresentation.NATIVE_ALARM,
        AlarmPresentationPolicy.initial(
            identity(AlarmClockBasis.TIMER_ELAPSED_REALTIME),
            confidentlyVisible = false,
        ),
    )

    @Test fun absoluteTimeAlwaysUsesNativeAlarm() = assertEquals(
        AlarmPresentation.NATIVE_ALARM,
        AlarmPresentationPolicy.initial(
            identity(AlarmClockBasis.ABSOLUTE_RTC),
            confidentlyVisible = true,
        ),
    )

    @Test fun visibilityLossPromotesQuietSession() = assertEquals(
        AlarmPresentation.NATIVE_ALARM,
        AlarmPresentationPolicy.afterVisibilityChange(
            AlarmPresentation.FOREGROUND_QUIET,
            confidentlyVisible = false,
        ),
    )

    @Test fun normalAlarmIsNeverDemoted() = assertEquals(
        AlarmPresentation.NATIVE_ALARM,
        AlarmPresentationPolicy.afterVisibilityChange(
            AlarmPresentation.NATIVE_ALARM,
            confidentlyVisible = true,
        ),
    )
}
