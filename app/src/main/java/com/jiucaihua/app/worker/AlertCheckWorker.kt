package com.jiucaihua.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.usecase.CheckAlertsUseCase
import com.jiucaihua.app.domain.usecase.IsMarketOpenUseCase
import com.jiucaihua.app.domain.usecase.TriggeredAlert
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlertCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val checkAlertsUseCase: CheckAlertsUseCase,
    private val isMarketOpenUseCase: IsMarketOpenUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!isMarketOpenUseCase.isAnyMarketTrading()) {
                return Result.success()
            }
            val triggered = checkAlertsUseCase.checkAlerts()
            triggered.forEach { sendNotification(it) }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(triggeredAlert: TriggeredAlert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        ensureNotificationChannel()

        val alert = triggeredAlert.alert
        val typeLabel = when (alert.alertType) {
            AlertType.PRICE_ABOVE -> "价格已达 ${triggeredAlert.currentValue}，超过预警值 ${alert.threshold}"
            AlertType.PRICE_BELOW -> "价格已跌至 ${triggeredAlert.currentValue}，低于预警值 ${alert.threshold}"
            AlertType.CHANGE_ABOVE -> "涨幅已达 ${triggeredAlert.currentValue}%，超过预警值 ${alert.threshold}%"
            AlertType.CHANGE_BELOW -> "跌幅已达 ${triggeredAlert.currentValue}%，超过预警值 ${alert.threshold}%"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${alert.name} 预警触发")
            .setContentText(typeLabel)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(alert.id.toInt(), notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "价格预警",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "证券价格预警通知"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val WORK_NAME = "alert_check_worker"
        const val CHANNEL_ID = "price_alerts"
    }
}
