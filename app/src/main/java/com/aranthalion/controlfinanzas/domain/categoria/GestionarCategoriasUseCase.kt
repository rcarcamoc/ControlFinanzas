package com.aranthalion.controlfinanzas.domain.categoria

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GestionarCategoriasUseCase @Inject constructor(
    private val categoriaRepository: CategoriaRepository
) {
    fun getAllCategorias(): Flow<List<Categoria>> {
        return categoriaRepository.getAllCategorias()
    }

    fun getCategoriasByTipo(tipo: String): Flow<List<Categoria>> {
        return categoriaRepository.getCategoriasByTipo(tipo)
    }

    suspend fun insertCategoria(categoria: Categoria) {
        categoriaRepository.insertCategoria(categoria)
    }

    suspend fun deleteCategoria(categoria: Categoria) {
        categoriaRepository.deleteCategoria(categoria)
    }

    suspend fun insertDefaultCategorias() {
        categoriaRepository.insertDefaultCategorias()
    }

    suspend fun existeCategoria(nombre: String): Boolean {
        return categoriaRepository.existeCategoria(nombre)
    }

    suspend fun limpiarYEliminarDuplicados() {
        categoriaRepository.limpiarYEliminarDuplicados()
    }

    suspend fun actualizarCategoria(categoria: Categoria) {
        categoriaRepository.updateCategoria(categoria)
    }
} 