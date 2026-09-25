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
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

internal enum class SessionStart { STARTED, DUPLICATE, QUEUED, FAILED }

internal object TimeAlarmSessionController {
    const val CHANNEL_ID = "ping_place_time_alarm_alerts_v2"
    const val CHANNEL_NAME = "Ping Place Time Alarms"
    const val QUIET_CHANNEL_ID = "ping_place_time_alarm_foreground_v1"
    const val QUIET_CHANNEL_NAME = "Ping Place active alarm session"
    const val MAX_ALERT_DURATION_MILLIS = 10L * 60L * 1000L

    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var legacyFocusListener: AudioManager.OnAudioFocusChangeListener? = null
    private var vibrator: Vibrator? = null
    private var active: AlarmIdentity? = null
    private var presentation: AlarmPresentation? = null
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
            presentation = AlarmPresentationPolicy.initial(
                identity,
                ForegroundVisibilityAuthority.isConfidentlyVisible(targetService),
            )
            createChannels(targetService)
            targetService.startForeground(
                identity.notificationId,
                notification(targetService, identity, presentation!!),
            )
            startOutputs(targetService)
            scheduleTimeout(targetService, identity)
            publishActive()
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

    private fun startOutputs(context: Context) {
        val manager = context.getSystemService(AudioManager::class.java)
        val policy = AlarmOutputPolicy.forRingerMode(manager?.ringerMode)
        if (policy.vibrate) startVibration(context)
        if (policy.playAudio) startAudio(context, manager)
    }

    private fun startAudio(context: Context, manager: AudioManager?) {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: throw IllegalStateException("No system alarm sound")
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { }
                .build()
            manager?.requestAudioFocus(request)
            focusRequest = request
        } else {
            @Suppress("DEPRECATION")
            legacyFocusListener = AudioManager.OnAudioFocusChangeListener { }.also { listener ->
                manager?.requestAudioFocus(
                    listener,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
                )
            }
        }
        audioManager = manager
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(attributes)
            setDataSource(context, alarmUri)
            setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            isLooping = true
            setOnPreparedListener { it.start() }
            prepareAsync()
        }
    }

    private fun startVibration(context: Context) {
        val target = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!target.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            target.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 700L, 500L), 0))
        } else {
            @Suppress("DEPRECATION")
            target.vibrate(longArrayOf(0L, 700L, 500L), 0)
        }
        vibrator = target
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
        runCatching { vibrator?.cancel() }
        vibrator = null
        focusRequest?.let { request ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { audioManager?.abandonAudioFocusRequest(request) }
            }
        }
        focusRequest = null
        legacyFocusListener?.let { listener ->
            @Suppress("DEPRECATION")
            runCatching { audioManager?.abandonAudioFocus(listener) }
        }
        legacyFocusListener = null
        audioManager = null
        context.getSystemService(NotificationManager::class.java)?.cancel(identity.notificationId)
        AlarmStore(context).setActive(null)
        active = null
        presentation = null
        service = null
        AlarmSessionEvents.publish(null)
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Active Ping Place Time Alarm controls"
                setSound(null, null)
                // The serialized native session owns vibration so notification
                // delivery cannot add a second, independently controlled output.
                enableVibration(false)
                setBypassDnd(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                QUIET_CHANNEL_ID,
                QUIET_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Quiet controls while Ping Place is already visible"
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
    }

    private fun notification(
        context: Context,
        identity: AlarmIdentity,
        mode: AlarmPresentation,
    ): Notification {
        val foregroundQuiet = mode == AlarmPresentation.FOREGROUND_QUIET
        val builder = NotificationCompat.Builder(
            context,
            if (foregroundQuiet) QUIET_CHANNEL_ID else CHANNEL_ID,
        )
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Ping Place Time Alarm")
            .setContentText(identity.title)
            .setCategory(if (foregroundQuiet) NotificationCompat.CATEGORY_SERVICE else NotificationCompat.CATEGORY_ALARM)
            .setPriority(if (foregroundQuiet) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MAX)
            .setVisibility(if (foregroundQuiet) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(AlarmIntentFactory.openTask(context, identity))
            .setDeleteIntent(AlarmIntentFactory.stopSilently(context, identity))
            .addAction(0, "STOP", AlarmIntentFactory.stopAndOpen(context, identity))
        if (!foregroundQuiet) {
            builder.setFullScreenIntent(AlarmIntentFactory.ringFullScreen(context, identity), true)
        }
        AlarmIntentFactory.restartAndOpen(context, identity)?.let { restart ->
            builder.addAction(0, "RESTART", restart)
        }
        return builder.build()
    }

    private fun publishActive() {
        AlarmSessionEvents.publish(activeSnapshot())
    }

    fun activeSnapshot(): Map<String, Any>? = synchronized(lock) {
        val identity = active ?: return null
        val mode = presentation ?: AlarmPresentation.NATIVE_ALARM
        identity.toMap() + mapOf("presentationMode" to mode.wireValue)
    }

    fun visibilityChanged(context: Context, confidentlyVisible: Boolean) = synchronized(lock) {
        val identity = active ?: return
        val current = presentation ?: AlarmPresentation.NATIVE_ALARM
        val next = AlarmPresentationPolicy.afterVisibilityChange(current, confidentlyVisible)
        if (next == current) return
        val targetService = service ?: return
        presentation = next
        createChannels(context)
        targetService.startForeground(
            identity.notificationId,
            notification(context, identity, next),
        )
        publishActive()
    }
}
