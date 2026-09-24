package it.socialblock.usage

import it.socialblock.domain.model.UsageSnapshot

class IncrementalUsageSnapshotCache(private val fullReconciliationIntervalMillis: Long = DEFAULT_RECONCILIATION_INTERVAL_MILLIS) {
    private val lock = Any()
    private var state: CacheState? = null

    init {
        require(fullReconciliationIntervalMillis > 0L)
    }

    fun snapshot(
        packageNames: Set<String>,
        fromMillis: Long,
        atMillis: Long,
        eventLoader: (fromMillis: Long, toMillis: Long) -> List<UsageEventRecord>,
    ): UsageSnapshot = synchronized(lock) {
        if (packageNames.isEmpty() || atMillis < fromMillis) {
            state = null
            return@synchronized UsageSnapshot(atMillis, emptyMap(), null)
        }

        val trackedPackages = packageNames.toSet()
        val cached = state
        val requiresFullReconciliation =
            cached == null ||
                cached.fromMillis != fromMillis ||
                cached.trackedPackages != trackedPackages ||
                atMillis < cached.lastQueryAtMillis ||
                atMillis - cached.lastFullReconciliationAtMillis >=
                fullReconciliationIntervalMillis

        val activeState =
            if (requiresFullReconciliation) {
                CacheState(
                    fromMillis = fromMillis,
                    trackedPackages = trackedPackages,
                    accumulator = UsageEventAccumulator(fromMillis, trackedPackages),
                    lastQueryAtMillis = fromMillis,
                    lastFullReconciliationAtMillis = atMillis,
                )
            } else {
                checkNotNull(cached)
            }
        val queryStartMillis =
            if (requiresFullReconciliation) fromMillis else activeState.lastQueryAtMillis
        val events = eventLoader(queryStartMillis, atMillis)
        activeState.accumulator.add(events, atMillis)
        activeState.lastQueryAtMillis = atMillis
        state = activeState
        activeState.accumulator.snapshot(atMillis)
    }

    private data class CacheState(
        val fromMillis: Long,
        val trackedPackages: Set<String>,
        val accumulator: UsageEventAccumulator,
        var lastQueryAtMillis: Long,
        val lastFullReconciliationAtMillis: Long,
    )

    private class UsageEventAccumulator(private val fromMillis: Long, private val trackedPackages: Set<String>) {
        private val totals = trackedPackages.associateWith { 0L }.toMutableMap()
        private val activeSince = mutableMapOf<String, Long>()
        private val activePackages = linkedSetOf<String>()
        private var lastProcessedEventTimestamp = fromMillis - 1L

        fun add(events: List<UsageEventRecord>, throughMillis: Long) {
            val previousCutoff = lastProcessedEventTimestamp
            events.asSequence()
                .filter { it.timestampMillis in fromMillis..throughMillis }
                .filter { it.timestampMillis > previousCutoff }
                .sortedBy(UsageEventRecord::timestampMillis)
                .forEach { event ->
                    when (event.type) {
                        UsageEventType.FOREGROUND -> {
                            if (event.packageName in trackedPackages) {
                                activeSince.putIfAbsent(
                                    event.packageName,
                                    event.timestampMillis,
                                )
                            }
                            activePackages.remove(event.packageName)
                            activePackages.add(event.packageName)
                        }

                        UsageEventType.BACKGROUND -> {
                            activeSince.remove(event.packageName)?.let { startedAt ->
                                totals[event.packageName] =
                                    totals.getValue(event.packageName) +
                                    (event.timestampMillis - startedAt).coerceAtLeast(0L)
                            }
                            activePackages.remove(event.packageName)
                        }
                    }
                    lastProcessedEventTimestamp =
                        maxOf(lastProcessedEventTimestamp, event.timestampMillis)
                }
        }

        fun snapshot(atMillis: Long): UsageSnapshot = UsageSnapshot(
            capturedAtMillis = atMillis,
            usageByPackage =
                trackedPackages.associateWith { packageName ->
                    totals.getValue(packageName) +
                        activeSince[packageName]
                            ?.let { (atMillis - it).coerceAtLeast(0L) }
                            .orZero()
                },
            foregroundPackage = activePackages.lastOrNull(),
        )

        private fun Long?.orZero(): Long = this ?: 0L
    }

    companion object {
        const val DEFAULT_RECONCILIATION_INTERVAL_MILLIS = 15 * 60_000L
    }
}
