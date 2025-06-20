package com.aranthalion.controlfinanzas.data.categoria

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val descripcion: String = "",
    val color: Int = 0
) 