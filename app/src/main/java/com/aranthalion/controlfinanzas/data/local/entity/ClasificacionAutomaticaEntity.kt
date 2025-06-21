package com.aranthalion.controlfinanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clasificacion_automatica")
data class ClasificacionAutomaticaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patron: String,
    val categoriaId: Long,
    val nivelConfianza: Double,
    val frecuencia: Int = 1,
    val ultimaActualizacion: Long = System.currentTimeMillis()
) 