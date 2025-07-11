package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

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
        val resumenFinanciero: ResumenFinanciero?,
        val historicoCategorias: List<ResumenHistoricoCategoria>,
        val presupuestosConBrecha: List<PresupuestoConBrecha>
    ) : DashboardAnalisisUiState()
    data class Error(val mensaje: String, val onRetry: () -> Unit) : DashboardAnalisisUiState()
}

@HiltViewModel
class DashboardAnalisisViewModel @Inject constructor(
    private val analisisFinancieroUseCase: AnalisisFinancieroUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardAnalisisUiState>(DashboardAnalisisUiState.Loading)
    val uiState: StateFlow<DashboardAnalisisUiState> = _uiState.asStateFlow()

    private var lastPeriodo: String? = null
    private var lastSuccessState: DashboardAnalisisUiState.Success? = null

    fun cargarAnalisis(periodo: String, force: Boolean = false) {
        if (!force && lastPeriodo == periodo && lastSuccessState != null) {
            _uiState.value = lastSuccessState!!
            return
        }
        viewModelScope.launch {
            try {
                _uiState.value = DashboardAnalisisUiState.Loading
                lastPeriodo = periodo
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
                val historicoCategorias = analisisFinancieroUseCase.obtenerHistoricoGasto(periodo)
                val presupuestosConBrecha = analisisFinancieroUseCase.obtenerPresupuestosConBrecha(periodo)

                val success = DashboardAnalisisUiState.Success(
                    kpis = kpis,
                    tendencias = tendencias,
                    analisisCategorias = analisisCategorias,
                    predicciones = predicciones,
                    analisisTendenciaTemporal = analisisTendenciaTemporal,
                    analisisVolatilidad = analisisVolatilidad,
                    comparacionPeriodo = comparacionPeriodo,
                    gastosInusuales = gastosInusuales,
                    metricasRendimiento = metricasRendimiento,
                    resumenFinanciero = resumenFinanciero,
                    historicoCategorias = historicoCategorias,
                    presupuestosConBrecha = presupuestosConBrecha
                )
                lastSuccessState = success
                _uiState.value = success
            } catch (e: Exception) {
                _uiState.value = DashboardAnalisisUiState.Error(
                    mensaje = e.message ?: "Error al cargar análisis financiero",
                    onRetry = { cargarAnalisis(periodo, force = true) }
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

        // 1. INSIGHTS DE BRECHA VS PRESUPUESTO (PRIORIDAD ALTA)
        currentState.presupuestosConBrecha.forEach { presupuesto ->
            val diffPct = (presupuesto.gastoActual - presupuesto.presupuesto) / presupuesto.presupuesto * 100
            when {
                diffPct > 0 -> {
                    val montoFaltante = presupuesto.presupuesto - presupuesto.gastoActual
                    insights.add(
                        "Estás gastando un ${"%.1f".format(diffPct)}% sobre el presupuesto en '${presupuesto.nombreCategoria}'. " +
                        "Necesitas recortar ${"%.0f".format(abs(montoFaltante))} para no pasarte."
                    )
                }
                diffPct < -20 -> {
                    val excedente = abs(presupuesto.brechaPresupuesto)
                    insights.add(
                        "Llevas un ${"%.1f".format(-diffPct)}% por debajo del presupuesto en '${presupuesto.nombreCategoria}'. " +
                        "Tienes ${"%.0f".format(excedente)} disponibles para invertir o reasignar."
                    )
                }
            }
        }

        // 2. INSIGHTS DE RITMO HISTÓRICO
        currentState.historicoCategorias.forEach { historico ->
            val presupuestoActual = currentState.presupuestosConBrecha.find { it.categoriaId == historico.categoriaId }
            if (presupuestoActual != null) {
                val ritmo = presupuestoActual.ritmoHistorico
                if (ritmo > 110) {
                    insights.add(
                        "Este mes tu gasto en '${historico.nombreCategoria}' va un ${"%.1f".format(ritmo)}% " +
                        "sobre el promedio de los últimos ${historico.mesesAnalizados} meses. Revisa si hay compras excepcionales."
                    )
                }
            }
        }

        // 3. INSIGHTS DE PROYECCIÓN MENSUAL
        currentState.presupuestosConBrecha.forEach { presupuesto ->
            if (presupuesto.proyeccionMensual > presupuesto.presupuesto * 1.1) {
                val exceso = presupuesto.proyeccionMensual - presupuesto.presupuesto
                insights.add(
                    "Proyección: '${presupuesto.nombreCategoria}' se pasará ${"%.0f".format(exceso)} " +
                    "del presupuesto si mantienes el ritmo actual. Ajusta tus gastos."
                )
            }
        }

        // 4. INSIGHTS DE TASA DE AHORRO (mantener los buenos)
        currentState.resumenFinanciero?.let { resumen ->
            when {
                resumen.tasaAhorro > 20 -> insights.add("¡Excelente! Tu tasa de ahorro del ${resumen.tasaAhorro.toInt()}% está por encima del objetivo recomendado.")
                resumen.tasaAhorro > 10 -> insights.add("Buena tasa de ahorro del ${resumen.tasaAhorro.toInt()}%. Considera aumentar un 5% más para mayor seguridad financiera.")
                else -> insights.add("Tu tasa de ahorro del ${resumen.tasaAhorro.toInt()}% está por debajo del recomendado. Revisa tus gastos no esenciales.")
            }
        }

        // 5. INSIGHTS DE GASTOS INUSUALES (mantener)
        if (currentState.gastosInusuales.isNotEmpty()) {
            val gastoMasInusual = currentState.gastosInusuales.first()
            insights.add("Detectamos un gasto inusual: ${gastoMasInusual.descripcion} por ${gastoMasInusual.monto.toInt()}. Revisa si fue necesario.")
        }

        return insights.take(5) // Máximo 5 insights, priorizando presupuesto > riesgo > ahorro
    }

    fun obtenerRecomendaciones(): List<String> {
        val currentState = _uiState.value
        if (currentState !is DashboardAnalisisUiState.Success) return emptyList()

        val recomendaciones = mutableListOf<String>()

        // 1. RECOMENDACIONES BASADAS EN BRECHA DE PRESUPUESTO
        currentState.presupuestosConBrecha.forEach { presupuesto ->
            val diffPct = (presupuesto.gastoActual - presupuesto.presupuesto) / presupuesto.presupuesto * 100
            when {
                diffPct > 10 -> {
                    val montoFaltante = presupuesto.presupuesto - presupuesto.gastoActual
                    recomendaciones.add(
                        "Reduce un ${"%.1f".format(diffPct)}% las compras de '${presupuesto.nombreCategoria}' " +
                        "esta quincena o agrupa gastos en menos días para no pasarte del presupuesto mensual."
                    )
                }
                diffPct < -30 -> {
                    val excedente = abs(presupuesto.brechaPresupuesto)
                    recomendaciones.add(
                        "Tienes margen para destinar hasta ${"%.0f".format(excedente)} en '${presupuesto.nombreCategoria}'. " +
                        "¿Quizás invertir en formación o en un fondo de emergencia?"
                    )
                }
            }
        }

        // 2. RECOMENDACIONES BASADAS EN RITMO HISTÓRICO
        currentState.historicoCategorias.forEach { historico ->
            val presupuestoActual = currentState.presupuestosConBrecha.find { it.categoriaId == historico.categoriaId }
            if (presupuestoActual != null && presupuestoActual.ritmoHistorico > 120) {
                recomendaciones.add(
                    "Tu gasto en '${historico.nombreCategoria}' está ${"%.1f".format(presupuestoActual.ritmoHistorico)}% " +
                    "sobre tu promedio histórico. Considera revisar suscripciones o gastos recurrentes."
                )
            }
        }

        // 3. RECOMENDACIONES BASADAS EN PROYECCIÓN MENSUAL
        currentState.presupuestosConBrecha.forEach { presupuesto ->
            if (presupuesto.proyeccionMensual > presupuesto.presupuesto * 1.2) {
                val exceso = presupuesto.proyeccionMensual - presupuesto.presupuesto
                recomendaciones.add(
                    "Si mantienes el ritmo actual, '${presupuesto.nombreCategoria}' se pasará ${"%.0f".format(exceso)}. " +
                    "Programa un recordatorio para revisar gastos a mitad de mes."
                )
            }
        }

        // 4. RECOMENDACIONES BASADAS EN TASA DE AHORRO
        currentState.resumenFinanciero?.let { resumen ->
            when {
                resumen.tasaAhorro < 10 -> {
                    recomendaciones.add("Aumenta tu tasa de ahorro estableciendo transferencias automáticas al inicio del mes.")
                }
                resumen.tasaAhorro > 25 -> {
                    recomendaciones.add("Excelente tasa de ahorro. Considera diversificar en inversiones o crear un fondo de emergencia.")
                }
            }
        }

        // 5. RECOMENDACIONES BASADAS EN GASTOS INUSUALES
        if (currentState.gastosInusuales.size > 2) {
            recomendaciones.add("Tienes varios gastos inusuales. Revisa si puedes reducir gastos no esenciales o agrupar compras.")
        }

        // 6. RECOMENDACIONES BASADAS EN VOLATILIDAD
        val categoriasVolatiles = currentState.analisisVolatilidad.filter { it.nivelVolatilidad == "ALTA" }
        if (categoriasVolatiles.isNotEmpty()) {
            val categoriaMasVolatil = categoriasVolatiles.first()
            recomendaciones.add("Establece presupuestos fijos para '${categoriaMasVolatil.nombreCategoria}' para mejor control.")
        }

        return recomendaciones.take(4) // Máximo 4 recomendaciones más específicas
    }
} 