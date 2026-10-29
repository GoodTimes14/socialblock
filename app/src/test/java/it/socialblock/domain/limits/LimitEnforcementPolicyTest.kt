package it.socialblock.domain.limits

import it.socialblock.domain.shortform.ShortFormPanels
import org.junit.Assert.assertEquals
import org.junit.Test

class LimitEnforcementPolicyTest {
    private val policy = LimitEnforcementPolicy()

    @Test
    fun `instagram and youtube block only short form when the switch and blocker are on`() {
        listOf(
            ShortFormPanels.INSTAGRAM_PACKAGE to "Reels",
            ShortFormPanels.YOUTUBE_PACKAGE to "Shorts",
        ).forEach { (packageName, featureName) ->
            val enforcement =
                policy.enforcementFor(
                    packageName = packageName,
                    shortFormOnlyBlockEnabled = true,
                    shortFormAlwaysBlockEnabled = false,
                    shortFormBlockerConnected = true,
                )

            assertEquals(featureName, (enforcement as LimitEnforcement.BlockShortForm).feature.name)
        }
    }

    @Test
    fun `whole app is closed when the switch is off`() {
        val enforcement =
            policy.enforcementFor(
                packageName = ShortFormPanels.INSTAGRAM_PACKAGE,
                shortFormOnlyBlockEnabled = false,
                shortFormAlwaysBlockEnabled = false,
                shortFormBlockerConnected = true,
            )

        assertEquals(LimitEnforcement.ReturnHome, enforcement)
    }

    @Test
    fun `whole app is closed while the blocker is not connected`() {
        val enforcement =
            policy.enforcementFor(
                packageName = ShortFormPanels.YOUTUBE_PACKAGE,
                shortFormOnlyBlockEnabled = true,
                shortFormAlwaysBlockEnabled = false,
                shortFormBlockerConnected = false,
            )

        assertEquals(LimitEnforcement.ReturnHome, enforcement)
    }

    @Test
    fun `switch does not affect other apps`() {
        val enforcement =
            policy.enforcementFor(
                packageName = "com.zhiliaoapp.musically",
                shortFormOnlyBlockEnabled = true,
                shortFormAlwaysBlockEnabled = false,
                shortFormBlockerConnected = true,
            )

        assertEquals(LimitEnforcement.ReturnHome, enforcement)
    }

    @Test
    fun `whole app is closed when reels and shorts are always blocked`() {
        listOf(ShortFormPanels.INSTAGRAM_PACKAGE, ShortFormPanels.YOUTUBE_PACKAGE).forEach { packageName ->
            val enforcement =
                policy.enforcementFor(
                    packageName = packageName,
                    shortFormOnlyBlockEnabled = true,
                    shortFormAlwaysBlockEnabled = true,
                    shortFormBlockerConnected = true,
                )

            assertEquals(LimitEnforcement.ReturnHome, enforcement)
        }
    }

    @Test
    fun `x is closed as a whole at its limit`() {
        val enforcement =
            policy.enforcementFor(
                packageName = ShortFormPanels.X_PACKAGE,
                shortFormOnlyBlockEnabled = true,
                shortFormAlwaysBlockEnabled = false,
                shortFormBlockerConnected = true,
            )

        assertEquals(LimitEnforcement.ReturnHome, enforcement)
    }
}
