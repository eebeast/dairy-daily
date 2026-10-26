package com.milksummer.tracker.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.milksummer.tracker.data.local.MilkEntryDao
import com.milksummer.tracker.data.local.entity.MilkEntryEntity
import com.milksummer.tracker.domain.model.*
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DuplicateEntryException : Exception("An entry already exists for this date and shift.")
class EntryNotFoundException : Exception("This entry no longer exists.")

interface MilkEntryRepository {
    suspend fun saveEntry(entry: MilkEntry): MilkEntry
    suspend fun updateEntry(entry: MilkEntry): MilkEntry
    suspend fun deleteEntry(id: Long)
    suspend fun getEntry(id: Long): MilkEntry?
    suspend fun getEntry(date: LocalDate, shift: Shift): MilkEntry?
    fun getAll(): Flow<List<MilkEntry>>
    fun getEntriesByDateRange(start: LocalDate, end: LocalDate): Flow<List<MilkEntry>>
    fun getMonthlySummary(month: YearMonth): Flow<MilkSummary>
    fun getWeeklySummary(start: LocalDate, end: LocalDate): Flow<MilkSummary>
}

class RoomMilkEntryRepository @Inject constructor(private val dao: MilkEntryDao) : MilkEntryRepository {
    override suspend fun saveEntry(entry: MilkEntry): MilkEntry = constraintChecked {
        val calculated = entry.copy(total = entry.quantity * entry.rate)
        calculated.copy(id = dao.insert(calculated.toEntity()))
    }

    override suspend fun updateEntry(entry: MilkEntry): MilkEntry = constraintChecked {
        val calculated = entry.copy(total = entry.quantity * entry.rate)
        if (dao.update(calculated.toEntity()) == 0) throw EntryNotFoundException()
        calculated
    }

    override suspend fun deleteEntry(id: Long) = dao.delete(id)
    override suspend fun getEntry(id: Long) = dao.getById(id)?.toDomain()
    override suspend fun getEntry(date: LocalDate, shift: Shift) = dao.getByDateAndShift(date, shift)?.toDomain()
    override fun getAll() = dao.getAll().map { rows -> rows.map { it.toDomain() } }
    override fun getEntriesByDateRange(start: LocalDate, end: LocalDate) =
        dao.getByDateRange(start, end).map { rows -> rows.map { it.toDomain() } }

    override fun getMonthlySummary(month: YearMonth) = dao.getByMonth(month.year, month.monthValue).map { rows ->
        summarizeEntries(rows.map { it.toDomain() }, month.atDay(1), month.atEndOfMonth())
    }

    override fun getWeeklySummary(start: LocalDate, end: LocalDate) = dao.getByWeek(start, end).map { rows ->
        summarizeEntries(rows.map { it.toDomain() }, start, end)
    }

    private suspend fun <T> constraintChecked(block: suspend () -> T): T = try {
        block()
    } catch (_: SQLiteConstraintException) {
        // The database UNIQUE constraint also protects against concurrent saves.
        throw DuplicateEntryException()
    }
}

fun MilkEntryEntity.toDomain() = MilkEntry(
    date = date, shift = shift, quantity = quantity, fat = fat, snf = snf, rate = rate,
    id = id, total = total, slipTotal = slipTotal, createdAt = createdAt, updatedAt = updatedAt
)

fun MilkEntry.toEntity() = MilkEntryEntity(
    id = id, date = date, shift = shift, quantity = quantity, fat = fat, snf = snf,
    rate = rate, total = total, slipTotal = slipTotal, createdAt = createdAt, updatedAt = updatedAt
)
