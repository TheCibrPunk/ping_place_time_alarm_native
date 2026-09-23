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

    fun stop(context: Context, identity: AlarmIdentity): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            identity.notificationId,
            identity.putInto(Intent(context, TimeAlarmActionReceiver::class.java).apply {
                action = TimeAlarmActionReceiver.ACTION_STOP
                data = identity.pendingIntentUri.buildUpon().appendPath("stop").build()
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
