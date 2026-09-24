package com.pingplace.timealarm

import android.app.AlarmManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AlarmClockPolicyTest {
    private fun identity(
        basis: AlarmClockBasis,
        target: Long = 1_300_000L,
    ) = AlarmIdentity(
        ownerUid = "owner-a",
        taskPath = "tasks/task-a",
        scheduleGeneration = 1,
        notificationId = 123,
        title = "Timer",
        scheduledAtEpochMillis = target,
        clockBasis = basis,
    )

    @Test
    fun absoluteTimeUsesRtcWakeupAtDurableTarget() {
        val plan = assertNotNull(
            AlarmClockPolicy.plan(
                identity(AlarmClockBasis.ABSOLUTE_RTC),
                wallNowMillis = 1_000_000L,
                elapsedNowMillis = 20_000L,
            ),
        )
        assertEquals(AlarmManager.RTC_WAKEUP, plan.alarmType)
        assertEquals(1_300_000L, plan.triggerAtMillis)
        assertNull(plan.persistedElapsedDeadlineMillis)
    }

    @Test
    fun timerUsesElapsedRealtimeWakeupWithOnlyRemainingDuration() {
        val plan = assertNotNull(
            AlarmClockPolicy.plan(
                identity(AlarmClockBasis.TIMER_ELAPSED_REALTIME),
                wallNowMillis = 1_120_000L,
                elapsedNowMillis = 50_000L,
            ),
        )
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, plan.alarmType)
        assertEquals(230_000L, plan.triggerAtMillis)
        assertEquals(230_000L, plan.persistedElapsedDeadlineMillis)
    }

    @Test
    fun reconstructionNeverRestartsOriginalDuration() {
        val initial = assertNotNull(
            AlarmClockPolicy.plan(
                identity(AlarmClockBasis.TIMER_ELAPSED_REALTIME),
                wallNowMillis = 1_000_000L,
                elapsedNowMillis = 10_000L,
            ),
        )
        val reconstructed = assertNotNull(
            AlarmClockPolicy.plan(
                identity(AlarmClockBasis.TIMER_ELAPSED_REALTIME),
                wallNowMillis = 1_180_000L,
                elapsedNowMillis = 190_000L,
            ),
        )
        assertEquals(310_000L, initial.triggerAtMillis)
        assertEquals(310_000L, reconstructed.triggerAtMillis)
        assertEquals(120_000L, reconstructed.triggerAtMillis - 190_000L)
    }

    @Test
    fun pastDurableTargetFailsClosed() {
        assertNull(
            AlarmClockPolicy.plan(
                identity(AlarmClockBasis.TIMER_ELAPSED_REALTIME),
                wallNowMillis = 1_300_000L,
                elapsedNowMillis = 200_000L,
            ),
        )
    }

    @Test
    fun armedSameBootTimerKeepsElapsedDeadlineAcrossWallAndTimezoneChanges() {
        val incoming = identity(AlarmClockBasis.TIMER_ELAPSED_REALTIME)
        val armed = incoming.withElapsedDeadline(310_000L)
        assertEquals(true, AlarmClockPolicy.canReuse(armed, incoming, 20_000L))
        // No wall-clock or timezone input participates once the elapsed
        // deadline is armed; only monotonic elapsed time can expire it.
        assertEquals(true, AlarmClockPolicy.canReuse(armed, incoming, 250_000L))
        assertEquals(false, AlarmClockPolicy.canReuse(armed, incoming, 310_000L))
    }
}
