package com.pingplace.timealarm

import io.flutter.plugin.common.EventChannel

internal object AlarmSessionEvents {
    private val lock = Any()
    private var sink: EventChannel.EventSink? = null

    fun attach(target: EventChannel.EventSink?) = synchronized(lock) {
        sink = target
    }

    fun publish(snapshot: Map<String, Any>?) = synchronized(lock) {
        sink?.success(snapshot)
    }
}
