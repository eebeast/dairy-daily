package com.milksummer.tracker.domain.usecase

import com.milksummer.tracker.data.repository.MilkEntryRepository
import com.milksummer.tracker.domain.model.summarizeEntries
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class GetSummaryUseCase @Inject constructor(
    private val repository: MilkEntryRepository,
    private val clock: Clock
) {
    fun monthly(month: YearMonth) = range(month.atDay(1), month.atEndOfMonth())

    fun weekly(date: LocalDate): kotlinx.coroutines.flow.Flow<com.milksummer.tracker.domain.model.MilkSummary> {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return range(monday, monday.plusDays(6))
    }

    fun range(start: LocalDate, end: LocalDate) = repository.getEntriesByDateRange(start, end).map {
        summarizeEntries(it, start, end, LocalDate.now(clock))
    }
}
