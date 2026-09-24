package it.socialblock

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import it.socialblock.ui.MainViewModel
import it.socialblock.ui.SocialBlockApp
import it.socialblock.ui.theme.SocialBlockTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SocialBlockTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val permissionActions = rememberPermissionActions(viewModel)
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    viewModel.setUiActive(true)
                }
                LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
                    viewModel.setUiActive(false)
                }
                SocialBlockApp(
                    state = state,
                    viewModel = viewModel,
                    permissionActions = permissionActions,
                )
            }
        }
    }
}

data class PermissionActions(val requestUsageAccess: () -> Unit, val requestOverlay: () -> Unit, val requestNotifications: () -> Unit)

@Composable
private fun rememberPermissionActions(viewModel: MainViewModel): PermissionActions {
    val context = LocalContext.current
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.setUiActive(true)
        }
    return PermissionActions(
        requestUsageAccess = {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        },
        requestOverlay = {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri(),
                ),
            )
        },
        requestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
        },
    )
}
