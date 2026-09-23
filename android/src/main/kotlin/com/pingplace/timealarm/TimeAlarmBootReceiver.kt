package com.pingplace.timealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Clears device-side execution records so Flutter reconciliation restores future alarms. */
class TimeAlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            AlarmStore(context).clearAll()
        }
    }
}
