package it.socialblock.monitoring

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringServiceController @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun start(): Boolean = try {
        ContextCompat.startForegroundService(
            context,
            Intent(context, UsageMonitoringService::class.java),
        )
        true
    } catch (_: RuntimeException) {
        false
    }

    fun stop() {
        context.stopService(Intent(context, UsageMonitoringService::class.java))
    }
}
