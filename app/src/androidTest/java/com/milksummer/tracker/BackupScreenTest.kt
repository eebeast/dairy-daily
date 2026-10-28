package com.milksummer.tracker

import android.net.Uri
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.milksummer.tracker.data.backup.*
import com.milksummer.tracker.data.local.MilkDatabase
import com.milksummer.tracker.data.repository.ReminderSettings
import com.milksummer.tracker.domain.model.*
import com.milksummer.tracker.ui.backup.*
import com.milksummer.tracker.ui.theme.MilkSummerTheme
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BackupScreenTest {
    @get:Rule(order = 0) val unlocked = UnlockedDeviceRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private lateinit var database: MilkDatabase
    private lateinit var model: BackupViewModel
    private var input = byteArrayOf()
    private var failClose = false
    private var applied = 0
    private val uri = Uri.parse("content://test/backup.json")

    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MilkDatabase::class.java).build()
        val codec = BackupCodec()
        val clock = Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC)
        val data = BackupData(Instant.now(clock), listOf(MilkEntry(LocalDate.of(2026, 10, 26), Shift.MORNING,
            5.2, 6.5, 8.8, 38.5, createdAt = 100)), BackupReminders(false, LocalTime.of(6, 30), LocalTime.of(19, 0)))
        input = ByteArrayOutputStream().also { codec.write(data, it) }.toByteArray()
        val service = BackupService(database, codec, clock, { ReminderSettings() }, { applied++ })
        compose.runOnUiThread {
            model = BackupViewModel(service, { input.inputStream() }, {
                object : ByteArrayOutputStream() {
                    override fun close() { if (failClose) throw IOException("provider closed with failure") }
                }
            })
        }
        compose.setContent {
            val state by model.state.collectAsState()
            MilkSummerTheme { BackupContent(state, { model.export(uri) }, { model.preview(uri) },
                model::confirmRestore, model::cancelPreview, model::dismissMessage) }
        }
    }

    @After fun close() {
        compose.runOnUiThread { ViewModelStore().apply { put("backup", model); clear() } }
        database.close()
    }

    private fun preview() {
        compose.onNodeWithText("Restore backup").performClick()
        compose.waitUntil(5000) { model.state.value.preview != null }
    }

    @Test fun previewShowsDatePolicyAndSettingsAndCancellationWritesNothing() {
        preview()
        compose.onNodeWithText("Restore this backup?").assertIsDisplayed()
        compose.onNodeWithText("1 entries").assertIsDisplayed()
        compose.onNodeWithText("2026-10-26 to 2026-10-26").assertIsDisplayed()
        compose.onNodeWithText("Existing entries for the same date and shift will be kept and skipped.").assertIsDisplayed()
        compose.onNodeWithText("Reminders: off. Morning 6:30 AM, evening 7:00 PM.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Restore this backup?").assertDoesNotExist()
        assertTrue(runBlocking { database.milkEntryDao().getAll().first().isEmpty() })
        assertEquals(0, applied)
    }

    @Test fun confirmationImportsOnceAndSecondRestoreReportsSkippedEntry() {
        preview()
        compose.onNodeWithText("Restore entries").performClick()
        compose.waitUntil(5000) { model.state.value.message != null }
        compose.onNodeWithText("Restored 1 entries; skipped 0 existing entries. Reminder settings restored.").assertIsDisplayed()
        assertEquals(1, runBlocking { database.milkEntryDao().getAll().first().size })
        preview()
        compose.runOnUiThread { model.confirmRestore(); model.confirmRestore() }
        compose.waitUntil(5000) { !model.state.value.busy }
        compose.onNodeWithText("Restored 0 entries; skipped 1 existing entries. Reminder settings restored.").assertIsDisplayed()
        assertEquals(2, applied)
    }

    @Test fun cancelledPickersAndFileFailuresDoNotReportSuccessOrWriteEntries() {
        compose.runOnUiThread { model.export(null); model.preview(null) }
        assertEquals(BackupUiState(), model.state.value)
        input = "{}".toByteArray()
        compose.onNodeWithText("Restore backup").performClick()
        compose.waitUntil(5000) { model.state.value.error != null }
        compose.onNodeWithText("Restore this backup?").assertDoesNotExist()
        assertTrue(runBlocking { database.milkEntryDao().getAll().first().isEmpty() })
        failClose = true
        compose.onNodeWithText("Create backup").performClick()
        compose.waitUntil(5000) { !model.state.value.busy }
        compose.onNodeWithText("Backup could not be saved. Delete any incomplete file and try another destination.").assertIsDisplayed()
        assertNull(model.state.value.message)
    }
}
