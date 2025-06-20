package com.aranthalion.controlfinanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "movimientos")
data class MovimientoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipo: String, // "INGRESO" o "GASTO"
    val monto: Double,
    val descripcion: String = "",
    val fecha: Date,
    val periodoFacturacion: String, // Formato: "YYYY-MM"
    val categoriaId: Long? = null,
    val tipoTarjeta: String? = null // Nuevo campo para mostrar tipo de tarjeta
) 