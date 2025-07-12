package com.aranthalion.controlfinanzas.data.util

import com.aranthalion.controlfinanzas.data.movimiento.MovimientoManualEntity
import com.aranthalion.controlfinanzas.domain.movimiento.MovimientoManual
import javax.inject.Inject

class MovimientoManualMapper @Inject constructor() {
    
    fun toDomain(entity: MovimientoManualEntity): MovimientoManual {
        return MovimientoManual(
            id = entity.id,
            fecha = entity.fecha,
            descripcion = entity.descripcion,
            monto = entity.monto,
            tipo = entity.tipo,
            categoriaId = entity.categoriaId,
            notas = entity.notas
        )
    }
    
    fun toEntity(domain: MovimientoManual): MovimientoManualEntity {
        return MovimientoManualEntity(
            id = domain.id,
            fecha = domain.fecha,
            descripcion = domain.descripcion,
            descripcionLimpia = limpiarDescripcion(domain.descripcion),
            monto = domain.monto,
            tipo = domain.tipo,
            categoriaId = domain.categoriaId,
            notas = domain.notas
        )
    }

    private fun limpiarDescripcion(descripcion: String): String {
        return descripcion
            .replace(Regex("[0-9]+"), "")
            .replace(Regex("[^A-Za-zÁÉÍÓÚáéíóúÑñüÜ\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }
} 