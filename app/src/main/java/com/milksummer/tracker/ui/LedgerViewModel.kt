package com.milksummer.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milksummer.tracker.data.repository.MilkEntryRepository
import com.milksummer.tracker.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LedgerUiState(
    val entries: List<MilkEntry> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val month: MilkSummary = MilkSummary(),
    val loading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class LedgerViewModel @Inject constructor(private val repository: MilkEntryRepository, private val clock: Clock) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now(clock))
    private val errors = MutableStateFlow<String?>(null)
    private val entries = repository.getAll().catch {
        errors.value = "Could not load entries. Reopen the app to retry."
        emit(emptyList())
    }
    val state = combine(entries, today, errors) { entries, date, error ->
        LedgerUiState(entries.sortedWith(compareByDescending<MilkEntry> { it.date }.thenBy { it.shift }), date,
            summarizeEntries(entries, date.withDayOfMonth(1), date, date), false, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerUiState(today = today.value))

    fun refreshToday() { today.value = LocalDate.now(clock) }
    fun dismissError() { errors.value = null }
    fun delete(entry: MilkEntry) {
        viewModelScope.launch {
            try { repository.deleteEntry(entry.id) }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                errors.value = "Could not delete entry. Please try again."
            }
        }
    }
}
