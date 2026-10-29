package it.socialblock.domain.shortform

import it.socialblock.domain.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortFormBlockPolicyTest {
    private val policy = ShortFormBlockPolicy()

    @Test
    fun `always on block covers every supported app even without a limit`() {
        val blocked = policy.blockedPackages(AppSettings(shortFormAlwaysBlockEnabled = true), NOW_MILLIS, emptySet())

        assertEquals(
            mapOf(
                ShortFormPanels.INSTAGRAM_PACKAGE to ShortFormBlockReason.ALWAYS,
                ShortFormPanels.YOUTUBE_PACKAGE to ShortFormBlockReason.ALWAYS,
                ShortFormPanels.X_PACKAGE to ShortFormBlockReason.ALWAYS,
            ),
            blocked,
        )
    }

    @Test
    fun `without the always on block only apps at their limit are blocked`() {
        val blocked =
            policy.blockedPackages(
                AppSettings(shortFormOnlyBlockEnabled = true),
                NOW_MILLIS,
                setOf(ShortFormPanels.YOUTUBE_PACKAGE),
            )

        assertEquals(mapOf(ShortFormPanels.YOUTUBE_PACKAGE to ShortFormBlockReason.LIMIT_REACHED), blocked)
    }

    @Test
    fun `nothing is blocked when both switches are off`() {
        assertEquals(emptyMap<String, ShortFormBlockReason>(), policy.blockedPackages(AppSettings(), NOW_MILLIS, emptySet()))
    }

    @Test
    fun `emergency pause suspends the always on block`() {
        val settings = AppSettings(shortFormAlwaysBlockEnabled = true, emergencyBypassUntilMillis = NOW_MILLIS + 1)

        assertEquals(emptyMap<String, ShortFormBlockReason>(), policy.blockedPackages(settings, NOW_MILLIS, emptySet()))
    }

    private companion object {
        const val NOW_MILLIS = 1_000_000L
    }
}
