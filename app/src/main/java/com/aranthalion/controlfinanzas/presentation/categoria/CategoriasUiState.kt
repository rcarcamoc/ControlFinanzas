package com.aranthalion.controlfinanzas.presentation.categoria

import com.aranthalion.controlfinanzas.domain.categoria.Categoria

sealed class CategoriasUiState {
    data object Loading : CategoriasUiState()
    data class Success(val categorias: List<Categoria>) : CategoriasUiState()
    data class Error(val mensaje: String) : CategoriasUiState()
} 