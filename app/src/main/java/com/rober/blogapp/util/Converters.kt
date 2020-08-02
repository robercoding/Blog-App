package com.rober.blogapp.util

import androidx.room.TypeConverter
import java.util.*

class Converters {

    @TypeConverter
    fun fromTimeStamp(value: Long?): Date?{
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun fromDate(value: Date?): Long?{
        return value?.time
    }
}