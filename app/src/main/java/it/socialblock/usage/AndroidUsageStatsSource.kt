package it.socialblock.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import it.socialblock.domain.model.UsageSnapshot
import it.socialblock.platform.time.TimeProvider
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidUsageStatsSource @Inject constructor(
    private val usageStatsManager: UsageStatsManager,
    private val timeProvider: TimeProvider,
) {
    private val snapshotCache = IncrementalUsageSnapshotCache()

    fun snapshot(packageNames: Set<String>, atMillis: Long = timeProvider.nowMillis()): UsageSnapshot {
        val startMillis = startOfLocalDay(atMillis)
        return snapshotCache.snapshot(
            packageNames = packageNames,
            fromMillis = startMillis,
            atMillis = atMillis,
            eventLoader = ::readEvents,
        )
    }

    private fun readEvents(fromMillis: Long, toMillis: Long): List<UsageEventRecord> {
        val usageEvents = usageStatsManager.queryEvents(fromMillis, toMillis)
        val event = UsageEvents.Event()
        return buildList {
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val packageName = event.packageName ?: continue
                val mappedType = event.eventType.toUsageEventType() ?: continue
                add(UsageEventRecord(packageName, event.timeStamp, mappedType))
            }
        }
    }

    companion object {
        fun startOfLocalDay(atMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long = Instant.ofEpochMilli(atMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}

@Suppress("DEPRECATION")
internal fun Int.toUsageEventType(): UsageEventType? = when (this) {
    UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageEventType.FOREGROUND
    UsageEvents.Event.MOVE_TO_BACKGROUND -> UsageEventType.BACKGROUND
    else -> null
}
