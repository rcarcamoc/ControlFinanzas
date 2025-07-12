package com.aranthalion.controlfinanzas.data.movimiento

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import java.util.Date

@Entity(tableName = "movimientos_manuales")
data class MovimientoManualEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fecha: Date,
    val descripcion: String,
    val descripcionLimpia: String = "", // Nuevo campo para la descripción normalizada
    val monto: Double,
    val tipo: TipoMovimiento,
    val categoriaId: Long? = null,
    val notas: String = ""
) 