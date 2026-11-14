package com.milksummer.tracker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.milksummer.tracker.data.backup.*
import com.milksummer.tracker.data.local.MilkDatabase
import com.milksummer.tracker.data.repository.*
import com.milksummer.tracker.domain.model.*
import com.milksummer.tracker.domain.usecase.*
import com.milksummer.tracker.ui.*
import com.milksummer.tracker.ui.entry.EntryViewModel
import com.milksummer.tracker.ui.ocr.*
import com.milksummer.tracker.ui.summary.*
import com.milksummer.tracker.ui.theme.MilkSummerTheme
import java.io.ByteArrayOutputStream
import java.time.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Generated photo fixtures exercise bundled ML Kit, confirmation, Room, UI and backup together. */
class OptionalShiftFlowTest {
    @get:Rule(order = 0) val unlocked = UnlockedDeviceRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun recognizedAndAbsentShiftsSurviveScanSaveSummaryAndRestore() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val clock = Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), ZoneOffset.UTC)
        val database = Room.inMemoryDatabaseBuilder(context, MilkDatabase::class.java).build()
        val recovered = Room.inMemoryDatabaseBuilder(context, MilkDatabase::class.java).build()
        val store = ViewModelStore()
        val repository = RoomMilkEntryRepository(database.milkEntryDao())
        val validate = ValidateEntryUseCase()
        lateinit var scanner: OcrViewModel
        lateinit var ledger: LedgerViewModel
        lateinit var summary: SummaryViewModel
        lateinit var entry: EntryViewModel
        var page by mutableStateOf("home")
        var fields by mutableStateOf(ParsedSlip())
        var scanId by mutableStateOf("")
        instrumentation.runOnMainSync {
            scanner = ViewModelProvider(store, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    OcrViewModel(context, SavedStateHandle()) as T
            })[OcrViewModel::class.java]
            ledger = LedgerViewModel(repository, clock).also { store.put("ledger", it) }
            summary = SummaryViewModel(repository, GetSummaryUseCase(repository, clock), clock)
                .also { store.put("summary", it) }
        }
        try {
            compose.setContent {
                MilkSummerTheme {
                    when (page) {
                        "home" -> Column {
                            Button(onClick = { page = "summary" }) { Text("View summary") }
                            Box(Modifier.weight(1f)) {
                                val state by ledger.state.collectAsState()
                                HomeScreen(state, {}, { page = "capture" }, {}, {})
                            }
                        }
                        "review" -> OcrConfirmScreen(fields, scanId, {}, { page = "home" }, {}, {}, entry)
                        "summary" -> SummaryScreen({}, summary)
                        else -> Text("Capture fixture")
                    }
                }
            }
            repeat(2) { index ->
                compose.onNodeWithText("Scan Slip").performClick()
                val lines = if (index == 0) listOf("Shift: Morning", "Qty: 5.2", "FAT: 6.5", "SNF: 8.8", "Rate: 38.50", "Total: 200.20")
                    else listOf("Qty: 4.8", "FAT: 6.5", "SNF: 8.8", "Rate: 38.00", "Total: 182.40")
                val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 70f }
                lines.forEachIndexed { row, text -> canvas.drawText(text, 80f, 250f + row * 180f, paint) }
                lateinit var uri: android.net.Uri
                instrumentation.runOnMainSync { scanner.retake(); uri = scanner.createCapture() }
                context.contentResolver.openOutputStream(uri)!!.use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
                bitmap.recycle()
                instrumentation.runOnMainSync { scanner.processCapture() }
                runBlocking { withTimeout(20000) { while (scanner.state.value.status == OcrStatus.PROCESSING) delay(50) } }
                assertEquals(scanner.state.value.error, OcrStatus.DONE, scanner.state.value.status)
                assertEquals(if (index == 0) Shift.MORNING else null, scanner.state.value.fields.shift)
                instrumentation.runOnMainSync {
                    entry = EntryViewModel(SavedStateHandle(), repository, SaveEntryUseCase(repository, validate, clock), validate, clock)
                        .also { store.put("entry-$index", it) }
                    fields = scanner.state.value.fields
                    scanId = scanner.state.value.scanId
                    page = "review"
                }
                compose.waitUntil(5000) { !entry.state.value.loading && entry.state.value.quantity.isNotBlank() }
                compose.onNodeWithText(if (index == 0) "Morning" else "No shift").performScrollTo().assertIsSelected()
                compose.onNodeWithText("Confirm and save").performScrollTo().performClick()
                compose.waitUntil(5000) { page == "home" }
            }
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Today: 10.00 L · ${money(382.6)}"))
            compose.onNodeWithText("Today: 10.00 L · ${money(382.6)}").assertIsDisplayed()
            compose.onNodeWithText("View summary").performClick()
            compose.waitUntil(5000) { !summary.state.value.loading }
            compose.onNodeWithText("10.00 liters").assertIsDisplayed()
            compose.onNodeWithText(money(382.6)).assertIsDisplayed()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("2026-10-24"))
            compose.onAllNodesWithText("2026-10-24").assertCountEquals(1)
            compose.onNodeWithText("2026-10-23").assertDoesNotExist()
            runBlocking {
                val codec = BackupCodec()
                val service = BackupService(database, codec, clock, { ReminderSettings(enabled = false) }, {})
                val output = ByteArrayOutputStream()
                assertEquals(2, service.export(output))
                val backup = service.preview(output.toByteArray().inputStream())
                assertEquals(listOf(null, Shift.MORNING), backup.entries.map { it.shift }.sortedBy { it?.ordinal ?: -1 })
                val restore = BackupService(recovered, codec, clock, { ReminderSettings() }, {})
                assertEquals(RestoreResult(2, 0, true), restore.restore(backup))
                assertEquals(RestoreResult(0, 2, true), restore.restore(backup))
                val before = database.milkEntryDao().getAll().first().map { it.toDomain().copy(id = 0) }.sortedBy { it.portableId }
                val after = recovered.milkEntryDao().getAll().first().map { it.toDomain().copy(id = 0) }.sortedBy { it.portableId }
                assertEquals(before, after)
                assertEquals(382.6, summarizeEntries(after, LocalDate.now(clock), LocalDate.now(clock), LocalDate.now(clock)).totalEarnings, 0.000001)
            }
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            database.close()
            recovered.close()
        }
    }
}
