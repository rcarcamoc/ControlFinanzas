package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClasificacionPendienteViewModel @Inject constructor(
    private val movimientoRepository: MovimientoRepository,
    private val clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ClasificacionPendienteUiState>(ClasificacionPendienteUiState.Loading)
    val uiState: StateFlow<ClasificacionPendienteUiState> = _uiState.asStateFlow()

    init {
        cargarTransaccionesPendientes()
    }

    private fun cargarTransaccionesPendientes() {
        viewModelScope.launch {
            try {
                _uiState.value = ClasificacionPendienteUiState.Loading
                
                // Obtener todas las transacciones sin categoría asignada
                val todasLasTransacciones = movimientoRepository.obtenerMovimientos()
                val transaccionesPendientes = todasLasTransacciones.filter { 
                    it.categoriaId == null || it.categoriaId == 0L 
                }
                
                _uiState.value = ClasificacionPendienteUiState.Success(transaccionesPendientes)
            } catch (e: Exception) {
                _uiState.value = ClasificacionPendienteUiState.Error(
                    "Error al cargar transacciones: ${e.message}"
                )
            }
        }
    }

    fun clasificarTransaccion(transaccion: MovimientoEntity, categoriaId: Long) {
        viewModelScope.launch {
            try {
                // Actualizar la transacción con la categoría seleccionada
                val transaccionActualizada = transaccion.copy(categoriaId = categoriaId)
                movimientoRepository.actualizarMovimiento(transaccionActualizada)
                
                // Aprender del patrón para futuras clasificaciones
                clasificacionUseCase.aprenderPatron(transaccion.descripcion, categoriaId)
                
                // Recargar la lista de transacciones pendientes
                cargarTransaccionesPendientes()
            } catch (e: Exception) {
                _uiState.value = ClasificacionPendienteUiState.Error(
                    "Error al clasificar transacción: ${e.message}"
                )
            }
        }
    }

    fun recargarTransacciones() {
        cargarTransaccionesPendientes()
    }
} 