package com.milksummer.tracker

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.milksummer.tracker.data.local.MilkDatabase
import com.milksummer.tracker.data.repository.toEntity
import com.milksummer.tracker.domain.model.MilkEntry
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MilkMigrationTest {
    @Test fun populatedVersionOneMigratesWithoutLosingAnyFields() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${UUID.randomUUID()}.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { legacy ->
                legacy.execSQL("""CREATE TABLE milk_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date TEXT NOT NULL, shift TEXT NOT NULL, quantity REAL NOT NULL,
                    fat REAL NOT NULL, snf REAL NOT NULL, rate REAL NOT NULL, total REAL NOT NULL,
                    slip_total REAL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)""")
                legacy.execSQL("CREATE UNIQUE INDEX index_milk_entries_date_shift ON milk_entries(date, shift)")
                legacy.execSQL("INSERT INTO milk_entries VALUES(42, '2026-10-24', 'MORNING', 5.2, 6.5, 8.8, 38.5, 200.2, 201.0, 100, 200)")
                legacy.execSQL("INSERT INTO milk_entries VALUES(43, '2026-10-24', 'EVENING', 4.8, 6.0, 8.0, 38.0, 182.4, NULL, 300, 400)")
                legacy.version = 1
            }
            val db = Room.databaseBuilder(context, MilkDatabase::class.java, name)
                .addMigrations(MilkDatabase.MIGRATION_1_2).build()
            try {
                    val dao = db.milkEntryDao()
                    val first = dao.getById(42)!!
                    assertEquals("MORNING", first.shift!!.name)
                    assertEquals(LocalDate.of(2026, 10, 24), first.date)
                    assertEquals(listOf(5.2, 6.5, 8.8, 38.5, 200.2, 201.0),
                        listOf(first.quantity, first.fat, first.snf, first.rate, first.total, first.slipTotal))
                    assertEquals(100L, first.createdAt)
                    assertEquals(200L, first.updatedAt)
                    val second = dao.getById(43)!!
                    assertEquals("EVENING", second.shift!!.name)
                    assertEquals(first.date, second.date)
                    assertEquals(listOf(4.8, 6.0, 8.0, 38.0, 182.4),
                        listOf(second.quantity, second.fat, second.snf, second.rate, second.total))
                    assertNull(second.slipTotal)
                    assertEquals(300L, second.createdAt)
                    assertEquals(400L, second.updatedAt)
                    assertNotEquals(UUID.fromString(first.portableId), UUID.fromString(second.portableId))
                    repeat(2) { dao.insert(MilkEntry(first.date, null, 1.0, 6.0, 8.0, 40.0).toEntity()) }
                    assertEquals(4, dao.getAll().first().size)
                    try { dao.insert(first.copy(id = 0, portableId = UUID.randomUUID().toString())); fail("Known duplicate accepted") }
                    catch (_: android.database.sqlite.SQLiteConstraintException) { }
            } finally { db.close() }
            val reopened = Room.databaseBuilder(context, MilkDatabase::class.java, name).build()
            try { assertEquals(4, reopened.milkEntryDao().getAll().first().size) }
            finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }
}
