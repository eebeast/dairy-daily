package com.milksummer.tracker.ui.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.milksummer.tracker.domain.model.Shift
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onRequestPermission: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        if (state.loading) CircularProgressIndicator()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Entry reminders")
            Switch(checked = state.settings.enabled && state.canNotify, enabled = !state.loading,
                onCheckedChange = { value -> if (value && !state.canNotify) onRequestPermission() else viewModel.enable(value) })
        }
        if (!state.canNotify) {
            Text("Reminders are disabled because notifications are not allowed.")
            OutlinedButton(onClick = onRequestPermission) { Text("Enable notifications") }
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Open notification settings") }
        }
        Shift.entries.forEach { shift ->
            val time = if (shift == Shift.MORNING) state.settings.morning else state.settings.evening
            OutlinedButton(onClick = {
                TimePickerDialog(context, { _, hour, minute -> viewModel.time(shift, LocalTime.of(hour, minute)) }, time.hour, time.minute, false).show()
            }, enabled = !state.loading) { Text("${shift.name.lowercase().replaceFirstChar { it.uppercase() }}: ${time.format(DateTimeFormatter.ofPattern("h:mm a"))}") }
        }
        Text("Changes to reminder times apply from tomorrow. A reminder is skipped when that shift's entry is already saved.")
        if (Build.VERSION.SDK_INT >= 31 && !state.exactAllowed) {
            Text("Reminders may arrive late unless precise alarms are allowed.")
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }) { Text("Allow precise reminder times") }
        }
        Text("MilkSummer · Offline milk sale tracker", style = MaterialTheme.typography.bodySmall)
        state.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = viewModel::clearError) { Text("Dismiss") }
        }
    }
}
