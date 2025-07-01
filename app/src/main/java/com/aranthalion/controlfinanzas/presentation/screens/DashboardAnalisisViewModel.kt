package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.usecase.AnalisisFinancieroUseCase
import com.aranthalion.controlfinanzas.domain.usecase.*
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

sealed class DashboardAnalisisUiState {
    object Loading : DashboardAnalisisUiState()
    data class Success(
        val kpis: List<KPIFinanciero>,
        val tendencias: List<TendenciaMensual>,
        val analisisCategorias: List<AnalisisCategoria>,
        val predicciones: List<PrediccionGasto>,
        val analisisTendenciaTemporal: List<AnalisisTendenciaTemporal>,
        val analisisVolatilidad: List<AnalisisVolatilidad>,
        val comparacionPeriodo: ComparacionPeriodo?,
        val gastosInusuales: List<GastoInusual>,
        val metricasRendimiento: MetricasRendimiento?,
        val resumenFinanciero: ResumenFinanciero?
    ) : DashboardAnalisisUiState()
    data class Error(val mensaje: String) : DashboardAnalisisUiState()
}

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

                // Cargar todos los análisis en paralelo para mejor rendimiento
                val kpis = analisisFinancieroUseCase.obtenerKPIsJerarquicos(periodo)
                val tendencias = analisisFinancieroUseCase.obtenerTendenciasMensuales(6)
                val analisisCategorias = analisisFinancieroUseCase.obtenerAnalisisCategorias(periodo)
                val predicciones = analisisFinancieroUseCase.obtenerPrediccionesGasto(periodo)
                val analisisTendenciaTemporal = analisisFinancieroUseCase.obtenerAnalisisTendenciaTemporal(meses = 6)
                val analisisVolatilidad = analisisFinancieroUseCase.obtenerAnalisisVolatilidad(periodo)
                val comparacionPeriodo = analisisFinancieroUseCase.obtenerComparacionPeriodo(periodo)
                val gastosInusuales = analisisFinancieroUseCase.obtenerGastosInusuales(periodo)
                val metricasRendimiento = analisisFinancieroUseCase.obtenerMetricasRendimiento(periodo)
                val resumenFinanciero = analisisFinancieroUseCase.obtenerResumenFinancieroPorPeriodo(periodo)

                _uiState.value = DashboardAnalisisUiState.Success(
                    kpis = kpis,
                    tendencias = tendencias,
                    analisisCategorias = analisisCategorias,
                    predicciones = predicciones,
                    analisisTendenciaTemporal = analisisTendenciaTemporal,
                    analisisVolatilidad = analisisVolatilidad,
                    comparacionPeriodo = comparacionPeriodo,
                    gastosInusuales = gastosInusuales,
                    metricasRendimiento = metricasRendimiento,
                    resumenFinanciero = resumenFinanciero
                )
            } catch (e: Exception) {
                _uiState.value = DashboardAnalisisUiState.Error(
                    e.message ?: "Error al cargar análisis financiero"
                )
            }
        }
    }

    fun cargarAnalisisTendenciaCategoria(categoriaId: Long, meses: Int = 6) {
        viewModelScope.launch {
            try {
                val tendenciaCategoria = analisisFinancieroUseCase.obtenerAnalisisTendenciaTemporal(categoriaId, meses)
                
                // Actualizar el estado actual con la nueva tendencia de categoría
                val currentState = _uiState.value
                if (currentState is DashboardAnalisisUiState.Success) {
                    _uiState.value = currentState.copy(
                        analisisTendenciaTemporal = tendenciaCategoria
                    )
                }
            } catch (e: Exception) {
                // Manejar error sin cambiar el estado principal
                println("Error al cargar tendencia de categoría: ${e.message}")
            }
        }
    }

    fun obtenerInsightsFinancieros(): List<String> {
        val currentState = _uiState.value
        if (currentState !is DashboardAnalisisUiState.Success) return emptyList()

        val insights = mutableListOf<String>()

        // Insight sobre tasa de ahorro
        currentState.resumenFinanciero?.let { resumen ->
            when {
                resumen.tasaAhorro > 20 -> insights.add("¡Excelente! Tu tasa de ahorro del ${resumen.tasaAhorro.toInt()}% está por encima del objetivo recomendado.")
                resumen.tasaAhorro > 10 -> insights.add("Buena tasa de ahorro del ${resumen.tasaAhorro.toInt()}%. Considera aumentar un 5% más para mayor seguridad financiera.")
                else -> insights.add("Tu tasa de ahorro del ${resumen.tasaAhorro.toInt()}% está por debajo del recomendado. Revisa tus gastos no esenciales.")
            }
        }

        // Insight sobre gastos inusuales
        if (currentState.gastosInusuales.isNotEmpty()) {
            val gastoMasInusual = currentState.gastosInusuales.first()
            insights.add("Detectamos un gasto inusual: ${gastoMasInusual.descripcion} por ${gastoMasInusual.monto.toInt()}. Revisa si fue necesario.")
        }

        // Insight sobre volatilidad
        val categoriasVolatiles = currentState.analisisVolatilidad.filter { it.nivelVolatilidad == "ALTA" }
        if (categoriasVolatiles.isNotEmpty()) {
            val categoriaMasVolatil = categoriasVolatiles.first()
            insights.add("La categoría '${categoriaMasVolatil.nombreCategoria}' muestra alta volatilidad. Considera establecer un presupuesto fijo.")
        }

        // Insight sobre comparación con período anterior
        currentState.comparacionPeriodo?.let { comparacion ->
            when {
                comparacion.cambioBalance > 10 -> insights.add("¡Excelente progreso! Tu balance mejoró ${comparacion.cambioBalance.toInt()}% respecto al período anterior.")
                comparacion.cambioBalance < -10 -> insights.add("Tu balance disminuyó ${comparacion.cambioBalance.toInt()}%. Revisa tus gastos y considera ajustar tu presupuesto.")
            }
        }

        // Insight sobre score financiero
        currentState.metricasRendimiento?.let { metricas ->
            when {
                metricas.scoreFinanciero >= 80 -> insights.add("¡Tu salud financiera es excelente! Score: ${metricas.scoreFinanciero}/100")
                metricas.scoreFinanciero >= 60 -> insights.add("Tu salud financiera es buena. Score: ${metricas.scoreFinanciero}/100. Hay espacio para mejorar.")
                else -> insights.add("Tu salud financiera necesita atención. Score: ${metricas.scoreFinanciero}/100. Considera revisar tus hábitos financieros.")
            }
        }

        return insights.take(5) // Máximo 5 insights
    }

    fun obtenerRecomendaciones(): List<String> {
        val currentState = _uiState.value
        if (currentState !is DashboardAnalisisUiState.Success) return emptyList()

        val recomendaciones = mutableListOf<String>()

        // Recomendación basada en tasa de ahorro
        currentState.resumenFinanciero?.let { resumen ->
            if (resumen.tasaAhorro < 15) {
                recomendaciones.add("Aumenta tu tasa de ahorro estableciendo transferencias automáticas al inicio del mes.")
            }
        }

        // Recomendación basada en gastos inusuales
        if (currentState.gastosInusuales.size > 2) {
            recomendaciones.add("Tienes varios gastos inusuales. Revisa si puedes reducir gastos no esenciales.")
        }

        // Recomendación basada en volatilidad
        val categoriasVolatiles = currentState.analisisVolatilidad.filter { it.nivelVolatilidad == "ALTA" }
        if (categoriasVolatiles.isNotEmpty()) {
            recomendaciones.add("Establece presupuestos fijos para categorías con alta volatilidad para mejor control.")
        }

        // Recomendación basada en predicciones
        val prediccionesAltas = currentState.predicciones.filter { it.prediccion > 100000 }
        if (prediccionesAltas.isNotEmpty()) {
            recomendaciones.add("Las predicciones indican gastos altos en algunas categorías. Planifica con anticipación.")
        }

        // Recomendación basada en score financiero
        currentState.metricasRendimiento?.let { metricas ->
            if (metricas.scoreFinanciero < 70) {
                recomendaciones.add("Mejora tu score financiero diversificando gastos y aumentando ahorros.")
            }
        }

        return recomendaciones.take(3) // Máximo 3 recomendaciones
    }
} 