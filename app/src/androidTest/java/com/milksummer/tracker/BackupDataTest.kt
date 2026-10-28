package com.milksummer.tracker

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.milksummer.tracker.data.backup.*
import com.milksummer.tracker.data.local.MilkDatabase
import com.milksummer.tracker.data.repository.*
import com.milksummer.tracker.domain.model.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BackupDataTest {
    private lateinit var database: MilkDatabase
    private val codec = BackupCodec()
    private val clock = Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC)
    private val reminder = BackupReminders(false, LocalTime.of(6, 30), LocalTime.of(18, 45))
    private fun entry(shift: Shift = Shift.MORNING) = MilkEntry(LocalDate.of(2026, 10, 26), shift,
        5.2, 6.5, 8.8, 38.5, slipTotal = 200.7, createdAt = 100, updatedAt = 200)
    private fun backup(entries: List<MilkEntry> = listOf(entry())) = BackupData(Instant.now(clock), entries, reminder)
    private fun json(data: BackupData = backup()): String = ByteArrayOutputStream().also { codec.write(data, it) }.toString("UTF-8")
    private fun reject(value: String) {
        try { codec.read(value.byteInputStream()); fail("Invalid backup accepted") }
        catch (_: BackupException) { }
    }
    private fun service(apply: suspend (BackupReminders) -> Unit = {}) = BackupService(database, codec, clock,
        { ReminderSettings(enabled = false, morning = reminder.morning, evening = reminder.evening) }, apply)

    @Before fun open() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MilkDatabase::class.java).build()
    }
    @After fun close() { database.close() }

    @Test fun roundTripIncludesAllSaleValuesAndSupportsEmptyBackups() {
        assertEquals(backup(), codec.read(json().byteInputStream()))
        assertEquals(backup(emptyList()), codec.read(json(backup(emptyList())).byteInputStream()))
        assertFalse(json().contains("\"id\""))
        assertFalse(json().contains("notificationAsked"))
    }

    @Test fun rejectsMalformedUnsupportedAndInvalidBackups() {
        val valid = json()
        listOf(
            "{}", valid.dropLast(2), valid + " {}",
            valid.replace("\"version\": 1", "\"version\": 2"),
            valid.replace("\"version\": 1", "\"version\": 1, \"version\": 1"),
            valid.replace("\"quantity\": 5.2", "\"quantity\": -1"),
            valid.replace("\"fat\": 6.5", "\"fat\": 16"),
            valid.replace("\"snf\": 8.8", "\"snf\": 16"),
            valid.replace("\"rate\": 38.5", "\"rate\": 0"),
            valid.replace("\"slipTotal\": 200.7", "\"slipTotal\": -1"),
            valid.replace("\"quantity\": 5.2", "\"quantity\": 1e999"),
            valid.replace("\"quantity\": 5.2", "\"quantity\": \"5.2\""),
            valid.replace("\"total\": 200.2", "\"total\": 999.2"),
            valid.replace("MORNING", "NIGHT"), valid.replace("2026-10-26", "2026-02-30"),
            valid.replace("06:30", "25:30"), valid.replace("06:30", "06:30:10"),
            valid.replace("\"updatedAt\": 200", "\"updatedAt\": 99"),
            valid.replace("\"createdAt\": 100", "\"createdAt\": -1")
        ).forEach { reject(it) }
    }

    @Test fun rejectsRepeatedEntriesAndOversizedFiles() {
        val valid = json()
        val row = valid.substringAfter("\"entries\": [").substringBeforeLast("]").trim()
        reject(valid.substringBefore("\"entries\": [") + "\"entries\": [$row,$row]}")
        try { codec.read(ByteArray(BackupCodec.MAX_BYTES + 1) { 32 }.inputStream()); fail("Size limit ignored") }
        catch (_: BackupException) { }
        try { codec.validate(backup(List(BackupCodec.MAX_ENTRIES + 1) { entry() })); fail("Entry limit ignored") }
        catch (_: BackupException) { }
    }

    @Test fun mergePreservesExistingRowsAssignsIdsAndIsRepeatable() = runBlocking {
        val dao = database.milkEntryDao()
        val original = entry().copy(id = 42, quantity = 9.0, total = 346.5).toEntity()
        dao.insert(original)
        var restored: BackupReminders? = null
        val service = service { restored = it }
        val data = backup(listOf(entry(), entry(Shift.EVENING).copy(id = 42)))
        assertEquals(RestoreResult(1, 1, true), service.restore(data))
        assertEquals(original, dao.getById(42))
        val evening = dao.getByDateAndShift(entry().date, Shift.EVENING)!!
        assertNotEquals(42L, evening.id)
        assertEquals(100L, evening.createdAt)
        assertEquals(200L, evening.updatedAt)
        assertEquals(200.7, evening.slipTotal!!, 0.000001)
        assertEquals(reminder, restored)
        assertEquals(RestoreResult(0, 2, true), service.restore(data))
        assertEquals(2, dao.getAll().first().size)
    }

    @Test fun failedInsertRollsBackEveryImportedRowAndDoesNotApplySettings() = runBlocking {
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_evening BEFORE INSERT ON milk_entries WHEN NEW.shift = 'EVENING' BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        var applied = false
        try { service { applied = true }.restore(backup(listOf(entry(), entry(Shift.EVENING)))); fail("Expected insert failure") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(database.milkEntryDao().getAll().first().isEmpty())
        assertFalse(applied)
    }

    @Test fun previewDoesNotMutateAndExportReadsRealStoredEntries() = runBlocking {
        val service = service { fail("Preview must not apply settings") }
        assertEquals(backup(), service.preview(json().byteInputStream()))
        assertTrue(database.milkEntryDao().getAll().first().isEmpty())
        database.milkEntryDao().insert(entry().toEntity())
        val output = ByteArrayOutputStream()
        assertEquals(1, service.export(output))
        assertEquals(backup(), codec.read(output.toByteArray().inputStream()))
    }

    @Test fun settingsFailureReportsCommittedEntriesAndRetrySkipsThem() = runBlocking {
        assertEquals(RestoreResult(1, 0, false), service { throw IOException("disk full") }.restore(backup()))
        assertEquals(1, database.milkEntryDao().getAll().first().size)
        assertEquals(RestoreResult(0, 1, true), service().restore(backup()))
    }
}
