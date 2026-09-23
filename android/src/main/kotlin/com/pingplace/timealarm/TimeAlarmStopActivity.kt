package com.pingplace.timealarm

import android.app.Activity
import android.os.Bundle

/** User-launched notification action: silence natively, then forward to the host task route. */
class TimeAlarmStopActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val identity = AlarmIdentity.fromIntent(intent)
        if (intent.action != TimeAlarmActionReceiver.ACTION_STOP || identity == null) {
            finish()
            return
        }
        TimeAlarmStopCoordinator.stopAndOpen(this, identity)
        finish()
    }
}
