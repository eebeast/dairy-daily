package com.milksummer.tracker

import com.milksummer.tracker.domain.model.Shift
import com.milksummer.tracker.domain.usecase.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class EntryUseCaseTest {
    private val validate = ValidateEntryUseCase()
    private val clock = Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), ZoneOffset.UTC)

    @Test fun zeroAndNegativeQuantityAndRateAreRejected() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { invalid ->
            assertTrue(validate(sampleEntry().copy(quantity = invalid)).errors.containsKey("quantity"))
            assertTrue(validate(sampleEntry().copy(rate = invalid)).errors.containsKey("rate"))
        }
        assertTrue(validate(sampleEntry().copy(quantity = 0.001, rate = 0.001)).isValid)
    }

    @Test fun qualityIncludesBothBoundariesAndRejectsOutside() {
        listOf(0.0, 15.0).forEach { assertTrue(validate(sampleEntry().copy(fat = it, snf = it)).isValid) }
        listOf(-0.01, 15.01, Double.NaN, Double.NEGATIVE_INFINITY).forEach {
            val errors = validate(sampleEntry().copy(fat = it, snf = it)).errors
            assertTrue(errors.containsKey("fat"))
            assertTrue(errors.containsKey("snf"))
        }
    }

    @Test fun overflowAndInvalidSlipTotalsAreRejected() {
        assertFalse(validate(sampleEntry().copy(quantity = Double.MAX_VALUE, rate = 2.0)).isValid)
        assertTrue(validate(sampleEntry().copy(slipTotal = -1.0)).errors.containsKey("slipTotal"))
        assertTrue(validate(sampleEntry().copy(slipTotal = 0.0)).isValid)
    }

    @Test fun saveComputesTotalAndTimestamps() = runBlocking {
        val repo = FakeMilkEntryRepository()
        val result = SaveEntryUseCase(repo, validate, clock)(sampleEntry().copy(total = 1.0)) as SaveEntryResult.Success
        assertEquals(200.20, result.entry.total, 0.000001)
        assertEquals(clock.millis(), result.entry.createdAt)
        assertEquals(clock.millis(), result.entry.updatedAt)
        assertTrue(result.entry.id > 0)
    }

    @Test fun invalidEntryNeverPersists() = runBlocking {
        val repo = FakeMilkEntryRepository()
        assertTrue(SaveEntryUseCase(repo, validate, clock)(sampleEntry().copy(quantity = -1.0)) is SaveEntryResult.Invalid)
        assertTrue(repo.entries.value.isEmpty())
    }

    @Test fun duplicateRejectedButOtherShiftAllowed() = runBlocking {
        val repo = FakeMilkEntryRepository()
        val save = SaveEntryUseCase(repo, validate, clock)
        assertTrue(save(sampleEntry()) is SaveEntryResult.Success)
        assertEquals(SaveEntryResult.Duplicate, save(sampleEntry()))
        assertTrue(save(sampleEntry(shift = Shift.EVENING)) is SaveEntryResult.Success)
        assertEquals(2, repo.entries.value.size)
    }

    @Test fun concurrentDatabaseDuplicateBecomesDuplicateResult() = runBlocking {
        val repo = FakeMilkEntryRepository().apply { failWithDuplicate = true }
        assertEquals(SaveEntryResult.Duplicate, SaveEntryUseCase(repo, validate, clock)(sampleEntry()))
    }

    @Test fun editPreservesCreationAndRejectsOccupiedShift() = runBlocking {
        val repo = FakeMilkEntryRepository()
        val save = SaveEntryUseCase(repo, validate, clock)
        val morning = (save(sampleEntry()) as SaveEntryResult.Success).entry
        save(sampleEntry(shift = Shift.EVENING))
        val edited = (save(morning.copy(quantity = 5.5, createdAt = 0)) as SaveEntryResult.Success).entry
        assertEquals(morning.createdAt, edited.createdAt)
        assertEquals(211.75, edited.total, 0.000001)
        assertEquals(SaveEntryResult.Duplicate, save(morning.copy(shift = Shift.EVENING)))
        assertEquals(SaveEntryResult.Missing, save(morning.copy(id = 999)))
    }
}
