package com.milksummer.tracker.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BackupSection(viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json"), viewModel::export)
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), viewModel::preview)
    BackupContent(state,
        onBackup = { create.launch("MilkSummer-${LocalDate.now()}.json") },
        onRestore = { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        onConfirm = viewModel::confirmRestore, onCancel = viewModel::cancelPreview,
        onDismiss = viewModel::dismissMessage)
}

@Composable
fun BackupContent(
    state: BackupUiState, onBackup: () -> Unit, onRestore: () -> Unit,
    onConfirm: () -> Unit, onCancel: () -> Unit, onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider()
        Text("Backup and restore", style = MaterialTheme.typography.titleLarge)
        Text("Save your milk entries and reminder settings to a file, or restore a saved backup.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBackup, enabled = !state.busy && state.preview == null) { Text("Create backup") }
            OutlinedButton(onClick = onRestore, enabled = !state.busy && state.preview == null) { Text("Restore backup") }
        }
        if (state.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Working…")
        }
        state.message?.let { Text(it) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.message != null || state.error != null) TextButton(onClick = onDismiss) { Text("Dismiss result") }
    }
    state.preview?.let { data ->
        val time = DateTimeFormatter.ofPattern("h:mm a")
        AlertDialog(
            onDismissRequest = { if (!state.busy) onCancel() },
            title = { Text("Restore this backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${data.entries.size} entries")
                    Text(if (data.firstDate == null) "No recorded dates" else "${data.firstDate} to ${data.lastDate}")
                    Text("Existing entries for the same date and shift will be kept and skipped.")
                    Text("Reminders: ${if (data.reminders.enabled) "on" else "off"}. Morning ${data.reminders.morning.format(time)}, evening ${data.reminders.evening.format(time)}.")
                    Text("Reminder times apply from tomorrow. Notification permissions stay controlled by this device.")
                    if (state.busy) Text("Restoring…")
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = onConfirm, enabled = !state.busy) { Text("Restore entries") } },
            dismissButton = { TextButton(onClick = onCancel, enabled = !state.busy) { Text("Cancel") } }
        )
    }
}
