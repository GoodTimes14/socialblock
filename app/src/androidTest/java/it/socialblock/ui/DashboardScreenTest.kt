package it.socialblock.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
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
import org.junit.Assert.assertEquals
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
                    onShortFormOnlyBlockChanged = {},
                    onShortFormAlwaysBlockChanged = {},
                    onEnableEmergencyBypass = { emergencyRequested = true },
                    onClearEmergencyBypass = {},
                    permissionActions = PermissionActions({}, {}, {}, {}),
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

    @Test
    fun shortFormSwitchAsksForTheBlockerOnlyWhenItIsMissing() {
        var requestedEnabled: Boolean? = null
        var blockerRequested = false
        var state by mutableStateOf(populatedState())
        composeRule.setContent {
            SocialBlockTheme {
                DashboardScreen(
                    state = state,
                    onOpenSettings = {},
                    onOpenApp = {},
                    onShowAppPicker = {},
                    onHideAppPicker = {},
                    onAddApp = {},
                    onMonitoringChanged = {},
                    onShortFormOnlyBlockChanged = { requestedEnabled = it },
                    onShortFormAlwaysBlockChanged = {},
                    onEnableEmergencyBypass = {},
                    onClearEmergencyBypass = {},
                    permissionActions = PermissionActions({}, {}, {}, { blockerRequested = true }),
                )
            }
        }

        composeRule.onNodeWithTag("short_form_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Configura").assertDoesNotExist()
        composeRule.onNodeWithTag("short_form_switch").assertIsOff().performClick()
        assertEquals(true, requestedEnabled)

        state = state.copy(settings = state.settings.copy(shortFormOnlyBlockEnabled = true))
        composeRule.onNodeWithTag("short_form_switch").assertIsOn()
        composeRule.onNodeWithText("Configura").performScrollTo().performClick()
        assertTrue(blockerRequested)

        state = state.copy(permissions = state.permissions.copy(shortFormBlocker = true))
        composeRule.onNodeWithText("Configura").assertDoesNotExist()
    }

    @Test
    fun alwaysOnSwitchOverridesTheLimitOnlySwitchAndAsksForTheBlocker() {
        var requestedAlways: Boolean? = null
        var blockerRequested = false
        var state by mutableStateOf(populatedState().let { it.copy(settings = it.settings.copy(shortFormOnlyBlockEnabled = true)) })
        composeRule.setContent {
            SocialBlockTheme {
                DashboardScreen(
                    state = state,
                    onOpenSettings = {},
                    onOpenApp = {},
                    onShowAppPicker = {},
                    onHideAppPicker = {},
                    onAddApp = {},
                    onMonitoringChanged = {},
                    onShortFormOnlyBlockChanged = {},
                    onShortFormAlwaysBlockChanged = { requestedAlways = it },
                    onEnableEmergencyBypass = {},
                    onClearEmergencyBypass = {},
                    permissionActions = PermissionActions({}, {}, {}, { blockerRequested = true }),
                )
            }
        }

        composeRule.onNodeWithTag("short_form_switch").performScrollTo().assertIsOn().assertIsEnabled()
        composeRule.onNodeWithTag("short_form_always_switch").performScrollTo().assertIsOff().performClick()
        assertEquals(true, requestedAlways)

        state = state.copy(settings = state.settings.copy(shortFormAlwaysBlockEnabled = true))
        composeRule.onNodeWithTag("short_form_always_switch").assertIsOn()
        composeRule.onNodeWithTag("short_form_switch").assertIsOff().assertIsNotEnabled()
        composeRule.onNodeWithText("Configura").performScrollTo().performClick()
        assertTrue(blockerRequested)
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
