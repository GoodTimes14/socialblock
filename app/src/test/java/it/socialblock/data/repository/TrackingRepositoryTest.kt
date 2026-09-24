package it.socialblock.data.repository

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import it.socialblock.data.local.DailyUsageDao
import it.socialblock.data.local.TrackedAppDao
import it.socialblock.data.local.TrackedAppEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingRepositoryTest {
    private val trackedAppDao = mockk<TrackedAppDao>()
    private val dailyUsageDao = mockk<DailyUsageDao>(relaxed = true)
    private val repository = TrackingRepository(trackedAppDao, dailyUsageDao)

    @Test
    fun `maps Room entities into domain stream`() = runTest {
        every { trackedAppDao.observeAll() } returns
            flowOf(
                listOf(
                    TrackedAppEntity(
                        packageName = "com.example.social",
                        displayName = "Social",
                        dailyLimitMinutes = 25,
                        timerEnabled = true,
                        overlayEnabled = false,
                        createdAtMillis = 1L,
                    ),
                ),
            )

        repository.observeTrackedApps().test {
            val app = awaitItem().single()
            assertEquals("Social", app.displayName)
            assertEquals(25, app.dailyLimitMinutes)
            assertTrue(app.timerEnabled)
            assertEquals(false, app.overlayEnabled)
            awaitComplete()
        }
    }

    @Test
    fun `new limit is clamped before persistence`() = runTest {
        val entity = slot<TrackedAppEntity>()
        coEvery { trackedAppDao.upsert(capture(entity)) } returns Unit

        repository.addApp(
            "com.example.social",
            "Social",
            dailyLimitMinutes = 1,
            nowMillis = 99L,
        )

        coVerify(exactly = 1) { trackedAppDao.upsert(any()) }
        assertEquals(TrackingRepository.MIN_LIMIT_MINUTES, entity.captured.dailyLimitMinutes)
    }
}
