package com.milksummer.tracker

import com.milksummer.tracker.data.repository.*
import com.milksummer.tracker.domain.model.*
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMilkEntryRepository : MilkEntryRepository {
    val entries = MutableStateFlow<List<MilkEntry>>(emptyList())
    var failWithDuplicate = false

    override suspend fun saveEntry(entry: MilkEntry): MilkEntry {
        if (failWithDuplicate || entries.value.any { it.portableId == entry.portableId ||
                (entry.shift != null && it.date == entry.date && it.shift == entry.shift) }) {
            throw DuplicateEntryException()
        }
        val saved = entry.copy(id = (entries.value.maxOfOrNull { it.id } ?: 0) + 1)
        entries.value += saved
        return saved
    }

    override suspend fun updateEntry(entry: MilkEntry): MilkEntry {
        if (entries.value.none { it.id == entry.id }) throw EntryNotFoundException()
        entries.value = entries.value.map { if (it.id == entry.id) entry else it }
        return entry
    }

    override suspend fun deleteEntry(id: Long) { entries.value = entries.value.filterNot { it.id == id } }
    override suspend fun getEntry(id: Long) = entries.value.find { it.id == id }
    override suspend fun getEntry(date: LocalDate, shift: Shift) = entries.value.find { it.date == date && it.shift == shift }
    override fun getAll() = entries
    override fun getEntriesByDateRange(start: LocalDate, end: LocalDate) = entries.map { rows ->
        rows.filter { it.date in start..end }
    }
    override fun getMonthlySummary(month: YearMonth) = entries.map {
        summarizeEntries(it, month.atDay(1), month.atEndOfMonth())
    }
    override fun getWeeklySummary(start: LocalDate, end: LocalDate) = entries.map { summarizeEntries(it, start, end) }
}

fun sampleEntry(
    date: LocalDate = LocalDate.of(2026, 10, 1),
    shift: Shift? = Shift.MORNING
) = MilkEntry(date, shift, quantity = 5.2, fat = 6.5, snf = 8.8, rate = 38.50)
