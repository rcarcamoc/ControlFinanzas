package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.usecase.InsightsAvanzadosUseCase
import com.aranthalion.controlfinanzas.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class InsightsAvanzadosUiState {
    object Loading : InsightsAvanzadosUiState()
    data class Success(
        val insights: List<InsightComportamiento>,
        val agrupaciones: List<AgrupacionTransacciones>,
        val recomendaciones: List<RecomendacionPersonalizada>,
        val patronesTemporales: List<AnalisisPatronTemporal>,
        val resumen: ResumenInsights,
        val periodo: String
    ) : InsightsAvanzadosUiState()
    data class Error(val mensaje: String, val onRetry: () -> Unit) : InsightsAvanzadosUiState()
}

@HiltViewModel
class InsightsAvanzadosViewModel @Inject constructor(
    private val insightsAvanzadosUseCase: InsightsAvanzadosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<InsightsAvanzadosUiState>(InsightsAvanzadosUiState.Loading)
    val uiState: StateFlow<InsightsAvanzadosUiState> = _uiState.asStateFlow()

    private var lastPeriodo: String? = null
    private var lastSuccessState: InsightsAvanzadosUiState.Success? = null

    fun cargarInsights(periodo: String, force: Boolean = false) {
        if (!force && lastPeriodo == periodo && lastSuccessState != null) {
            _uiState.value = lastSuccessState!!
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = InsightsAvanzadosUiState.Loading
                lastPeriodo = periodo

                // Cargar todos los análisis en paralelo
                val insights = insightsAvanzadosUseCase.generarInsightsAvanzados(periodo)
                val agrupaciones = insightsAvanzadosUseCase.generarAgrupaciones(periodo)
                val recomendaciones = insightsAvanzadosUseCase.generarRecomendacionesPersonalizadas(periodo)
                val patronesTemporales = insightsAvanzadosUseCase.analizarPatronesTemporales(periodo)
                val resumen = insightsAvanzadosUseCase.generarResumenInsights(periodo)

                val successState = InsightsAvanzadosUiState.Success(
                    insights = insights,
                    agrupaciones = agrupaciones,
                    recomendaciones = recomendaciones,
                    patronesTemporales = patronesTemporales,
                    resumen = resumen,
                    periodo = periodo
                )

                lastSuccessState = successState
                _uiState.value = successState

            } catch (e: Exception) {
                _uiState.value = InsightsAvanzadosUiState.Error(
                    mensaje = "Error al cargar insights: ${e.message}",
                    onRetry = { cargarInsights(periodo, force = true) }
                )
            }
        }
    }

    fun obtenerInsightsPorSeveridad(severidad: SeveridadInsight): List<InsightComportamiento> {
        val currentState = _uiState.value
        if (currentState !is InsightsAvanzadosUiState.Success) return emptyList()
        return currentState.insights.filter { it.severidad == severidad }
    }

    fun obtenerAgrupacionesPorTipo(tipo: TipoAgrupacion): List<AgrupacionTransacciones> {
        val currentState = _uiState.value
        if (currentState !is InsightsAvanzadosUiState.Success) return emptyList()
        return currentState.agrupaciones.filter { it.tipo == tipo }
    }

    fun obtenerRecomendacionesPorPrioridad(prioridad: PrioridadRecomendacion): List<RecomendacionPersonalizada> {
        val currentState = _uiState.value
        if (currentState !is InsightsAvanzadosUiState.Success) return emptyList()
        return currentState.recomendaciones.filter { it.prioridad == prioridad }
    }

    fun obtenerInsightsCriticos(): List<InsightComportamiento> {
        return obtenerInsightsPorSeveridad(SeveridadInsight.ALTA)
    }

    fun obtenerInsightsPositivos(): List<InsightComportamiento> {
        return obtenerInsightsPorSeveridad(SeveridadInsight.POSITIVA)
    }

    fun obtenerRecomendacionesAltaPrioridad(): List<RecomendacionPersonalizada> {
        return obtenerRecomendacionesPorPrioridad(PrioridadRecomendacion.ALTA)
    }

    fun obtenerAgrupacionesMasRelevantes(): List<AgrupacionTransacciones> {
        val currentState = _uiState.value
        if (currentState !is InsightsAvanzadosUiState.Success) return emptyList()
        return currentState.agrupaciones.take(5) // Top 5 por total
    }

    fun obtenerPatronesMasFrecuentes(): List<AnalisisPatronTemporal> {
        val currentState = _uiState.value
        if (currentState !is InsightsAvanzadosUiState.Success) return emptyList()
        return currentState.patronesTemporales.sortedByDescending { it.frecuencia }.take(5)
    }

    fun obtenerScoreComportamiento(): Int {
        val currentState = _uiState.value
        if (currentState !is InsightsAvanzadosUiState.Success) return 0
        return currentState.resumen.scoreComportamiento
    }

    fun obtenerAreasMejora(): List<String> {
        val currentState = _uiState.value
        if (currentState !is InsightsAvanzadosUiState.Success) return emptyList()
        return currentState.resumen.areasMejora
    }

    fun obtenerFortalezas(): List<String> {
        val currentState = _uiState.value
        if (currentState !is InsightsAvanzadosUiState.Success) return emptyList()
        return currentState.resumen.fortalezas
    }
} 