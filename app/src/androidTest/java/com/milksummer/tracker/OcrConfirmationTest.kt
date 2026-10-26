package com.milksummer.tracker

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.milksummer.tracker.data.local.MilkDatabase
import com.milksummer.tracker.data.repository.RoomMilkEntryRepository
import com.milksummer.tracker.domain.model.Shift
import com.milksummer.tracker.domain.usecase.SaveEntryUseCase
import com.milksummer.tracker.domain.usecase.ValidateEntryUseCase
import com.milksummer.tracker.ui.entry.EntryViewModel
import com.milksummer.tracker.ui.money
import com.milksummer.tracker.ui.ocr.OcrConfirmScreen
import com.milksummer.tracker.ui.ocr.ParsedSlip
import com.milksummer.tracker.ui.theme.MilkSummerTheme
import java.time.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OcrConfirmationTest {
    @get:Rule(order = 0) val unlockedDevice = UnlockedDeviceRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private lateinit var database: MilkDatabase
    private lateinit var repository: RoomMilkEntryRepository
    private lateinit var viewModel: EntryViewModel
    private val clock = Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), ZoneOffset.UTC)

    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MilkDatabase::class.java).build()
        repository = RoomMilkEntryRepository(database.milkEntryDao())
        val validate = ValidateEntryUseCase()
        compose.runOnUiThread {
            viewModel = EntryViewModel(SavedStateHandle(), repository, SaveEntryUseCase(repository, validate, clock), validate, clock)
        }
    }

    @After fun close() { database.close() }

    private fun show(fields: ParsedSlip) {
        compose.setContent {
            MilkSummerTheme {
                OcrConfirmScreen(fields, "test-capture", {}, {}, {}, {}, viewModel)
            }
        }
        compose.waitUntil(5000) { !viewModel.state.value.loading && viewModel.state.value.quantity.isNotBlank() }
    }

    @Test fun extractedValuesRemainEditableAndCorrectedValuesAreSaved() {
        show(ParsedSlip(5.2, 6.8, 8.8, 38.5, 280.2))
        compose.onNodeWithText("FAT (%)").performScrollTo().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("6.8")))
        compose.onNodeWithText("FAT (%)").performTextReplacement("6.5")
        compose.onNodeWithText("⚠ Difference: ${money(80.0)}. Verify the slip before saving.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Retake photo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Enter manually").performScrollTo().assertIsDisplayed()
        assertNull(runBlocking { repository.getEntry(LocalDate.now(clock), Shift.MORNING) })
        compose.onNodeWithText("Confirm and save").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.state.value.saved != null }
        val saved = runBlocking { repository.getEntry(LocalDate.now(clock), Shift.MORNING) }!!
        assertEquals(6.5, saved.fat, 0.000001)
        assertEquals(200.2, saved.total, 0.000001)
        assertEquals(280.2, saved.slipTotal!!, 0.000001)
    }

    @Test fun unrecognizedFieldsStayBlankAndMustBeCompletedBeforeConfirmation() {
        show(ParsedSlip(quantity = 5.2, rate = 38.5))
        listOf("FAT (%)", "SNF (%)", "Slip total (optional, ₹)").forEach { label ->
            compose.onNodeWithText(label).performScrollTo().assert(
                SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        }
        compose.onNodeWithText("Confirm and save").performScrollTo().performClick()
        compose.onNodeWithText("FAT must be between 0 and 15.").performScrollTo().assertIsDisplayed()
        assertNull(runBlocking { repository.getEntry(LocalDate.now(clock), Shift.MORNING) })
        compose.onNodeWithText("FAT (%)").performScrollTo().performTextInput("6.5")
        compose.onNodeWithText("SNF (%)").performScrollTo().performTextInput("8.8")
        compose.onNodeWithText("Confirm and save").performScrollTo().performClick()
        compose.waitUntil(5000) { viewModel.state.value.saved != null }
        assertNull(runBlocking { repository.getEntry(LocalDate.now(clock), Shift.MORNING) }!!.slipTotal)
    }
}
