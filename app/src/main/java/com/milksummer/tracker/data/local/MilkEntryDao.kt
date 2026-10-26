package com.milksummer.tracker.data.local

import androidx.room.*
import com.milksummer.tracker.data.local.entity.MilkEntryEntity
import com.milksummer.tracker.domain.model.Shift
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface MilkEntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: MilkEntryEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(entry: MilkEntryEntity): Int

    @Query("DELETE FROM milk_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM milk_entries WHERE id = :id")
    suspend fun getById(id: Long): MilkEntryEntity?

    @Query("SELECT * FROM milk_entries WHERE date = :date AND shift = :shift LIMIT 1")
    suspend fun getByDateAndShift(date: LocalDate, shift: Shift): MilkEntryEntity?

    @Query("SELECT * FROM milk_entries WHERE date BETWEEN :start AND :end ORDER BY date DESC, shift DESC")
    fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<MilkEntryEntity>>

    @Query("SELECT * FROM milk_entries WHERE strftime('%Y-%m', date) = printf('%04d-%02d', :year, :month) ORDER BY date DESC, shift DESC")
    fun getByMonth(year: Int, month: Int): Flow<List<MilkEntryEntity>>

    @Query("SELECT * FROM milk_entries WHERE date BETWEEN :start AND :end ORDER BY date DESC, shift DESC")
    fun getByWeek(start: LocalDate, end: LocalDate): Flow<List<MilkEntryEntity>>

    @Query("SELECT * FROM milk_entries ORDER BY date DESC, shift DESC")
    fun getAll(): Flow<List<MilkEntryEntity>>
}
