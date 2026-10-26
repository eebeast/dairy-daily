package com.milksummer.tracker.di

import com.milksummer.tracker.data.repository.MilkEntryRepository
import com.milksummer.tracker.data.repository.RoomMilkEntryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun repository(implementation: RoomMilkEntryRepository): MilkEntryRepository
}
