package com.milksummer.tracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.milksummer.tracker.data.repository.ReminderPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var preferences: ReminderPreferences
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { scheduler.createChannel(); scheduler.schedule(preferences.settings.first()) }
            catch (error: Exception) { Log.e("MilkReminder", "Unable to reschedule reminders", error) }
            finally { pending.finish() }
        }
    }
}
