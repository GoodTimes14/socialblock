package it.socialblock.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import it.socialblock.data.repository.TrackingRepository
import it.socialblock.notifications.AppNotifier
import it.socialblock.platform.time.TimeProvider
import it.socialblock.usage.AndroidUsageStatsSource
import java.time.Instant
import java.time.ZoneId

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val trackingRepository: TrackingRepository,
    private val usageStatsSource: AndroidUsageStatsSource,
    private val notifier: AppNotifier,
    private val timeProvider: TimeProvider,
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result = try {
        val apps = trackingRepository.getTrackedApps()
        if (apps.isNotEmpty()) {
            val nowMillis = timeProvider.nowMillis()
            val snapshot = usageStatsSource.snapshot(
                apps.map {
                    it.packageName
                }.toSet(),
                nowMillis,
            )
            val today = localDate(nowMillis)
            trackingRepository.saveUsageSnapshot(today, snapshot.usageByPackage, nowMillis)
            trackingRepository.pruneUsageHistory(
                localDate(nowMillis - HISTORY_RETENTION_MILLIS),
            )
            notifier.dailySummary(apps, snapshot.usageByPackage)
        }
        Result.success()
    } catch (_: SecurityException) {
        Result.success()
    } catch (_: RuntimeException) {
        Result.retry()
    }

    private fun localDate(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    companion object {
        private const val HISTORY_RETENTION_MILLIS = 31L * 24 * 60 * 60 * 1_000
    }
}
