package com.pingplace.timealarm

import android.app.Activity
import android.content.Intent

/** Shared silence-first authority for every user-visible STOP surface. */
internal object TimeAlarmStopCoordinator {
    fun stopAndOpen(activity: Activity, identity: AlarmIdentity): Boolean {
        if (!TimeAlarmSessionController.stopIfMatching(activity, identity)) return false
        activity.stopService(Intent(activity, TimeAlarmService::class.java))
        TimeAlarmSessionController.promoteNext(activity)
        openHostTask(activity, identity)
        return true
    }

    private fun openHostTask(activity: Activity, identity: AlarmIdentity) {
        val launchIntent = activity.packageManager
            .getLaunchIntentForPackage(activity.packageName) ?: return
        runCatching {
            activity.startActivity(launchIntent.apply {
                action = Intent.ACTION_VIEW
                data = identity.taskDeepLink
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
    }
}
