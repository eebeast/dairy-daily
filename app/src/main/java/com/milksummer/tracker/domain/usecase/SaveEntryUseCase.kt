package com.milksummer.tracker.domain.usecase

import com.milksummer.tracker.data.repository.*
import com.milksummer.tracker.domain.model.MilkEntry
import java.time.Clock
import javax.inject.Inject

sealed interface SaveEntryResult {
    data class Success(val entry: MilkEntry) : SaveEntryResult
    data class Invalid(val errors: Map<String, String>) : SaveEntryResult
    data object Duplicate : SaveEntryResult
    data object Missing : SaveEntryResult
}

class SaveEntryUseCase @Inject constructor(
    private val repository: MilkEntryRepository,
    private val validate: ValidateEntryUseCase,
    private val clock: Clock
) {
    suspend operator fun invoke(entry: MilkEntry): SaveEntryResult {
        val validation = validate(entry)
        if (!validation.isValid) return SaveEntryResult.Invalid(validation.errors)
        val previous = if (entry.id == 0L) null else {
            repository.getEntry(entry.id) ?: return SaveEntryResult.Missing
        }
        val duplicate = repository.getEntry(entry.date, entry.shift)
        if (duplicate != null && duplicate.id != entry.id) return SaveEntryResult.Duplicate
        val calculated = entry.copy(
            total = entry.quantity * entry.rate,
            createdAt = previous?.createdAt ?: clock.millis(),
            updatedAt = clock.millis()
        )
        return try {
            SaveEntryResult.Success(
                if (entry.id == 0L) repository.saveEntry(calculated) else repository.updateEntry(calculated)
            )
        } catch (_: DuplicateEntryException) {
            SaveEntryResult.Duplicate
        } catch (_: EntryNotFoundException) {
            SaveEntryResult.Missing
        }
    }
}
