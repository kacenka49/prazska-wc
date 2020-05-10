package cz.bezvadilna.prazskawc.model.database.converter

import androidx.room.TypeConverter
import java.util.*


class DateTypeConverter {

    @TypeConverter
    fun toDate(value: Long?): Date? {
        value?.let {
            return Date(it)
        }
        return null
    }

    @TypeConverter
    fun toLong(value: Date?): Long? {
        value?.let {
            return it.time
        }
        return null
    }
}