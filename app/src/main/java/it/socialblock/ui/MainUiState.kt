package it.socialblock.ui

import it.socialblock.domain.model.AppSettings
import it.socialblock.domain.model.TrackedAppUsage
import it.socialblock.platform.apps.InstalledApp
import it.socialblock.platform.permissions.PermissionState

data class MainUiState(
    val isLoading: Boolean = true,
    val permissions: PermissionState = PermissionState(),
    val settings: AppSettings = AppSettings(),
    val trackedApps: List<TrackedAppUsage> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val appPickerVisible: Boolean = false,
    val appCatalogLoading: Boolean = false,
    val nowMillis: Long = System.currentTimeMillis(),
    val userMessage: String? = null,
) {
    val totalUsageMillis: Long = trackedApps.sumOf(TrackedAppUsage::usedMillis)
    val bypassActive: Boolean = settings.isEmergencyBypassActive(nowMillis)
}
