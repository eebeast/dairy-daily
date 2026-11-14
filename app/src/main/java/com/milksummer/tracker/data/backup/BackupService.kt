package com.milksummer.tracker.data.backup

import androidx.room.withTransaction
import com.milksummer.tracker.data.local.MilkDatabase
import com.milksummer.tracker.data.repository.ReminderPreferences
import com.milksummer.tracker.data.repository.ReminderSettings
import com.milksummer.tracker.data.repository.toDomain
import com.milksummer.tracker.data.repository.toEntity
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class RestoreResult(val imported: Int, val skipped: Int, val settingsRestored: Boolean)

class BackupService internal constructor(
    private val database: MilkDatabase,
    private val codec: BackupCodec,
    private val clock: Clock,
    private val readSettings: suspend () -> ReminderSettings,
    private val restoreSettings: suspend (BackupReminders) -> Unit
) {
    @Inject constructor(database: MilkDatabase, codec: BackupCodec, preferences: ReminderPreferences, clock: Clock) :
        this(database, codec, clock, { preferences.settings.first() }, { preferences.restore(it) })

    suspend fun export(output: OutputStream): Int = withContext(Dispatchers.IO) {
        val settings = readSettings()
        val entries = database.milkEntryDao().getAll().first().map { it.toDomain() }
        codec.write(BackupData(Instant.now(clock), entries,
            BackupReminders(settings.enabled, settings.morning, settings.evening)), output)
        entries.size
    }

    suspend fun preview(input: InputStream): BackupData = withContext(Dispatchers.IO) { codec.read(input) }

    suspend fun restore(data: BackupData): RestoreResult = withContext(Dispatchers.IO) {
        codec.validate(data)
        val inserted = database.withTransaction {
            val dao = database.milkEntryDao()
            var count = 0
            data.entries.forEach { entry ->
                val sameIdentity = dao.getByPortableId(entry.portableId)
                val sameShift = entry.shift?.let { dao.getByDateAndShift(entry.date, it) }
                if (sameIdentity == null && sameShift == null) {
                    dao.insert(entry.copy(id = 0, total = entry.quantity * entry.rate).toEntity())
                    count++
                }
            }
            count
        }
        val settingsRestored = try {
            restoreSettings(data.reminders)
            true
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            false
        }
        RestoreResult(inserted, data.entries.size - inserted, settingsRestored)
    }
}
