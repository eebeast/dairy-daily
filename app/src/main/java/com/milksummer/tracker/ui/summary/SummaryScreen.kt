package com.milksummer.tracker.ui.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.milksummer.tracker.domain.model.*
import com.milksummer.tracker.ui.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun SummaryScreen(onDay: (LocalDate) -> Unit, viewModel: SummaryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    var compare by remember(state.period) { mutableStateOf(false) }
    val days = remember(state.period) {
        (0..ChronoUnit.DAYS.between(state.period.start, state.period.end)).map { state.period.start.plusDays(it) }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        item { Text("Summary", style = MaterialTheme.typography.headlineMedium) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!state.period.weekly, { viewModel.weekly(false) }, label = { Text("Monthly") })
            FilterChip(state.period.weekly, { viewModel.weekly(true) }, label = { Text("Weekly") })
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { viewModel.move(-1) }) { Text("‹ Previous") }
            Text(if (state.period.weekly) "${state.period.start}\n${state.period.end}" else state.period.month.toString())
            TextButton(onClick = { viewModel.move(1) }) { Text("Next ›") }
        } }
        if (state.loading) item { CircularProgressIndicator() }
        state.error?.let { error -> item { Text(error); TextButton(onClick = viewModel::refresh) { Text("Retry") } } }
        if (!state.loading && state.error == null) {
            item { SummaryCard(state.summary) }
            if (!state.period.weekly) {
                item { OutlinedButton(onClick = { compare = !compare }) { Text(if (compare) "Hide comparison" else "Compare with previous month") } }
                if (compare) item {
                    if (state.previous.entryCount == 0) Text("No prior month data available for comparison.")
                    else ComparisonTable(state.period.month.toString(), state.period.month.minusMonths(1).toString(), state.summary, state.previous)
                }
            }
            item { Text("Daily breakdown", style = MaterialTheme.typography.titleLarge) }
            items(days, key = { it.toString() }) { day ->
                val entries = state.entries.filter { it.date == day }
                Card(Modifier.fillMaxWidth().clickable { onDay(day) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(day.toString(), style = MaterialTheme.typography.titleMedium)
                        Text("${decimal(entries.sumOf { it.quantity })} L · ${money(entries.sumOf { it.total })}")
                        Text(when {
                            day > LocalDate.now() -> "Upcoming"
                            entries.size == 2 -> "Both shifts recorded"
                            else -> "${2 - entries.size} shift(s) missing"
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(summary: MilkSummary) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${decimal(summary.totalLiters)} liters", style = MaterialTheme.typography.titleLarge)
            Text(money(summary.totalEarnings), style = MaterialTheme.typography.headlineSmall)
            Text("Average FAT: ${decimal(summary.averageFat)}%")
            Text("Average SNF: ${decimal(summary.averageSnf)}%")
            Text("Average rate: ${money(summary.averageRate)}/liter")
            Text("Entries: ${summary.entryCount} · Missed days: ${summary.missedDays}")
        }
    }
}

@Composable
private fun ComparisonTable(currentLabel: String, previousLabel: String, current: MilkSummary, previous: MilkSummary) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                listOf("Metric", currentLabel, previousLabel, "Difference").forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall) }
            }
            listOf(
                Triple("Liters", current.totalLiters, previous.totalLiters),
                Triple("Earnings ₹", current.totalEarnings, previous.totalEarnings),
                Triple("FAT %", current.averageFat, previous.averageFat),
                Triple("SNF %", current.averageSnf, previous.averageSnf),
                Triple("Rate ₹", current.averageRate, previous.averageRate)
            ).forEach { (label, now, before) ->
                Row(Modifier.fillMaxWidth()) {
                    listOf(label, decimal(now), decimal(before), decimal(now - before)).forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
