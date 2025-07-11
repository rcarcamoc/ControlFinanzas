package com.aranthalion.controlfinanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "cuentas_por_cobrar")
data class CuentaPorCobrarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val motivo: String,
    val monto: Double,
    val usuarioId: Long, // Referencia al usuario a quien cobrar
    val fechaCobro: Date? = null,
    val periodoCobro: String? = null, // Formato: "YYYY-MM"
    val estado: String = "PENDIENTE", // PENDIENTE, COBRADO, CANCELADO
    val notas: String? = null,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
) 