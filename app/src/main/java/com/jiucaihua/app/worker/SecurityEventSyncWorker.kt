package com.jiucaihua.app.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jiucaihua.app.domain.model.SecurityEventKind
import com.jiucaihua.app.domain.model.SecurityId
import com.jiucaihua.app.domain.repository.HoldingRepository
import com.jiucaihua.app.domain.repository.SecurityEventRepository
import com.jiucaihua.app.domain.repository.WatchlistRepository
import com.jiucaihua.app.JiucaihuaApplication
import com.jiucaihua.app.MainActivity
import com.jiucaihua.app.R
import com.jiucaihua.app.presentation.navigation.NavExtras
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException

/** Syncs only the user's own holdings and watchlist; it never accesses Tencent account data. */
@HiltWorker
class SecurityEventSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val holdingRepository: HoldingRepository,
    private val watchlistRepository: WatchlistRepository,
    private val securityEventRepository: SecurityEventRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val codes = (holdingRepository.getActiveHoldings().first().map { it.code } +
            watchlistRepository.getAllWatchlist().first().map { it.code })
            .mapNotNull(SecurityId::parse)
            .filter { it.marketType.name in EVENT_SUPPORTED_MARKETS }
            .distinct()
        codes.forEach { code ->
            val initialized = securityEventRepository.hasSuccessfulSync(code)
            val events = securityEventRepository.syncEvents(code, EVENT_KINDS)
            if (initialized) notifyNewEvents(code, events)
        }
        Result.success()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        Result.retry()
    }

    private suspend fun notifyNewEvents(code: SecurityId, events: List<com.jiucaihua.app.domain.model.SecurityEvent>) {
        if (!canNotify()) return
        val dayStart = System.currentTimeMillis() - DAY_MILLIS
        var remaining = (MAX_DAILY_NOTIFICATIONS - securityEventRepository.countEventNotifications(code, dayStart)).coerceAtLeast(0)
        if (remaining == 0) return
        events.filter(::isNotifiable).forEach { event ->
            if (remaining == 0 || securityEventRepository.isEventNotified(event)) return@forEach
            sendNotification(code, event)
            securityEventRepository.markEventNotified(event, code)
            remaining -= 1
        }
    }

    private fun isNotifiable(event: com.jiucaihua.app.domain.model.SecurityEvent): Boolean {
        if (event.publishedAt > 0 && System.currentTimeMillis() - event.publishedAt > DAY_MILLIS) return false
        return event.kind != SecurityEventKind.NEWS || (event.importance ?: 0) > 0
    }

    private fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun sendNotification(code: SecurityId, event: com.jiucaihua.app.domain.model.SecurityEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NavExtras.EXTRA_TARGET_ROUTE, "detail")
            putExtra(NavExtras.EXTRA_TARGET_CODE, code.value)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            event.externalId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        NotificationCompat.Builder(applicationContext, JiucaihuaApplication.CHANNEL_NEWS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(event.kind.toLabel())
            .setContentText(event.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.title))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            .also { notification -> NotificationManagerCompat.from(applicationContext).notify(event.externalId.hashCode(), notification) }
    }

    private fun SecurityEventKind.toLabel(): String = when (this) {
        SecurityEventKind.NEWS -> "重要个股资讯"
        SecurityEventKind.ANNOUNCEMENT -> "个股公告"
        SecurityEventKind.PERIODIC_REPORT -> "定期报告"
        SecurityEventKind.RESEARCH -> "机构研报"
        SecurityEventKind.INDUSTRY_NEWS -> "行业资讯"
    }

    companion object {
        const val WORK_NAME = "security_event_sync_worker"
        private val EVENT_KINDS = setOf(
            SecurityEventKind.NEWS,
            SecurityEventKind.ANNOUNCEMENT,
            SecurityEventKind.PERIODIC_REPORT,
            SecurityEventKind.RESEARCH,
        )
        val EVENT_SUPPORTED_MARKETS = setOf("A_STOCK", "HK_STOCK", "US_STOCK")
        const val MAX_DAILY_NOTIFICATIONS = 5
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
