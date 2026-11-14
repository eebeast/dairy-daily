package com.milksummer.tracker.data.backup

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.milksummer.tracker.domain.model.MilkEntry
import com.milksummer.tracker.domain.model.Shift
import com.milksummer.tracker.domain.usecase.ValidateEntryUseCase
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

class BackupException(message: String) : Exception(message)

data class BackupReminders(val enabled: Boolean, val morning: LocalTime, val evening: LocalTime)
data class BackupData(val createdAt: Instant, val entries: List<MilkEntry>, val reminders: BackupReminders) {
    val firstDate: LocalDate? get() = entries.minOfOrNull { it.date }
    val lastDate: LocalDate? get() = entries.maxOfOrNull { it.date }
}

/** Reads versions 1 and 2; exports version 2. See docs/backup-format.md. */
class BackupCodec @Inject constructor() {
    companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
        const val MAX_ENTRIES = 50_000
        const val FORMAT = "com.milksummer.tracker.backup"
    }

    fun read(input: InputStream): BackupData {
        try {
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (bytes.size() + count > MAX_BYTES) throw BackupException("Backup exceeds the 10 MiB limit.")
                bytes.write(buffer, 0, count)
            }
            val root = JsonReader(bytes.toByteArray().inputStream().reader(Charsets.UTF_8)).use { reader ->
                val value = objectValue(readValue(reader, 0))
                require(reader.peek() == JsonToken.END_DOCUMENT) { "Unexpected content after backup." }
                value
            }
            require(root["format"] == FORMAT) { "This is not a MilkSummer backup." }
            val version = integer(root["version"])
            require(version == 1L || version == 2L) { "Unsupported backup version. Update MilkSummer to restore this file." }
            val created = Instant.parse(string(root["createdAt"]))
            val reminders = objectValue(root["reminders"])
            val settings = BackupReminders(
                reminders["enabled"] as? Boolean ?: error("Missing reminder enabled state."),
                LocalTime.parse(string(reminders["morning"])), LocalTime.parse(string(reminders["evening"]))
            )
            val rows = root["entries"] as? List<*> ?: error("Missing entries.")
            require(rows.size <= MAX_ENTRIES) { "Backup exceeds the 50,000 entry limit." }
            val entries = rows.map { row ->
                val fields = objectValue(row)
                require(fields.containsKey("slipTotal")) { "Missing slip total field." }
                require(fields.containsKey("shift")) { "Missing shift field." }
                val shift = if (version == 2L && fields["shift"] == null) null
                    else Shift.valueOf(string(fields["shift"]))
                MilkEntry(
                    date = LocalDate.parse(string(fields["date"])), shift = shift,
                    quantity = decimal(fields["quantity"]), fat = decimal(fields["fat"]), snf = decimal(fields["snf"]),
                    rate = decimal(fields["rate"]), total = decimal(fields["total"]),
                    slipTotal = fields["slipTotal"]?.let(::decimal),
                    createdAt = integer(fields["createdAt"]), updatedAt = integer(fields["updatedAt"]),
                    portableId = if (version == 2L) portableId(string(fields["portableId"])) else UUID.randomUUID().toString()
                )
            }
            return BackupData(created, entries, settings).also(::validate)
        } catch (error: BackupException) {
            throw error
        } catch (error: Exception) {
            throw BackupException("Invalid backup. ${error.message ?: "Check the file and try again."}")
        }
    }

    fun validate(data: BackupData) {
        requireBackup(data.entries.size <= MAX_ENTRIES, "Backup exceeds the 50,000 entry limit.")
        requireBackup(data.reminders.morning.second == 0 && data.reminders.morning.nano == 0 &&
            data.reminders.evening.second == 0 && data.reminders.evening.nano == 0, "Reminder times must use whole minutes.")
        val keys = HashSet<Pair<LocalDate, Shift>>()
        val identities = HashSet<String>()
        val validateEntry = ValidateEntryUseCase()
        data.entries.forEach { entry ->
            requireBackup(validateEntry(entry).isValid, "Invalid values for ${entry.date}.")
            requireBackup(entry.total.isFinite() && abs(entry.total - entry.quantity * entry.rate) <= 0.000001,
                "An entry total does not match quantity × rate.")
            requireBackup(entry.createdAt >= 0 && entry.updatedAt >= entry.createdAt, "Invalid entry timestamps.")
            val identity = try { portableId(entry.portableId) }
                catch (_: IllegalArgumentException) { throw BackupException("Invalid portable entry identity.") }
            requireBackup(identities.add(identity), "Backup contains repeated portable identities.")
            entry.shift?.let { requireBackup(keys.add(entry.date to it), "Backup contains repeated date/shift entries.") }
        }
    }

    fun write(data: BackupData, output: OutputStream) {
        validate(data)
        // Prepare the complete bounded document before touching the destination.
        val bytes = ByteArrayOutputStream()
        JsonWriter(bytes.writer(Charsets.UTF_8)).use { writer ->
            writer.setIndent("  ")
            writer.beginObject().name("format").value(FORMAT).name("version").value(2)
                .name("createdAt").value(data.createdAt.toString()).name("reminders").beginObject()
                .name("enabled").value(data.reminders.enabled).name("morning").value(data.reminders.morning.toString())
                .name("evening").value(data.reminders.evening.toString()).endObject().name("entries").beginArray()
            data.entries.forEach { row ->
                writer.beginObject().name("portableId").value(row.portableId)
                    .name("date").value(row.date.toString()).name("shift")
                if (row.shift == null) writer.nullValue() else writer.value(row.shift.name)
                writer.name("quantity").value(row.quantity).name("fat").value(row.fat).name("snf").value(row.snf)
                    .name("rate").value(row.rate).name("total").value(row.total).name("slipTotal")
                if (row.slipTotal == null) writer.nullValue() else writer.value(row.slipTotal)
                writer.name("createdAt").value(row.createdAt).name("updatedAt").value(row.updatedAt).endObject()
                requireBackup(bytes.size() <= MAX_BYTES, "Backup exceeds the 10 MiB limit.")
            }
            writer.endArray().endObject()
        }
        requireBackup(bytes.size() <= MAX_BYTES, "Backup exceeds the 10 MiB limit.")
        bytes.writeTo(output)
    }

    private data class NumberText(val value: String)
    private fun readValue(reader: JsonReader, depth: Int): Any? {
        require(depth <= 16) { "Backup nesting is too deep." }
        return when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                val fields = linkedMapOf<String, Any?>()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    require(!fields.containsKey(name)) { "Repeated field: $name." }
                    fields[name] = readValue(reader, depth + 1)
                }
                reader.endObject(); fields
            }
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                val values = arrayListOf<Any?>()
                while (reader.hasNext()) {
                    require(values.size < MAX_ENTRIES) { "Backup exceeds the 50,000 entry limit." }
                    values.add(readValue(reader, depth + 1))
                }
                reader.endArray(); values
            }
            JsonToken.STRING -> reader.nextString()
            JsonToken.NUMBER -> NumberText(reader.nextString())
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NULL -> { reader.nextNull(); null }
            else -> error("Unexpected JSON content.")
        }
    }
    private fun objectValue(value: Any?): Map<*, *> = value as? Map<*, *> ?: error("Expected an object.")
    private fun string(value: Any?): String = value as? String ?: error("Missing or invalid text field.")
    private fun decimal(value: Any?): Double = (value as? NumberText)?.value?.toDoubleOrNull()
        ?: error("Missing or invalid number.")
    private fun integer(value: Any?): Long = (value as? NumberText)?.value?.toLongOrNull()
        ?: error("Missing or invalid integer.")
    private fun portableId(value: String): String {
        val canonical = UUID.fromString(value).toString()
        require(value == canonical) { "Portable identity must be a canonical UUID." }
        return canonical
    }
    private fun requireBackup(condition: Boolean, message: String) { if (!condition) throw BackupException(message) }
}
