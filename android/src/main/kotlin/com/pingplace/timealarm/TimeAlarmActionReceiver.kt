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
    }

    companion object {
        const val ACTION_STOP = "com.pingplace.timealarm.action.STOP"
    }
}
