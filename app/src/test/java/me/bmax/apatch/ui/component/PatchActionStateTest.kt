package me.bmax.apatch.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatchActionStateTest {
    @Test fun repeatedClicksQueueOnlyOneOperationBeforeBackendPublishesBusy() {
        val state = PatchActionState()
        var submissions = 0
        repeat(10) {
            if (state.claimSubmission()) submissions++
        }
        assertEquals(1, submissions)
        assertTrue(state.submitted)
        // An explicitly reopened page has a fresh UI latch, not a global operation lock.
        assertTrue(PatchActionState().claimSubmission())
    }
}
