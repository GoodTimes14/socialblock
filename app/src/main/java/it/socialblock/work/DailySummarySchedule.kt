package it.socialblock.work

import java.time.Duration
import java.time.ZonedDateTime

object DailySummarySchedule {
    fun delayUntilNextRun(now: ZonedDateTime, hour: Int, minute: Int): Duration {
        var next = now.withHour(
            hour.coerceIn(0, 23),
        ).withMinute(minute.coerceIn(0, 59)).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
