package it.socialblock.domain.monitoring

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringStartupPolicyTest {
    private val policy = MonitoringStartupPolicy()

    @Test
    fun `enables monitoring when the first app is added with usage access`() {
        assertTrue(
            policy.shouldEnableAfterAddingApp(
                hadTrackedApps = false,
                usageAccessGranted = true,
            ),
        )
    }

    @Test
    fun `does not override a deliberate monitoring choice for later apps`() {
        assertFalse(
            policy.shouldEnableAfterAddingApp(
                hadTrackedApps = true,
                usageAccessGranted = true,
            ),
        )
    }

    @Test
    fun `waits for usage access before automatically enabling monitoring`() {
        assertFalse(
            policy.shouldEnableAfterAddingApp(
                hadTrackedApps = false,
                usageAccessGranted = false,
            ),
        )
    }
}
