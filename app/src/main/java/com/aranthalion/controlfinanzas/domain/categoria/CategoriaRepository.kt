package com.aranthalion.controlfinanzas.domain.categoria

import kotlinx.coroutines.flow.Flow

interface CategoriaRepository {
    fun getAllCategorias(): Flow<List<Categoria>>
    fun getCategoriasByTipo(tipo: String): Flow<List<Categoria>>
    suspend fun insertCategoria(categoria: Categoria)
    suspend fun deleteCategoria(categoria: Categoria)
    suspend fun insertDefaultCategorias()
} 