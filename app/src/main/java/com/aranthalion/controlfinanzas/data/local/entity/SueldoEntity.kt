package com.aranthalion.controlfinanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sueldos")
data class SueldoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombrePersona: String,
    val periodo: String, // formato YYYY-MM
    val sueldo: Double
) 