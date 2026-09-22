package com.pingplace.timealarm

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Compile-only future alarm controller. It cannot start or perform work. */
class TimeAlarmService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
