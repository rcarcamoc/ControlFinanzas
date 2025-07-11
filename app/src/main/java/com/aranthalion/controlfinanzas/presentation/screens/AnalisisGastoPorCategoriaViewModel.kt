package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.usecase.AnalisisGastoPorCategoriaUseCase
import com.aranthalion.controlfinanzas.domain.usecase.AnalisisGastoCategoria
import com.aranthalion.controlfinanzas.domain.usecase.ResumenAnalisisGasto
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.domain.usecase.GestionarMovimientosUseCase
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalisisGastoPorCategoriaUiState(
    val isLoading: Boolean = false,
    val analisis: List<AnalisisGastoCategoria> = emptyList(),
    val resumen: ResumenAnalisisGasto? = null,
    val error: String? = null,
    val periodoActual: String = ""
)

@HiltViewModel
class AnalisisGastoPorCategoriaViewModel @Inject constructor(
    private val analisisGastoUseCase: AnalisisGastoPorCategoriaUseCase,
    private val configuracionPreferences: ConfiguracionPreferences,
    private val gestionarMovimientosUseCase: GestionarMovimientosUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalisisGastoPorCategoriaUiState())
    val uiState: StateFlow<AnalisisGastoPorCategoriaUiState> = _uiState.asStateFlow()
    
    init {
        cargarAnalisis()
    }
    
    fun cargarAnalisis() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val periodoActual = configuracionPreferences.obtenerPeriodoGlobal() ?: obtenerPeriodoActual()
                val analisis = analisisGastoUseCase.obtenerAnalisisGastoPorCategoria(periodoActual)
                val resumen = analisisGastoUseCase.obtenerResumenAnalisis(periodoActual)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analisis = analisis,
                    resumen = resumen,
                    periodoActual = periodoActual
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar el análisis: ${e.message}"
                )
            }
        }
    }
    
    fun obtenerCategoriasConAlerta(): List<AnalisisGastoCategoria> {
        return _uiState.value.analisis.filter { 
            it.estado == com.aranthalion.controlfinanzas.domain.usecase.EstadoAnalisis.CRITICO || 
            it.estado == com.aranthalion.controlfinanzas.domain.usecase.EstadoAnalisis.ADVERTENCIA 
        }
    }
    
    fun obtenerCategoriasExcelentes(): List<AnalisisGastoCategoria> {
        return _uiState.value.analisis.filter { 
            it.estado == com.aranthalion.controlfinanzas.domain.usecase.EstadoAnalisis.EXCELENTE 
        }
    }
    
    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun obtenerPeriodoActual(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date())
    }

    suspend fun obtenerMovimientosPorCategoriaYPeriodo(categoriaId: Long, periodo: String): List<MovimientoEntity> {
        val movimientos = gestionarMovimientosUseCase.obtenerMovimientos()
        return movimientos.filter {
            (if (categoriaId == -1L) it.categoriaId == null else it.categoriaId == categoriaId) &&
            it.periodoFacturacion == periodo &&
            it.tipo != "OMITIR"
        }
    }
} 