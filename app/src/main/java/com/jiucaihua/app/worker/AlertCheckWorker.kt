package com.jiucaihua.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.jiucaihua.app.MainActivity
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.presentation.navigation.NavExtras
import com.jiucaihua.app.domain.usecase.CheckAlertsUseCase
import com.jiucaihua.app.domain.usecase.TriggeredAlert
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlertCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val checkAlertsUseCase: CheckAlertsUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
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
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NavExtras.EXTRA_TARGET_ROUTE, "detail")
            putExtra(NavExtras.EXTRA_TARGET_CODE, alert.code)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_alert_title, alert.name))
            .setContentText(formatNotificationText(triggeredAlert))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(formatNotificationText(triggeredAlert))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(alert.id.toInt(), notification)
    }

    private fun formatNotificationText(triggeredAlert: TriggeredAlert): String {
        val alert = triggeredAlert.alert
        val typeLabel = when (alert.alertType) {
            AlertType.PRICE_ABOVE -> context.getString(R.string.notification_price_above, triggeredAlert.currentValue.toString(), alert.threshold.toString())
            AlertType.PRICE_BELOW -> context.getString(R.string.notification_price_below, triggeredAlert.currentValue.toString(), alert.threshold.toString())
            AlertType.CHANGE_ABOVE -> context.getString(R.string.notification_change_above, triggeredAlert.currentValue.toString(), alert.threshold.toString())
            AlertType.CHANGE_BELOW -> context.getString(R.string.notification_change_below, triggeredAlert.currentValue.toString(), alert.threshold.toString())
            AlertType.VOLUME_ABOVE -> context.getString(R.string.notification_volume_above, triggeredAlert.currentValue.toString(), alert.threshold.toString())
            AlertType.NEW_HIGH -> {
                val period = alert.params["period"] ?: "20"
                context.getString(R.string.notification_new_high, period, triggeredAlert.currentValue.toString())
            }
            AlertType.NEW_LOW -> {
                val period = alert.params["period"] ?: "20"
                context.getString(R.string.notification_new_low, period, triggeredAlert.currentValue.toString())
            }
            AlertType.MA_CROSS_ABOVE -> {
                val shortPeriod = alert.params["short_period"] ?: "5"
                val longPeriod = alert.params["long_period"] ?: "20"
                context.getString(R.string.notification_ma_cross_above, shortPeriod, longPeriod)
            }
            AlertType.MA_CROSS_BELOW -> {
                val shortPeriod = alert.params["short_period"] ?: "5"
                val longPeriod = alert.params["long_period"] ?: "20"
                context.getString(R.string.notification_ma_cross_below, shortPeriod, longPeriod)
            }
        }
        return if (alert.actionHint != null) {
            context.getString(R.string.notification_action_hint, typeLabel, alert.actionHint)
        } else {
            typeLabel
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_alert_channel_description)
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
