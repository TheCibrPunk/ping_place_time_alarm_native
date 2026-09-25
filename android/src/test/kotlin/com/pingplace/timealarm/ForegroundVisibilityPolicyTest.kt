package com.pingplace.timealarm

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForegroundVisibilityPolicyTest {
    private val visible = ForegroundVisibilityInputs(
        attached = true,
        resumed = true,
        focused = true,
        interactive = true,
        unlocked = true,
        currentActivity = true,
    )

    @Test fun allSignalsAreRequired() = assertTrue(
        ForegroundVisibilityPolicy.isConfidentlyVisible(visible),
    )

    @Test fun unattachedFailsClosed() = rejects(visible.copy(attached = false))
    @Test fun pausedFailsClosed() = rejects(visible.copy(resumed = false))
    @Test fun focusLossFailsClosed() = rejects(visible.copy(focused = false))
    @Test fun nonInteractiveFailsClosed() = rejects(visible.copy(interactive = false))
    @Test fun lockedFailsClosed() = rejects(visible.copy(unlocked = false))
    @Test fun staleActivityFailsClosed() = rejects(visible.copy(currentActivity = false))

    private fun rejects(inputs: ForegroundVisibilityInputs) = assertFalse(
        ForegroundVisibilityPolicy.isConfidentlyVisible(inputs),
    )
}
