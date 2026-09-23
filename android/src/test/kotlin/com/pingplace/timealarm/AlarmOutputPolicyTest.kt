package com.pingplace.timealarm

import android.media.AudioManager
import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmOutputPolicyTest {
    @Test
    fun `normal mode permits audio and vibration`() {
        assertEquals(
            AlarmOutputPolicy(playAudio = true, vibrate = true),
            AlarmOutputPolicy.forRingerMode(AudioManager.RINGER_MODE_NORMAL),
        )
    }

    @Test
    fun `vibrate mode suppresses audio and permits vibration`() {
        assertEquals(
            AlarmOutputPolicy(playAudio = false, vibrate = true),
            AlarmOutputPolicy.forRingerMode(AudioManager.RINGER_MODE_VIBRATE),
        )
    }

    @Test
    fun `silent and unknown modes suppress all output`() {
        assertEquals(
            AlarmOutputPolicy(playAudio = false, vibrate = false),
            AlarmOutputPolicy.forRingerMode(AudioManager.RINGER_MODE_SILENT),
        )
        assertEquals(
            AlarmOutputPolicy(playAudio = false, vibrate = false),
            AlarmOutputPolicy.forRingerMode(null),
        )
    }
}
