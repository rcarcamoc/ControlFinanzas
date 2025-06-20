package com.aranthalion.controlfinanzas.data.util

import androidx.room.TypeConverter
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromTipoMovimiento(value: TipoMovimiento): String {
        return value.name
    }

    @TypeConverter
    fun toTipoMovimiento(value: String): TipoMovimiento {
        return TipoMovimiento.valueOf(value)
    }
} 