package com.pingplace.timealarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

internal enum class SessionStart { STARTED, DUPLICATE, QUEUED, FAILED }

internal object TimeAlarmSessionController {
    const val CHANNEL_ID = "ping_place_time_alarm_alerts"
    const val CHANNEL_NAME = "Ping Place Time Alarms"
    const val MAX_ALERT_DURATION_MILLIS = 10L * 60L * 1000L

    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var active: AlarmIdentity? = null
    private var service: TimeAlarmService? = null
    private var timeout: Runnable? = null

    fun begin(targetService: TimeAlarmService, identity: AlarmIdentity): SessionStart = synchronized(lock) {
        val current = active
        when (AlarmSessionPolicy.arrival(current?.token, identity.token)) {
            AlarmArrivalDecision.DUPLICATE -> return SessionStart.DUPLICATE
            AlarmArrivalDecision.QUEUE -> {
                AlarmStore(targetService).enqueue(identity)
                return SessionStart.QUEUED
            }
            AlarmArrivalDecision.START -> Unit
        }
        service = targetService
        active = identity
        AlarmStore(targetService).setActive(identity)
        return try {
            createChannel(targetService)
            targetService.startForeground(identity.notificationId, notification(targetService, identity))
            startAudio(targetService)
            scheduleTimeout(targetService, identity)
            SessionStart.STARTED
        } catch (_: Throwable) {
            stopLocked(targetService, identity)
            SessionStart.FAILED
        }
    }

    fun stopIfMatching(context: Context, identity: AlarmIdentity): Boolean = synchronized(lock) {
        val current = active ?: AlarmStore(context).active()
        if (!AlarmSessionPolicy.canStop(current?.token, identity.token)) return false
        stopLocked(context, identity)
        true
    }

    fun promoteNext(context: Context) {
        val next = synchronized(lock) {
            if (active != null) null else AlarmStore(context).takeNext()
        } ?: return
        ContextCompat.startForegroundService(context, TimeAlarmService.startIntent(context, next))
    }

    fun serviceDestroyed(targetService: TimeAlarmService) = synchronized(lock) {
        if (service !== targetService) return
        active?.let { stopLocked(targetService, it) }
        service = null
    }

    private fun startAudio(context: Context) {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: throw IllegalStateException("No system alarm sound")
        val manager = context.getSystemService(AudioManager::class.java)
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setOnAudioFocusChangeListener { }
            .build()
        manager?.requestAudioFocus(request)
        audioManager = manager
        focusRequest = request
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(context, alarmUri)
            setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            isLooping = true
            setOnPreparedListener { it.start() }
            prepareAsync()
        }
    }

    private fun scheduleTimeout(context: Context, identity: AlarmIdentity) {
        timeout?.let(handler::removeCallbacks)
        timeout = Runnable {
            if (stopIfMatching(context, identity)) {
                context.stopService(Intent(context, TimeAlarmService::class.java))
                promoteNext(context)
            }
        }.also { handler.postDelayed(it, MAX_ALERT_DURATION_MILLIS) }
    }

    private fun stopLocked(context: Context, identity: AlarmIdentity) {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.reset() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        focusRequest?.let { request -> runCatching { audioManager?.abandonAudioFocusRequest(request) } }
        focusRequest = null
        audioManager = null
        context.getSystemService(NotificationManager::class.java)?.cancel(identity.notificationId)
        AlarmStore(context).setActive(null)
        active = null
        service = null
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Active Ping Place Time Alarm controls"
                setSound(null, null)
                enableVibration(true)
                setBypassDnd(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    private fun notification(context: Context, identity: AlarmIdentity): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Ping Place Time Alarm")
            .setContentText(identity.title)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(AlarmIntentFactory.openTask(context, identity))
            .setFullScreenIntent(AlarmIntentFactory.ringFullScreen(context, identity), true)
            .setDeleteIntent(AlarmIntentFactory.stopSilently(context, identity))
            .addAction(0, "STOP", AlarmIntentFactory.stopAndOpen(context, identity))
            .build()
}
