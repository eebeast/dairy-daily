package com.milksummer.tracker.ui.entry

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.milksummer.tracker.domain.model.Shift
import com.milksummer.tracker.ui.money
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EntryViewModel = hiltViewModel(),
    title: String? = null,
    confirmLabel: String? = null,
    extraActions: @Composable () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.saved) {
        if (state.saved != null) {
            snackbar.showSnackbar(if (state.id == 0L) "Entry saved" else "Entry updated", duration = SnackbarDuration.Short)
            onSaved()
        }
    }
    Scaffold(
        topBar = { TopAppBar(
            title = { Text(title ?: if (state.id == 0L) "Add entry" else "Edit entry") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        ) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.padding(padding).padding(24.dp)) { CircularProgressIndicator() }
        } else {
            Column(Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    DatePickerDialog(context, { _, year, month, day -> viewModel.selectDate(LocalDate.of(year, month + 1, day)) }, state.date.year, state.date.monthValue - 1, state.date.dayOfMonth).show()
                }, enabled = !state.saving && state.saved == null) { Text("Date: ${state.date}") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.shift == null, onClick = { viewModel.selectShift(null) },
                        label = { Text("No shift") }, enabled = !state.saving && state.saved == null)
                    Shift.entries.forEach { shift ->
                        FilterChip(selected = state.shift == shift, onClick = { viewModel.selectShift(shift) }, label = { Text(shift.name.lowercase().replaceFirstChar { it.uppercase() }) }, enabled = !state.saving && state.saved == null)
                    }
                }
                listOf("quantity" to "Quantity (liters)", "fat" to "FAT (%)", "snf" to "SNF (%)", "rate" to "Rate (₹/liter)", "slipTotal" to "Slip total (optional, ₹)").forEach { (name, label) ->
                    val value = when (name) { "quantity" -> state.quantity; "fat" -> state.fat; "snf" -> state.snf; "rate" -> state.rate; else -> state.slipTotal }
                    OutlinedTextField(
                        value = value, onValueChange = { viewModel.field(name, it) }, label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.saving && state.saved == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = state.errors.containsKey(name),
                        supportingText = state.errors[name]?.let { error -> { Text(error) } }
                    )
                }
                Text("Calculated total: ${state.calculatedTotal?.let { money(it) } ?: "—"}", style = MaterialTheme.typography.titleLarge)
                state.comparison?.let { comparison ->
                    Text(
                        if (comparison.matches) "✓ Slip total matches" else "⚠ Difference: ${money(comparison.difference.toDouble())}. Verify the slip before saving.",
                        color = if (comparison.matches) Color(0xFF238636) else MaterialTheme.colorScheme.error
                    )
                }
                Button(onClick = viewModel::save, enabled = !state.saving && state.saved == null, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.saving) "Saving…" else confirmLabel ?: if (state.id == 0L) "Save entry" else "Update")
                }
                extraActions()
            }
        }
    }
    if (state.duplicate || state.message != null) {
        AlertDialog(onDismissRequest = viewModel::dismissError,
            title = { Text(if (state.duplicate) "Entry already exists" else "Entry unavailable") },
            text = { Text(if (state.duplicate) "An entry already exists for ${state.date}${state.shift?.let { ", ${it.name.lowercase()}" }.orEmpty()}. Edit it in History, or choose another date or shift." else state.message.orEmpty()) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } })
    }
}
