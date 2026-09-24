package it.socialblock.domain.monitoring

import javax.inject.Inject

class MonitoringStartupPolicy @Inject constructor() {
    fun shouldEnableAfterAddingApp(hadTrackedApps: Boolean, usageAccessGranted: Boolean): Boolean = !hadTrackedApps && usageAccessGranted
}
