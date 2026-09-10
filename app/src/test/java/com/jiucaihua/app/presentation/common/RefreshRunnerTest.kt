package com.jiucaihua.app.presentation.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshRunnerTest {
    @Test
    fun repeatedRequestsJoinSlowRound() = runTest {
        val response = CompletableDeferred<Unit>()
        var calls = 0
        val runner = RefreshRunner(backgroundScope) { calls++; response.await() }
        val first = runner.request()
        runCurrent()
        repeat(20) { assertSame(first, runner.request()) }
        runCurrent()
        assertEquals(1, calls)
        response.complete(Unit)
        runCurrent()
        assertTrue(first.isCompleted)
    }

    @Test
    fun changesDuringFetchCoalesceIntoOneFollowUp() = runTest {
        val response = CompletableDeferred<Unit>()
        var calls = 0
        val runner = RefreshRunner(backgroundScope) { calls++; response.await() }
        runner.request()
        runCurrent()
        repeat(10) { runner.request(invalidateRunning = true) }
        response.complete(Unit)
        runCurrent()
        assertEquals(2, calls)
        runner.request()
        runCurrent()
        assertEquals(3, calls)
    }

    @Test
    fun leavingPageCancelsPendingWorkAndReturningCanRefresh() = runTest {
        var calls = 0
        var cancellations = 0
        val runner = RefreshRunner(backgroundScope) {
            calls++
            try { awaitCancellation() } finally { cancellations++ }
        }
        val first = runner.request()
        runCurrent()
        runner.request(invalidateRunning = true)
        runner.cancel()
        runCurrent()
        assertTrue(first.isCancelled)
        assertEquals(1, calls)
        assertEquals(1, cancellations)
        val second = runner.request()
        runCurrent()
        assertFalse(second.isCancelled)
        assertEquals(2, calls)
        runner.cancel()
    }
}
