package com.milksummer.tracker

import com.milksummer.tracker.domain.model.Shift
import com.milksummer.tracker.domain.usecase.GetSummaryUseCase
import java.time.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SummaryUseCaseTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC)

    @Test fun weekStartsMondayAndStopsToday() = runBlocking {
        val repo = FakeMilkEntryRepository()
        val monday = LocalDate.of(2026, 10, 19)
        repo.entries.value = listOf(sampleEntry(monday), sampleEntry(monday, Shift.EVENING), sampleEntry(monday.minusDays(1)), sampleEntry(monday.plusDays(3)))
        val result = GetSummaryUseCase(repo, clock).weekly(monday.plusDays(2)).first()
        assertEquals(2, result.entryCount)
        assertEquals(2, result.missedDays)
        assertEquals(400.40, result.totalEarnings, 0.000001)
    }

    @Test fun monthExcludesAdjacentMonthsAndFutureDays() = runBlocking {
        val repo = FakeMilkEntryRepository()
        repo.entries.value = listOf(sampleEntry(LocalDate.of(2026, 9, 30)), sampleEntry(LocalDate.of(2026, 10, 1)), sampleEntry(LocalDate.of(2026, 10, 22)))
        val result = GetSummaryUseCase(repo, clock).monthly(YearMonth.of(2026, 10)).first()
        assertEquals(1, result.entryCount)
        assertEquals(21, result.missedDays)
    }
}
