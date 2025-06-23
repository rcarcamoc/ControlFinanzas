package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.entity.SueldoEntity
import com.aranthalion.controlfinanzas.domain.usecase.AporteProporcionalUseCase
import com.aranthalion.controlfinanzas.domain.usecase.AporteProporcional
import com.aranthalion.controlfinanzas.domain.usecase.ResumenAporteProporcional
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AporteProporcionalViewModel @Inject constructor(
    private val aporteProporcionalUseCase: AporteProporcionalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AporteProporcionalUiState>(AporteProporcionalUiState.Loading)
    val uiState: StateFlow<AporteProporcionalUiState> = _uiState.asStateFlow()

    private val _periodoSeleccionado = MutableStateFlow(obtenerPeriodoActual())
    val periodoSeleccionado: StateFlow<String> = _periodoSeleccionado.asStateFlow()

    private val _periodosDisponibles = MutableStateFlow<List<String>>(emptyList())
    val periodosDisponibles: StateFlow<List<String>> = _periodosDisponibles.asStateFlow()

    private val _personasDisponibles = MutableStateFlow<List<String>>(emptyList())
    val personasDisponibles: StateFlow<List<String>> = _personasDisponibles.asStateFlow()

    private val _sueldosActuales = MutableStateFlow<List<SueldoEntity>>(emptyList())
    val sueldosActuales: StateFlow<List<SueldoEntity>> = _sueldosActuales.asStateFlow()

    init {
        cargarDatosIniciales()
    }

    private fun cargarDatosIniciales() {
        viewModelScope.launch {
            try {
                _uiState.value = AporteProporcionalUiState.Loading
                
                // Cargar períodos y personas disponibles
                val periodos = aporteProporcionalUseCase.obtenerPeriodosDisponibles()
                val personas = aporteProporcionalUseCase.obtenerPersonasDisponibles()
                
                _periodosDisponibles.value = periodos
                _personasDisponibles.value = personas
                
                // Calcular aporte proporcional para el período actual
                calcularAporteProporcional(_periodoSeleccionado.value)
                
            } catch (e: Exception) {
                _uiState.value = AporteProporcionalUiState.Error(e.message ?: "Error al cargar datos")
            }
        }
    }

    fun calcularAporteProporcional(periodo: String) {
        viewModelScope.launch {
            try {
                _uiState.value = AporteProporcionalUiState.Loading
                _periodoSeleccionado.value = periodo
                
                // Cargar sueldos del período
                val sueldos = aporteProporcionalUseCase.obtenerSueldosPorPeriodo(periodo)
                _sueldosActuales.value = sueldos
                
                val resumen = aporteProporcionalUseCase.calcularAporteProporcional(periodo)
                _uiState.value = AporteProporcionalUiState.Success(resumen)
                
            } catch (e: Exception) {
                _uiState.value = AporteProporcionalUiState.Error(e.message ?: "Error al calcular aporte proporcional")
            }
        }
    }

    fun guardarSueldo(nombrePersona: String, periodo: String, sueldo: Double) {
        viewModelScope.launch {
            try {
                aporteProporcionalUseCase.guardarSueldo(nombrePersona, periodo, sueldo)
                
                // Recargar datos
                recargarDatos()
                
            } catch (e: Exception) {
                _uiState.value = AporteProporcionalUiState.Error(e.message ?: "Error al guardar sueldo")
            }
        }
    }

    fun actualizarSueldo(sueldo: SueldoEntity) {
        viewModelScope.launch {
            try {
                aporteProporcionalUseCase.actualizarSueldo(sueldo)
                
                // Recargar datos
                recargarDatos()
                
            } catch (e: Exception) {
                _uiState.value = AporteProporcionalUiState.Error(e.message ?: "Error al actualizar sueldo")
            }
        }
    }

    fun eliminarSueldo(sueldo: SueldoEntity) {
        viewModelScope.launch {
            try {
                aporteProporcionalUseCase.eliminarSueldo(sueldo)
                
                // Recargar datos
                recargarDatos()
                
            } catch (e: Exception) {
                _uiState.value = AporteProporcionalUiState.Error(e.message ?: "Error al eliminar sueldo")
            }
        }
    }

    private fun recargarDatos() {
        viewModelScope.launch {
            try {
                // Recargar períodos y personas disponibles
                val periodos = aporteProporcionalUseCase.obtenerPeriodosDisponibles()
                val personas = aporteProporcionalUseCase.obtenerPersonasDisponibles()
                
                _periodosDisponibles.value = periodos
                _personasDisponibles.value = personas
                
                // Recargar sueldos del período actual
                val sueldos = aporteProporcionalUseCase.obtenerSueldosPorPeriodo(_periodoSeleccionado.value)
                _sueldosActuales.value = sueldos
                
                // Recalcular aporte proporcional
                calcularAporteProporcional(_periodoSeleccionado.value)
                
            } catch (e: Exception) {
                _uiState.value = AporteProporcionalUiState.Error(e.message ?: "Error al recargar datos")
            }
        }
    }

    fun obtenerHistorialAportes(periodos: List<String>) {
        viewModelScope.launch {
            try {
                _uiState.value = AporteProporcionalUiState.Loading
                val historial = aporteProporcionalUseCase.obtenerHistorialAportes(periodos)
                _uiState.value = AporteProporcionalUiState.HistorialSuccess(historial)
                
            } catch (e: Exception) {
                _uiState.value = AporteProporcionalUiState.Error(e.message ?: "Error al cargar historial")
            }
        }
    }

    private fun obtenerPeriodoActual(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }
}

sealed class AporteProporcionalUiState {
    object Loading : AporteProporcionalUiState()
    data class Success(val resumen: ResumenAporteProporcional) : AporteProporcionalUiState()
    data class HistorialSuccess(val historial: List<ResumenAporteProporcional>) : AporteProporcionalUiState()
    data class Error(val mensaje: String) : AporteProporcionalUiState()
} 