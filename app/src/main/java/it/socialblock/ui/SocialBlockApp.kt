package it.socialblock.ui

import android.net.Uri
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.socialblock.PermissionActions
import it.socialblock.ui.screens.AppDetailScreen
import it.socialblock.ui.screens.DashboardScreen
import it.socialblock.ui.screens.SettingsScreen

@Composable
fun SocialBlockApp(state: MainUiState, viewModel: MainViewModel, permissionActions: PermissionActions) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    state.userMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                state = state,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenApp = { packageName ->
                    navController.navigate("${Routes.APP_DETAIL}/${Uri.encode(packageName)}")
                },
                onShowAppPicker = viewModel::showAppPicker,
                onHideAppPicker = viewModel::hideAppPicker,
                onAddApp = viewModel::addApp,
                onMonitoringChanged = viewModel::setMonitoringEnabled,
                onEnableEmergencyBypass = viewModel::enableEmergencyBypass,
                onClearEmergencyBypass = viewModel::clearEmergencyBypass,
                permissionActions = permissionActions,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                state = state,
                onBack = navController::popBackStack,
                onUpdateSummaryTime = viewModel::updateDailySummaryTime,
                onUpdateOverlayPosition = { position ->
                    viewModel.updateOverlaySettings(OverlaySettingsUpdate.ChangePosition(position))
                },
                onUpdateOverlayOpacity = { percent ->
                    viewModel.updateOverlaySettings(OverlaySettingsUpdate.ChangeOpacity(percent))
                },
                onAddOverlayMessage = { message ->
                    viewModel.updateOverlaySettings(OverlaySettingsUpdate.AddMessage(message))
                },
                onUpdateOverlayMessage = { index, message ->
                    viewModel.updateOverlaySettings(OverlaySettingsUpdate.EditMessage(index, message))
                },
                onRemoveOverlayMessage = { index ->
                    viewModel.updateOverlaySettings(OverlaySettingsUpdate.RemoveMessage(index))
                },
                onUpdateOverlayMessageChangeDelay = { seconds ->
                    viewModel.updateOverlaySettings(OverlaySettingsUpdate.ChangeDelay(seconds))
                },
                permissionActions = permissionActions,
            )
        }
        composable(
            route = "${Routes.APP_DETAIL}/{packageName}",
            arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
        ) { entry ->
            val packageName = Uri.decode(entry.arguments?.getString("packageName").orEmpty())
            val appUsage = state.trackedApps.firstOrNull { it.app.packageName == packageName }
            AppDetailScreen(
                appUsage = appUsage,
                bypassActive = state.bypassActive,
                bypassUntilMillis = state.settings.emergencyBypassUntilMillis,
                onBack = navController::popBackStack,
                onUpdate = viewModel::updateApp,
                onRemove = {
                    viewModel.removeApp(packageName)
                    navController.popBackStack()
                },
                onEnableEmergencyBypass = viewModel::enableEmergencyBypass,
                onClearEmergencyBypass = viewModel::clearEmergencyBypass,
            )
        }
    }
}

private object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val APP_DETAIL = "app"
}
