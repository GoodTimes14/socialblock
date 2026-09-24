package it.socialblock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.socialblock.PermissionActions
import it.socialblock.domain.model.TrackedAppUsage
import it.socialblock.platform.apps.InstalledApp
import it.socialblock.platform.format.formatDuration
import it.socialblock.ui.MainUiState
import it.socialblock.ui.components.InstalledAppIcon
import it.socialblock.ui.theme.Forest
import it.socialblock.ui.theme.WarmGold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: MainUiState,
    snackbarHost: @Composable () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenApp: (String) -> Unit,
    onShowAppPicker: () -> Unit,
    onHideAppPicker: () -> Unit,
    onAddApp: (InstalledApp) -> Unit,
    onMonitoringChanged: (Boolean) -> Unit,
    onEnableEmergencyBypass: () -> Unit,
    onClearEmergencyBypass: () -> Unit,
    permissionActions: PermissionActions,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("SocialBlock", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Impostazioni")
                    }
                },
            )
        },
        snackbarHost = snackbarHost,
    ) { contentPadding ->
        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(contentPadding).testTag("dashboard"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    OverviewCard(
                        totalUsageMillis = state.totalUsageMillis,
                        trackedCount = state.trackedApps.size,
                        monitoringEnabled = state.settings.monitoringEnabled,
                        canMonitor = state.permissions.usageAccess && state.trackedApps.isNotEmpty(),
                        onMonitoringChanged = onMonitoringChanged,
                    )
                }
                item {
                    EmergencyCard(
                        bypassActive = state.bypassActive,
                        bypassUntilMillis = state.settings.emergencyBypassUntilMillis,
                        onEnable = onEnableEmergencyBypass,
                        onClear = onClearEmergencyBypass,
                    )
                }
                if (!state.permissions.allRequiredGranted) {
                    item {
                        PermissionCard(state, permissionActions)
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("App monitorate", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Limiti giornalieri e messaggi dissuasivi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(onClick = onShowAppPicker) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("Aggiungi")
                        }
                    }
                }
                if (state.trackedApps.isEmpty()) {
                    item { EmptyAppsCard(onShowAppPicker) }
                } else {
                    items(state.trackedApps, key = { it.app.packageName }) { appUsage ->
                        AppUsageCard(appUsage, state.bypassActive) {
                            onOpenApp(appUsage.app.packageName)
                        }
                    }
                }
                item {
                    Text(
                        "I dati di utilizzo restano su questo dispositivo. SocialBlock non legge lo schermo né la navigazione.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }

    if (state.appPickerVisible) {
        AppPickerDialog(
            apps = state.installedApps,
            loading = state.appCatalogLoading,
            onDismiss = onHideAppPicker,
            onSelect = onAddApp,
        )
    }
}

@Composable
private fun OverviewCard(
    totalUsageMillis: Long,
    trackedCount: Int,
    monitoringEnabled: Boolean,
    canMonitor: Boolean,
    onMonitoringChanged: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Forest, contentColor = Color.White),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Utilizzo di oggi", color = Color.White.copy(alpha = 0.72f))
                    Text(
                        formatDuration(totalUsageMillis),
                        style = MaterialTheme.typography.displaySmall,
                        color = WarmGold,
                        modifier = Modifier.testTag("total_usage"),
                    )
                    Text(
                        "$trackedCount app ${if (trackedCount == 1) "monitorata" else "monitorate"}",
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }
                Box(
                    modifier = Modifier.size(
                        58.dp,
                    ).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = WarmGold,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Monitoraggio continuo", fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            trackedCount == 0 -> "Aggiungi almeno un'app"
                            !canMonitor -> "Concedi l'accesso all'utilizzo"
                            monitoringEnabled -> "Limiti e avvisi sono attivi"
                            else -> "Attivalo per applicare i limiti"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }
                Switch(
                    checked = monitoringEnabled,
                    enabled = canMonitor,
                    onCheckedChange = onMonitoringChanged,
                    modifier = Modifier.testTag("monitoring_switch"),
                )
            }
        }
    }
}

@Composable
private fun EmergencyCard(bypassActive: Boolean, bypassUntilMillis: Long, onEnable: () -> Unit, onClear: () -> Unit) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (bypassActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
            ),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth().testTag("emergency_card"),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(30.dp),
            )
            Column(Modifier.weight(1f)) {
                Text("Pausa di emergenza", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (bypassActive) {
                        "Timer sospesi fino alle ${formatClockTime(bypassUntilMillis)}"
                    } else {
                        "Sospende tutti i timer per 15 minuti"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (bypassActive) {
                OutlinedButton(onClick = onClear) { Text("Termina") }
            } else {
                OutlinedButton(onClick = onEnable) { Text("Attiva") }
            }
        }
    }
}

@Composable
private fun PermissionCard(state: MainUiState, actions: PermissionActions) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text("Completa la configurazione", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ogni permesso viene usato solo per la funzione indicata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PermissionRow(
                icon = Icons.Outlined.Apps,
                title = "Accesso all'utilizzo",
                detail = "Misura solo il tempo delle app selezionate",
                granted = state.permissions.usageAccess,
                onClick = actions.requestUsageAccess,
            )
            PermissionRow(
                icon = Icons.Outlined.Visibility,
                title = "Mostra sopra le app",
                detail = "Visualizza messaggi trasparenti e non interattivi",
                granted = state.permissions.overlay,
                onClick = actions.requestOverlay,
            )
            PermissionRow(
                icon = Icons.Outlined.Notifications,
                title = "Notifiche",
                detail = "Avvisa 5 minuti prima e mostra il riepilogo",
                granted = state.permissions.notifications,
                onClick = actions.requestNotifications,
            )
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, detail: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(
            enabled = !granted,
            onClick = onClick,
        ).padding(18.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(9.dp).size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (granted) "Attivo" else "Configura",
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun AppUsageCard(appUsage: TrackedAppUsage, bypassActive: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InstalledAppIcon(
                    packageName = appUsage.app.packageName,
                    displayName = appUsage.app.displayName,
                    modifier = Modifier.size(46.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        appUsage.app.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        when {
                            bypassActive -> "Pausa di emergenza attiva"

                            !appUsage.app.timerEnabled -> "Timer disattivato"

                            appUsage.progress >= 1f -> "Limite raggiunto"

                            else -> "${formatDuration(
                                appUsage.usedMillis,
                            )} di ${appUsage.app.dailyLimitMinutes} min"
                        },
                        color =
                            if (appUsage.progress >= 1f && !bypassActive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = "Modifica ${appUsage.app.displayName}",
                )
            }
            LinearProgressIndicator(
                progress = { appUsage.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (appUsage.progress >=
                    0.85f
                ) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (appUsage.app.overlayEnabled) "Messaggi attivi" else "Messaggi disattivati",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val remaining = (appUsage.limitMillis - appUsage.usedMillis).coerceAtLeast(0L)
                Text(
                    if (appUsage.progress >=
                        1f
                    ) {
                        "0 min rimasti"
                    } else {
                        "${formatDuration(remaining)} rimasti"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EmptyAppsCard(onAdd: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Apps, contentDescription = null, modifier = Modifier.size(42.dp))
            Text(
                "Scegli le app che vuoi usare con più intenzione",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Puoi impostare un limite diverso per ogni app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAdd) { Text("Scegli un'app") }
        }
    }
}

private fun formatClockTime(millis: Long): String = Instant.ofEpochMilli(millis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("HH:mm"))
