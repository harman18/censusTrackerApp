package com.censustracker.data.local.room.typeconverters

import androidx.room.TypeConverter
import java.util.Date

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time?.let { it }

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").filter { it.isNotBlank() }
    }

    @TypeConverter
    fun stringListToString(list: List<String>?): String? = list?.joinToString(",")
}
