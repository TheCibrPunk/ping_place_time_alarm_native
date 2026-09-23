package com.pingplace.timealarm

internal enum class AlarmArrivalDecision { START, DUPLICATE, QUEUE }

internal object AlarmSessionPolicy {
    fun arrival(activeToken: String?, incomingToken: String): AlarmArrivalDecision = when {
        activeToken == null -> AlarmArrivalDecision.START
        activeToken == incomingToken -> AlarmArrivalDecision.DUPLICATE
        else -> AlarmArrivalDecision.QUEUE
    }

    fun canStop(activeToken: String?, requestedToken: String): Boolean =
        activeToken != null && activeToken == requestedToken

    fun enqueue(tokens: List<String>, incomingToken: String): List<String> =
        if (incomingToken in tokens) tokens else tokens + incomingToken
}
