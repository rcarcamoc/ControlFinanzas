package com.aranthalion.controlfinanzas.domain.categoria

import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import kotlinx.coroutines.flow.Flow

interface CategoriaRepository {
    fun getAllCategorias(): Flow<List<Categoria>>
    fun getCategoriasByTipo(tipo: String): Flow<List<Categoria>>
    suspend fun insertCategoria(categoria: Categoria)
    suspend fun updateCategoria(categoria: Categoria)
    suspend fun deleteCategoria(categoria: Categoria)
    suspend fun insertDefaultCategorias()
    suspend fun existeCategoria(nombre: String): Boolean
    suspend fun limpiarYEliminarDuplicados()
    suspend fun obtenerCategorias(): List<Categoria>
} 