package it.socialblock.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import it.socialblock.PermissionActions
import it.socialblock.domain.model.AppSettings
import it.socialblock.domain.model.TrackedApp
import it.socialblock.domain.model.TrackedAppUsage
import it.socialblock.platform.permissions.PermissionState
import it.socialblock.ui.screens.DashboardScreen
import it.socialblock.ui.theme.SocialBlockTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun dashboardShowsUsageAndEmergencyAction() {
        var emergencyRequested = false
        composeRule.setContent {
            SocialBlockTheme {
                DashboardScreen(
                    state = populatedState(),
                    onOpenSettings = {},
                    onOpenApp = {},
                    onShowAppPicker = {},
                    onHideAppPicker = {},
                    onAddApp = {},
                    onMonitoringChanged = {},
                    onEnableEmergencyBypass = { emergencyRequested = true },
                    onClearEmergencyBypass = {},
                    permissionActions = PermissionActions({}, {}, {}),
                )
            }
        }

        composeRule.onNodeWithText("SocialBlock").assertIsDisplayed()
        composeRule.onNodeWithTag("total_usage").assertIsDisplayed()
        composeRule.onNodeWithTag("emergency_card").assertIsDisplayed()
        composeRule.onNodeWithTag("app_icon_com.example.social", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Attiva").performClick()
        assertTrue(emergencyRequested)
    }

    private fun populatedState(): MainUiState {
        val app =
            TrackedApp(
                packageName = "com.example.social",
                displayName = "Social",
                dailyLimitMinutes = 30,
                timerEnabled = true,
                overlayEnabled = true,
                lastApproachingNoticeDate = null,
                lastLimitNoticeDate = null,
            )
        return MainUiState(
            isLoading = false,
            permissions = PermissionState(true, true, true),
            settings = AppSettings(monitoringEnabled = true),
            trackedApps = listOf(TrackedAppUsage(app, 12 * 60_000L)),
            nowMillis = 1_000L,
        )
    }
}
