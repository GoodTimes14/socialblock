package it.socialblock.ui

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import it.socialblock.data.repository.TrackingRepository
import it.socialblock.data.settings.SettingsRepository
import it.socialblock.domain.model.AppSettings
import it.socialblock.domain.model.TrackedApp
import it.socialblock.domain.model.UsageSnapshot
import it.socialblock.domain.monitoring.MonitoringStartupPolicy
import it.socialblock.monitoring.MonitoringServiceController
import it.socialblock.notifications.AppNotifier
import it.socialblock.platform.apps.InstalledAppsProvider
import it.socialblock.platform.permissions.PermissionChecker
import it.socialblock.platform.permissions.PermissionState
import it.socialblock.platform.time.TimeProvider
import it.socialblock.testing.MainDispatcherRule
import it.socialblock.usage.AndroidUsageStatsSource
import it.socialblock.work.DailySummaryScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val trackingRepository = mockk<TrackingRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val usageStatsSource = mockk<AndroidUsageStatsSource>()
    private val installedAppsProvider = mockk<InstalledAppsProvider>()
    private val permissionChecker = mockk<PermissionChecker>()
    private val monitoringServiceController = mockk<MonitoringServiceController>()
    private val dailySummaryScheduler = mockk<DailySummaryScheduler>()
    private val notifier = mockk<AppNotifier>()
    private val timeProvider = mockk<TimeProvider>()

    @Test
    fun `usage refresh stops while the UI is paused`() = runTest(mainDispatcherRule.dispatcher) {
        val trackedApp =
            TrackedApp(
                packageName = "com.example.social",
                displayName = "Social",
                dailyLimitMinutes = 30,
                timerEnabled = true,
                overlayEnabled = true,
                lastApproachingNoticeDate = null,
                lastLimitNoticeDate = null,
            )
        every { trackingRepository.observeTrackedApps() } returns
            flowOf(listOf(trackedApp))
        every { settingsRepository.settings } returns
            flowOf(AppSettings(monitoringEnabled = false))
        every { permissionChecker.current() } returns
            PermissionState(usageAccess = true, overlay = true, notifications = true)
        every { timeProvider.nowMillis() } answers {
            mainDispatcherRule.dispatcher.scheduler.currentTime + 1_000L
        }
        every { usageStatsSource.snapshot(any(), any()) } returns
            UsageSnapshot(1_000L, mapOf(trackedApp.packageName to 0L), null)

        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()

        viewModel.setUiActive(true)
        runCurrent()
        verify(timeout = 1_000L, exactly = 1) {
            usageStatsSource.snapshot(setOf(trackedApp.packageName), any())
        }
        runCurrent()
        viewModel.setUiActive(false)
        clearMocks(usageStatsSource, answers = false, recordedCalls = true)

        advanceTimeBy(30_000L)
        runCurrent()

        verify(exactly = 0) { usageStatsSource.snapshot(any(), any()) }
    }

    private fun createViewModel() = MainViewModel(
        trackingRepository = trackingRepository,
        settingsRepository = settingsRepository,
        usageStatsSource = usageStatsSource,
        installedAppsProvider = installedAppsProvider,
        permissionChecker = permissionChecker,
        monitoringServiceController = monitoringServiceController,
        monitoringStartupPolicy = MonitoringStartupPolicy(),
        dailySummaryScheduler = dailySummaryScheduler,
        notifier = notifier,
        timeProvider = timeProvider,
    )
}
