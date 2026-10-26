package com.milksummer.tracker.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.milksummer.tracker.data.repository.ReminderSettings
import com.milksummer.tracker.domain.model.Shift
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

const val REMINDER_CHANNEL = "milk_entries"
const val SHIFT_EXTRA = "milk_shift"

@Singleton
class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context, private val clock: Clock) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun createChannel() {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(REMINDER_CHANNEL, "Milk entry reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Morning and evening reminders for missing milk entries"
            }
        )
    }

    fun notificationsAllowed(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        context.getSystemService(NotificationManager::class.java).getNotificationChannel(REMINDER_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    fun exactAllowed(): Boolean = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()

    private fun pending(shift: Shift): PendingIntent = PendingIntent.getBroadcast(
        context, shift.ordinal,
        Intent(context, ReminderReceiver::class.java).setAction("milk.reminder.${shift.name}").putExtra(SHIFT_EXTRA, shift.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun cancel() { Shift.entries.forEach { alarmManager.cancel(pending(it)) } }

    fun schedule(settings: ReminderSettings) {
        if (!settings.enabled || !notificationsAllowed()) return cancel()
        Shift.entries.forEach { scheduleShift(settings, it) }
    }

    fun scheduleShift(settings: ReminderSettings, shift: Shift, tomorrow: Boolean = false) {
        if (!settings.enabled || !notificationsAllowed()) return cancel()
        val now = ZonedDateTime.now(clock)
        val configuredFrom = if (shift == Shift.MORNING) settings.morningFrom else settings.eveningFrom
        val notBefore = maxOf(configuredFrom, if (tomorrow) LocalDate.now(clock).plusDays(1) else LocalDate.now(clock))
        val time = if (shift == Shift.MORNING) settings.morning else settings.evening
        val trigger = nextReminderTime(now, time, notBefore).toInstant().toEpochMilli()
        val pending = pending(shift)
        if (exactAllowed()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                return
            } catch (_: SecurityException) { /* Permission may have been revoked since the check. */ }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }
}
