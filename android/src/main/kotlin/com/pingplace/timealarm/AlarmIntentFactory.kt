package com.pingplace.timealarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

internal object AlarmIntentFactory {
    fun delivery(context: Context, identity: AlarmIdentity, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            identity.notificationId,
            identity.putInto(Intent(context, TimeAlarmReceiver::class.java).apply {
                action = TimeAlarmReceiver.ACTION_FIRE
                data = identity.pendingIntentUri
            }),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    fun stopSilently(context: Context, identity: AlarmIdentity): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            identity.notificationId,
            identity.putInto(Intent(context, TimeAlarmActionReceiver::class.java).apply {
                action = TimeAlarmActionReceiver.ACTION_STOP
                data = identity.pendingIntentUri.buildUpon().appendPath("stop").build()
            }),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun stopAndOpen(context: Context, identity: AlarmIdentity): PendingIntent =
        PendingIntent.getActivity(
            context,
            identity.notificationId,
            identity.putInto(Intent(context, TimeAlarmStopActivity::class.java).apply {
                action = TimeAlarmActionReceiver.ACTION_STOP
                data = identity.pendingIntentUri.buildUpon().appendPath("stop-and-open").build()
            }),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun ringFullScreen(context: Context, identity: AlarmIdentity): PendingIntent =
        PendingIntent.getActivity(
            context,
            identity.notificationId xor 0x40000000,
            identity.putInto(Intent(context, TimeAlarmRingingActivity::class.java).apply {
                action = TimeAlarmRingingActivity.ACTION_RING
                data = identity.pendingIntentUri.buildUpon().appendPath("ringing").build()
            }),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun openTask(context: Context, identity: AlarmIdentity): PendingIntent =
        PendingIntent.getActivity(
            context,
            identity.notificationId,
            Intent(Intent.ACTION_VIEW, identity.taskDeepLink).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
