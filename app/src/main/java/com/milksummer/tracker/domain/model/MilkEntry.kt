package com.milksummer.tracker.domain.model

import java.time.LocalDate

data class MilkEntry(
    val date: LocalDate,
    val shift: Shift?,
    val quantity: Double,
    val fat: Double,
    val snf: Double,
    val rate: Double,
    val id: Long = 0,
    val total: Double = quantity * rate,
    val slipTotal: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val portableId: String = java.util.UUID.randomUUID().toString()
)
