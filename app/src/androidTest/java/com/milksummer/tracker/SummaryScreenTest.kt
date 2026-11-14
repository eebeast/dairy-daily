package com.milksummer.tracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.milksummer.tracker.data.local.MilkDatabase
import com.milksummer.tracker.data.repository.RoomMilkEntryRepository
import com.milksummer.tracker.domain.model.*
import com.milksummer.tracker.domain.usecase.GetSummaryUseCase
import com.milksummer.tracker.ui.money
import com.milksummer.tracker.ui.summary.*
import com.milksummer.tracker.ui.theme.MilkSummerTheme
import java.time.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SummaryScreenTest {
    @get:Rule(order = 0) val unlockedDevice = UnlockedDeviceRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private lateinit var database: MilkDatabase
    private lateinit var viewModel: SummaryViewModel
    private lateinit var repository: RoomMilkEntryRepository
    private val clock = Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), ZoneOffset.UTC)
    private var selectedDay: LocalDate? = null

    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MilkDatabase::class.java).build()
        repository = RoomMilkEntryRepository(database.milkEntryDao())
        runBlocking {
            repository.saveEntry(MilkEntry(LocalDate.of(2026, 10, 24), Shift.MORNING, 5.2, 6.5, 8.8, 38.5))
            repository.saveEntry(MilkEntry(LocalDate.of(2026, 10, 24), Shift.EVENING, 4.8, 6.5, 8.8, 38.0))
            repository.saveEntry(MilkEntry(LocalDate.of(2026, 9, 24), Shift.MORNING, 3.0, 7.0, 9.0, 40.0))
        }
        compose.runOnUiThread { viewModel = SummaryViewModel(repository, GetSummaryUseCase(repository, clock), clock) }
        compose.setContent { MilkSummerTheme { SummaryScreen({ selectedDay = it }, viewModel) } }
        compose.waitUntil(5000) { !viewModel.state.value.loading && viewModel.state.value.summary.entryCount == 2 }
    }

    @After fun close() { database.close() }

    @Test fun monthlyTotalsMissingShiftsAndDaySelectionUseStoredData() {
        compose.onNodeWithText("10.00 liters").assertIsDisplayed()
        compose.onNodeWithText(money(382.6)).assertIsDisplayed()
        compose.onNodeWithText("Entries: 2 · Missed days: 23").assertExists()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("2026-10-24"))
        compose.onNode(hasText("2026-10-24") and hasText("Both shifts recorded")).assertExists().performClick()
        compose.runOnIdle { assertEquals(LocalDate.of(2026, 10, 24), selectedDay) }
        compose.onNodeWithText("2026-10-23").assertDoesNotExist()
        compose.onNodeWithText("Upcoming").assertDoesNotExist()
    }

    @Test fun weeklyViewUsesMondayThroughSundayAndNavigatesPeriods() {
        compose.onNodeWithText("Weekly").performClick()
        compose.waitUntil(5000) { viewModel.state.value.period.weekly && !viewModel.state.value.loading }
        compose.onNodeWithText("2026-10-19\n2026-10-25").assertIsDisplayed()
        compose.onNodeWithText("10.00 liters").assertIsDisplayed()
        compose.onNodeWithText("Entries: 2 · Missed days: 5").assertExists()
        compose.onNodeWithText("‹ Previous").performClick()
        compose.waitUntil(5000) { viewModel.state.value.period.start == LocalDate.of(2026, 10, 12) && !viewModel.state.value.loading }
        compose.onNodeWithText("0.00 liters").assertIsDisplayed()
        compose.onNodeWithText("No milk sales recorded for this period.").performScrollTo().assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToIndex(0)
        compose.onNodeWithText("Next ›").performClick()
        compose.waitUntil(5000) { viewModel.state.value.period.start == LocalDate.of(2026, 10, 19) && !viewModel.state.value.loading }
        compose.onNodeWithText("10.00 liters").assertIsDisplayed()
    }

    @Test fun sparseUnknownShiftDatesAreGroupedOrderedAndRemovedReactively() {
        val day = LocalDate.of(2026, 10, 22)
        val ids = runBlocking {
            val first = repository.saveEntry(MilkEntry(day, null, 1.0, 6.0, 8.0, 40.0))
            val second = repository.saveEntry(MilkEntry(day, null, 2.0, 6.0, 8.0, 40.0))
            repository.saveEntry(MilkEntry(day.plusDays(3), null, 99.0, 6.0, 8.0, 40.0))
            listOf(first.id, second.id)
        }
        compose.waitUntil(5000) { viewModel.state.value.summary.entryCount == 4 }
        compose.onNodeWithText("13.00 liters").assertExists()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(day.toString()))
        compose.onAllNodesWithText(day.toString()).assertCountEquals(1)
        compose.onNode(hasText(day.toString()) and hasText("3.00 L · ${money(120.0)}")).performClick()
        compose.runOnIdle { assertEquals(day, selectedDay) }
        val firstY = compose.onNodeWithText(day.toString()).fetchSemanticsNode().boundsInRoot.top
        val lastY = compose.onNodeWithText("2026-10-24").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue(firstY < lastY)
        compose.onNodeWithText("2026-10-23").assertDoesNotExist()
        compose.onNodeWithText("2026-10-25").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToIndex(0)
        compose.onNodeWithText("Weekly").performClick()
        compose.waitUntil(5000) { viewModel.state.value.period.weekly && !viewModel.state.value.loading }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(day.toString()))
        compose.onNodeWithText(day.toString()).assertExists()
        runBlocking { ids.forEach { repository.deleteEntry(it) } }
        compose.waitUntil(5000) { viewModel.state.value.summary.entryCount == 2 }
        compose.onNodeWithText(day.toString()).assertDoesNotExist()
    }

    @Test fun futurePeriodHasNoCalendarRowsAndZeroTotals() {
        compose.onNodeWithText("Next ›").performClick()
        compose.waitUntil(5000) { viewModel.state.value.period.month == YearMonth.of(2026, 11) && !viewModel.state.value.loading }
        compose.onNodeWithText("0.00 liters").assertExists()
        compose.onNodeWithText("No milk sales recorded for this period.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("2026-11-01").assertDoesNotExist()
        compose.onNodeWithText("Upcoming").assertDoesNotExist()
    }

    @Test fun monthComparisonDisplaysDifferencesAndHandlesNoPriorData() {
        compose.onNodeWithText("Compare with previous month").performScrollTo().performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Difference"))
        compose.onNodeWithText("Difference").assertIsDisplayed()
        compose.onNodeWithText("262.60").assertExists()
        compose.onNode(hasScrollAction()).performScrollToIndex(0)
        compose.onNodeWithText("‹ Previous").performClick()
        compose.waitUntil(5000) { viewModel.state.value.period.month == YearMonth.of(2026, 9) && !viewModel.state.value.loading }
        compose.onNodeWithText("3.00 liters").assertIsDisplayed()
        compose.onNodeWithText("Compare with previous month").performScrollTo().performClick()
        compose.onNodeWithText("No prior month data available for comparison.").performScrollTo().assertIsDisplayed()
    }
}
