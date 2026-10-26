package com.milksummer.tracker

import com.milksummer.tracker.data.local.MilkEntryConverters
import com.milksummer.tracker.data.repository.toDomain
import com.milksummer.tracker.data.repository.toEntity
import com.milksummer.tracker.domain.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class MilkDataTest {
    @Test fun mappingPreservesEveryField() {
        val entry = sampleEntry().copy(id = 9, slipTotal = 201.0, createdAt = 1234, updatedAt = 5678)
        assertEquals(entry, entry.toEntity().toDomain())
        assertNull(sampleEntry().toEntity().toDomain().slipTotal)
        assertEquals(200.20, entry.total, 0.000001)
    }

    @Test fun convertersPreserveIsoDatesAndShifts() {
        val converters = MilkEntryConverters()
        val date = LocalDate.of(2024, 2, 29)
        assertEquals("2024-02-29", converters.dateToString(date))
        assertEquals(date, converters.stringToDate(converters.dateToString(date)))
        Shift.entries.forEach { assertEquals(it, converters.stringToShift(converters.shiftToString(it))) }
    }

    @Test fun completeMonthAggregatesAndCountsNoMissedDays() {
        val start = LocalDate.of(2026, 9, 1)
        val end = start.plusDays(29)
        val entries = (0L..29L).flatMap { offset -> Shift.entries.map { sampleEntry(start.plusDays(offset), it) } }
        val result = summarizeEntries(entries, start, end, end)
        assertEquals(312.0, result.totalLiters, 0.000001)
        assertEquals(12012.0, result.totalEarnings, 0.000001)
        assertEquals(6.5, result.averageFat, 0.000001)
        assertEquals(8.8, result.averageSnf, 0.000001)
        assertEquals(38.5, result.averageRate, 0.000001)
        assertEquals(60, result.entryCount)
        assertEquals(0, result.missedDays)
    }

    @Test fun missingAndSingleShiftDaysCountButFutureDaysDoNot() {
        val start = LocalDate.of(2026, 10, 1)
        val entries = listOf(sampleEntry(start), sampleEntry(start, Shift.EVENING), sampleEntry(start.plusDays(1)))
        val result = summarizeEntries(entries, start, start.plusDays(30), start.plusDays(3))
        assertEquals(3, result.missedDays)
        assertEquals(3, result.entryCount)
    }

    @Test fun emptyAndFutureRangesHaveFiniteZeroAverages() {
        val date = LocalDate.of(2026, 10, 1)
        val empty = summarizeEntries(emptyList(), date, date.plusDays(6), date.plusDays(6))
        assertEquals(MilkSummary(missedDays = 7), empty)
        assertEquals(MilkSummary(), summarizeEntries(emptyList(), date, date.plusDays(6), date.minusDays(1)))
    }

    @Test fun averagesAreArithmeticAndOutOfPeriodEntriesAreExcluded() {
        val date = LocalDate.of(2026, 10, 1)
        val entries = listOf(sampleEntry(date), sampleEntry(date, Shift.EVENING).copy(fat = 7.5, snf = 9.8, rate = 40.5), sampleEntry(date.minusDays(1)))
        val result = summarizeEntries(entries, date, date, date)
        assertEquals(7.0, result.averageFat, 0.000001)
        assertEquals(9.3, result.averageSnf, 0.000001)
        assertEquals(39.5, result.averageRate, 0.000001)
        assertEquals(2, result.entryCount)
    }
}
