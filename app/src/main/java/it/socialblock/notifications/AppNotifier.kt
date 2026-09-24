package it.socialblock.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import it.socialblock.MainActivity
import it.socialblock.R
import it.socialblock.domain.model.TrackedApp
import it.socialblock.monitoring.EmergencyBypassReceiver
import it.socialblock.platform.format.formatDuration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotifier @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun monitoringNotification(totalUsageMillis: Long, bypassUntilMillis: Long?): Notification {
        val content =
            if (bypassUntilMillis != null) {
                val time =
                    Instant.ofEpochMilli(bypassUntilMillis)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                "Pausa di emergenza attiva fino alle $time"
            } else {
                "Oggi: ${formatDuration(totalUsageMillis)} nelle app monitorate"
            }
        return baseBuilder(NotificationChannels.MONITORING)
            .setContentTitle("SocialBlock è attivo")
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                0,
                "Pausa emergenza 15 min",
                emergencyBypassPendingIntent(),
            ).build()
    }

    fun approachingLimit(app: TrackedApp, remainingMillis: Long) {
        notifySafely(
            approachingId(app.packageName),
            baseBuilder(NotificationChannels.ALERTS)
                .setContentTitle("${app.displayName}: limite vicino")
                .setContentText("Restano circa ${formatDuration(remainingMillis)} per oggi.")
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build(),
        )
    }

    fun limitReached(app: TrackedApp) {
        notifySafely(
            reachedId(app.packageName),
            baseBuilder(NotificationChannels.ALERTS)
                .setContentTitle("Limite raggiunto per ${app.displayName}")
                .setContentText("L'app verrà chiusa. Usa la pausa solo in caso di necessità.")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(0, "Pausa emergenza 15 min", emergencyBypassPendingIntent())
                .build(),
        )
    }

    fun emergencyBypassEnabled(untilMillis: Long) {
        val time =
            Instant.ofEpochMilli(untilMillis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        notifySafely(
            EMERGENCY_NOTIFICATION_ID,
            baseBuilder(NotificationChannels.ALERTS)
                .setContentTitle("Pausa di emergenza attivata")
                .setContentText("I timer sono sospesi fino alle $time.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build(),
        )
    }

    fun dailySummary(apps: List<TrackedApp>, usageByPackage: Map<String, Long>) {
        val total = apps.sumOf { usageByPackage[it.packageName] ?: 0L }
        val detail =
            apps.sortedByDescending { usageByPackage[it.packageName] ?: 0L }
                .joinToString(separator = "\n") { app ->
                    "${app.displayName}: ${formatDuration(usageByPackage[app.packageName] ?: 0L)}"
                }
        notifySafely(
            DAILY_SUMMARY_NOTIFICATION_ID,
            baseBuilder(NotificationChannels.SUMMARY)
                .setContentTitle("Oggi hai usato i social per ${formatDuration(total)}")
                .setContentText(detail.lineSequence().firstOrNull() ?: "Nessun utilizzo registrato")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        detail.ifBlank {
                            "Nessun utilizzo registrato"
                        },
                    ),
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build(),
        )
    }

    private fun baseBuilder(channelId: String): NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setColor(0xFFF1C75B.toInt())
        .setContentIntent(openAppPendingIntent())
        .setAutoCancel(channelId != NotificationChannels.MONITORING)

    private fun openAppPendingIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun emergencyBypassPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        1,
        Intent(
            context,
            EmergencyBypassReceiver::class.java,
        ).setAction(EmergencyBypassReceiver.ACTION_ENABLE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notifySafely(id: Int, notification: Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Notification permission is optional at runtime; monitoring still works locally.
        }
    }

    private fun approachingId(packageName: String): Int = APPROACHING_NOTIFICATION_BASE + packageName.hashCode().and(PACKAGE_ID_MASK)

    private fun reachedId(packageName: String): Int = REACHED_NOTIFICATION_BASE + packageName.hashCode().and(PACKAGE_ID_MASK)

    companion object {
        const val MONITORING_NOTIFICATION_ID = 41
        private const val EMERGENCY_NOTIFICATION_ID = 42
        private const val DAILY_SUMMARY_NOTIFICATION_ID = 43
        private const val APPROACHING_NOTIFICATION_BASE = 1_000
        private const val REACHED_NOTIFICATION_BASE = 10_000
        private const val PACKAGE_ID_MASK = 0x0FFF
    }
}
