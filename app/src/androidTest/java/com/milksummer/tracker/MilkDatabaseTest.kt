package com.milksummer.tracker

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.milksummer.tracker.data.local.MilkDatabase
import com.milksummer.tracker.data.local.entity.MilkEntryEntity
import com.milksummer.tracker.domain.model.Shift
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MilkDatabaseTest {
    private lateinit var database: MilkDatabase

    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MilkDatabase::class.java).build()
    }

    @After fun close() { database.close() }

    private fun entry(shift: Shift? = Shift.MORNING) = MilkEntryEntity(
        date = LocalDate.of(2026, 10, 24), shift = shift, quantity = 5.2,
        fat = 6.5, snf = 8.8, rate = 38.5, total = 200.2, slipTotal = null, createdAt = 100, updatedAt = 100
    )

    @Test fun uniqueDateAndShiftConstraintRejectsDuplicateAndPreservesOriginal() = runBlocking {
        val dao = database.milkEntryDao()
        val id = dao.insert(entry())
        try { dao.insert(entry()); fail("Duplicate must be rejected by SQLite") }
        catch (_: SQLiteConstraintException) { }
        assertEquals(id, dao.getByDateAndShift(entry().date, Shift.MORNING)!!.id)
        dao.insert(entry(Shift.EVENING))
        assertEquals(2, dao.getByMonth(2026, 10).first().size)
        assertEquals(2, dao.getByWeek(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 25)).first().size)
        assertTrue(dao.getByMonth(2026, 9).first().isEmpty())
    }

    @Test fun editingAndDeletingAreReflectedInReactiveQueries() = runBlocking {
        val dao = database.milkEntryDao()
        val original = entry()
        val id = dao.insert(original)
        val stored = dao.getById(id)!!
        assertEquals(original.copy(id = id), stored)
        dao.update(stored.copy(quantity = 5.5, total = 211.75, updatedAt = 200))
        val edited = dao.getByDateRange(stored.date, stored.date).first().single()
        assertEquals(211.75, edited.total, 0.000001)
        assertEquals(100L, edited.createdAt)
        dao.delete(id)
        assertNull(dao.getById(id))
        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test fun nullShiftsAllowDistinctSalesButPortableIdsAreUnique() = runBlocking {
        val dao = database.milkEntryDao()
        val first = entry(null)
        val id = dao.insert(first)
        dao.insert(entry(null))
        dao.insert(entry())
        dao.insert(entry(Shift.EVENING))
        assertEquals(id, dao.getByPortableId(first.portableId)!!.id)
        try { dao.insert(first.copy(date = first.date.plusDays(1))); fail("Repeated identity accepted") }
        catch (_: SQLiteConstraintException) { }
        assertEquals(4, dao.getAll().first().size)
    }
}
