package com.aranthalion.controlfinanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val descripcion: String = "",
    val tipo: String = "Gasto" // "Gasto" o "Ingreso"
) 