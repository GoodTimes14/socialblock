package it.socialblock.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import it.socialblock.data.settings.SettingsRepository
import it.socialblock.notifications.AppNotifier
import it.socialblock.platform.time.TimeProvider
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmergencyBypassReceiver : BroadcastReceiver() {
    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var notifier: AppNotifier

    @Inject lateinit var timeProvider: TimeProvider

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ENABLE) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val until = settingsRepository.enableEmergencyBypass(timeProvider.nowMillis())
                notifier.emergencyBypassEnabled(until)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ENABLE = "it.socialblock.action.ENABLE_EMERGENCY_BYPASS"
    }
}
