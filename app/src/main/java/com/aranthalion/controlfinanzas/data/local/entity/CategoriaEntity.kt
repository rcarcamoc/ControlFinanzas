package com.aranthalion.controlfinanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val tipo: String, // "INGRESO" o "GASTO"
    val presupuestoMensual: Double? = null
) 