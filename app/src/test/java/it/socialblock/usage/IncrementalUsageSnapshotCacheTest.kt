package it.socialblock.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class IncrementalUsageSnapshotCacheTest {
    @Test
    fun `queries only new interval after initial reconciliation`() {
        val events =
            listOf(
                event("social", 1_000L, UsageEventType.FOREGROUND),
                event("social", 4_000L, UsageEventType.BACKGROUND),
            )
        val queries = mutableListOf<Pair<Long, Long>>()
        val cache = IncrementalUsageSnapshotCache()
        val loader = loader(events, queries)

        val first = cache.snapshot(setOf("social"), 0L, 2_000L, loader)
        val second = cache.snapshot(setOf("social"), 0L, 5_000L, loader)

        assertEquals(1_000L, first.usageByPackage["social"])
        assertEquals(3_000L, second.usageByPackage["social"])
        assertEquals(listOf(0L to 2_000L, 2_000L to 5_000L), queries)
    }

    @Test
    fun `active session advances without rereading old events`() {
        val events = listOf(event("social", 1_000L, UsageEventType.FOREGROUND))
        val cache = IncrementalUsageSnapshotCache()
        val loader = loader(events)

        cache.snapshot(setOf("social"), 0L, 2_000L, loader)
        val later = cache.snapshot(setOf("social"), 0L, 8_000L, loader)

        assertEquals(7_000L, later.usageByPackage["social"])
        assertEquals("social", later.foregroundPackage)
    }

    @Test
    fun `tracked package changes rebuild from start of day`() {
        val events =
            listOf(
                event("social", 1_000L, UsageEventType.FOREGROUND),
                event("social", 2_000L, UsageEventType.BACKGROUND),
                event("video", 3_000L, UsageEventType.FOREGROUND),
                event("video", 5_000L, UsageEventType.BACKGROUND),
            )
        val queries = mutableListOf<Pair<Long, Long>>()
        val cache = IncrementalUsageSnapshotCache()
        val loader = loader(events, queries)

        cache.snapshot(setOf("social"), 0L, 4_000L, loader)
        val expanded =
            cache.snapshot(
                setOf("social", "video"),
                0L,
                6_000L,
                loader,
            )

        assertEquals(2_000L, expanded.usageByPackage["video"])
        assertEquals(0L to 6_000L, queries.last())
    }

    @Test
    fun `periodically reconciles and resets at a new day`() {
        val queries = mutableListOf<Pair<Long, Long>>()
        val cache = IncrementalUsageSnapshotCache(fullReconciliationIntervalMillis = 1_000L)
        val loader = loader(emptyList(), queries)

        cache.snapshot(setOf("social"), 0L, 500L, loader)
        cache.snapshot(setOf("social"), 0L, 1_501L, loader)
        cache.snapshot(setOf("social"), 2_000L, 2_500L, loader)

        assertEquals(
            listOf(
                0L to 500L,
                0L to 1_501L,
                2_000L to 2_500L,
            ),
            queries,
        )
    }

    @Test
    fun `keeps foreground identity for an untracked package`() {
        val cache = IncrementalUsageSnapshotCache()
        val events =
            listOf(
                event("social", 1_000L, UsageEventType.FOREGROUND),
                event("social", 2_000L, UsageEventType.BACKGROUND),
                event("other", 3_000L, UsageEventType.FOREGROUND),
            )

        val snapshot =
            cache.snapshot(
                setOf("social"),
                0L,
                4_000L,
                loader(events),
            )

        assertEquals(1_000L, snapshot.usageByPackage["social"])
        assertEquals("other", snapshot.foregroundPackage)
    }

    private fun loader(
        events: List<UsageEventRecord>,
        queries: MutableList<Pair<Long, Long>> = mutableListOf(),
    ): (Long, Long) -> List<UsageEventRecord> = { fromMillis, toMillis ->
        queries += fromMillis to toMillis
        events.filter { it.timestampMillis in fromMillis..toMillis }
    }

    private fun event(packageName: String, time: Long, type: UsageEventType) = UsageEventRecord(packageName, time, type)
}
