package com.milksummer.tracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.milksummer.tracker.domain.model.Shift
import com.milksummer.tracker.data.backup.BackupReminders
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore by preferencesDataStore("reminders")

data class ReminderSettings(
    val enabled: Boolean = true,
    val morning: LocalTime = LocalTime.of(7, 0),
    val evening: LocalTime = LocalTime.of(19, 0),
    val morningFrom: LocalDate = LocalDate.ofEpochDay(0),
    val eveningFrom: LocalDate = LocalDate.ofEpochDay(0),
    val notificationAsked: Boolean = false
)

@Singleton
class ReminderPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.reminderDataStore
    private val enabled = booleanPreferencesKey("enabled")
    private val morning = intPreferencesKey("morning_minutes")
    private val evening = intPreferencesKey("evening_minutes")
    private val morningFrom = longPreferencesKey("morning_from")
    private val eveningFrom = longPreferencesKey("evening_from")
    private val asked = booleanPreferencesKey("notification_asked")

    val settings = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { data ->
        ReminderSettings(
            enabled = data[enabled] ?: true,
            morning = LocalTime.ofSecondOfDay((data[morning] ?: 420).toLong() * 60),
            evening = LocalTime.ofSecondOfDay((data[evening] ?: 1140).toLong() * 60),
            morningFrom = LocalDate.ofEpochDay(data[morningFrom] ?: 0),
            eveningFrom = LocalDate.ofEpochDay(data[eveningFrom] ?: 0),
            notificationAsked = data[asked] ?: false
        )
    }

    suspend fun enable(value: Boolean) { store.edit { it[enabled] = value } }
    suspend fun markNotificationAsked() { store.edit { it[asked] = true } }
    suspend fun restore(value: BackupReminders) {
        val tomorrow = LocalDate.now().plusDays(1).toEpochDay()
        store.edit {
            it[enabled] = value.enabled
            it[morning] = value.morning.hour * 60 + value.morning.minute
            it[evening] = value.evening.hour * 60 + value.evening.minute
            it[morningFrom] = tomorrow
            it[eveningFrom] = tomorrow
        }
    }
    suspend fun time(shift: Shift, value: LocalTime) {
        store.edit {
            val key = if (shift == Shift.MORNING) morning else evening
            val from = if (shift == Shift.MORNING) morningFrom else eveningFrom
            it[key] = value.hour * 60 + value.minute
            it[from] = LocalDate.now().plusDays(1).toEpochDay()
        }
    }
}
