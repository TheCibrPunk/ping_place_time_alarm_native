package com.pingplace.timealarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal class AlarmStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    fun scheduled(): List<AlarmIdentity> = decode(preferences.getString(SCHEDULED, null))

    @Synchronized
    fun putScheduled(identity: AlarmIdentity) {
        val values = scheduled().filterNot { it.token == identity.token } + identity
        save(SCHEDULED, values)
    }

    @Synchronized
    fun removeScheduled(identity: AlarmIdentity) {
        save(SCHEDULED, scheduled().filterNot { it.token == identity.token })
    }

    @Synchronized
    fun enqueue(identity: AlarmIdentity) {
        val active = active()
        if (active?.token == identity.token) return
        val values = queue().filterNot { it.token == identity.token } + identity
        save(QUEUE, values)
    }

    @Synchronized
    fun takeNext(): AlarmIdentity? {
        val values = queue()
        val next = values.firstOrNull()
        save(QUEUE, values.drop(1))
        return next
    }

    @Synchronized
    fun removeQueued(identity: AlarmIdentity) {
        save(QUEUE, queue().filterNot { it.token == identity.token })
    }

    @Synchronized
    fun queue(): List<AlarmIdentity> = decode(preferences.getString(QUEUE, null))

    @Synchronized
    fun active(): AlarmIdentity? = decode(preferences.getString(ACTIVE, null)).firstOrNull()

    @Synchronized
    fun setActive(identity: AlarmIdentity?) {
        save(ACTIVE, if (identity == null) emptyList() else listOf(identity))
    }

    @Synchronized
    fun clearAll() {
        preferences.edit().remove(SCHEDULED).remove(QUEUE).remove(ACTIVE).remove(OWNER).apply()
    }

    fun activeOwner(): String? = preferences.getString(OWNER, null)

    fun setActiveOwner(ownerUid: String) {
        preferences.edit().putString(OWNER, ownerUid).apply()
    }

    private fun save(key: String, values: List<AlarmIdentity>) {
        val array = JSONArray()
        values.forEach { identity ->
            array.put(JSONObject(identity.toMap()))
        }
        preferences.edit().putString(key, array.toString()).apply()
    }

    private fun decode(value: String?): List<AlarmIdentity> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            buildList {
                for (index in 0 until array.length()) {
                    val raw = array.optJSONObject(index) ?: continue
                    AlarmIdentity.fromMap(
                        mapOf(
                            "ownerUid" to raw.optString("ownerUid"),
                            "taskPath" to raw.optString("taskPath"),
                            "scheduleGeneration" to raw.optInt("scheduleGeneration"),
                            "notificationId" to raw.optInt("notificationId"),
                            "title" to raw.optString("title"),
                            "scheduledAtEpochMillis" to raw.optLong("scheduledAtEpochMillis"),
                            "clockBasis" to raw.optString(
                                "clockBasis",
                                AlarmClockBasis.ABSOLUTE_RTC.wireValue,
                            ),
                            "elapsedDeadlineMillis" to raw.takeIf {
                                it.has("elapsedDeadlineMillis")
                            }?.optLong("elapsedDeadlineMillis"),
                        ),
                    )?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFERENCES = "ping_place_time_alarm_native"
        const val SCHEDULED = "scheduled"
        const val QUEUE = "queue"
        const val ACTIVE = "active"
        const val OWNER = "owner"
    }
}
