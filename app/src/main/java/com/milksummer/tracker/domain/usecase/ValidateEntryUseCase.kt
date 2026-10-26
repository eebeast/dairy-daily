package com.milksummer.tracker.domain.usecase

import com.milksummer.tracker.domain.model.MilkEntry
import javax.inject.Inject

data class EntryValidation(val errors: Map<String, String>) {
    val isValid: Boolean get() = errors.isEmpty()
}

class ValidateEntryUseCase @Inject constructor() {
    operator fun invoke(entry: MilkEntry): EntryValidation = EntryValidation(buildMap {
        if (!entry.quantity.isFinite() || entry.quantity <= 0) put("quantity", "Quantity must be greater than zero.")
        if (!entry.fat.isFinite() || entry.fat !in 0.0..15.0) put("fat", "FAT must be between 0 and 15.")
        if (!entry.snf.isFinite() || entry.snf !in 0.0..15.0) put("snf", "SNF must be between 0 and 15.")
        if (!entry.rate.isFinite() || entry.rate <= 0) put("rate", "Rate must be greater than zero.")
        if (!(entry.quantity * entry.rate).isFinite()) put("rate", "Quantity × rate is too large.")
        if (entry.slipTotal != null && (!entry.slipTotal.isFinite() || entry.slipTotal < 0)) {
            put("slipTotal", "Slip total must be a valid non-negative amount.")
        }
    })
}
