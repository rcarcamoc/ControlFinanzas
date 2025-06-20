package com.aranthalion.controlfinanzas.presentation.categoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.categoria.GestionarCategoriasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriaViewModel @Inject constructor(
    private val gestionarCategoriasUseCase: GestionarCategoriasUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriaUiState())
    val uiState: StateFlow<CategoriaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            gestionarCategoriasUseCase.getAllCategorias()
                .collect { categorias ->
                    _uiState.update { it.copy(categorias = categorias) }
                }
        }
    }

    fun agregarCategoria(nombre: String, tipo: String) {
        viewModelScope.launch {
            gestionarCategoriasUseCase.insertCategoria(
                Categoria(nombre = nombre, descripcion = tipo)
            )
        }
    }

    fun eliminarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            gestionarCategoriasUseCase.deleteCategoria(categoria)
        }
    }

    fun insertarCategoriasDefault() {
        viewModelScope.launch {
            gestionarCategoriasUseCase.insertDefaultCategorias()
        }
    }
}

data class CategoriaUiState(
    val categorias: List<Categoria> = emptyList()
) 