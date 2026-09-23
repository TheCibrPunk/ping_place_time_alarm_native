package com.pingplace.timealarm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AlarmIdentityTest {
    private fun valid(generation: Int = 1, taskPath: String = "tasks/task-a") =
        AlarmIdentity.fromMap(
            mapOf(
                "ownerUid" to "owner-a",
                "taskPath" to taskPath,
                "scheduleGeneration" to generation,
                "notificationId" to 12345,
                "title" to "Call Mom",
                "scheduledAtEpochMillis" to 2_000_000_000_000L,
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
}
