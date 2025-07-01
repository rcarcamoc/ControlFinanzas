package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.usecase.AnalisisFinancieroUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Datos de prueba para desarrollo
data class CategoriaPrueba(
    val nombreCategoria: String,
    val totalGastado: Double,
    val porcentajeDelTotal: Double,
    val promedioDiario: Double,
    val tendencia: String
)

data class PrediccionPrueba(
    val nombreCategoria: String,
    val prediccion: Double,
    val intervaloConfianza: Pair<Double, Double>,
    val confiabilidad: Double
)

@HiltViewModel
class DashboardAnalisisViewModel @Inject constructor(
    private val analisisFinancieroUseCase: AnalisisFinancieroUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardAnalisisUiState>(DashboardAnalisisUiState.Loading)
    val uiState: StateFlow<DashboardAnalisisUiState> = _uiState.asStateFlow()

    fun cargarAnalisis(periodo: String) {
        viewModelScope.launch {
            try {
                _uiState.value = DashboardAnalisisUiState.Loading

                // Crear datos de prueba para desarrollo
                val kpis = listOf("KPI 1", "KPI 2", "KPI 3")
                val tendencias = listOf("Tendencia 1", "Tendencia 2")
                
                val categorias = listOf(
                    CategoriaPrueba("Alimentación", 150000.0, 25.0, 5000.0, "AUMENTO"),
                    CategoriaPrueba("Transporte", 80000.0, 13.3, 2667.0, "ESTABLE"),
                    CategoriaPrueba("Entretenimiento", 120000.0, 20.0, 4000.0, "DISMINUCION"),
                    CategoriaPrueba("Salud", 90000.0, 15.0, 3000.0, "AUMENTO"),
                    CategoriaPrueba("Servicios", 60000.0, 10.0, 2000.0, "ESTABLE")
                )
                
                val predicciones = listOf(
                    PrediccionPrueba("Alimentación", 160000.0, Pair(150000.0, 170000.0), 0.85),
                    PrediccionPrueba("Transporte", 85000.0, Pair(80000.0, 90000.0), 0.92),
                    PrediccionPrueba("Entretenimiento", 110000.0, Pair(100000.0, 120000.0), 0.78),
                    PrediccionPrueba("Salud", 95000.0, Pair(90000.0, 100000.0), 0.88),
                    PrediccionPrueba("Servicios", 65000.0, Pair(60000.0, 70000.0), 0.75)
                )

                _uiState.value = DashboardAnalisisUiState.Success(
                    kpis = kpis,
                    tendencias = tendencias,
                    analisisCategorias = categorias,
                    predicciones = predicciones
                )
            } catch (e: Exception) {
                _uiState.value = DashboardAnalisisUiState.Error(
                    e.message ?: "Error al cargar análisis financiero"
                )
            }
        }
    }
} 