package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.entity.Categoria as CategoriaEntity
import com.aranthalion.controlfinanzas.domain.categoria.Categoria

object CategoriaMapper {
    fun fromEntity(entity: CategoriaEntity): Categoria = Categoria(
        id = entity.id,
        nombre = entity.nombre,
        descripcion = entity.descripcion,
        tipo = entity.tipo,
        presupuestoMensual = entity.presupuestoMensual
    )

    fun toEntity(domain: Categoria): CategoriaEntity = CategoriaEntity(
        id = domain.id,
        nombre = domain.nombre,
        descripcion = domain.descripcion,
        tipo = domain.tipo,
        presupuestoMensual = domain.presupuestoMensual
    )
} 