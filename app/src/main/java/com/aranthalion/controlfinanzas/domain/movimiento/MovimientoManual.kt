package com.aranthalion.controlfinanzas.domain.movimiento

import java.util.Date

enum class TipoMovimiento {
    GASTO,
    INGRESO
}

data class MovimientoManual(
    val id: Long = 0,
    val fecha: Date,
    val descripcion: String,
    val monto: Double,
    val tipo: TipoMovimiento,
    val categoriaId: Long? = null,
    val notas: String = ""
) 