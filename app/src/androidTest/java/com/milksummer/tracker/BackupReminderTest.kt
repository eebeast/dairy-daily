package com.milksummer.tracker

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.milksummer.tracker.data.backup.BackupReminders
import com.milksummer.tracker.data.repository.ReminderPreferences
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class BackupReminderTest {
    @Test fun restoresTimesTogetherWithoutTransferringNotificationRationaleState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File.createTempFile("backup-settings-", ".preferences_pb", context.cacheDir).apply { delete() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            val preferences = ReminderPreferences(store)
            preferences.markNotificationAsked()
            preferences.restore(BackupReminders(false, LocalTime.of(6, 30), LocalTime.of(18, 45)))
            val restored = preferences.settings.first()
            assertFalse(restored.enabled)
            assertEquals(LocalTime.of(6, 30), restored.morning)
            assertEquals(LocalTime.of(18, 45), restored.evening)
            assertEquals(LocalDate.now().plusDays(1), restored.morningFrom)
            assertEquals(restored.morningFrom, restored.eveningFrom)
            assertTrue(restored.notificationAsked)
            preferences.restore(BackupReminders(true, LocalTime.of(7, 0), LocalTime.of(19, 0)))
            assertTrue(preferences.settings.first().enabled)
            assertTrue(preferences.settings.first().notificationAsked)
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }
}
