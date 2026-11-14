package com.milksummer.tracker.data.local

import androidx.room.TypeConverter
import com.milksummer.tracker.domain.model.Shift
import java.time.LocalDate

class MilkEntryConverters {
    @TypeConverter
    fun dateToString(date: LocalDate): String = date.toString()

    @TypeConverter
    fun stringToDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun shiftToString(shift: Shift?): String? = shift?.name

    @TypeConverter
    fun stringToShift(value: String?): Shift? = value?.let(Shift::valueOf)
}
