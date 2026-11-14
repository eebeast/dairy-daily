package com.milksummer.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.milksummer.tracker.data.local.entity.MilkEntryEntity

@Database(entities = [MilkEntryEntity::class], version = 2, exportSchema = true)
@TypeConverters(MilkEntryConverters::class)
abstract class MilkDatabase : RoomDatabase() {
    abstract fun milkEntryDao(): MilkEntryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE milk_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL, shift TEXT, quantity REAL NOT NULL,
                        fat REAL NOT NULL, snf REAL NOT NULL, rate REAL NOT NULL,
                        total REAL NOT NULL, slip_total REAL,
                        created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
                        portable_id TEXT NOT NULL
                    )
                """.trimIndent())
                // Bind a UUID per row; preserve every legacy column, including local IDs.
                db.query("SELECT id FROM milk_entries").use { rows ->
                    while (rows.moveToNext()) {
                        db.execSQL("""
                            INSERT INTO milk_entries_new
                            SELECT id, date, shift, quantity, fat, snf, rate, total,
                                   slip_total, created_at, updated_at, ?
                            FROM milk_entries WHERE id = ?
                        """.trimIndent(), arrayOf<Any>(java.util.UUID.randomUUID().toString(), rows.getLong(0)))
                    }
                }
                db.execSQL("DROP TABLE milk_entries")
                db.execSQL("ALTER TABLE milk_entries_new RENAME TO milk_entries")
                db.execSQL("CREATE UNIQUE INDEX index_milk_entries_date_shift ON milk_entries(date, shift)")
                db.execSQL("CREATE UNIQUE INDEX index_milk_entries_portable_id ON milk_entries(portable_id)")
            }
        }
    }
}
