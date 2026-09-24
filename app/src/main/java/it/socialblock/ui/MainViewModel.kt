package it.socialblock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.socialblock.data.repository.TrackingRepository
import it.socialblock.data.settings.SettingsRepository
import it.socialblock.domain.model.AppSettings
import it.socialblock.domain.model.OverlayPosition
import it.socialblock.domain.model.TrackedApp
import it.socialblock.domain.model.TrackedAppUsage
import it.socialblock.domain.model.UsageSnapshot
import it.socialblock.domain.monitoring.MonitoringStartupPolicy
import it.socialblock.monitoring.MonitoringServiceController
import it.socialblock.notifications.AppNotifier
import it.socialblock.platform.apps.InstalledApp
import it.socialblock.platform.apps.InstalledAppsProvider
import it.socialblock.platform.permissions.PermissionChecker
import it.socialblock.platform.permissions.PermissionState
import it.socialblock.platform.time.TimeProvider
import it.socialblock.usage.AndroidUsageStatsSource
import it.socialblock.work.DailySummaryScheduler
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface OverlaySettingsUpdate {
    data class ChangePosition(val position: OverlayPosition) : OverlaySettingsUpdate

    data class ChangeOpacity(val percent: Int) : OverlaySettingsUpdate

    data class AddMessage(val message: String) : OverlaySettingsUpdate

    data class EditMessage(val index: Int, val message: String) : OverlaySettingsUpdate

    data class RemoveMessage(val index: Int) : OverlaySettingsUpdate

    data class ChangeDelay(val seconds: Int) : OverlaySettingsUpdate
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository,
    private val settingsRepository: SettingsRepository,
    private val usageStatsSource: AndroidUsageStatsSource,
    private val installedAppsProvider: InstalledAppsProvider,
    private val permissionChecker: PermissionChecker,
    private val monitoringServiceController: MonitoringServiceController,
    private val monitoringStartupPolicy: MonitoringStartupPolicy,
    private val dailySummaryScheduler: DailySummaryScheduler,
    private val notifier: AppNotifier,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val permissions = MutableStateFlow(permissionChecker.current())
    private val usageSnapshot = MutableStateFlow(UsageSnapshot(0L, emptyMap(), null))
    private val pickerState = MutableStateFlow(PickerState())
    private val nowMillis = MutableStateFlow(timeProvider.nowMillis())
    private val userMessage = MutableStateFlow<String?>(null)
    private var uiRefreshJob: Job? = null

    private val trackedApps =
        trackingRepository.observeTrackedApps()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STATE_STOP_TIMEOUT_MILLIS),
                emptyList(),
            )
    private val settings =
        settingsRepository.settings
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STATE_STOP_TIMEOUT_MILLIS),
                AppSettings(),
            )

    val uiState: StateFlow<MainUiState> =
        combine(trackedApps, settings, usageSnapshot, permissions) {
                apps,
                appSettings,
                snapshot,
                grants,
            ->
            BaseState(
                permissions = grants,
                settings = appSettings,
                trackedApps = apps.map {
                    TrackedAppUsage(
                        it,
                        snapshot.usageByPackage[it.packageName] ?: 0L,
                    )
                },
            )
        }.combine(pickerState) { base, picker -> base to picker }
            .combine(nowMillis) { (base, picker), now -> Triple(base, picker, now) }
            .combine(userMessage) { (base, picker, now), message ->
                MainUiState(
                    isLoading = false,
                    permissions = base.permissions,
                    settings = base.settings,
                    trackedApps = base.trackedApps,
                    installedApps = picker.apps,
                    appPickerVisible = picker.visible,
                    appCatalogLoading = picker.loading,
                    nowMillis = now,
                    userMessage = message,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STATE_STOP_TIMEOUT_MILLIS),
                MainUiState(),
            )

    init {
        viewModelScope.launch {
            if (settingsRepository.settings.first().monitoringEnabled) {
                startMonitoringOrDisable()
            }
        }
    }

    fun setUiActive(active: Boolean) {
        if (!active) {
            uiRefreshJob?.cancel()
            uiRefreshJob = null
            return
        }
        permissions.value = permissionChecker.current()
        if (uiRefreshJob?.isActive == true) {
            viewModelScope.launch { refreshUsageInternal() }
            return
        }
        uiRefreshJob =
            viewModelScope.launch {
                var lastUsageRefreshAt = 0L
                while (isActive) {
                    val now = timeProvider.nowMillis()
                    nowMillis.value = now
                    if (
                        lastUsageRefreshAt == 0L ||
                        now < lastUsageRefreshAt ||
                        now - lastUsageRefreshAt >= USAGE_REFRESH_MILLIS
                    ) {
                        refreshUsageInternal()
                        lastUsageRefreshAt = now
                    }
                    delay(CLOCK_TICK_MILLIS)
                }
            }
    }

    fun showAppPicker() {
        pickerState.value = pickerState.value.copy(visible = true)
        if (pickerState.value.apps.isEmpty()) {
            viewModelScope.launch {
                pickerState.value = pickerState.value.copy(loading = true)
                val installed = withContext(Dispatchers.IO) { installedAppsProvider.launcherApps() }
                val trackedPackages = trackedApps.value.map(TrackedApp::packageName).toSet()
                pickerState.value =
                    PickerState(
                        visible = true,
                        loading = false,
                        apps = installed.filterNot { it.packageName in trackedPackages },
                    )
            }
        }
    }

    fun hideAppPicker() {
        pickerState.value = pickerState.value.copy(visible = false)
    }

    fun addApp(app: InstalledApp) {
        viewModelScope.launch(Dispatchers.IO) {
            val hadTrackedApps = trackingRepository.getTrackedApps().isNotEmpty()
            trackingRepository.addApp(
                packageName = app.packageName,
                displayName = app.displayName,
                nowMillis = timeProvider.nowMillis(),
            )
            pickerState.value =
                pickerState.value.copy(
                    visible = false,
                    apps = pickerState.value.apps.filterNot { it.packageName == app.packageName },
                )
            refreshUsageInternal()
            if (
                monitoringStartupPolicy.shouldEnableAfterAddingApp(
                    hadTrackedApps = hadTrackedApps,
                    usageAccessGranted = permissionChecker.current().usageAccess,
                )
            ) {
                updateMonitoringEnabled(true)
            }
        }
    }

    fun updateApp(app: TrackedApp) {
        viewModelScope.launch(Dispatchers.IO) { trackingRepository.updateApp(app) }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            trackingRepository.removeApp(packageName)
            if (trackingRepository.getTrackedApps().isEmpty()) {
                updateMonitoringEnabled(false)
            }
            refreshUsageInternal()
        }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch { updateMonitoringEnabled(enabled) }
    }

    fun enableEmergencyBypass() {
        viewModelScope.launch {
            val until = settingsRepository.enableEmergencyBypass(timeProvider.nowMillis())
            notifier.emergencyBypassEnabled(until)
        }
    }

    fun clearEmergencyBypass() {
        viewModelScope.launch { settingsRepository.clearEmergencyBypass() }
    }

    fun updateDailySummaryTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setDailySummaryTime(hour, minute)
            dailySummaryScheduler.schedule(hour, minute)
            userMessage.value = "Orario del riepilogo aggiornato"
        }
    }

    fun updateOverlaySettings(update: OverlaySettingsUpdate) {
        viewModelScope.launch {
            when (update) {
                is OverlaySettingsUpdate.ChangePosition -> settingsRepository.setOverlayPosition(update.position)
                is OverlaySettingsUpdate.ChangeOpacity -> settingsRepository.setOverlayOpacityPercent(update.percent)
                is OverlaySettingsUpdate.AddMessage -> settingsRepository.addOverlayMessage(update.message)
                is OverlaySettingsUpdate.EditMessage -> settingsRepository.updateOverlayMessage(update.index, update.message)
                is OverlaySettingsUpdate.RemoveMessage -> settingsRepository.removeOverlayMessage(update.index)
                is OverlaySettingsUpdate.ChangeDelay -> settingsRepository.setOverlayMessageChangeDelaySeconds(update.seconds)
            }
        }
    }

    fun clearUserMessage() {
        userMessage.value = null
    }

    private suspend fun refreshUsageInternal() {
        val apps = trackedApps.first()
        if (apps.isEmpty() || !permissions.value.usageAccess) {
            usageSnapshot.value = UsageSnapshot(timeProvider.nowMillis(), emptyMap(), null)
            return
        }
        usageSnapshot.value =
            withContext(Dispatchers.Default) {
                usageStatsSource.snapshot(
                    apps.map(TrackedApp::packageName).toSet(),
                    timeProvider.nowMillis(),
                )
            }
    }

    private suspend fun updateMonitoringEnabled(enabled: Boolean) {
        settingsRepository.setMonitoringEnabled(enabled)
        if (enabled) {
            startMonitoringOrDisable()
        } else {
            monitoringServiceController.stop()
        }
    }

    private suspend fun startMonitoringOrDisable() {
        if (!monitoringServiceController.start()) {
            settingsRepository.setMonitoringEnabled(false)
            userMessage.value = "Impossibile avviare il monitoraggio in background"
        }
    }

    private data class BaseState(val permissions: PermissionState, val settings: AppSettings, val trackedApps: List<TrackedAppUsage>)

    private data class PickerState(val visible: Boolean = false, val loading: Boolean = false, val apps: List<InstalledApp> = emptyList())

    companion object {
        private const val STATE_STOP_TIMEOUT_MILLIS = 5_000L
        private const val CLOCK_TICK_MILLIS = 1_000L
        private const val USAGE_REFRESH_MILLIS = 10_000L
    }
}
