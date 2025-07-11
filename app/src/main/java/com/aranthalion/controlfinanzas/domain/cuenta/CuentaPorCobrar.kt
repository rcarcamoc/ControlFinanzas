package com.aranthalion.controlfinanzas.domain.cuenta

import java.util.Date

data class CuentaPorCobrar(
    val id: Long = 0,
    val motivo: String,
    val monto: Double,
    val usuarioId: Long,
    val usuarioNombre: String? = null, // Para mostrar el nombre del usuario
    val fechaCobro: Date? = null,
    val periodoCobro: String? = null,
    val estado: EstadoCuenta = EstadoCuenta.PENDIENTE,
    val notas: String? = null,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
)

enum class EstadoCuenta {
    PENDIENTE,
    COBRADO,
    CANCELADO
} 