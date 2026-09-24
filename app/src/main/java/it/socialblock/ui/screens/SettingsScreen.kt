package it.socialblock.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.socialblock.PermissionActions
import it.socialblock.domain.model.AppSettings
import it.socialblock.domain.model.OverlayPosition
import it.socialblock.ui.MainUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onUpdateSummaryTime: (Int, Int) -> Unit,
    onUpdateOverlayPosition: (OverlayPosition) -> Unit,
    onUpdateOverlayOpacity: (Int) -> Unit,
    onAddOverlayMessage: (String) -> Unit,
    onUpdateOverlayMessage: (Int, String) -> Unit,
    onRemoveOverlayMessage: (Int) -> Unit,
    onUpdateOverlayMessageChangeDelay: (Int) -> Unit,
    permissionActions: PermissionActions,
) {
    var showTimeDialog by remember { mutableStateOf(false) }
    var messageEditor by remember { mutableStateOf<MessageEditorState?>(null) }
    var showMessageDelayDialog by remember { mutableStateOf(false) }
    var showOverlayOpacityDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Messaggi overlay", style = MaterialTheme.typography.titleLarge)
            Card(Modifier.fillMaxWidth()) {
                OverlayMessagesEditor(
                    messages = state.settings.overlayMessages,
                    changeDelaySeconds = state.settings.overlayMessageChangeDelaySeconds,
                    onAdd = { messageEditor = MessageEditorState() },
                    onEdit = { index, message ->
                        messageEditor = MessageEditorState(index = index, initialText = message)
                    },
                    onRemove = onRemoveOverlayMessage,
                    onChangeDelay = { showMessageDelayDialog = true },
                )
            }
            OverlayAppearanceCard(
                settings = state.settings,
                onPositionSelected = onUpdateOverlayPosition,
                onEditOpacity = { showOverlayOpacityDialog = true },
            )

            Text("Notifiche", style = MaterialTheme.typography.titleLarge)
            Card(Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.Schedule,
                    title = "Riepilogo giornaliero",
                    detail = "%02d:%02d".format(
                        state.settings.notificationHour,
                        state.settings.notificationMinute,
                    ),
                    onClick = { showTimeDialog = true },
                )
                SettingsRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Permesso notifiche",
                    detail = if (state.permissions.notifications) "Attivo" else "Necessario per gli avvisi",
                    onClick = permissionActions.requestNotifications,
                )
            }

            Text("Permessi speciali", style = MaterialTheme.typography.titleLarge)
            Card(Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.Apps,
                    title = "Accesso all'utilizzo",
                    detail = if (state.permissions.usageAccess) "Attivo" else "Non concesso",
                    onClick = permissionActions.requestUsageAccess,
                )
                SettingsRow(
                    icon = Icons.Outlined.Visibility,
                    title = "Mostra sopra le app",
                    detail = if (state.permissions.overlay) "Attivo" else "Non concesso",
                    onClick = permissionActions.requestOverlay,
                )
            }

            Text("Privacy", style = MaterialTheme.typography.titleLarge)
            PrivacySettingsCard()
        }
    }

    if (showTimeDialog) {
        SummaryTimeDialog(
            initialHour = state.settings.notificationHour,
            initialMinute = state.settings.notificationMinute,
            onDismiss = { showTimeDialog = false },
            onConfirm = { hour, minute ->
                onUpdateSummaryTime(hour, minute)
                showTimeDialog = false
            },
        )
    }

    messageEditor?.let { editor ->
        OverlayMessageDialog(
            editor = editor,
            onDismiss = { messageEditor = null },
            onConfirm = { message ->
                if (editor.index == null) {
                    onAddOverlayMessage(message)
                } else {
                    onUpdateOverlayMessage(editor.index, message)
                }
                messageEditor = null
            },
        )
    }

    if (showMessageDelayDialog) {
        OverlayMessageDelayDialog(
            initialSeconds = state.settings.overlayMessageChangeDelaySeconds,
            onDismiss = { showMessageDelayDialog = false },
            onConfirm = { seconds ->
                onUpdateOverlayMessageChangeDelay(seconds)
                showMessageDelayDialog = false
            },
        )
    }

    if (showOverlayOpacityDialog) {
        OverlayOpacityDialog(
            initialPercent = state.settings.overlayOpacityPercent,
            onDismiss = { showOverlayOpacityDialog = false },
            onConfirm = { percent ->
                onUpdateOverlayOpacity(percent)
                showOverlayOpacityDialog = false
            },
        )
    }
}

@Composable
private fun OverlayAppearanceCard(settings: AppSettings, onPositionSelected: (OverlayPosition) -> Unit, onEditOpacity: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        OverlayPositionSelector(
            selectedPosition = settings.overlayPosition,
            onPositionSelected = onPositionSelected,
        )
        HorizontalDivider()
        SettingsRow(
            icon = Icons.Outlined.Visibility,
            title = "Trasparenza overlay",
            detail =
                "Opacità ${settings.overlayOpacityPercent}% · " +
                    "trasparenza ${PERCENT_TOTAL - settings.overlayOpacityPercent}%",
            onClick = onEditOpacity,
            modifier = Modifier.testTag("overlay_opacity_row"),
        )
    }
}

