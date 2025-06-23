package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import javax.inject.Inject

interface CategoriaRepository {
    suspend fun obtenerCategorias(): List<Categoria>
    suspend fun agregarCategoria(categoria: Categoria)
    suspend fun actualizarCategoria(categoria: Categoria)
    suspend fun eliminarCategoria(categoria: Categoria)
    suspend fun insertarCategoriasDefault()
    suspend fun existeCategoria(nombre: String): Boolean
    suspend fun limpiarYEliminarDuplicados()
} 