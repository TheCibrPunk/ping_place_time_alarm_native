package com.pingplace.timealarm

import android.content.Intent
import android.net.Uri
import java.security.MessageDigest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class AlarmIdentity(
    val ownerUid: String,
    val taskPath: String,
    val scheduleGeneration: Int,
    val notificationId: Int,
    val title: String,
    val scheduledAtEpochMillis: Long,
    val clockBasis: AlarmClockBasis,
    val elapsedDeadlineMillis: Long? = null,
    val elapsedBootCount: Int? = null,
) {
    val token: String
        get() = "$ownerUid|$taskPath|$scheduleGeneration|$notificationId"

    val pendingIntentUri: Uri
        get() = Uri.Builder()
            .scheme("ping-place-time-alarm")
            .authority("schedule")
            .appendPath(sha256(ownerUid))
            .appendPath(Uri.encode(taskPath))
            .appendQueryParameter("generation", scheduleGeneration.toString())
            .appendQueryParameter("notificationId", notificationId.toString())
            .build()

    val taskDeepLinkString: String
        get() = "pingplace://pingplace.com/time-task" +
            "?taskPath=${encode(taskPath)}" +
            "&scheduleGeneration=$scheduleGeneration" +
            "&notificationId=$notificationId"

    val taskDeepLink: Uri
        get() = Uri.parse(taskDeepLinkString)

    val stopDeepLinkString: String
        get() = taskDeepLinkString + if (
            clockBasis == AlarmClockBasis.TIMER_ELAPSED_REALTIME
        ) "&timerStop=true" else ""

    val stopDeepLink: Uri
        get() = Uri.parse(stopDeepLinkString)

    val restartDeepLinkString: String?
        get() = if (clockBasis == AlarmClockBasis.TIMER_ELAPSED_REALTIME) {
            "$taskDeepLinkString&timerRestart=true"
        } else {
            null
        }

    val restartDeepLink: Uri?
        get() = restartDeepLinkString?.let(Uri::parse)

    fun putInto(intent: Intent): Intent = intent
        .putExtra(EXTRA_OWNER_UID, ownerUid)
        .putExtra(EXTRA_TASK_PATH, taskPath)
        .putExtra(EXTRA_GENERATION, scheduleGeneration)
        .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        .putExtra(EXTRA_TITLE, title)
        .putExtra(EXTRA_SCHEDULED_AT, scheduledAtEpochMillis)
        .putExtra(EXTRA_CLOCK_BASIS, clockBasis.wireValue)
        .also { intent ->
            elapsedDeadlineMillis?.let { intent.putExtra(EXTRA_ELAPSED_DEADLINE, it) }
            elapsedBootCount?.let { intent.putExtra(EXTRA_ELAPSED_BOOT_COUNT, it) }
        }

    fun toMap(): Map<String, Any> = mapOf(
        "ownerUid" to ownerUid,
        "taskPath" to taskPath,
        "scheduleGeneration" to scheduleGeneration,
        "notificationId" to notificationId,
        "title" to title,
        "scheduledAtEpochMillis" to scheduledAtEpochMillis,
        "clockBasis" to clockBasis.wireValue,
    ) + (elapsedDeadlineMillis?.let { mapOf("elapsedDeadlineMillis" to it) } ?: emptyMap()) +
        (elapsedBootCount?.let { mapOf("elapsedBootCount" to it) } ?: emptyMap())

    fun withElapsedSchedule(deadlineMillis: Long?, bootCount: Int?): AlarmIdentity =
        copy(elapsedDeadlineMillis = deadlineMillis, elapsedBootCount = bootCount)

    companion object {
        const val EXTRA_OWNER_UID = "com.pingplace.timealarm.ownerUid"
        const val EXTRA_TASK_PATH = "com.pingplace.timealarm.taskPath"
        const val EXTRA_GENERATION = "com.pingplace.timealarm.scheduleGeneration"
        const val EXTRA_NOTIFICATION_ID = "com.pingplace.timealarm.notificationId"
        const val EXTRA_TITLE = "com.pingplace.timealarm.title"
        const val EXTRA_SCHEDULED_AT = "com.pingplace.timealarm.scheduledAtEpochMillis"
        const val EXTRA_CLOCK_BASIS = "com.pingplace.timealarm.clockBasis"
        const val EXTRA_ELAPSED_DEADLINE = "com.pingplace.timealarm.elapsedDeadlineMillis"
        const val EXTRA_ELAPSED_BOOT_COUNT = "com.pingplace.timealarm.elapsedBootCount"

        private val taskPathPattern = Regex("^tasks/[^/]+$")

        fun fromMap(raw: Map<*, *>): AlarmIdentity? = create(
            ownerUid = raw["ownerUid"] as? String,
            taskPath = raw["taskPath"] as? String,
            scheduleGeneration = (raw["scheduleGeneration"] as? Number)?.toInt(),
            notificationId = (raw["notificationId"] as? Number)?.toInt(),
            title = raw["title"] as? String,
            scheduledAtEpochMillis =
                (raw["scheduledAtEpochMillis"] as? Number)?.toLong(),
            clockBasis = (raw["clockBasis"] as? String) ?: AlarmClockBasis.ABSOLUTE_RTC.wireValue,
            elapsedDeadlineMillis = (raw["elapsedDeadlineMillis"] as? Number)?.toLong(),
            elapsedBootCount = (raw["elapsedBootCount"] as? Number)?.toInt(),
        )

        fun fromIntent(intent: Intent): AlarmIdentity? = create(
            ownerUid = intent.getStringExtra(EXTRA_OWNER_UID),
            taskPath = intent.getStringExtra(EXTRA_TASK_PATH),
            scheduleGeneration = intent.getIntExtra(EXTRA_GENERATION, 0),
            notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0),
            title = intent.getStringExtra(EXTRA_TITLE),
            scheduledAtEpochMillis = intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L),
            clockBasis = intent.getStringExtra(EXTRA_CLOCK_BASIS)
                ?: AlarmClockBasis.ABSOLUTE_RTC.wireValue,
            elapsedDeadlineMillis = intent.takeIf { it.hasExtra(EXTRA_ELAPSED_DEADLINE) }
                ?.getLongExtra(EXTRA_ELAPSED_DEADLINE, 0L),
            elapsedBootCount = intent.takeIf { it.hasExtra(EXTRA_ELAPSED_BOOT_COUNT) }
                ?.getIntExtra(EXTRA_ELAPSED_BOOT_COUNT, -1),
        )

        private fun create(
            ownerUid: String?,
            taskPath: String?,
            scheduleGeneration: Int?,
            notificationId: Int?,
            title: String?,
            scheduledAtEpochMillis: Long?,
            clockBasis: String?,
            elapsedDeadlineMillis: Long?,
            elapsedBootCount: Int?,
        ): AlarmIdentity? {
            val cleanOwner = ownerUid?.trim().orEmpty()
            val cleanPath = taskPath?.trim().orEmpty()
            val cleanTitle = title?.trim().orEmpty()
            val parsedClockBasis = AlarmClockBasis.fromWire(clockBasis)
            if (cleanOwner.isEmpty() ||
                !taskPathPattern.matches(cleanPath) ||
                cleanTitle.isEmpty() ||
                scheduleGeneration == null || scheduleGeneration <= 0 ||
                notificationId == null || notificationId <= 0 ||
                scheduledAtEpochMillis == null || scheduledAtEpochMillis <= 0L ||
                parsedClockBasis == null ||
                (elapsedDeadlineMillis != null && elapsedDeadlineMillis <= 0L) ||
                (elapsedBootCount != null && elapsedBootCount < 0)
            ) {
                return null
            }
            return AlarmIdentity(
                cleanOwner,
                cleanPath,
                scheduleGeneration,
                notificationId,
                cleanTitle,
                scheduledAtEpochMillis,
                parsedClockBasis,
                elapsedDeadlineMillis,
                elapsedBootCount,
            )
        }

        private fun sha256(value: String): String = MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)

        private fun encode(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                .replace("+", "%20")
    }
}
