package com.pingplace.timealarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class TimeAlarmService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val identity = AlarmIdentity.fromIntent(intent)
        if (identity == null || !PingPlaceTimeAlarmNativePlugin.notificationsAllowed(this)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        when (TimeAlarmSessionController.begin(this, identity)) {
            SessionStart.STARTED -> Unit
            SessionStart.DUPLICATE -> Unit
            SessionStart.QUEUED -> Unit
            SessionStart.FAILED -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        TimeAlarmSessionController.serviceDestroyed(this)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.pingplace.timealarm.action.START"

        internal fun startIntent(context: Context, identity: AlarmIdentity): Intent =
            identity.putInto(Intent(context, TimeAlarmService::class.java).apply {
                action = ACTION_START
            })
    }
}
