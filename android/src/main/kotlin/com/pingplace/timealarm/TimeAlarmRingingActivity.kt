package com.pingplace.timealarm

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Minimal lock-screen alarm surface. It contains no completion authority. */
class TimeAlarmRingingActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var identity: AlarmIdentity? = null
    private val finishAtTimeout = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val parsed = AlarmIdentity.fromIntent(intent)
        if (intent.action != ACTION_RING || parsed == null ||
            AlarmStore(this).active()?.token != parsed.token
        ) {
            finish()
            return
        }
        identity = parsed
        showOverLockScreen()
        setContentView(content(parsed))
        handler.postDelayed(
            finishAtTimeout,
            TimeAlarmSessionController.MAX_ALERT_DURATION_MILLIS,
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(finishAtTimeout)
        super.onDestroy()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    private fun content(alarm: AlarmIdentity): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(48, 48, 48, 48)
        setBackgroundColor(Color.rgb(16, 24, 40))
        addView(TextView(context).apply {
            text = "Ping Place"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, matchWidth())
        addView(TextView(context).apply {
            text = alarm.title
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 56)
        }, matchWidth())
        addView(Button(context).apply {
            text = "STOP"
            textSize = 24f
            minHeight = 128
            setOnClickListener {
                identity?.let { active ->
                    TimeAlarmStopCoordinator.stopAndOpen(this@TimeAlarmRingingActivity, active)
                }
                finish()
            }
        }, matchWidth())
        if (alarm.clockBasis == AlarmClockBasis.TIMER_ELAPSED_REALTIME) {
            addView(Button(context).apply {
                text = "RESTART"
                textSize = 24f
                minHeight = 128
                setOnClickListener {
                    identity?.let { active ->
                        TimeAlarmStopCoordinator.restartAndOpen(
                            this@TimeAlarmRingingActivity,
                            active,
                        )
                    }
                    finish()
                }
            }, matchWidth())
        }
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    companion object {
        const val ACTION_RING = "com.pingplace.timealarm.action.RING"
    }
}
