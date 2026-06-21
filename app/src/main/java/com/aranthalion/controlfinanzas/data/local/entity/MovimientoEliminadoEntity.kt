package com.aranthalion.controlfinanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movimientos_eliminados")
data class MovimientoEliminadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idUnico: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val syncPending: Boolean = true
)
