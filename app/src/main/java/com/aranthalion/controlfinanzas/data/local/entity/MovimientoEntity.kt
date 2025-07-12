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
    val descripcionLimpia: String = "", // Nuevo campo para la descripción normalizada
    val fecha: Date,
    val periodoFacturacion: String, // Formato: "YYYY-MM"
    val categoriaId: Long? = null,
    val tipoTarjeta: String? = null, // Nuevo campo para mostrar tipo de tarjeta
    val idUnico: String,
    // Campos de auditoría
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis(),
    val metodoActualizacion: String = "", // "INSERT", "UPDATE", "IMPORT_EXCEL", etc.
    val daoResponsable: String = "", // "MovimientoDao", "ClasificacionAutomaticaDao", etc.
    val usuarioResponsable: String = "SYSTEM" // Para futuras implementaciones de usuarios
) 