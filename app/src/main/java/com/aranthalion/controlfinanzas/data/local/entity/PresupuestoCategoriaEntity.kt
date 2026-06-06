package com.aranthalion.controlfinanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "presupuesto_categoria",
    indices = [Index(value = ["categoriaId", "periodo"], unique = true)]
)
data class PresupuestoCategoriaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoriaId: Long,
    val monto: Double,
    val periodo: String // formato YYYY-MM
) 