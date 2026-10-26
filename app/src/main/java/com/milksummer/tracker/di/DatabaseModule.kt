package com.milksummer.tracker.di

import android.content.Context
import androidx.room.Room
import com.milksummer.tracker.data.local.MilkDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MilkDatabase =
        Room.databaseBuilder(context, MilkDatabase::class.java, "milk_entries.db").build()

    @Provides
    fun dao(database: MilkDatabase) = database.milkEntryDao()

    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()
}
