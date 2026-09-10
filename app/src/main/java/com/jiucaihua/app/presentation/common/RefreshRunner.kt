package com.jiucaihua.app.presentation.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Main-thread confined. Joins duplicate requests; mutations schedule one follow-up refresh. */
internal class RefreshRunner(
    private val scope: CoroutineScope,
    private val refresh: suspend () -> Unit,
) {
    private var job: Job? = null
    private var invalidated = false

    fun request(invalidateRunning: Boolean = false): Job {
        job?.takeIf { it.isActive }?.let {
            invalidated = invalidated || invalidateRunning
            return it
        }
        // Publish the job before executing even if refresh completes without suspending.
        val next = scope.launch(start = CoroutineStart.LAZY) {
            do {
                invalidated = false
                refresh()
                currentCoroutineContext().ensureActive()
            } while (invalidated)
        }
        job = next
        next.start()
        return next
    }

    fun cancel() {
        job?.cancel()
        job = null
        invalidated = false
    }
}
