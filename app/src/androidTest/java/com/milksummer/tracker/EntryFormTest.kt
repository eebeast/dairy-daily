package com.milksummer.tracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.milksummer.tracker.data.local.MilkDatabase
import com.milksummer.tracker.data.repository.RoomMilkEntryRepository
import com.milksummer.tracker.domain.model.*
import com.milksummer.tracker.domain.usecase.*
import com.milksummer.tracker.ui.entry.*
import com.milksummer.tracker.ui.money
import com.milksummer.tracker.ui.theme.MilkSummerTheme
import java.time.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EntryFormTest {
    @get:Rule(order = 0) val unlockedDevice = UnlockedDeviceRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private lateinit var database: MilkDatabase
    private lateinit var repository: RoomMilkEntryRepository
    private lateinit var viewModel: EntryViewModel
    private val clock = Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), ZoneOffset.UTC)

    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MilkDatabase::class.java).build()
        repository = RoomMilkEntryRepository(database.milkEntryDao())
    }
    @After fun close() { database.close() }

    private fun show(id: Long = 0) {
        val validate = ValidateEntryUseCase()
        compose.runOnUiThread {
            viewModel = EntryViewModel(SavedStateHandle(mapOf("entryId" to id.toString())), repository,
                SaveEntryUseCase(repository, validate, clock), validate, clock)
        }
        compose.setContent { MilkSummerTheme { EntryScreen({}, {}, viewModel) } }
        compose.waitUntil(5000) { !viewModel.state.value.loading }
    }

    private fun fill() {
        listOf("Quantity (liters)" to "5.2", "FAT (%)" to "6.5", "SNF (%)" to "8.8", "Rate (₹/liter)" to "38.5").forEach { (label, value) ->
            compose.onNodeWithText(label).performScrollTo().performTextInput(value)
        }
    }

    @Test fun validFormSavesThroughRoomAndShowsSuccess() {
        show()
        compose.onNodeWithText("No shift").assertIsSelected()
        fill()
        val save = compose.onNodeWithText("Save entry").performScrollTo()
        // Keep the transient snackbar on screen while checking its semantics.
        // Room completes independently of the Compose animation clock.
        compose.mainClock.autoAdvance = false
        try {
            save.performClick()
            compose.waitUntil(5000) { viewModel.state.value.saved != null }
            compose.mainClock.advanceTimeBy(500)
            compose.onNodeWithText("Entry saved").assertIsDisplayed()
            val stored = runBlocking { repository.getAll().first().single() }
            assertNotNull(stored)
            assertEquals(200.2, stored.total, 0.000001)
            assertNull(stored.shift)
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test fun missingValuesShowInlineErrorsAndDoNotSave() {
        show()
        compose.onNodeWithText("Save entry").performScrollTo().performClick()
        compose.onNodeWithText("Quantity must be greater than zero.").assertExists()
        assertNull(runBlocking { repository.getEntry(LocalDate.now(clock), Shift.MORNING) })
    }

    @Test fun editLoadsValuesAndRecalculatesStoredTotal() {
        val original = runBlocking { repository.saveEntry(MilkEntry(LocalDate.now(clock), Shift.MORNING, 5.2, 6.5, 8.8, 38.5)) }
        show(original.id)
        compose.onNodeWithText("Quantity (liters)").performScrollTo().performTextReplacement("5.5")
        compose.onNodeWithText("Update").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.state.value.saved != null }
        assertEquals(211.75, runBlocking { repository.getEntry(original.id) }!!.total, 0.000001)
    }

    @Test fun slipComparisonShowsMatchAtToleranceAndWarningAboveIt() {
        show(); fill()
        val slip = compose.onNodeWithText("Slip total (optional, ₹)")
        slip.performScrollTo().performTextInput("200.70")
        compose.onNodeWithText("✓ Slip total matches").performScrollTo().assertIsDisplayed()
        slip.performScrollTo().performTextReplacement("200.71")
        compose.onNodeWithText("⚠ Difference: ${money(0.51)}. Verify the slip before saving.").performScrollTo().assertIsDisplayed()
        assertNull(runBlocking { repository.getEntry(LocalDate.now(clock), Shift.MORNING) })
    }

    @Test fun invalidFieldsAndDuplicateDialogAreRenderedWithoutAnotherSave() {
        runBlocking { repository.saveEntry(MilkEntry(LocalDate.now(clock), Shift.MORNING, 5.2, 6.5, 8.8, 38.5)) }
        show()
        compose.onNodeWithText("Morning").performClick()
        listOf("Quantity (liters)" to "0", "FAT (%)" to "16", "SNF (%)" to "16", "Rate (₹/liter)" to "0").forEach { (label, value) ->
            compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)
        }
        compose.onNodeWithText("Save entry").performScrollTo().performClick()
        listOf("Quantity must be greater than zero.", "FAT must be between 0 and 15.", "SNF must be between 0 and 15.", "Rate must be greater than zero.").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        listOf("Quantity (liters)" to "5.2", "FAT (%)" to "6.5", "SNF (%)" to "8.8", "Rate (₹/liter)" to "38.5").forEach { (label, value) ->
            compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)
        }
        compose.onNodeWithText("Save entry").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.state.value.duplicate }
        compose.onNodeWithText("Entry already exists").assertIsDisplayed()
        assertEquals(1, runBlocking { repository.getAll().first() }.size)
    }
}
