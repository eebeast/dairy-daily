package com.milksummer.tracker.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.milksummer.tracker.MainActivity
import com.milksummer.tracker.R
import com.milksummer.tracker.data.repository.MilkEntryRepository
import com.milksummer.tracker.data.repository.ReminderPreferences
import com.milksummer.tracker.domain.model.Shift
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: MilkEntryRepository
    @Inject lateinit var preferences: ReminderPreferences
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        val shift = Shift.entries.find { it.name == intent.getStringExtra(SHIFT_EXTRA) } ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = preferences.settings.first()
                scheduler.scheduleShift(settings, shift, tomorrow = true)
                if (settings.enabled && scheduler.notificationsAllowed() && repository.getEntry(LocalDate.now(clock), shift) == null) {
                    val tap = PendingIntent.getActivity(context, 100 + shift.ordinal,
                        Intent(context, MainActivity::class.java).putExtra(SHIFT_EXTRA, shift.name)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Log your ${shift.name.lowercase()} milk sale")
                        .setContentText("Tap to record today's ${shift.name.lowercase()} entry.")
                        .setContentIntent(tap).setAutoCancel(true).build()
                    try { NotificationManagerCompat.from(context).notify(100 + shift.ordinal, notification) }
                    catch (_: SecurityException) { scheduler.cancel() }
                }
            } catch (error: Exception) {
                Log.e("MilkReminder", "Unable to deliver reminder", error)
            } finally { pending.finish() }
        }
    }
}
