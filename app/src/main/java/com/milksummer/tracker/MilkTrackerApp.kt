package com.milksummer.tracker

import android.app.Application
import android.util.Log
import com.milksummer.tracker.data.repository.ReminderPreferences
import com.milksummer.tracker.notification.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.*

@HiltAndroidApp
class MilkTrackerApp : Application() {
    @Inject lateinit var reminders: ReminderPreferences
    @Inject lateinit var scheduler: ReminderScheduler
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        scheduler.createChannel()
        scope.launch {
            try { reminders.settings.collect { scheduler.schedule(it) } }
            catch (error: Exception) { Log.e("MilkReminder", "Unable to initialize reminders", error) }
        }
    }
}
