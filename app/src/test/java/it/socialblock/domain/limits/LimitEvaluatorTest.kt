package it.socialblock.domain.limits

import it.socialblock.domain.model.TrackedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitEvaluatorTest {
    private val evaluator = LimitEvaluator()
    private val app =
        TrackedApp(
            packageName = "com.example.social",
            displayName = "Social",
            dailyLimitMinutes = 30,
            timerEnabled = true,
            overlayEnabled = true,
            lastApproachingNoticeDate = null,
            lastLimitNoticeDate = null,
        )

    @Test
    fun `available before approaching window`() {
        val status = evaluator.evaluate(app, usedMillis = 20 * 60_000L, bypassActive = false)

        assertEquals(LimitStatus.Available, status)
    }

    @Test
    fun `approaching during final five minutes`() {
        val status = evaluator.evaluate(app, usedMillis = 26 * 60_000L, bypassActive = false)

        assertTrue(status is LimitStatus.Approaching)
        assertEquals(4 * 60_000L, (status as LimitStatus.Approaching).remainingMillis)
    }

    @Test
    fun `reached at exact limit`() {
        val status = evaluator.evaluate(app, usedMillis = 30 * 60_000L, bypassActive = false)

        assertEquals(LimitStatus.Reached, status)
    }

    @Test
    fun `emergency bypass takes precedence over reached limit`() {
        val status = evaluator.evaluate(app, usedMillis = 90 * 60_000L, bypassActive = true)

        assertEquals(LimitStatus.Bypassed, status)
    }

    @Test
    fun `disabled timer never blocks`() {
        val status = evaluator.evaluate(
            app.copy(timerEnabled = false),
            usedMillis = 90 * 60_000L,
            bypassActive = false,
        )

        assertEquals(LimitStatus.Bypassed, status)
    }
}
