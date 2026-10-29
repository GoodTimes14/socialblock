package it.socialblock.monitoring

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import it.socialblock.data.settings.SettingsRepository
import it.socialblock.domain.model.AppSettings
import it.socialblock.domain.shortform.ShortFormBlockPolicy
import it.socialblock.domain.shortform.ShortFormBlockReason
import it.socialblock.domain.shortform.ShortFormPanels
import it.socialblock.platform.time.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShortFormBlockStateTest {
    private val settings = MutableStateFlow(AppSettings())
    private val settingsRepository = mockk<SettingsRepository> { every { settings } returns this@ShortFormBlockStateTest.settings }
    private var sleptMillis = 0L

    @Test
    fun `always on block applies without any limit reached`() = runTest {
        val state = createState()

        state.blockedPackages.test {
            assertEquals(emptyMap<String, ShortFormBlockReason>(), awaitItem())

            settings.value = AppSettings(shortFormAlwaysBlockEnabled = true)

            assertEquals(ShortFormPanels.packageNames, awaitItem().keys)
        }
    }

    @Test
    fun `apps at their limit are blocked while the always on block is off`() = runTest {
        val state = createState()

        state.blockedPackages.test {
            assertEquals(emptyMap<String, ShortFormBlockReason>(), awaitItem())

            state.updateLimitBlockedPackages(setOf(ShortFormPanels.INSTAGRAM_PACKAGE))

            assertEquals(mapOf(ShortFormPanels.INSTAGRAM_PACKAGE to ShortFormBlockReason.LIMIT_REACHED), awaitItem())
        }
    }

    @Test
    fun `always on block resumes by itself when the emergency pause ends`() = runTest {
        settings.value = AppSettings(shortFormAlwaysBlockEnabled = true, emergencyBypassUntilMillis = PAUSE_MILLIS)
        val state = createState()

        state.blockedPackages.test {
            assertEquals(emptyMap<String, ShortFormBlockReason>(), awaitItem())

            advanceTimeBy(PAUSE_MILLIS - 1)
            expectNoEvents()

            assertEquals(ShortFormPanels.packageNames, awaitItem().keys)
            assertEquals(PAUSE_MILLIS, currentTime)
        }
    }

    @Test
    fun `always on block resumes soon after the device sleeps through the emergency pause`() = runTest {
        settings.value = AppSettings(shortFormAlwaysBlockEnabled = true, emergencyBypassUntilMillis = PAUSE_MILLIS)
        val state = createState()

        state.blockedPackages.test {
            assertEquals(emptyMap<String, ShortFormBlockReason>(), awaitItem())

            // In deep sleep the wall clock moves on while delay() stands still.
            sleptMillis = PAUSE_MILLIS

            assertEquals(ShortFormPanels.packageNames, awaitItem().keys)
            assertTrue(currentTime <= WAKE_TOLERANCE_MILLIS)
        }
    }

    private fun TestScope.createState() = ShortFormBlockState(
        settingsRepository = settingsRepository,
        timeProvider = object : TimeProvider {
            override fun nowMillis(): Long = testScheduler.currentTime + sleptMillis
        },
        policy = ShortFormBlockPolicy(),
    )

    private companion object {
        const val PAUSE_MILLIS = 15 * 60_000L
        const val WAKE_TOLERANCE_MILLIS = 10_000L
    }
}
