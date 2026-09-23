package com.pingplace.timealarm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlarmSessionPolicyTest {
    @Test
    fun firstDeliveryStartsAndDuplicateIsIdempotent() {
        assertEquals(AlarmArrivalDecision.START, AlarmSessionPolicy.arrival(null, "a|1"))
        assertEquals(AlarmArrivalDecision.DUPLICATE, AlarmSessionPolicy.arrival("a|1", "a|1"))
    }

    @Test
    fun concurrentDistinctDeliveryQueuesWithoutSecondLoop() {
        assertEquals(AlarmArrivalDecision.QUEUE, AlarmSessionPolicy.arrival("a|1", "b|1"))
        assertEquals(listOf("b|1"), AlarmSessionPolicy.enqueue(emptyList(), "b|1"))
        assertEquals(listOf("b|1"), AlarmSessionPolicy.enqueue(listOf("b|1"), "b|1"))
        assertEquals(listOf("b|1", "c|1"), AlarmSessionPolicy.enqueue(listOf("b|1"), "c|1"))
    }

    @Test
    fun onlyMatchingGenerationCanStop() {
        assertTrue(AlarmSessionPolicy.canStop("task|2", "task|2"))
        assertFalse(AlarmSessionPolicy.canStop("task|2", "task|1"))
        assertFalse(AlarmSessionPolicy.canStop(null, "task|2"))
    }
}
