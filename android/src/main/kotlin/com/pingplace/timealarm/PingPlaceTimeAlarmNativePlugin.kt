package com.pingplace.timealarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class PingPlaceTimeAlarmNativePlugin : FlutterPlugin, MethodChannel.MethodCallHandler,
    ActivityAware, Application.ActivityLifecycleCallbacks, EventChannel.StreamHandler {
    private lateinit var channel: MethodChannel
    private lateinit var events: EventChannel
    private lateinit var applicationContext: Context
    private var application: Application? = null
    private var activity: Activity? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
        events = EventChannel(binding.binaryMessenger, EVENT_CHANNEL_NAME)
        events.setStreamHandler(this)
        application = applicationContext as? Application
        application?.registerActivityLifecycleCallbacks(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            METHOD_IS_AVAILABLE -> result.success(true)
            METHOD_SCHEDULE -> result.success(schedule(call.arguments as? Map<*, *>))
            METHOD_CANCEL -> result.success(cancel(call.arguments as? Map<*, *>))
            METHOD_PENDING -> result.success(AlarmStore(applicationContext).scheduled().map { it.toMap() })
            METHOD_ACTIVE -> result.success(TimeAlarmSessionController.activeSnapshot())
            METHOD_ACTIVATE_OWNER -> result.success(activateOwner(call.arguments as? String))
            METHOD_CLEAR_ALL -> result.success(clearAll())
            METHOD_CAN_USE_FULL_SCREEN -> result.success(canUseFullScreenIntent())
            METHOD_OPEN_FULL_SCREEN_SETTINGS -> result.success(openFullScreenIntentSettings())
            else -> result.notImplemented()
        }
    }

    private fun schedule(raw: Map<*, *>?): String {
        val identity = raw?.let(AlarmIdentity::fromMap) ?: return "rejected:authority"
        if (AlarmStore(applicationContext).activeOwner() != identity.ownerUid) {
            return "rejected:owner-session"
        }
        val store = AlarmStore(applicationContext)
        val existing = store.scheduled().firstOrNull { it.token == identity.token }
        if (existing != null && AlarmClockPolicy.canReuse(
                existing,
                identity,
                SystemClock.elapsedRealtime(),
                currentBootCount(),
            )
        ) {
            return "scheduled"
        }
        val plan = AlarmClockPolicy.plan(
            identity,
            System.currentTimeMillis(),
            SystemClock.elapsedRealtime(),
        ) ?: return "rejected:past"
        if (!notificationsAllowed(applicationContext)) return "notification-permission-required"
        val manager = applicationContext.getSystemService(AlarmManager::class.java)
            ?: return "error"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            return "exact-alarm-permission-required"
        }
        return runCatching {
            store.scheduled()
                .filter { it.taskPath == identity.taskPath && it.token != identity.token }
                .forEach { stale ->
                    AlarmIntentFactory.delivery(
                        applicationContext,
                        stale,
                        PendingIntent.FLAG_NO_CREATE,
                    )?.let { pending ->
                        manager.cancel(pending)
                        pending.cancel()
                    }
                    store.removeScheduled(stale)
                }
            store.queue()
                .filter { it.taskPath == identity.taskPath && it.token != identity.token }
                .forEach(store::removeQueued)
            store.active()
                ?.takeIf { it.taskPath == identity.taskPath && it.token != identity.token }
                ?.let { stale ->
                    if (TimeAlarmSessionController.stopIfMatching(applicationContext, stale)) {
                        applicationContext.stopService(
                            android.content.Intent(applicationContext, TimeAlarmService::class.java),
                        )
                        TimeAlarmSessionController.promoteNext(applicationContext)
                    }
                }
            val scheduledIdentity = identity.withElapsedSchedule(
                plan.persistedElapsedDeadlineMillis,
                if (plan.persistedElapsedDeadlineMillis == null) null else currentBootCount(),
            )
            val intent = AlarmIntentFactory.delivery(
                applicationContext,
                scheduledIdentity,
                PendingIntent.FLAG_UPDATE_CURRENT,
            ) ?: return "error"
            manager.setExactAndAllowWhileIdle(
                plan.alarmType,
                plan.triggerAtMillis,
                intent,
            )
            store.putScheduled(scheduledIdentity)
            "scheduled"
        }.getOrElse { "error" }
    }

    private fun cancel(raw: Map<*, *>?): String {
        val identity = raw?.let(AlarmIdentity::fromMap) ?: return "rejected:authority"
        val manager = applicationContext.getSystemService(AlarmManager::class.java)
            ?: return "error"
        return runCatching {
            val intent = AlarmIntentFactory.delivery(
                applicationContext,
                identity,
                PendingIntent.FLAG_NO_CREATE,
            )
            if (intent != null) {
                manager.cancel(intent)
                intent.cancel()
            }
            val store = AlarmStore(applicationContext)
            store.removeScheduled(identity)
            store.removeQueued(identity)
            if (TimeAlarmSessionController.stopIfMatching(applicationContext, identity)) {
                applicationContext.stopService(
                    android.content.Intent(applicationContext, TimeAlarmService::class.java),
                )
                TimeAlarmSessionController.promoteNext(applicationContext)
            }
            "cancelled"
        }.getOrElse { "error" }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        activity?.let(ForegroundVisibilityAuthority::detach)
        activity = null
        application?.unregisterActivityLifecycleCallbacks(this)
        application = null
        AlarmSessionEvents.attach(null)
        events.setStreamHandler(null)
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        ForegroundVisibilityAuthority.attach(applicationContext, binding.activity)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity?.let(ForegroundVisibilityAuthority::detach)
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) =
        onAttachedToActivity(binding)

    override fun onDetachedFromActivity() {
        activity?.let(ForegroundVisibilityAuthority::detach)
        activity = null
    }

    override fun onActivityResumed(target: Activity) {
        ForegroundVisibilityAuthority.resumed(target)
    }

    override fun onActivityPaused(target: Activity) {
        ForegroundVisibilityAuthority.paused(target)
    }

    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (this.activity === activity) {
            ForegroundVisibilityAuthority.detach(activity)
            this.activity = null
        }
    }

    override fun onListen(arguments: Any?, sink: EventChannel.EventSink?) {
        AlarmSessionEvents.attach(sink)
        sink?.success(TimeAlarmSessionController.activeSnapshot())
    }

    override fun onCancel(arguments: Any?) {
        AlarmSessionEvents.attach(null)
    }

    private fun currentBootCount(): Int? = runCatching {
        Settings.Global.getInt(applicationContext.contentResolver, Settings.Global.BOOT_COUNT)
    }.getOrNull()?.takeIf { it >= 0 }

    private fun activateOwner(rawOwnerUid: String?): String {
        val ownerUid = rawOwnerUid?.trim().orEmpty()
        if (ownerUid.isEmpty()) return "rejected:owner"
        val store = AlarmStore(applicationContext)
        if (store.activeOwner() != null && store.activeOwner() != ownerUid) clearAll()
        store.setActiveOwner(ownerUid)
        return "owner-active"
    }

    private fun clearAll(): String {
        val store = AlarmStore(applicationContext)
        val manager = applicationContext.getSystemService(AlarmManager::class.java)
        store.scheduled().forEach { identity ->
            AlarmIntentFactory.delivery(
                applicationContext,
                identity,
                PendingIntent.FLAG_NO_CREATE,
            )?.let { pending ->
                manager?.cancel(pending)
                pending.cancel()
            }
        }
        store.active()?.let { active ->
            TimeAlarmSessionController.stopIfMatching(applicationContext, active)
        }
        applicationContext.stopService(
            android.content.Intent(applicationContext, TimeAlarmService::class.java),
        )
        store.clearAll()
        return "cleared"
    }

    private fun canUseFullScreenIntent(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            applicationContext.getSystemService(NotificationManager::class.java)
                ?.canUseFullScreenIntent() == true

    private fun openFullScreenIntentSettings(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return "not-required"
        return runCatching {
            applicationContext.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:${applicationContext.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            "opened"
        }.getOrElse { "error" }
    }

    internal companion object {
        const val CHANNEL_NAME = "ping_place_time_alarm_native"
        const val METHOD_IS_AVAILABLE = "isNativePluginAvailable"
        const val METHOD_SCHEDULE = "schedule"
        const val METHOD_CANCEL = "cancel"
        const val METHOD_PENDING = "pendingAlarms"
        const val METHOD_ACTIVE = "activeAlarmSession"
        const val METHOD_ACTIVATE_OWNER = "activateOwner"
        const val METHOD_CLEAR_ALL = "clearAll"
        const val METHOD_CAN_USE_FULL_SCREEN = "canUseFullScreenIntent"
        const val METHOD_OPEN_FULL_SCREEN_SETTINGS = "openFullScreenIntentSettings"
        const val EVENT_CHANNEL_NAME = "ping_place_time_alarm_native/session_events"

        fun notificationsAllowed(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }
}
