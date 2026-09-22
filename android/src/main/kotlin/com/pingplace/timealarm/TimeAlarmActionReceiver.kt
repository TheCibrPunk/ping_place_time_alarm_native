package com.pingplace.timealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Compile-only future alarm action entry point. Deliberately performs no action. */
class TimeAlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
