package com.jiucaihua.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jiucaihua.app.domain.usecase.GetPortfolioUseCase
import com.jiucaihua.app.domain.usecase.IsMarketOpenUseCase
import com.jiucaihua.app.domain.usecase.RecordSnapshotUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class QuoteRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val isMarketOpenUseCase: IsMarketOpenUseCase,
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val recordSnapshotUseCase: RecordSnapshotUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (isMarketOpenUseCase.isAnyMarketTrading()) {
                getPortfolioUseCase.getPortfolioWithQuotes()
            }
            recordSnapshotUseCase.recordSnapshot()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "quote_refresh_worker"
    }
}
