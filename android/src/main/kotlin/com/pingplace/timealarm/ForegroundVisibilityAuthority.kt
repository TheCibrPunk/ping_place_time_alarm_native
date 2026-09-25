package com.pingplace.timealarm

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.view.ViewTreeObserver
import java.lang.ref.WeakReference

internal data class ForegroundVisibilityInputs(
    val attached: Boolean,
    val resumed: Boolean,
    val focused: Boolean,
    val interactive: Boolean,
    val unlocked: Boolean,
    val currentActivity: Boolean,
)

internal object ForegroundVisibilityPolicy {
    fun isConfidentlyVisible(inputs: ForegroundVisibilityInputs): Boolean =
        inputs.attached && inputs.resumed && inputs.focused && inputs.interactive &&
            inputs.unlocked && inputs.currentActivity
}

internal object ForegroundVisibilityAuthority {
    private val lock = Any()
    private var activityRef = WeakReference<Activity>(null)
    private var activityIdentity: Int? = null
    private var resumed = false
    private var focused = false
    private var applicationContext: Context? = null
    private var focusObserver: ViewTreeObserver.OnWindowFocusChangeListener? = null

    fun attach(context: Context, activity: Activity) = synchronized(lock) {
        detachLocked()
        applicationContext = context.applicationContext
        activityRef = WeakReference(activity)
        activityIdentity = System.identityHashCode(activity)
        resumed = false
        focused = activity.hasWindowFocus()
        focusObserver = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            synchronized(lock) {
                if (!isCurrentLocked(activity)) return@OnWindowFocusChangeListener
                focused = hasFocus
                notifyVisibilityChangedLocked()
            }
        }.also { activity.window.decorView.viewTreeObserver.addOnWindowFocusChangeListener(it) }
        notifyVisibilityChangedLocked()
    }

    fun detach(activity: Activity? = null) = synchronized(lock) {
        if (activity != null && !isCurrentLocked(activity)) return
        detachLocked()
        notifyVisibilityChangedLocked()
    }

    fun resumed(activity: Activity) = synchronized(lock) {
        if (!isCurrentLocked(activity)) return
        resumed = true
        focused = activity.hasWindowFocus()
        notifyVisibilityChangedLocked()
    }

    fun paused(activity: Activity) = synchronized(lock) {
        if (!isCurrentLocked(activity)) return
        resumed = false
        focused = false
        notifyVisibilityChangedLocked()
    }

    fun isConfidentlyVisible(context: Context): Boolean = synchronized(lock) {
        val activity = activityRef.get()
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        ForegroundVisibilityPolicy.isConfidentlyVisible(
            ForegroundVisibilityInputs(
                attached = activity != null && activityIdentity != null,
                resumed = resumed,
                focused = focused && activity?.hasWindowFocus() == true,
                interactive = power?.isInteractive == true,
                unlocked = keyguard?.isKeyguardLocked == false,
                currentActivity = activity != null && isCurrentLocked(activity),
            ),
        )
    }

    private fun isCurrentLocked(activity: Activity): Boolean =
        activityRef.get() === activity && activityIdentity == System.identityHashCode(activity)

    private fun detachLocked() {
        val oldActivity = activityRef.get()
        val observer = focusObserver
        if (oldActivity != null && observer != null) {
            runCatching {
                oldActivity.window.decorView.viewTreeObserver
                    .removeOnWindowFocusChangeListener(observer)
            }
        }
        activityRef = WeakReference(null)
        activityIdentity = null
        resumed = false
        focused = false
        focusObserver = null
    }

    private fun notifyVisibilityChangedLocked() {
        val context = applicationContext ?: return
        TimeAlarmSessionController.visibilityChanged(context, isConfidentlyVisible(context))
    }
}
