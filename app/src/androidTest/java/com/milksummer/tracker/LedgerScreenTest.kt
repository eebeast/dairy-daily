package com.milksummer.tracker

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.milksummer.tracker.data.local.MilkDatabase
import com.milksummer.tracker.data.repository.RoomMilkEntryRepository
import com.milksummer.tracker.domain.model.*
import com.milksummer.tracker.ui.*
import com.milksummer.tracker.ui.theme.MilkSummerTheme
import java.time.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LedgerScreenTest {
    @get:Rule(order = 0) val unlockedDevice = UnlockedDeviceRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private lateinit var database: MilkDatabase
    private lateinit var repository: RoomMilkEntryRepository
    private lateinit var viewModel: LedgerViewModel
    private val clock = Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.now(clock)
    private var selectedEntry: Long? = null

    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MilkDatabase::class.java).build()
        repository = RoomMilkEntryRepository(database.milkEntryDao())
        compose.runOnUiThread { viewModel = LedgerViewModel(repository, clock) }
    }

    @After fun close() { database.close() }

    private fun show(home: Boolean, day: LocalDate? = null) {
        compose.setContent {
            MilkSummerTheme {
                val state by viewModel.state.collectAsState()
                var pendingDelete by remember { mutableStateOf<MilkEntry?>(null) }
                if (home) HomeScreen(state, {}, {}, { selectedEntry = it }, { pendingDelete = it })
                else HistoryScreen(state, { selectedEntry = it }, { pendingDelete = it }, day)
                pendingDelete?.let { entry ->
                    DeleteEntryDialog(entry,
                        onConfirm = { viewModel.delete(entry); pendingDelete = null },
                        onDismiss = { pendingDelete = null })
                }
            }
        }
        compose.waitUntil(5000) { !viewModel.state.value.loading }
    }

    private fun entry(date: LocalDate = today, shift: Shift? = Shift.MORNING, quantity: Double = 5.2, rate: Double = 38.5) =
        MilkEntry(date, shift, quantity, 6.5, 8.8, rate)

    @Test fun homeUpdatesFromEmptyToMixedShiftSalesAndMonthTotals() {
        show(home = true)
        compose.onNodeWithText("No milk sales recorded today.").assertIsDisplayed()
        compose.onNodeWithText("Today's shifts").assertDoesNotExist()
        compose.onNodeWithText("Add morning entry").assertDoesNotExist()
        compose.onNodeWithText("Add evening entry").assertDoesNotExist()
        compose.onNodeWithText("Add Entry").assertIsDisplayed()
        compose.onNodeWithText("Scan Slip").assertIsDisplayed()
        runBlocking {
            repository.saveEntry(entry())
            repository.saveEntry(entry(shift = null, quantity = 4.8, rate = 38.0))
            repository.saveEntry(entry(shift = null, quantity = 1.0, rate = 40.0))
        }
        compose.waitUntil(5000) { viewModel.state.value.entries.size == 3 }
        compose.onNodeWithText("Morning: no entry recorded").assertDoesNotExist()
        compose.onNodeWithText("Evening: no entry recorded").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Today: 11.00 L · ${money(422.6)}"))
        compose.onNodeWithText("Today: 11.00 L · ${money(422.6)}").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Month to date"))
        compose.onNodeWithText("11.00 liters").assertIsDisplayed()
        compose.onNodeWithText(money(422.6)).assertIsDisplayed()
    }

    @Test fun unknownShiftRowsRemainSeparateAndActionsTargetTheCorrectSale() {
        val first = runBlocking { repository.saveEntry(entry(shift = null, quantity = 1.0)) }
        val second = runBlocking { repository.saveEntry(entry(shift = null, quantity = 2.0)) }
        show(home = false, day = today)
        compose.waitUntil(5000) { viewModel.state.value.entries.size == 2 }
        compose.onNodeWithText("No morning entry recorded.").assertDoesNotExist()
        compose.onNodeWithText("No evening entry recorded.").assertDoesNotExist()
        compose.onNodeWithText("1.00 L × ${money(38.5)} = ${money(38.5)}").performClick()
        compose.runOnIdle { assertEquals(first.id, selectedEntry) }
        compose.onAllNodesWithText("Delete")[1].performClick()
        compose.onNodeWithText("Delete the entry for $today? This cannot be undone.").assertIsDisplayed()
        compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(5000) { viewModel.state.value.entries.size == 1 }
        assertNotNull(runBlocking { repository.getEntry(first.id) })
        assertNull(runBlocking { repository.getEntry(second.id) })
    }

    @Test fun historyGroupsNewestDayFirstAndSelectsTheCorrectEntryForEditing() {
        val morning = runBlocking {
            repository.saveEntry(entry(today.minusDays(1), quantity = 1.0, rate = 40.0))
            repository.saveEntry(entry(shift = Shift.EVENING, quantity = 4.8, rate = 38.0))
            repository.saveEntry(entry())
        }
        show(home = false)
        compose.waitUntil(5000) { viewModel.state.value.entries.size == 3 }
        compose.onNodeWithText("$today · 10.00 L · ${money(382.6)}").assertIsDisplayed()
        compose.onNodeWithText("5.20 L × ${money(38.5)} = ${money(200.2)}").performClick()
        compose.runOnIdle { assertEquals(morning.id, selectedEntry) }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("${today.minusDays(1)} · 1.00 L · ${money(40.0)}"))
        compose.onNodeWithText("${today.minusDays(1)} · 1.00 L · ${money(40.0)}").assertIsDisplayed()
    }

    @Test fun cancellingDeletePreservesTheRowAndConfirmingRemovesIt() {
        val stored = runBlocking { repository.saveEntry(entry()) }
        show(home = false)
        compose.waitUntil(5000) { viewModel.state.value.entries.size == 1 }
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete entry?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertNotNull(runBlocking { repository.getEntry(stored.id) })
        compose.onNodeWithText("Delete entry?").assertDoesNotExist()
        compose.onNodeWithText("Delete").performClick()
        compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(5000) { viewModel.state.value.entries.isEmpty() }
        assertNull(runBlocking { repository.getEntry(stored.id) })
        compose.onNodeWithText("No entries recorded yet.").assertIsDisplayed()
    }
}
