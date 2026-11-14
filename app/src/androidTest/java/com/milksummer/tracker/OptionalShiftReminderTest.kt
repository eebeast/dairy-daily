package com.milksummer.tracker

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.milksummer.tracker.data.local.MilkDatabase
import com.milksummer.tracker.data.repository.toEntity
import com.milksummer.tracker.domain.model.MilkEntry
import com.milksummer.tracker.domain.model.Shift
import com.milksummer.tracker.notification.ReminderReceiver
import com.milksummer.tracker.notification.SHIFT_EXTRA
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class OptionalShiftReminderTest {
    @Test fun receiverOnlySuppressesTheMatchingKnownShift() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as MilkTrackerApp
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val previous = app.reminders.settings.first()
        val database = Room.databaseBuilder(context, MilkDatabase::class.java, "milk_entries.db")
            .addMigrations(MilkDatabase.MIGRATION_1_2).build()
        val ids = mutableListOf<Long>()
        val date = LocalDate.now()
        suspend fun fire(shift: Shift) {
            val finished = CompletableDeferred<Unit>()
            context.sendOrderedBroadcast(Intent(context, ReminderReceiver::class.java).putExtra(SHIFT_EXTRA, shift.name),
                null, object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) { finished.complete(Unit) }
                }, Handler(Looper.getMainLooper()), 0, null, null)
            withTimeout(10000) { finished.await() }
        }
        suspend fun awaitNotifications(expected: Set<Int>) {
            // NotificationManager posts/cancels asynchronously after the receiver finishes.
            withTimeout(5000) {
                while (manager.activeNotifications.map { it.id }.toSet() != expected) delay(50)
            }
            assertEquals(expected, manager.activeNotifications.map { it.id }.toSet())
        }
        try {
            val dao = database.milkEntryDao()
            assertTrue("Use a disposable emulator without existing known-shift sales today",
                dao.getByDateRange(date, date).first().none { it.shift != null })
            app.reminders.enable(true)
            app.scheduler.createChannel()
            assertTrue(app.scheduler.notificationsAllowed())
            ids += dao.insert(MilkEntry(date, null, 1.0, 6.0, 8.0, 40.0).toEntity())
            manager.cancel(100); manager.cancel(101)
            awaitNotifications(emptySet())
            fire(Shift.MORNING)
            fire(Shift.EVENING)
            awaitNotifications(setOf(100, 101))
            assertNull(dao.getById(ids.first())!!.shift)
            manager.cancel(100); manager.cancel(101)
            awaitNotifications(emptySet())
            ids += dao.insert(MilkEntry(date, Shift.MORNING, 1.0, 6.0, 8.0, 40.0).toEntity())
            fire(Shift.MORNING)
            fire(Shift.EVENING)
            awaitNotifications(setOf(101))
            manager.cancel(101)
            awaitNotifications(emptySet())
            ids += dao.insert(MilkEntry(date, Shift.EVENING, 1.0, 6.0, 8.0, 40.0).toEntity())
            fire(Shift.MORNING)
            fire(Shift.EVENING)
            awaitNotifications(emptySet())
        } finally {
            ids.forEach { database.milkEntryDao().delete(it) }
            database.close()
            manager.cancel(100); manager.cancel(101)
            app.reminders.enable(previous.enabled)
            app.scheduler.schedule(previous)
        }
    }
}
