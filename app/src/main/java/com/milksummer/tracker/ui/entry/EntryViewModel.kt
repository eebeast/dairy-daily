package com.milksummer.tracker.ui.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milksummer.tracker.data.repository.MilkEntryRepository
import com.milksummer.tracker.domain.model.*
import com.milksummer.tracker.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntryUiState(
    val id: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val shift: Shift? = null,
    val quantity: String = "",
    val fat: String = "",
    val snf: String = "",
    val rate: String = "",
    val slipTotal: String = "",
    val errors: Map<String, String> = emptyMap(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val duplicate: Boolean = false,
    val message: String? = null,
    val saved: MilkEntry? = null
) {
    val calculatedTotal: Double? get() = quantity.toDoubleOrNull()?.let { q ->
        rate.toDoubleOrNull()?.let { r -> (q * r).takeIf { it.isFinite() } }
    }
    val comparison: SlipComparison? get() {
        val q = quantity.toDoubleOrNull() ?: return null
        val r = rate.toDoubleOrNull() ?: return null
        val slip = slipTotal.toDoubleOrNull() ?: return null
        return compareSlip(q, r, slip)
    }
}

@HiltViewModel
class EntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MilkEntryRepository,
    private val saveEntry: SaveEntryUseCase,
    private val validate: ValidateEntryUseCase,
    clock: Clock
) : ViewModel() {
    private val mutableState = MutableStateFlow(EntryUiState(date = LocalDate.now(clock)))
    val state = mutableState.asStateFlow()
    private var lastScanId: String? = null
    private var pendingScan: Pair<String, com.milksummer.tracker.ui.ocr.ParsedSlip>? = null

    init {
        viewModelScope.launch {
            try {
                val id = savedStateHandle.get<String>("entryId")?.toLongOrNull() ?: 0L
                if (id > 0) {
                    mutableState.update { it.copy(id = id) }
                    val entry = repository.getEntry(id)
                    if (entry == null) mutableState.update { it.copy(loading = false, message = "This entry no longer exists.") }
                    else mutableState.value = EntryUiState(
                        id = entry.id, date = entry.date, shift = entry.shift,
                        quantity = entry.quantity.toString(), fat = entry.fat.toString(), snf = entry.snf.toString(),
                        rate = entry.rate.toString(), slipTotal = entry.slipTotal?.toString().orEmpty(), loading = false
                    )
                } else {
                    val requested = savedStateHandle.get<String>("shift")?.let { value -> Shift.entries.find { it.name == value } }
                    mutableState.update { it.copy(shift = requested, loading = false) }
                }
                pendingScan?.let { (scanId, fields) ->
                    pendingScan = null
                    prefillScan(scanId, fields)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.update { it.copy(loading = false, message = "Could not load entry. Please try again.") }
            }
        }
    }

    fun field(name: String, value: String) {
        mutableState.update {
            val next = when (name) {
                "quantity" -> it.copy(quantity = value.replace(',', '.'))
                "fat" -> it.copy(fat = value.replace(',', '.'))
                "snf" -> it.copy(snf = value.replace(',', '.'))
                "rate" -> it.copy(rate = value.replace(',', '.'))
                "slipTotal" -> it.copy(slipTotal = value.replace(',', '.'))
                else -> it
            }
            next.copy(errors = it.errors - name)
        }
    }

    fun selectDate(date: LocalDate) { mutableState.update { it.copy(date = date) } }
    fun selectShift(shift: Shift?) { mutableState.update { it.copy(shift = shift) } }
    fun dismissError() { mutableState.update { it.copy(duplicate = false, message = null) } }

    fun prefill(quantity: Double?, fat: Double?, snf: Double?, rate: Double?, total: Double?) {
        mutableState.update { it.copy(
            quantity = quantity?.toString().orEmpty(), fat = fat?.toString().orEmpty(),
            snf = snf?.toString().orEmpty(), rate = rate?.toString().orEmpty(),
            slipTotal = total?.toString().orEmpty(), errors = emptyMap()
        ) }
    }

    fun prefillScan(scanId: String, fields: com.milksummer.tracker.ui.ocr.ParsedSlip) {
        if (mutableState.value.loading) {
            pendingScan = scanId to fields
            return
        }
        if (lastScanId == scanId) return
        lastScanId = scanId
        prefill(fields.quantity, fields.fat, fields.snf, fields.rate, fields.total)
        mutableState.update { it.copy(shift = fields.shift) }
    }

    fun save() {
        val current = mutableState.value
        if (current.saving || current.loading) return
        val entry = MilkEntry(
            date = current.date, shift = current.shift, id = current.id,
            quantity = current.quantity.toDoubleOrNull() ?: Double.NaN,
            fat = current.fat.toDoubleOrNull() ?: Double.NaN,
            snf = current.snf.toDoubleOrNull() ?: Double.NaN,
            rate = current.rate.toDoubleOrNull() ?: Double.NaN,
            slipTotal = if (current.slipTotal.isBlank()) null else current.slipTotal.toDoubleOrNull() ?: Double.NaN
        )
        val validation = validate(entry)
        if (!validation.isValid) {
            mutableState.update { it.copy(errors = validation.errors) }
            return
        }
        mutableState.update { it.copy(saving = true, errors = emptyMap()) }
        viewModelScope.launch {
            try {
                when (val result = saveEntry(entry)) {
                    is SaveEntryResult.Success -> mutableState.update { it.copy(saved = result.entry) }
                    is SaveEntryResult.Invalid -> mutableState.update { it.copy(errors = result.errors) }
                    SaveEntryResult.Duplicate -> mutableState.update { it.copy(duplicate = true) }
                    SaveEntryResult.Missing -> mutableState.update { it.copy(message = "This entry no longer exists.") }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.update { it.copy(message = "Could not save entry. Your form is preserved; please try again.") }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }
}
