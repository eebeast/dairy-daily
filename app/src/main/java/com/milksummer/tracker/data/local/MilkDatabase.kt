package com.milksummer.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.milksummer.tracker.data.local.entity.MilkEntryEntity

@Database(entities = [MilkEntryEntity::class], version = 1, exportSchema = true)
@TypeConverters(MilkEntryConverters::class)
abstract class MilkDatabase : RoomDatabase() {
    abstract fun milkEntryDao(): MilkEntryDao
}
