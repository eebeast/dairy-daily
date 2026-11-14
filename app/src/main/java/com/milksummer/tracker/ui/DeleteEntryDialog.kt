package com.milksummer.tracker.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.milksummer.tracker.domain.model.MilkEntry

@Composable
fun DeleteEntryDialog(entry: MilkEntry, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete entry?") },
        text = { Text("Delete the ${entry.shift?.let { "${it.name.lowercase()} " }.orEmpty()}entry for ${entry.date}? This cannot be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
