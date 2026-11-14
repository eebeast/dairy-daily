package com.milksummer.tracker.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milksummer.tracker.data.repository.MilkEntryRepository
import com.milksummer.tracker.domain.model.*
import com.milksummer.tracker.domain.usecase.GetSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.*
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class SummaryPeriod(val month: YearMonth, val weekStart: LocalDate, val weekly: Boolean = false, val revision: Int = 0) {
    val start: LocalDate get() = if (weekly) weekStart else month.atDay(1)
    val end: LocalDate get() = if (weekly) weekStart.plusDays(6) else month.atEndOfMonth()
}

data class SummaryUiState(
    val period: SummaryPeriod,
    val summary: MilkSummary = MilkSummary(),
    val previous: MilkSummary = MilkSummary(),
    val entries: List<MilkEntry> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val through: LocalDate = period.end
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repository: MilkEntryRepository,
    private val summaries: GetSummaryUseCase,
    private val clock: Clock
) : ViewModel() {
    private val today get() = LocalDate.now(clock)
    private val selection = MutableStateFlow(SummaryPeriod(YearMonth.from(today), today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))))
    val state = selection.flatMapLatest { period ->
        val cutoff = today
        combine(
            repository.getEntriesByDateRange(period.start, period.end),
            summaries.range(period.start, period.end, cutoff),
            summaries.monthly(period.month.minusMonths(1))
        ) { entries, summary, previous ->
            SummaryUiState(period, summary, previous,
                entries.filter { it.date <= cutoff }, loading = false, through = minOf(period.end, cutoff))
        }
            .onStart { emit(SummaryUiState(period)) }
            .catch { emit(SummaryUiState(period, loading = false, error = "Could not load summary. Please retry.")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SummaryUiState(selection.value))

    fun weekly(enabled: Boolean) { selection.update { it.copy(weekly = enabled) } }
    fun move(direction: Long) {
        selection.update { if (it.weekly) it.copy(weekStart = it.weekStart.plusWeeks(direction)) else it.copy(month = it.month.plusMonths(direction)) }
    }
    fun refresh() { selection.update { it.copy(revision = it.revision + 1) } }
}
