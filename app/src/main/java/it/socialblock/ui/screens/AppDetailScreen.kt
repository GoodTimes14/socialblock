package it.socialblock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.socialblock.domain.model.TrackedApp
import it.socialblock.domain.model.TrackedAppUsage
import it.socialblock.platform.format.formatDuration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appUsage: TrackedAppUsage?,
    bypassActive: Boolean,
    bypassUntilMillis: Long,
    onBack: () -> Unit,
    onUpdate: (TrackedApp) -> Unit,
    onRemove: () -> Unit,
    onEnableEmergencyBypass: () -> Unit,
    onClearEmergencyBypass: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(appUsage?.app?.displayName ?: "App") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { contentPadding ->
        if (appUsage == null) {
            Column(
                Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Questa app non è più monitorata.")
                Button(onClick = onBack) { Text("Torna indietro") }
            }
        } else {
            DetailContent(
                appUsage = appUsage,
                bypassActive = bypassActive,
                bypassUntilMillis = bypassUntilMillis,
                onUpdate = onUpdate,
                onRemove = onRemove,
                onEnableEmergencyBypass = onEnableEmergencyBypass,
                onClearEmergencyBypass = onClearEmergencyBypass,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Composable
private fun DetailContent(
    appUsage: TrackedAppUsage,
    bypassActive: Boolean,
    bypassUntilMillis: Long,
    onUpdate: (TrackedApp) -> Unit,
    onRemove: () -> Unit,
    onEnableEmergencyBypass: () -> Unit,
    onClearEmergencyBypass: () -> Unit,
    modifier: Modifier,
) {
    var sliderValue by rememberSaveable(appUsage.app.packageName) {
        mutableFloatStateOf(appUsage.app.dailyLimitMinutes.toFloat())
    }
    var showRemoveConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Utilizzo di oggi", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatDuration(appUsage.usedMillis),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Limite attuale: ${appUsage.app.dailyLimitMinutes} minuti",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text("Limite giornaliero", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${sliderValue.roundToNearestFive()} min", fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        val rounded = sliderValue.roundToNearestFive()
                        sliderValue = rounded.toFloat()
                        onUpdate(appUsage.app.copy(dailyLimitMinutes = rounded))
                    },
                    valueRange = MIN_LIMIT_MINUTES.toFloat()..MAX_LIMIT_MINUTES.toFloat(),
                    steps = LIMIT_SLIDER_STEPS,
                )
                Text(
                    "Il conteggio riparte ogni giorno a mezzanotte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SettingSwitchRow(
                    title = "Chiudi al raggiungimento",
                    detail = "Riporta alla schermata Home quando il tempo è esaurito",
                    checked = appUsage.app.timerEnabled,
                    onCheckedChange = { onUpdate(appUsage.app.copy(timerEnabled = it)) },
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text("Messaggi dissuasivi", style = MaterialTheme.typography.titleMedium)
                }
                SettingSwitchRow(
                    title = "Mostra sopra l'app",
                    detail = "Il messaggio è semitrasparente, non riceve tocchi e non cambia l'interazione con l'app",
                    checked = appUsage.app.overlayEnabled,
                    onCheckedChange = { onUpdate(appUsage.app.copy(overlayEnabled = it)) },
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Column(Modifier.weight(1f)) {
                    Text("Emergenza", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (bypassActive) {
                            "Timer sospesi fino alle ${formatClockTime(bypassUntilMillis)}"
                        } else {
                            "Sospendi tutti i timer per 15 minuti"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = if (bypassActive) onClearEmergencyBypass else onEnableEmergencyBypass,
                ) {
                    Text(if (bypassActive) "Termina" else "Attiva")
                }
            }
        }

        OutlinedButton(
            onClick = { showRemoveConfirmation = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Rimuovi dal monitoraggio")
        }
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmation = false },
            title = { Text("Rimuovere ${appUsage.app.displayName}?") },
            text = { Text("Il limite e i messaggi per questa app verranno disattivati.") },
            confirmButton = { TextButton(onClick = onRemove) { Text("Rimuovi") } },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmation = false }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun SettingSwitchRow(title: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun Float.roundToNearestFive(): Int = (roundToInt() / LIMIT_STEP_MINUTES * LIMIT_STEP_MINUTES).coerceIn(
    MIN_LIMIT_MINUTES,
    MAX_LIMIT_MINUTES,
)

private fun formatClockTime(millis: Long): String = Instant.ofEpochMilli(millis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("HH:mm"))

private const val MIN_LIMIT_MINUTES = 5
private const val MAX_LIMIT_MINUTES = 240
private const val LIMIT_STEP_MINUTES = 5
private const val LIMIT_SLIDER_STEPS = 46
