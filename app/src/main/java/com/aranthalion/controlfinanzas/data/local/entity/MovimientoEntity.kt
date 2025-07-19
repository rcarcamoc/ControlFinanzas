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
    val descripcionLimpia: String = "", // Campo existente para descripción normalizada
    val fecha: Date,
    val periodoFacturacion: String, // Formato: "YYYY-MM"
    val categoriaId: Long? = null,
    val tipoTarjeta: String? = null, // Nuevo campo para mostrar tipo de tarjeta
    val idUnico: String,
    
    // Campos normalizados para optimización (HITO 1.1)
    val descripcionNormalizada: String = "undefined", // Sin acentos, minúsculas, sin caracteres especiales
    val montoCategoria: String = "undefined", // Agrupado por rangos (0-100, 100-500, 500-1000, etc.)
    val fechaMes: String = "undefined", // Mes extraído (01-12)
    val fechaDiaSemana: String = "undefined", // Día de la semana (LUNES, MARTES, etc.)
    val fechaDia: Int = 0, // Día del mes (1-31)
    val fechaAnio: Int = 0, // Año (2024, 2025, etc.)
    
    // Campos de auditoría
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis(),
    val metodoActualizacion: String = "", // "INSERT", "UPDATE", "IMPORT_EXCEL", etc.
    val daoResponsable: String = "", // "MovimientoDao", "ClasificacionAutomaticaDao", etc.
    val usuarioResponsable: String = "SYSTEM" // Para futuras implementaciones de usuarios
) 