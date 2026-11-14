package com.milksummer.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.milksummer.tracker.data.local.MilkEntryConverters
import com.milksummer.tracker.domain.model.Shift
import java.time.LocalDate

@Entity(
    tableName = "milk_entries",
    indices = [Index(value = ["date", "shift"], unique = true), Index(value = ["portable_id"], unique = true)]
)
@TypeConverters(MilkEntryConverters::class)
data class MilkEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val shift: Shift?,
    val quantity: Double,
    val fat: Double,
    val snf: Double,
    val rate: Double,
    val total: Double,
    @ColumnInfo(name = "slip_total")
    val slipTotal: Double?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "portable_id")
    val portableId: String = java.util.UUID.randomUUID().toString()
)
