package com.aranthalion.controlfinanzas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.data.repository.SueldoRepository
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirstRunViewModel @Inject constructor(
    private val configuracionPreferences: ConfiguracionPreferences,
    private val categoriaRepository: CategoriaRepository,
    private val movimientoRepository: MovimientoRepository,
    private val presupuestoCategoriaRepository: PresupuestoCategoriaRepository,
    private val sueldoRepository: SueldoRepository,
    private val clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FirstRunUiState>(FirstRunUiState.Initial)
    val uiState: StateFlow<FirstRunUiState> = _uiState.asStateFlow()

    fun cargarDatosHistoricos() {
        viewModelScope.launch {
            try {
                _uiState.value = FirstRunUiState.Loading
                
                // Cargar categorías por defecto
                categoriaRepository.insertDefaultCategorias()
                
                // Cargar datos históricos de movimientos
                movimientoRepository.cargarDatosHistoricos()
                
                // Cargar sistema de clasificación automática
                clasificacionUseCase.cargarDatosHistoricos()
                
                // Marcar que se han cargado los datos históricos
                configuracionPreferences.markHistoricalDataLoaded()
                
                // Marcar que ya no es la primera ejecución
                configuracionPreferences.markFirstRunComplete()
                
                _uiState.value = FirstRunUiState.Success
                
            } catch (e: Exception) {
                _uiState.value = FirstRunUiState.Error(
                    "Error al cargar datos históricos: ${e.message}"
                )
            }
        }
    }

    fun comenzarDesdeCero() {
        viewModelScope.launch {
            try {
                _uiState.value = FirstRunUiState.Loading
                
                // Limpiar todos los datos históricos para una instalación completamente limpia
                movimientoRepository.limpiarTodosLosDatos()
                
                // Marcar que NO se han cargado datos históricos
                configuracionPreferences.markHistoricalDataNotLoaded()
                
                // Marcar que ya no es la primera ejecución
                configuracionPreferences.markFirstRunComplete()
                
                _uiState.value = FirstRunUiState.Success
                
            } catch (e: Exception) {
                _uiState.value = FirstRunUiState.Error(
                    "Error al configurar la aplicación: ${e.message}"
                )
            }
        }
    }

    fun cargarSoloCategorias() {
        viewModelScope.launch {
            try {
                _uiState.value = FirstRunUiState.Loading
                
                // Solo cargar categorías básicas por defecto
                categoriaRepository.insertDefaultCategorias()
                
                // Marcar que ya no es la primera ejecución
                configuracionPreferences.markFirstRunComplete()
                
                _uiState.value = FirstRunUiState.Success
                
            } catch (e: Exception) {
                _uiState.value = FirstRunUiState.Error(
                    "Error al cargar categorías: ${e.message}"
                )
            }
        }
    }

    fun resetError() {
        _uiState.value = FirstRunUiState.Initial
    }
}

sealed class FirstRunUiState {
    object Initial : FirstRunUiState()
    object Loading : FirstRunUiState()
    object Success : FirstRunUiState()
    data class Error(val mensaje: String) : FirstRunUiState()
} 