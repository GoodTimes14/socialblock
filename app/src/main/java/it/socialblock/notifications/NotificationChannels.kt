package it.socialblock.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import it.socialblock.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannels @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun createAll() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    MONITORING,
                    context.getString(R.string.monitoring_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Stato del monitoraggio locale delle app selezionate"
                    setShowBadge(false)
                },
                NotificationChannel(
                    ALERTS,
                    context.getString(R.string.alerts_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Avvisi quando un limite si avvicina o viene raggiunto"
                },
                NotificationChannel(
                    SUMMARY,
                    context.getString(R.string.summary_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Riepilogo locale dell'utilizzo giornaliero"
                },
            ),
        )
    }

    companion object {
        const val MONITORING = "monitoring"
        const val ALERTS = "limit_alerts"
        const val SUMMARY = "daily_summary"
    }
}
