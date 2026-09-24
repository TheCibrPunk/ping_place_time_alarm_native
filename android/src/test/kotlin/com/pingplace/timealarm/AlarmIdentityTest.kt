package com.pingplace.timealarm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AlarmIdentityTest {
    private fun valid(
        generation: Int = 1,
        taskPath: String = "tasks/task-a",
        clockBasis: String = AlarmClockBasis.ABSOLUTE_RTC.wireValue,
        elapsedDeadlineMillis: Long? = null,
        elapsedBootCount: Int? = null,
    ) =
        AlarmIdentity.fromMap(
            mapOf(
                "ownerUid" to "owner-a",
                "taskPath" to taskPath,
                "scheduleGeneration" to generation,
                "notificationId" to 12345,
                "title" to "Call Mom",
                "scheduledAtEpochMillis" to 2_000_000_000_000L,
                "clockBasis" to clockBasis,
                "elapsedDeadlineMillis" to elapsedDeadlineMillis,
                "elapsedBootCount" to elapsedBootCount,
            ),
        )

    @Test
    fun validAuthorityCreatesStrictTaskDeepLink() {
        val identity = assertNotNull(valid())
        assertEquals(
            "pingplace://pingplace.com/time-task?taskPath=tasks%2Ftask-a" +
                "&scheduleGeneration=1&notificationId=12345",
            identity.taskDeepLinkString,
        )
    }

    @Test
    fun malformedAndPlacePathsFailClosed() {
        assertNull(valid(taskPath = "reminders/place-a"))
        assertNull(valid(taskPath = "tasks/a/children/b"))
        assertNull(valid(taskPath = "tasks/"))
    }

    @Test
    fun generationChangesIdentity() {
        assertNotEquals(assertNotNull(valid(1)).token, assertNotNull(valid(2)).token)
    }

    @Test
    fun timerIdentityPersistsClockBasisAndElapsedDeadline() {
        val identity = assertNotNull(
            valid(
                clockBasis = AlarmClockBasis.TIMER_ELAPSED_REALTIME.wireValue,
                elapsedDeadlineMillis = 456_000L,
                elapsedBootCount = 42,
            ),
        )
        assertEquals(AlarmClockBasis.TIMER_ELAPSED_REALTIME, identity.clockBasis)
        assertEquals(456_000L, identity.elapsedDeadlineMillis)
        assertEquals(42, identity.elapsedBootCount)
        assertEquals("timer_elapsed_realtime", identity.toMap()["clockBasis"])
        assertEquals(42, identity.toMap()["elapsedBootCount"])
    }

    @Test
    fun unknownClockBasisFailsClosed() {
        assertNull(valid(clockBasis = "unknown"))
    }

    @Test
    fun timerStopDeepLinkIsExplicitWhileAbsoluteTimeRemainsUnchanged() {
        val absolute = assertNotNull(valid())
        val timer = assertNotNull(
            valid(clockBasis = AlarmClockBasis.TIMER_ELAPSED_REALTIME.wireValue),
        )
        assertEquals(false, absolute.taskDeepLinkString.contains("timerStop"))
        assertEquals(false, timer.taskDeepLinkString.contains("timerStop"))
        assertEquals(false, absolute.stopDeepLinkString.contains("timerStop"))
        assertEquals(true, timer.stopDeepLinkString.endsWith("&timerStop=true"))
    }

    @Test
    fun timerRestartDeepLinkIsExplicitAndGenerationBound() {
        val absolute = assertNotNull(valid())
        val timer = assertNotNull(
            valid(
                generation = 7,
                clockBasis = AlarmClockBasis.TIMER_ELAPSED_REALTIME.wireValue,
            ),
        )
        assertNull(absolute.restartDeepLinkString)
        assertEquals(
            "pingplace://pingplace.com/time-task?taskPath=tasks%2Ftask-a" +
                "&scheduleGeneration=7&notificationId=12345&timerRestart=true",
            timer.restartDeepLinkString,
        )
    }
}
