package it.socialblock.data.repository

import it.socialblock.data.local.DailyUsageDao
import it.socialblock.data.local.DailyUsageEntity
import it.socialblock.data.local.TrackedAppDao
import it.socialblock.data.local.TrackedAppEntity
import it.socialblock.domain.model.TrackedApp
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TrackingRepository @Inject constructor(private val trackedAppDao: TrackedAppDao, private val dailyUsageDao: DailyUsageDao) {
    fun observeTrackedApps(): Flow<List<TrackedApp>> = trackedAppDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getTrackedApps(): List<TrackedApp> = trackedAppDao.getAll().map { it.toDomain() }

    suspend fun getTrackedApp(packageName: String): TrackedApp? = trackedAppDao.get(packageName)?.toDomain()

    suspend fun addApp(packageName: String, displayName: String, dailyLimitMinutes: Int = DEFAULT_LIMIT_MINUTES, nowMillis: Long) {
        trackedAppDao.upsert(
            TrackedAppEntity(
                packageName = packageName,
                displayName = displayName,
                dailyLimitMinutes = dailyLimitMinutes.coerceIn(
                    MIN_LIMIT_MINUTES,
                    MAX_LIMIT_MINUTES,
                ),
                timerEnabled = true,
                overlayEnabled = true,
                createdAtMillis = nowMillis,
            ),
        )
    }

    suspend fun updateApp(app: TrackedApp) {
        val current = trackedAppDao.get(app.packageName) ?: return
        trackedAppDao.upsert(
            current.copy(
                displayName = app.displayName,
                dailyLimitMinutes = app.dailyLimitMinutes.coerceIn(
                    MIN_LIMIT_MINUTES,
                    MAX_LIMIT_MINUTES,
                ),
                timerEnabled = app.timerEnabled,
                overlayEnabled = app.overlayEnabled,
                lastApproachingNoticeDate = app.lastApproachingNoticeDate,
                lastLimitNoticeDate = app.lastLimitNoticeDate,
            ),
        )
    }

    suspend fun removeApp(packageName: String) = trackedAppDao.delete(packageName)

    suspend fun markApproachingNotice(packageName: String, localDate: String) = trackedAppDao.markApproachingNotice(packageName, localDate)

    suspend fun markLimitNotice(packageName: String, localDate: String) = trackedAppDao.markLimitNotice(packageName, localDate)

    suspend fun saveUsageSnapshot(localDate: String, usageByPackage: Map<String, Long>, capturedAtMillis: Long) {
        if (usageByPackage.isEmpty()) return
        dailyUsageDao.upsertAll(
            usageByPackage.map { (packageName, usedMillis) ->
                DailyUsageEntity(
                    packageName = packageName,
                    localDate = localDate,
                    usedMillis = usedMillis.coerceAtLeast(0L),
                    updatedAtMillis = capturedAtMillis,
                )
            },
        )
    }

    suspend fun pruneUsageHistory(oldestDateToKeep: String) = dailyUsageDao.deleteOlderThan(oldestDateToKeep)

    private fun TrackedAppEntity.toDomain() = TrackedApp(
        packageName = packageName,
        displayName = displayName,
        dailyLimitMinutes = dailyLimitMinutes,
        timerEnabled = timerEnabled,
        overlayEnabled = overlayEnabled,
        lastApproachingNoticeDate = lastApproachingNoticeDate,
        lastLimitNoticeDate = lastLimitNoticeDate,
    )

    companion object {
        const val DEFAULT_LIMIT_MINUTES = 30
        const val MIN_LIMIT_MINUTES = 5
        const val MAX_LIMIT_MINUTES = 24 * 60
    }
}
