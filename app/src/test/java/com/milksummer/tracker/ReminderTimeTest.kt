package com.milksummer.tracker

import com.milksummer.tracker.notification.nextReminderTime
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ReminderTimeTest {
    @Test fun defaultMorningTimeSchedulesTodayBeforeSeven() {
        val now = ZonedDateTime.parse("2026-10-24T06:00:00+05:30[Asia/Kolkata]")
        assertEquals(now.withHour(7), nextReminderTime(now, LocalTime.of(7, 0)))
    }

    @Test fun elapsedTimeSchedulesTomorrowIncludingExactBoundary() {
        val now = ZonedDateTime.parse("2026-10-24T07:00:00+05:30[Asia/Kolkata]")
        assertEquals(now.plusDays(1), nextReminderTime(now, LocalTime.of(7, 0)))
    }

    @Test fun changedTimeCannotFireBeforeTomorrow() {
        val now = ZonedDateTime.parse("2026-10-24T05:00:00+05:30[Asia/Kolkata]")
        val result = nextReminderTime(now, LocalTime.of(6, 30), now.toLocalDate().plusDays(1))
        assertEquals(LocalDate.of(2026, 10, 25), result.toLocalDate())
        assertEquals(LocalTime.of(6, 30), result.toLocalTime())
    }

    @Test fun dayBoundaryUsesLocalCalendarAcrossDaylightSaving() {
        val now = ZonedDateTime.parse("2026-10-31T20:00:00-04:00[America/New_York]")
        val result = nextReminderTime(now, LocalTime.of(7, 0))
        assertEquals(LocalDate.of(2026, 11, 1), result.toLocalDate())
        assertEquals(LocalTime.of(7, 0), result.toLocalTime())
        assertEquals(ZoneOffset.ofHours(-5), result.offset)
    }
}
