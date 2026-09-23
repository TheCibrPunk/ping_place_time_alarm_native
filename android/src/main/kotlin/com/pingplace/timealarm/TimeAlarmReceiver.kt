package com.pingplace.timealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class TimeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val identity = AlarmIdentity.fromIntent(intent) ?: return
        val store = AlarmStore(context)
        if (store.activeOwner() != identity.ownerUid) return
        if (store.scheduled().none { it.token == identity.token }) return
        store.removeScheduled(identity)
        if (!PingPlaceTimeAlarmNativePlugin.notificationsAllowed(context)) return
        ContextCompat.startForegroundService(context, TimeAlarmService.startIntent(context, identity))
    }

    companion object {
        const val ACTION_FIRE = "com.pingplace.timealarm.action.FIRE"
    }
}
