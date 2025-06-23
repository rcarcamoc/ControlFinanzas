package com.aranthalion.controlfinanzas.presentation.categoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.categoria.GestionarCategoriasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriasViewModel @Inject constructor(
    private val gestionarCategoriasUseCase: GestionarCategoriasUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriasUiState>(CategoriasUiState.Loading)
    val uiState: StateFlow<CategoriasUiState> = _uiState

    init {
        viewModelScope.launch {
            gestionarCategoriasUseCase.limpiarYEliminarDuplicados()
            cargarCategorias()
        }
    }

    private fun cargarCategorias() {
        viewModelScope.launch {
            _uiState.value = CategoriasUiState.Loading
            gestionarCategoriasUseCase.getAllCategorias()
                .catch { e ->
                    _uiState.value = CategoriasUiState.Error(e.message ?: "Error desconocido")
                }
                .collect { categorias ->
                    if (categorias.isEmpty()) {
                        gestionarCategoriasUseCase.insertDefaultCategorias()
                    } else {
                        _uiState.value = CategoriasUiState.Success(categorias)
                    }
                }
        }
    }

    fun agregarCategoria(nombre: String, descripcion: String) {
        viewModelScope.launch {
            try {
                val nombreNormalizado = nombre.trim().lowercase()
                    .replace("á", "a")
                    .replace("é", "e")
                    .replace("í", "i")
                    .replace("ó", "o")
                    .replace("ú", "u")
                    .replace("ñ", "n")
                if (gestionarCategoriasUseCase.existeCategoria(nombreNormalizado)) {
                    _uiState.value = CategoriasUiState.Error("Ya existe una categoría con ese nombre.")
                    return@launch
                }
                val categoria = Categoria(
                    nombre = nombreNormalizado,
                    descripcion = descripcion
                )
                gestionarCategoriasUseCase.insertCategoria(categoria)
            } catch (e: Exception) {
                _uiState.value = CategoriasUiState.Error(e.message ?: "Error al agregar categoría")
            }
        }
    }

    fun eliminarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            try {
                gestionarCategoriasUseCase.deleteCategoria(categoria)
            } catch (e: Exception) {
                _uiState.value = CategoriasUiState.Error(e.message ?: "Error al eliminar categoría")
            }
        }
    }
} 