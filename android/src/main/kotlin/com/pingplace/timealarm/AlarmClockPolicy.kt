package com.pingplace.timealarm

import android.app.AlarmManager

internal enum class AlarmClockBasis(val wireValue: String) {
    ABSOLUTE_RTC("absolute_rtc"),
    TIMER_ELAPSED_REALTIME("timer_elapsed_realtime");

    companion object {
        fun fromWire(value: String?): AlarmClockBasis? =
            entries.firstOrNull { it.wireValue == value }
    }
}

internal data class AlarmSchedulePlan(
    val alarmType: Int,
    val triggerAtMillis: Long,
    val persistedElapsedDeadlineMillis: Long?,
)

internal object AlarmClockPolicy {
    fun canReuse(
        existing: AlarmIdentity,
        incoming: AlarmIdentity,
        elapsedNowMillis: Long,
    ): Boolean =
        existing.token == incoming.token &&
            existing.clockBasis == incoming.clockBasis &&
            (existing.clockBasis == AlarmClockBasis.ABSOLUTE_RTC ||
                existing.elapsedDeadlineMillis?.let { it > elapsedNowMillis } == true)

    fun plan(
        identity: AlarmIdentity,
        wallNowMillis: Long,
        elapsedNowMillis: Long,
    ): AlarmSchedulePlan? {
        val remainingMillis = identity.scheduledAtEpochMillis - wallNowMillis
        if (remainingMillis <= 0L) return null
        return when (identity.clockBasis) {
            AlarmClockBasis.ABSOLUTE_RTC -> AlarmSchedulePlan(
                alarmType = AlarmManager.RTC_WAKEUP,
                triggerAtMillis = identity.scheduledAtEpochMillis,
                persistedElapsedDeadlineMillis = null,
            )
            AlarmClockBasis.TIMER_ELAPSED_REALTIME -> {
                if (elapsedNowMillis > Long.MAX_VALUE - remainingMillis) return null
                val deadline = elapsedNowMillis + remainingMillis
                AlarmSchedulePlan(
                    alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis = deadline,
                    persistedElapsedDeadlineMillis = deadline,
                )
            }
        }
    }
}
