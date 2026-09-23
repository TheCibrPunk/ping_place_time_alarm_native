package com.pingplace.timealarm

import android.app.Activity
import android.content.Intent
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
        if (!TimeAlarmSessionController.stopIfMatching(this, identity)) {
            finish()
            return
        }
        stopService(Intent(this, TimeAlarmService::class.java))
        TimeAlarmSessionController.promoteNext(this)
        openHostTask(identity)
        finish()
    }

    private fun openHostTask(identity: AlarmIdentity) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        runCatching {
            startActivity(launchIntent.apply {
                action = Intent.ACTION_VIEW
                data = identity.taskDeepLink
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
    }
}
