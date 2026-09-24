package com.pingplace.timealarm

import android.app.Activity
import android.os.Bundle

/** User-launched alarm action: silence natively, then forward to the requested host route. */
class TimeAlarmStopActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val identity = AlarmIdentity.fromIntent(intent)
        if (identity == null) {
            finish()
            return
        }
        when (intent.action) {
            TimeAlarmActionReceiver.ACTION_STOP ->
                TimeAlarmStopCoordinator.stopAndOpen(this, identity)
            ACTION_RESTART ->
                TimeAlarmStopCoordinator.restartAndOpen(this, identity)
        }
        finish()
    }

    companion object {
        const val ACTION_RESTART = "com.pingplace.timealarm.action.RESTART"
    }
}
