package com.pingplace.timealarm

import android.media.AudioManager

internal data class AlarmOutputPolicy(
    val playAudio: Boolean,
    val vibrate: Boolean,
) {
    companion object {
        fun forRingerMode(mode: Int?): AlarmOutputPolicy = when (mode) {
            AudioManager.RINGER_MODE_NORMAL -> AlarmOutputPolicy(playAudio = true, vibrate = true)
            AudioManager.RINGER_MODE_VIBRATE -> AlarmOutputPolicy(playAudio = false, vibrate = true)
            else -> AlarmOutputPolicy(playAudio = false, vibrate = false)
        }
    }
}
