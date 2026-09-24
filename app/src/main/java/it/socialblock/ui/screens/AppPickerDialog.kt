package it.socialblock.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.socialblock.platform.apps.InstalledApp
import it.socialblock.ui.components.InstalledAppIcon

@Composable
fun AppPickerDialog(apps: List<InstalledApp>, loading: Boolean, onDismiss: () -> Unit, onSelect: (InstalledApp) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(apps, query) { apps.filter { it.displayName.contains(query, ignoreCase = true) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi un'app") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Cerca") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                )
                when {
                    loading -> {
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 180.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    filtered.isEmpty() -> {
                        Text(
                            if (apps.isEmpty()) "Non ci sono altre app da aggiungere." else "Nessun risultato.",
                            modifier = Modifier.padding(vertical = 24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                            items(filtered, key = InstalledApp::packageName) { app ->
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelect(app) }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    InstalledAppIcon(
                                        packageName = app.packageName,
                                        displayName = app.displayName,
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(app.displayName, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            app.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
