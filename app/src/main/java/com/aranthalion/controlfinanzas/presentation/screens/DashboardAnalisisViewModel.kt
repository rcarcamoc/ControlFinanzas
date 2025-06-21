package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.usecase.AnalisisFinancieroUseCase
import com.aranthalion.controlfinanzas.domain.usecase.MovimientoPorCategoria
import com.aranthalion.controlfinanzas.domain.usecase.ResumenFinanciero
import com.aranthalion.controlfinanzas.domain.usecase.TendenciaMensual
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class PeriodoAnalisis(val meses: Int, val descripcion: String) {
    ULTIMO_MES(1, "Último mes"),
    ULTIMOS_3_MESES(3, "Últimos 3 meses"),
    ULTIMOS_6_MESES(6, "Últimos 6 meses"),
    ULTIMOS_12_MESES(12, "Últimos 12 meses")
}

data class DashboardAnalisisUiState(
    val isLoading: Boolean = false,
    val resumenFinanciero: ResumenFinanciero? = null,
    val movimientosPorCategoria: List<MovimientoPorCategoria> = emptyList(),
    val tendenciaMensual: List<TendenciaMensual> = emptyList(),
    val periodoSeleccionado: PeriodoAnalisis = PeriodoAnalisis.ULTIMO_MES,
    val error: String? = null
)

@HiltViewModel
class DashboardAnalisisViewModel @Inject constructor(
    private val analisisFinancieroUseCase: AnalisisFinancieroUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardAnalisisUiState())
    val uiState: StateFlow<DashboardAnalisisUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cambiarPeriodo(periodo: PeriodoAnalisis) {
        _uiState.value = _uiState.value.copy(periodoSeleccionado = periodo)
        cargarDatos()
    }

    private fun cargarDatos() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val periodo = _uiState.value.periodoSeleccionado
                val (fechaInicio, fechaFin) = calcularFechasPeriodo(periodo)
                
                // Cargar resumen financiero
                val resumen = analisisFinancieroUseCase.obtenerResumenFinanciero(fechaInicio, fechaFin)
                
                // Cargar movimientos por categoría
                val movimientosPorCategoria = analisisFinancieroUseCase.obtenerMovimientosPorCategoria(fechaInicio, fechaFin)
                
                // Cargar tendencia mensual
                val tendenciaMensual = analisisFinancieroUseCase.obtenerTendenciaMensual(periodo.meses)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    resumenFinanciero = resumen,
                    movimientosPorCategoria = movimientosPorCategoria,
                    tendenciaMensual = tendenciaMensual
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar datos: ${e.message}"
                )
            }
        }
    }

    private fun calcularFechasPeriodo(periodo: PeriodoAnalisis): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val fechaFin = calendar.time
        
        calendar.add(Calendar.MONTH, -periodo.meses)
        val fechaInicio = calendar.time
        
        return Pair(fechaInicio, fechaFin)
    }

    fun recargarDatos() {
        cargarDatos()
    }
} 