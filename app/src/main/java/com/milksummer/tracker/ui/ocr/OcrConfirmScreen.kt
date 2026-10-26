package com.milksummer.tracker.ui.ocr

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.milksummer.tracker.ui.entry.EntryScreen
import com.milksummer.tracker.ui.entry.EntryViewModel

@Composable
fun OcrConfirmScreen(
    fields: ParsedSlip,
    scanId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onRetake: () -> Unit,
    onManual: () -> Unit,
    entryViewModel: EntryViewModel = hiltViewModel()
) {
    val state by entryViewModel.state.collectAsState()
    LaunchedEffect(scanId) { entryViewModel.prefillScan(scanId, fields) }
    EntryScreen(onBack, onSaved, entryViewModel, title = "Review scanned slip", confirmLabel = "Confirm and save") {
        OutlinedButton(onClick = onRetake, enabled = !state.saving && state.saved == null) { Text("Retake photo") }
        TextButton(onClick = onManual, enabled = !state.saving && state.saved == null) { Text("Enter manually") }
    }
}
