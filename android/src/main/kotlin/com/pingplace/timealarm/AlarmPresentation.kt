package com.pingplace.timealarm

internal enum class AlarmPresentation(val wireValue: String) {
    FOREGROUND_QUIET("foreground-quiet"),
    NATIVE_ALARM("native-alarm"),
}

internal object AlarmPresentationPolicy {
    fun initial(identity: AlarmIdentity, confidentlyVisible: Boolean): AlarmPresentation =
        if (identity.clockBasis == AlarmClockBasis.TIMER_ELAPSED_REALTIME && confidentlyVisible) {
            AlarmPresentation.FOREGROUND_QUIET
        } else {
            AlarmPresentation.NATIVE_ALARM
        }

    fun afterVisibilityChange(
        current: AlarmPresentation,
        confidentlyVisible: Boolean,
    ): AlarmPresentation = if (
        current == AlarmPresentation.FOREGROUND_QUIET && !confidentlyVisible
    ) {
        AlarmPresentation.NATIVE_ALARM
    } else {
        current
    }
}
