package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.entity.CategoriaEntity
import com.aranthalion.controlfinanzas.domain.categoria.Categoria

fun CategoriaEntity.toDomain(): Categoria = Categoria(
    id = this.id.toInt(),
    nombre = this.nombre,
    descripcion = this.tipo
)

fun Categoria.toEntity(): CategoriaEntity = CategoriaEntity(
    id = this.id.toLong(),
    nombre = this.nombre,
    tipo = this.descripcion
) 