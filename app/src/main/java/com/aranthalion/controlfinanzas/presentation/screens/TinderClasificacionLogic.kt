package com.aranthalion.controlfinanzas.presentation.screens

import com.aranthalion.controlfinanzas.data.util.ExcelTransaction
import kotlinx.coroutines.flow.MutableStateFlow

class TinderClasificacionLogic {
    fun cargarTransaccionesEspecificas(transacciones: List<ExcelTransaction>, uiState: MutableStateFlow<TinderClasificacionUiState>) {
        // Implementar la lógica de carga aquí, actualizando uiState
    }
    fun aceptarTransaccion(uiState: MutableStateFlow<TinderClasificacionUiState>) {
        // Implementar la lógica de aceptación aquí
    }
    fun rechazarTransaccion(uiState: MutableStateFlow<TinderClasificacionUiState>) {
        // Implementar la lógica de rechazo aquí
    }
    fun seleccionarCategoria(id: Long, uiState: MutableStateFlow<TinderClasificacionUiState>) {
        // Implementar la lógica de selección de categoría aquí
    }
    fun mostrarSelectorManual(uiState: MutableStateFlow<TinderClasificacionUiState>) {
        // Implementar la lógica para mostrar el selector manual
    }
    fun confirmarClasificacion(uiState: MutableStateFlow<TinderClasificacionUiState>) {
        // Implementar la lógica de confirmación aquí
    }
    fun seleccionarCategoriaManual(id: Long, uiState: MutableStateFlow<TinderClasificacionUiState>) {
        // Implementar la lógica de selección manual aquí
    }
} 