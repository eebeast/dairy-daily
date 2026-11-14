package com.milksummer.tracker

import androidx.lifecycle.SavedStateHandle
import com.milksummer.tracker.domain.model.Shift
import com.milksummer.tracker.domain.usecase.*
import com.milksummer.tracker.ui.entry.EntryViewModel
import com.milksummer.tracker.ui.ocr.ParsedSlip
import java.time.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EntryViewModelTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), ZoneOffset.UTC)
    private fun model(repository: FakeMilkEntryRepository, id: Long = 0): EntryViewModel {
        val validation = ValidateEntryUseCase()
        return EntryViewModel(SavedStateHandle(mapOf("entryId" to id.toString())), repository,
            SaveEntryUseCase(repository, validation, clock), validation, clock)
    }

    @Test fun newFormLeavesShiftBlankEvenWhenMorningExists() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeMilkEntryRepository()
            repository.saveEntry(sampleEntry(LocalDate.now(clock)))
            val viewModel = model(repository)
            advanceUntilIdle()
            assertNull(viewModel.state.value.shift)
            assertEquals(LocalDate.now(clock), viewModel.state.value.date)
            assertFalse(viewModel.state.value.loading)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun blankFormDisplaysFieldErrorsWithoutSaving() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeMilkEntryRepository()
            val viewModel = model(repository)
            advanceUntilIdle()
            viewModel.save()
            assertTrue(viewModel.state.value.errors.keys.containsAll(listOf("quantity", "fat", "snf", "rate")))
            assertTrue(repository.entries.value.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

    @Test fun scanCorrectionSurvivesRecompositionAndIsWhatGetsSaved() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeMilkEntryRepository()
            val viewModel = model(repository)
            advanceUntilIdle()
            val slip = ParsedSlip(5.2, 6.8, 8.8, 38.5, 280.2)
            viewModel.prefillScan("capture-1", slip)
            viewModel.field("fat", "6.5")
            viewModel.prefillScan("capture-1", slip)
            assertEquals("6.5", viewModel.state.value.fat)
            assertFalse(viewModel.state.value.comparison!!.matches)
            viewModel.save()
            viewModel.save()
            advanceUntilIdle()
            assertEquals(1, repository.entries.value.size)
            assertEquals(6.5, repository.entries.value.single().fat, 0.000001)
            assertEquals(280.2, repository.entries.value.single().slipTotal!!, 0.000001)
            assertNotNull(viewModel.state.value.saved)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun editPrefillsAndUpdatesSameEntry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeMilkEntryRepository()
            val original = repository.saveEntry(sampleEntry().copy(slipTotal = 200.2))
            val viewModel = model(repository, original.id)
            advanceUntilIdle()
            assertEquals(original.date, viewModel.state.value.date)
            assertEquals("200.2", viewModel.state.value.slipTotal)
            viewModel.field("quantity", "5.5")
            viewModel.save()
            advanceUntilIdle()
            assertEquals(1, repository.entries.value.size)
            assertEquals(original.id, repository.entries.value.single().id)
            assertEquals(original.portableId, repository.entries.value.single().portableId)
            assertEquals(211.75, repository.entries.value.single().total, 0.000001)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun scanWaitsForInitializationAndSameScanKeepsClearedShift() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeMilkEntryRepository()
            val viewModel = model(repo)
            val slip = ParsedSlip(5.2, 6.5, 8.8, 38.5, 200.2, Shift.EVENING)
            // The initialization coroutine has not run yet.
            viewModel.prefillScan("scan-1", slip)
            advanceUntilIdle()
            assertEquals(Shift.EVENING, viewModel.state.value.shift)
            viewModel.selectShift(null)
            viewModel.prefillScan("scan-1", slip)
            assertNull(viewModel.state.value.shift)
            viewModel.prefillScan("scan-2", slip.copy(shift = Shift.MORNING))
            assertEquals(Shift.MORNING, viewModel.state.value.shift)
            viewModel.prefillScan("scan-3", slip.copy(shift = null))
            assertNull(viewModel.state.value.shift)
            viewModel.save()
            advanceUntilIdle()
            assertNull(repo.entries.value.single().shift)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun reminderHintCanBeClearedAndKnownConflictDoesNotSave() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeMilkEntryRepository()
            repo.saveEntry(sampleEntry(LocalDate.now(clock)))
            val validate = ValidateEntryUseCase()
            val viewModel = EntryViewModel(SavedStateHandle(mapOf("shift" to "MORNING")), repo,
                SaveEntryUseCase(repo, validate, clock), validate, clock)
            advanceUntilIdle()
            assertEquals(Shift.MORNING, viewModel.state.value.shift)
            viewModel.prefill(5.2, 6.5, 8.8, 38.5, null)
            viewModel.save()
            advanceUntilIdle()
            assertTrue(viewModel.state.value.duplicate)
            assertEquals(1, repo.entries.value.size)
            viewModel.dismissError()
            viewModel.selectShift(null)
            viewModel.save()
            advanceUntilIdle()
            assertEquals(2, repo.entries.value.size)
            assertNull(viewModel.state.value.saved!!.shift)
        } finally { Dispatchers.resetMain() }
    }
}
