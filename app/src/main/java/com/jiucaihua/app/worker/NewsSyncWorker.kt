package com.jiucaihua.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jiucaihua.app.domain.repository.NewsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NewsSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val newsRepository: NewsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            newsRepository.refreshNews()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "news_sync_worker"
    }
}