@Composable
private fun PrivacySettingsCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text(
                    "Privato per impostazione predefinita",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                "Tutti i limiti, le preferenze e le statistiche restano sul dispositivo. " +
                    "SocialBlock non raccoglie contenuti dello schermo, cronologia di navigazione o dati personali. " +
                    "Non usa un servizio di accessibilità e non richiede una connessione internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OverlayMessagesEditor(
    messages: List<String>,
    changeDelaySeconds: Int,
    onAdd: () -> Unit,
    onEdit: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onChangeDelay: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Testi dei messaggi", fontWeight = FontWeight.SemiBold)
                Text(
                    "Vengono mostrati a rotazione sopra le app monitorate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onAdd, modifier = Modifier.testTag("add_overlay_message")) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("Aggiungi")
            }
        }

        if (messages.isEmpty()) {
            Text(
                "Nessun messaggio: l'overlay non verrà mostrato.",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            messages.forEachIndexed { index, message ->
                HorizontalDivider(Modifier.padding(horizontal = 18.dp))
                OverlayMessageRow(
                    index = index,
                    message = message,
                    onEdit = { onEdit(index, message) },
                    onRemove = { onRemove(index) },
                )
            }
        }

        HorizontalDivider()
        SettingsRow(
            icon = Icons.Outlined.Schedule,
            title = "Cambio messaggio",
            detail = "Ogni ${formatMessageDelay(changeDelaySeconds)}",
            onClick = onChangeDelay,
            modifier = Modifier.testTag("overlay_message_delay_row"),
        )
    }
}

@Composable
private fun OverlayMessageRow(index: Int, message: String, onEdit: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Modifica messaggio ${index + 1}")
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Delete, contentDescription = "Rimuovi messaggio ${index + 1}")
        }
    }
}

@Composable
private fun OverlayMessageDialog(editor: MessageEditorState, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember(editor) { mutableStateOf(editor.initialText) }
    val trimmedText = text.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.index == null) "Nuovo messaggio" else "Modifica messaggio") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(AppSettings.MAX_OVERLAY_MESSAGE_LENGTH) },
                modifier = Modifier.fillMaxWidth().testTag("overlay_message_text"),
                label = { Text("Testo del messaggio") },
                minLines = 3,
                maxLines = 5,
                supportingText = {
                    Text("${text.length}/${AppSettings.MAX_OVERLAY_MESSAGE_LENGTH}")
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmedText) },
                enabled = trimmedText.isNotEmpty(),
            ) {
                Text("Salva")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun OverlayMessageDelayDialog(initialSeconds: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var value by remember(initialSeconds) { mutableStateOf(initialSeconds.toString()) }
    val seconds = value.toIntOrNull()
    val valid =
        seconds != null &&
            seconds in AppSettings.MIN_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS..AppSettings.MAX_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Intervallo dei messaggi") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { input -> value = input.filter(Char::isDigit).take(MAX_DELAY_INPUT_DIGITS) },
                modifier = Modifier.fillMaxWidth().testTag("overlay_message_delay"),
                label = { Text("Secondi") },
                singleLine = true,
                isError = value.isNotEmpty() && !valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    Text(
                        "Da ${AppSettings.MIN_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS} a " +
                            "${AppSettings.MAX_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS} secondi",
                    )
                },
            )
        },
        confirmButton = {
            TextButton(onClick = { seconds?.let(onConfirm) }, enabled = valid) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun OverlayOpacityDialog(initialPercent: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var sliderValue by remember(initialPercent) { mutableFloatStateOf(initialPercent.toFloat()) }
    val opacityPercent =
        sliderValue.roundToInt().coerceIn(
            AppSettings.MIN_OVERLAY_OPACITY_PERCENT,
            AppSettings.MAX_OVERLAY_OPACITY_PERCENT,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trasparenza overlay") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Opacità $opacityPercent% · trasparenza ${PERCENT_TOTAL - opacityPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = AppSettings.MIN_OVERLAY_OPACITY_PERCENT.toFloat()..AppSettings.MAX_OVERLAY_OPACITY_PERCENT.toFloat(),
                    modifier = Modifier.testTag("overlay_opacity_slider"),
                )
                Text(
                    "L'avviso resta semitrasparente e non intercetta i tocchi. " +
                        "L'opacità massima è limitata per mantenere utilizzabile l'app sottostante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(opacityPercent) }) { Text("Salva") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun OverlayPositionSelector(selectedPosition: OverlayPosition, onPositionSelected: (OverlayPosition) -> Unit) {
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Posizione", fontWeight = FontWeight.SemiBold)
        Text(
            "Scegli dove mostrare l'avviso nelle app monitorate.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OverlayPosition.entries.forEach { position ->
                FilterChip(
                    selected = selectedPosition == position,
                    onClick = { onPositionSelected(position) },
                    label = {
                        Text(
                            text = position.displayName,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                            .testTag("overlay_position_${position.name.lowercase()}"),
                )
            }
        }
    }
}

private val OverlayPosition.displayName: String
    get() =
        when (this) {
            OverlayPosition.TOP -> "Alto"
            OverlayPosition.CENTER -> "Centro"
            OverlayPosition.BOTTOM -> "Basso"
        }

@Composable
private fun SettingsRow(icon: ImageVector, title: String, detail: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryTimeDialog(initialHour: Int, initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val timeState =
        rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Orario del riepilogo") },
        text = { TimeInput(state = timeState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

private fun formatMessageDelay(seconds: Int): String = when {
    seconds < SECONDS_PER_MINUTE -> "$seconds secondi"
    seconds % SECONDS_PER_MINUTE == 0 -> "${seconds / SECONDS_PER_MINUTE} min"
    else -> "${seconds / SECONDS_PER_MINUTE} min ${seconds % SECONDS_PER_MINUTE} sec"
}

private data class MessageEditorState(val index: Int? = null, val initialText: String = "")

private const val PERCENT_TOTAL = 100
private const val MAX_DELAY_INPUT_DIGITS = 4
private const val SECONDS_PER_MINUTE = 60
