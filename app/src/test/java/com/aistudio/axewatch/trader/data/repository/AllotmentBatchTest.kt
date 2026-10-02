package com.aistudio.axewatch.trader.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AllotmentBatchTest {
    @Test fun familyCheckContinuesAfterFailureAndReportsEveryApplicant() = runBlocking {
        val progress = mutableListOf<Int>()
        val result = runAllotmentBatch(listOf(1, 2, 3), { done, total ->
            assertEquals(3, total); progress.add(done)
        }, check = { applicant ->
            if (applicant == 2) error("Transport unavailable")
            "checked:$applicant"
        }, onFailure = { "failed:$it" })
        assertEquals(listOf("checked:1", "failed:2", "checked:3"), result)
        assertEquals(listOf(0, 1, 2, 3), progress)
    }

    @Test fun cancellationStopsTheBatchWithoutCreatingAFailedVerdict() = runBlocking {
        var fallbackCalled = false
        try {
            runAllotmentBatch(listOf(1, 2), { _, _ -> }, check = { throw CancellationException() },
                onFailure = { fallbackCalled = true; "failed" })
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertFalse(fallbackCalled)
    }
}
