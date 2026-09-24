package it.socialblock.work

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DailySummaryScheduleTest {
    private val zone = ZoneId.of("Europe/Rome")

    @Test
    fun `schedules later time on same day`() {
        val now = ZonedDateTime.of(2026, 7, 15, 18, 30, 0, 0, zone)

        val delay = DailySummarySchedule.delayUntilNextRun(now, hour = 22, minute = 0)

        assertEquals(Duration.ofHours(3).plusMinutes(30), delay)
    }

    @Test
    fun `schedules tomorrow when configured time has passed`() {
        val now = ZonedDateTime.of(2026, 7, 15, 23, 30, 0, 0, zone)

        val delay = DailySummarySchedule.delayUntilNextRun(now, hour = 22, minute = 0)

        assertEquals(Duration.ofHours(22).plusMinutes(30), delay)
    }
}
