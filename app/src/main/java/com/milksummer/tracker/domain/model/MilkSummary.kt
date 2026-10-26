package com.milksummer.tracker.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class MilkSummary(
    val totalLiters: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val averageFat: Double = 0.0,
    val averageSnf: Double = 0.0,
    val averageRate: Double = 0.0,
    val entryCount: Int = 0,
    val missedDays: Int = 0
)

// A current period ends today; future days are never counted as missed.
fun summarizeEntries(
    entries: List<MilkEntry>,
    start: LocalDate,
    end: LocalDate,
    today: LocalDate = LocalDate.now()
): MilkSummary {
    require(!end.isBefore(start))
    val through = minOf(end, today)
    val selected = entries.filter { !it.date.isBefore(start) && !it.date.isAfter(through) }
    val days = if (through < start) 0 else ChronoUnit.DAYS.between(start, through).toInt() + 1
    val completeDays = selected.groupBy { it.date }.count { (_, day) ->
        day.map { it.shift }.toSet().size == Shift.entries.size
    }
    return MilkSummary(
        totalLiters = selected.sumOf { it.quantity },
        totalEarnings = selected.sumOf { it.total },
        averageFat = selected.map { it.fat }.average().takeIf { selected.isNotEmpty() } ?: 0.0,
        averageSnf = selected.map { it.snf }.average().takeIf { selected.isNotEmpty() } ?: 0.0,
        averageRate = selected.map { it.rate }.average().takeIf { selected.isNotEmpty() } ?: 0.0,
        entryCount = selected.size,
        missedDays = days - completeDays
    )
}
