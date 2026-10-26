package com.milksummer.tracker.notification

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

fun nextReminderTime(now: ZonedDateTime, time: LocalTime, notBefore: LocalDate = now.toLocalDate()): ZonedDateTime {
    var date = maxOf(now.toLocalDate(), notBefore)
    var next = date.atTime(time).atZone(now.zone)
    if (!next.isAfter(now)) {
        date = date.plusDays(1)
        next = date.atTime(time).atZone(now.zone)
    }
    return next
}
