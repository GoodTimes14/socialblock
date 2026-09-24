package it.socialblock.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import it.socialblock.data.settings.SettingsRepository
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DailySummaryScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun scheduleFromSettings() {
        val settings = settingsRepository.settings.first()
        schedule(settings.notificationHour, settings.notificationMinute)
    }

    fun schedule(hour: Int, minute: Int) {
        val initialDelay = DailySummarySchedule.delayUntilNextRun(ZonedDateTime.now(), hour, minute)
        val request =
            PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS, 15, TimeUnit.MINUTES)
                .setInitialDelay(initialDelay)
                .addTag(DAILY_SUMMARY_WORK_NAME)
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_SUMMARY_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    companion object {
        const val DAILY_SUMMARY_WORK_NAME = "daily_usage_summary"
    }
}
