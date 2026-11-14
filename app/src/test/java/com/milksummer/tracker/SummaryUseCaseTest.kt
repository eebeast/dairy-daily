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

    @Test fun unknownShiftsCountInTotalsButCannotCompleteADay() = runBlocking {
        val repo = FakeMilkEntryRepository()
        val day = LocalDate.now(clock)
        val useCase = GetSummaryUseCase(repo, clock)
        repo.entries.value = listOf(sampleEntry(day), sampleEntry(day, null))
        assertEquals(1, useCase.range(day, day).first().missedDays)
        repo.entries.value = listOf(sampleEntry(day, null), sampleEntry(day, null))
        val unknown = useCase.range(day, day).first()
        assertEquals(1, unknown.missedDays)
        assertEquals(2, unknown.entryCount)
        assertEquals(400.4, unknown.totalEarnings, 0.000001)
        assertEquals(6.5, unknown.averageFat, 0.000001)
        assertEquals(8.8, unknown.averageSnf, 0.000001)
        repo.entries.value += listOf(sampleEntry(day), sampleEntry(day, Shift.EVENING))
        assertEquals(0, useCase.range(day, day).first().missedDays)
        assertEquals(4, useCase.range(day, day).first().entryCount)
    }

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
