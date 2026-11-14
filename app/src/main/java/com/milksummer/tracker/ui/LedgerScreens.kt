package com.milksummer.tracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.milksummer.tracker.domain.model.*
import java.time.LocalDate

@Composable
fun HomeScreen(state: LedgerUiState, onAdd: (Shift?) -> Unit, onScan: () -> Unit, onEdit: (Long) -> Unit, onDelete: (MilkEntry) -> Unit) {
    val entries = state.entries.filter { it.date == state.today }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        item { Text("MilkSummer", style = MaterialTheme.typography.headlineLarge); Text("Today · ${state.today}") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onAdd(null) }) { Text("Add Entry") }
            OutlinedButton(onClick = onScan) { Text("Scan Slip") }
        } }
        if (state.loading) item { CircularProgressIndicator() }
        item { Text("Today's sales", style = MaterialTheme.typography.titleLarge) }
        if (entries.isEmpty() && !state.loading) item { Text("No milk sales recorded today.") }
        items(entries, key = { it.id }) { entry ->
            EntryCard(entry, { onEdit(entry.id) }, { onDelete(entry) })
        }
        item { Text("Today: ${decimal(entries.sumOf { it.quantity })} L · ${money(entries.sumOf { it.total })}", style = MaterialTheme.typography.titleMedium) }
        item { Text("Month to date", style = MaterialTheme.typography.titleLarge) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("${decimal(state.month.totalLiters)} liters", style = MaterialTheme.typography.titleLarge)
            Text(money(state.month.totalEarnings), style = MaterialTheme.typography.headlineSmall)
        } } }
    }
}

@Composable
fun HistoryScreen(state: LedgerUiState, onEdit: (Long) -> Unit, onDelete: (MilkEntry) -> Unit, day: LocalDate? = null) {
    val entries = if (day == null) state.entries else state.entries.filter { it.date == day }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        item { Text(day?.toString() ?: "History", style = MaterialTheme.typography.headlineMedium) }
        if (state.loading) item { CircularProgressIndicator() }
        if (entries.isEmpty() && !state.loading) item { Text("No entries recorded${if (day != null) " for this day" else " yet"}.") }
        entries.groupBy { it.date }.forEach { (date, rows) ->
            item(key = "day-$date") { Text("$date · ${decimal(rows.sumOf { it.quantity })} L · ${money(rows.sumOf { it.total })}", style = MaterialTheme.typography.titleMedium) }
            items(rows, key = { it.id }) { entry -> EntryCard(entry, { onEdit(entry.id) }, { onDelete(entry) }) }
        }
        if (day != null && entries.none { it.shift == null }) Shift.entries.filter { shift -> entries.none { it.shift == shift } }.forEach { shift ->
            item { Text("No ${shift.name.lowercase()} entry recorded.") }
        }
    }
}

@Composable
private fun EntryCard(entry: MilkEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            entry.shift?.let { Text(it.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium) }
            Text("${decimal(entry.quantity)} L × ${money(entry.rate)} = ${money(entry.total)}")
            Text("FAT ${decimal(entry.fat)}% · SNF ${decimal(entry.snf)}%")
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
