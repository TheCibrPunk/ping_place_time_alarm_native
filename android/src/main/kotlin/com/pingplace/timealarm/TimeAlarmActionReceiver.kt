package com.pingplace.timealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimeAlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STOP) return
        val identity = AlarmIdentity.fromIntent(intent) ?: return
        if (!TimeAlarmSessionController.stopIfMatching(context, identity)) return
        context.stopService(Intent(context, TimeAlarmService::class.java))
        TimeAlarmSessionController.promoteNext(context)
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, identity.taskDeepLink).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }
    }

    companion object {
        const val ACTION_STOP = "com.pingplace.timealarm.action.STOP"
    }
}
