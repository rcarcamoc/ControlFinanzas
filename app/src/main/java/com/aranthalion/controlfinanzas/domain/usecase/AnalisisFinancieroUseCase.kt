package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.pow

data class ResumenFinanciero(
    val ingresos: Double,
    val gastos: Double,
    val balance: Double,
    val cantidadTransacciones: Int,
    val tasaAhorro: Double
)

data class TendenciaMensual(
    val periodo: String,
    val ingresos: Double,
    val gastos: Double,
    val balance: Double,
    val tasaAhorro: Double
)

data class AnalisisCategoria(
    val categoriaId: Long,
    val nombreCategoria: String,
    val totalGastado: Double,
    val porcentajeDelTotal: Double,
    val promedioDiario: Double,
    val tendencia: String // "AUMENTO", "DISMINUCION", "ESTABLE"
)

data class PrediccionGasto(
    val categoriaId: Long,
    val nombreCategoria: String,
    val prediccion: Double,
    val intervaloConfianza: Pair<Double, Double>,
    val confiabilidad: Double
)

data class KPIFinanciero(
    val nivel: Int, // 1: Resumen ejecutivo, 2: Tendencias, 3: Detalle
    val titulo: String,
    val valor: String,
    val descripcion: String,
    val tendencia: String? = null,
    val color: String = "primary"
)

// Nuevas estructuras para análisis avanzado
data class AnalisisTendenciaTemporal(
    val periodo: String,
    val valor: Double,
    val movingAverage: Double,
    val tendencia: String,
    val volatilidad: Double,
    val esOutlier: Boolean
)

data class AnalisisVolatilidad(
    val categoriaId: Long,
    val nombreCategoria: String,
    val desviacionEstandar: Double,
    val coeficienteVariacion: Double,
    val nivelVolatilidad: String // "BAJA", "MEDIA", "ALTA"
)

data class ComparacionPeriodo(
    val periodoActual: String,
    val periodoAnterior: String,
    val cambioIngresos: Double,
    val cambioGastos: Double,
    val cambioBalance: Double,
    val cambioTasaAhorro: Double,
    val tendencia: String
)

data class GastoInusual(
    val movimientoId: Long,
    val descripcion: String,
    val monto: Double,
    val categoria: String,
    val fecha: Date,
    val desviacion: Double,
    val factorInusual: Double
)

data class MetricasRendimiento(
    val ratioLiquidez: Double,
    val ratioGastosFijos: Double,
    val ratioAhorro: Double,
    val indiceEstabilidad: Double,
    val scoreFinanciero: Int // 0-100
)

class AnalisisFinancieroUseCase @Inject constructor(
    private val movimientoRepository: MovimientoRepository,
    private val presupuestoRepository: PresupuestoCategoriaRepository
) {
    
    suspend fun obtenerResumenFinanciero(fechaInicio: Date, fechaFin: Date): ResumenFinanciero {
        val movimientos = movimientoRepository.obtenerMovimientosPorPeriodo(fechaInicio, fechaFin)
        
        val ingresos = movimientos.filter { it.tipo == TipoMovimiento.INGRESO.name }.sumOf { it.monto }
        // Para gastos, sumamos todos los valores (positivos y negativos)
        // Los negativos representan reversas y reducen el gasto total
        val gastos = movimientos.filter { it.tipo == TipoMovimiento.GASTO.name }.sumOf { it.monto }
        val balance = ingresos - abs(gastos)
        val cantidadTransacciones = movimientos.size
        val tasaAhorro = if (ingresos > 0) (balance / ingresos) * 100 else 0.0
        
        return ResumenFinanciero(
            ingresos = ingresos,
            gastos = gastos,
            balance = balance,
            cantidadTransacciones = cantidadTransacciones,
            tasaAhorro = tasaAhorro
        )
    }

    /**
     * Obtiene tendencias mensuales de los últimos N meses
     */
    suspend fun obtenerTendenciasMensuales(cantidadMeses: Int = 6): List<TendenciaMensual> {
        val tendencias = mutableListOf<TendenciaMensual>()
        val calendar = Calendar.getInstance()
        
        for (i in 0 until cantidadMeses) {
            calendar.add(Calendar.MONTH, -i)
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val periodo = String.format("%04d-%02d", year, month)
            
            val resumen = obtenerResumenFinancieroPorPeriodo(periodo)
            tendencias.add(
                TendenciaMensual(
                    periodo = periodo,
                    ingresos = resumen.ingresos,
                    gastos = resumen.gastos,
                    balance = resumen.balance,
                    tasaAhorro = resumen.tasaAhorro
                )
            )
        }
        
        return tendencias.reversed()
    }

    /**
     * Analiza las categorías con mayor impacto en gastos
     */
    suspend fun obtenerAnalisisCategorias(periodo: String): List<AnalisisCategoria> {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        
        val gastosDelPeriodo = movimientos.filter { 
            it.tipo == TipoMovimiento.GASTO.name && 
            it.periodoFacturacion == periodo 
        }
        
        val totalGastos = gastosDelPeriodo.sumOf { abs(it.monto) }
        
        val analisisPorCategoria = gastosDelPeriodo
            .groupBy { it.categoriaId }
            .map { (categoriaId, movimientos) ->
                val categoria = categorias.find { it.id == categoriaId }
                val totalGastado = movimientos.sumOf { abs(it.monto) }
                val porcentajeDelTotal = if (totalGastos > 0) (totalGastado / totalGastos) * 100 else 0.0
                val promedioDiario = totalGastado / 30.0 // Aproximación mensual
                
                AnalisisCategoria(
                    categoriaId = categoriaId ?: 0,
                    nombreCategoria = categoria?.nombre ?: "Sin categoría",
                    totalGastado = totalGastado,
                    porcentajeDelTotal = porcentajeDelTotal,
                    promedioDiario = promedioDiario,
                    tendencia = calcularTendenciaCategoria(categoriaId, periodo)
                )
            }
            .sortedByDescending { it.totalGastado }
        
        return analisisPorCategoria
    }

    /**
     * Genera predicciones de gasto para el próximo mes
     */
    suspend fun obtenerPrediccionesGasto(periodoActual: String): List<PrediccionGasto> {
        val categorias = movimientoRepository.obtenerCategorias()
        val predicciones = mutableListOf<PrediccionGasto>()
        
        for (categoria in categorias) {
            val historial = obtenerHistorialCategoria(categoria.id, 3) // Últimos 3 meses
            if (historial.isNotEmpty()) {
                val prediccion = calcularPrediccionLineal(historial)
                val intervalo = calcularIntervaloConfianza(historial, prediccion)
                val confiabilidad = calcularConfiabilidad(historial)
                
                predicciones.add(
                    PrediccionGasto(
                        categoriaId = categoria.id,
                        nombreCategoria = categoria.nombre,
                        prediccion = prediccion,
                        intervaloConfianza = intervalo,
                        confiabilidad = confiabilidad
                    )
                )
            }
        }
        
        return predicciones.sortedByDescending { it.prediccion }
    }

    /**
     * Genera KPIs jerárquicos para dashboard ejecutivo
     */
    suspend fun obtenerKPIsJerarquicos(periodo: String): List<KPIFinanciero> {
        val kpis = mutableListOf<KPIFinanciero>()
        
        // Nivel 1: Resumen ejecutivo
        val resumen = obtenerResumenFinancieroPorPeriodo(periodo)
        kpis.add(KPIFinanciero(1, "Balance Neto", 
            FormatUtils.formatMoneyCLP(resumen.balance), 
            "Resultado financiero del período",
            if (resumen.balance > 0) "POSITIVO" else "NEGATIVO",
            if (resumen.balance > 0) "success" else "error"
        ))
        
        kpis.add(KPIFinanciero(1, "Tasa de Ahorro", 
            "${resumen.tasaAhorro.toInt()}%", 
            "Porcentaje de ingresos ahorrados",
            if (resumen.tasaAhorro > 20) "EXCELENTE" else if (resumen.tasaAhorro > 10) "BUENO" else "MEJORAR",
            if (resumen.tasaAhorro > 20) "success" else if (resumen.tasaAhorro > 10) "warning" else "error"
        ))
        
        // Nivel 2: Tendencias
        val tendencias = obtenerTendenciasMensuales(3)
        if (tendencias.size >= 2) {
            val tendenciaGastos = calcularTendencia(tendencias.map { it.gastos })
            kpis.add(KPIFinanciero(2, "Tendencia Gastos", 
                tendenciaGastos, 
                "Evolución de gastos últimos 3 meses",
                tendenciaGastos
            ))
        }
        
        // Nivel 3: Detalle por categorías
        val analisisCategorias = obtenerAnalisisCategorias(periodo)
        val topCategoria = analisisCategorias.firstOrNull()
        if (topCategoria != null) {
            kpis.add(KPIFinanciero(3, "Mayor Gasto", 
                topCategoria.nombreCategoria, 
                "${topCategoria.porcentajeDelTotal.toInt()}% del total",
                topCategoria.tendencia
            ))
        }
        
        return kpis
    }

    // NUEVOS MÉTODOS PARA ANÁLISIS AVANZADO

    /**
     * Analiza tendencias temporales con moving averages y detección de outliers
     */
    suspend fun obtenerAnalisisTendenciaTemporal(
        categoriaId: Long? = null, 
        meses: Int = 6
    ): List<AnalisisTendenciaTemporal> {
        val historial = mutableListOf<AnalisisTendenciaTemporal>()
        val calendar = Calendar.getInstance()
        
        for (i in 0 until meses) {
            calendar.add(Calendar.MONTH, -i)
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val periodo = String.format("%04d-%02d", year, month)
            
            val valor = if (categoriaId != null) {
                obtenerGastoCategoriaPeriodo(categoriaId, periodo)
            } else {
                obtenerResumenFinancieroPorPeriodo(periodo).gastos
            }
            
            historial.add(
                AnalisisTendenciaTemporal(
                    periodo = periodo,
                    valor = valor,
                    movingAverage = 0.0, // Se calculará después
                    tendencia = "ESTABLE",
                    volatilidad = 0.0,
                    esOutlier = false
                )
            )
        }
        
        val valores = historial.map { it.valor }
        val movingAverage = calcularMovingAverage(valores, 3)
        val volatilidad = calcularVolatilidad(valores)
        val outliers = detectarOutliers(valores)
        
        return historial.mapIndexed { index, item ->
            item.copy(
                movingAverage = movingAverage.getOrNull(index) ?: item.valor,
                volatilidad = volatilidad,
                esOutlier = outliers.contains(index),
                tendencia = calcularTendencia(valores.take(index + 1))
            )
        }.reversed()
    }

    /**
     * Analiza la volatilidad de gastos por categoría
     */
    suspend fun obtenerAnalisisVolatilidad(periodo: String): List<AnalisisVolatilidad> {
        val categorias = movimientoRepository.obtenerCategorias()
        val analisis = mutableListOf<AnalisisVolatilidad>()
        
        for (categoria in categorias) {
            val historial = obtenerHistorialCategoria(categoria.id, 6) // Últimos 6 meses
            if (historial.size >= 3) {
                val desviacionEstandar = calcularDesviacionEstandar(historial)
                val promedio = historial.average()
                val coeficienteVariacion = if (promedio > 0) desviacionEstandar / promedio else 0.0
                
                analisis.add(
                    AnalisisVolatilidad(
                        categoriaId = categoria.id,
                        nombreCategoria = categoria.nombre,
                        desviacionEstandar = desviacionEstandar,
                        coeficienteVariacion = coeficienteVariacion,
                        nivelVolatilidad = when {
                            coeficienteVariacion < 0.3 -> "BAJA"
                            coeficienteVariacion < 0.7 -> "MEDIA"
                            else -> "ALTA"
                        }
                    )
                )
            }
        }
        
        return analisis.sortedByDescending { it.coeficienteVariacion }
    }

    /**
     * Compara el período actual con el anterior
     */
    suspend fun obtenerComparacionPeriodo(periodoActual: String): ComparacionPeriodo? {
        val calendar = Calendar.getInstance()
        val (year, month) = parsePeriodo(periodoActual)
        
        calendar.set(year, month - 2, 1) // Mes anterior
        val periodoAnterior = String.format("%04d-%02d", 
            calendar.get(Calendar.YEAR), 
            calendar.get(Calendar.MONTH) + 1
        )
        
        val resumenActual = obtenerResumenFinancieroPorPeriodo(periodoActual)
        val resumenAnterior = obtenerResumenFinancieroPorPeriodo(periodoAnterior)
        
        val cambioIngresos = calcularCambioPorcentual(resumenAnterior.ingresos, resumenActual.ingresos)
        val cambioGastos = calcularCambioPorcentual(resumenAnterior.gastos, resumenActual.gastos)
        val cambioBalance = calcularCambioPorcentual(resumenAnterior.balance, resumenActual.balance)
        val cambioTasaAhorro = resumenActual.tasaAhorro - resumenAnterior.tasaAhorro
        
        return ComparacionPeriodo(
            periodoActual = periodoActual,
            periodoAnterior = periodoAnterior,
            cambioIngresos = cambioIngresos,
            cambioGastos = cambioGastos,
            cambioBalance = cambioBalance,
            cambioTasaAhorro = cambioTasaAhorro,
            tendencia = when {
                cambioBalance > 5 -> "MEJORANDO"
                cambioBalance < -5 -> "EMPEORANDO"
                else -> "ESTABLE"
            }
        )
    }

    /**
     * Detecta gastos inusuales (outliers)
     */
    suspend fun obtenerGastosInusuales(periodo: String): List<GastoInusual> {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        
        val gastosDelPeriodo = movimientos.filter { 
            it.tipo == TipoMovimiento.GASTO.name && 
            it.periodoFacturacion == periodo 
        }
        
        val gastosInusuales = mutableListOf<GastoInusual>()
        
        for (gasto in gastosDelPeriodo) {
            val categoria = categorias.find { it.id == gasto.categoriaId }
            val historialCategoria = obtenerHistorialCategoria(gasto.categoriaId ?: 0, 3)
            
            if (historialCategoria.isNotEmpty()) {
                val promedio = historialCategoria.average()
                val desviacion = calcularDesviacionEstandar(historialCategoria)
                val montoAbsoluto = abs(gasto.monto)
                val zScore = if (desviacion > 0) (montoAbsoluto - promedio) / desviacion else 0.0
                
                if (zScore > 2.0) { // Outlier si está más de 2 desviaciones estándar
                    gastosInusuales.add(
                        GastoInusual(
                            movimientoId = gasto.id,
                            descripcion = gasto.descripcion,
                            monto = montoAbsoluto,
                            categoria = categoria?.nombre ?: "Sin categoría",
                            fecha = gasto.fecha,
                            desviacion = zScore,
                            factorInusual = zScore / 2.0
                        )
                    )
                }
            }
        }
        
        return gastosInusuales.sortedByDescending { it.factorInusual }
    }

    /**
     * Calcula métricas de rendimiento financiero
     */
    suspend fun obtenerMetricasRendimiento(periodo: String): MetricasRendimiento {
        val resumen = obtenerResumenFinancieroPorPeriodo(periodo)
        val presupuestos = presupuestoRepository.obtenerPresupuestosPorPeriodo(periodo)
        
        // Ratio de liquidez (ingresos / gastos)
        val ratioLiquidez = if (resumen.gastos > 0) resumen.ingresos / resumen.gastos else 0.0
        
        // Ratio de gastos fijos (gastos en categorías esenciales / ingresos)
        val gastosFijos = presupuestos.filter { it.categoriaId in listOf(1L, 2L, 3L) } // Ejemplo: vivienda, alimentación, transporte
            .sumOf { it.monto }
        val ratioGastosFijos = if (resumen.ingresos > 0) gastosFijos / resumen.ingresos else 0.0
        
        // Ratio de ahorro
        val ratioAhorro = resumen.tasaAhorro / 100.0
        
        // Índice de estabilidad (basado en volatilidad de gastos)
        val tendencias = obtenerTendenciasMensuales(3)
        val volatilidad = if (tendencias.size >= 2) {
            calcularVolatilidad(tendencias.map { it.gastos })
        } else 0.0
        val indiceEstabilidad = (1 - volatilidad).coerceIn(0.0, 1.0)
        
        // Score financiero (0-100)
        val scoreFinanciero = calcularScoreFinanciero(
            ratioLiquidez, ratioGastosFijos, ratioAhorro, indiceEstabilidad
        )
        
        return MetricasRendimiento(
            ratioLiquidez = ratioLiquidez,
            ratioGastosFijos = ratioGastosFijos,
            ratioAhorro = ratioAhorro,
            indiceEstabilidad = indiceEstabilidad,
            scoreFinanciero = scoreFinanciero
        )
    }

    // Métodos auxiliares privados
    suspend fun obtenerResumenFinancieroPorPeriodo(periodo: String): ResumenFinanciero {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val movimientosDelPeriodo = movimientos.filter { it.periodoFacturacion == periodo }
        
        val ingresos = movimientosDelPeriodo.filter { it.tipo == TipoMovimiento.INGRESO.name }.sumOf { it.monto }
        // Para gastos, sumamos todos los valores (positivos y negativos)
        // Los negativos representan reversas y reducen el gasto total
        val gastos = movimientosDelPeriodo.filter { it.tipo == TipoMovimiento.GASTO.name }.sumOf { it.monto }
        val balance = ingresos - abs(gastos)
        val cantidadTransacciones = movimientosDelPeriodo.size
        val tasaAhorro = if (ingresos > 0) (balance / ingresos) * 100 else 0.0
        
        return ResumenFinanciero(ingresos, gastos, balance, cantidadTransacciones, tasaAhorro)
    }

    private suspend fun calcularTendenciaCategoria(categoriaId: Long?, periodo: String): String {
        // Implementación simplificada - en producción usar análisis estadístico
        return "ESTABLE"
    }

    private suspend fun obtenerHistorialCategoria(categoriaId: Long, meses: Int): List<Double> {
        val historial = mutableListOf<Double>()
        val calendar = Calendar.getInstance()
        
        for (i in 0 until meses) {
            calendar.add(Calendar.MONTH, -i)
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val periodo = String.format("%04d-%02d", year, month)
            
            val movimientos = movimientoRepository.obtenerMovimientos()
            val gastosCategoria = movimientos.filter { 
                it.categoriaId == categoriaId && 
                it.tipo == TipoMovimiento.GASTO.name && 
                it.periodoFacturacion == periodo 
            }
            
            // Para gastos, sumamos todos los valores (positivos y negativos)
            // Los negativos representan reversas y reducen el gasto total
            val gastoTotal = gastosCategoria.sumOf { it.monto }
            historial.add(abs(gastoTotal))
        }
        
        return historial.reversed()
    }

    private fun calcularPrediccionLineal(historial: List<Double>): Double {
        if (historial.size < 2) return historial.lastOrNull() ?: 0.0
        
        // Regresión lineal simple
        val n = historial.size
        val sumX = (0 until n).sum()
        val sumY = historial.sum()
        val sumXY = historial.mapIndexed { index, value -> index * value }.sum()
        val sumX2 = (0 until n).map { it * it }.sum()
        
        val pendiente = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX).toDouble()
        val intercepto = (sumY - pendiente * sumX) / n
        
        return pendiente * n + intercepto
    }

    private fun calcularIntervaloConfianza(historial: List<Double>, prediccion: Double): Pair<Double, Double> {
        val desviacion = sqrt(historial.map { Math.pow(it - historial.average(), 2.0) }.average())
        val margen = desviacion * 1.96 // 95% de confianza
        return Pair(prediccion - margen, prediccion + margen)
    }

    private fun calcularConfiabilidad(historial: List<Double>): Double {
        if (historial.size < 2) return 0.0
        
        val promedio = historial.average()
        val variabilidad = historial.map { abs(it - promedio) / promedio }.average()
        return (1 - variabilidad).coerceIn(0.0, 1.0)
    }

    private fun calcularTendencia(valores: List<Double>): String {
        if (valores.size < 2) return "ESTABLE"
        
        val primerValor = valores.first()
        val ultimoValor = valores.last()
        val cambio = ((ultimoValor - primerValor) / primerValor) * 100
        
        return when {
            cambio > 10 -> "AUMENTO"
            cambio < -10 -> "DISMINUCION"
            else -> "ESTABLE"
        }
    }

    // NUEVOS MÉTODOS AUXILIARES PARA ANÁLISIS AVANZADO

    private suspend fun obtenerGastoCategoriaPeriodo(categoriaId: Long, periodo: String): Double {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val gastosCategoria = movimientos.filter { 
            it.categoriaId == categoriaId && 
            it.tipo == TipoMovimiento.GASTO.name && 
            it.periodoFacturacion == periodo 
        }
        // Para gastos, sumamos todos los valores (positivos y negativos)
        // Los negativos representan reversas y reducen el gasto total
        val gastoTotal = gastosCategoria.sumOf { it.monto }
        return abs(gastoTotal)
    }

    private fun calcularMovingAverage(valores: List<Double>, ventana: Int): List<Double> {
        if (valores.size < ventana) return valores
        
        val movingAverages = mutableListOf<Double>()
        for (i in 0 until valores.size) {
            val inicio = maxOf(0, i - ventana + 1)
            val fin = i + 1
            val promedio = valores.subList(inicio, fin).average()
            movingAverages.add(promedio)
        }
        return movingAverages
    }

    private fun calcularVolatilidad(valores: List<Double>): Double {
        if (valores.size < 2) return 0.0
        val promedio = valores.average()
        val varianza = valores.map { (it - promedio).pow(2) }.average()
        return sqrt(varianza)
    }

    private fun detectarOutliers(valores: List<Double>): Set<Int> {
        if (valores.size < 3) return emptySet()
        
        val promedio = valores.average()
        val desviacion = calcularDesviacionEstandar(valores)
        val outliers = mutableSetOf<Int>()
        
        valores.forEachIndexed { index, valor ->
            val zScore = abs(valor - promedio) / desviacion
            if (zScore > 2.0) { // Más de 2 desviaciones estándar
                outliers.add(index)
            }
        }
        
        return outliers
    }

    private fun calcularDesviacionEstandar(valores: List<Double>): Double {
        if (valores.size < 2) return 0.0
        val promedio = valores.average()
        val varianza = valores.map { (it - promedio).pow(2) }.average()
        return sqrt(varianza)
    }

    private fun parsePeriodo(periodo: String): Pair<Int, Int> {
        val partes = periodo.split("-")
        return Pair(partes[0].toInt(), partes[1].toInt())
    }

    private fun calcularCambioPorcentual(valorAnterior: Double, valorActual: Double): Double {
        return if (valorAnterior > 0) {
            ((valorActual - valorAnterior) / valorAnterior) * 100
        } else 0.0
    }

    private fun calcularScoreFinanciero(
        ratioLiquidez: Double,
        ratioGastosFijos: Double,
        ratioAhorro: Double,
        indiceEstabilidad: Double
    ): Int {
        var score = 0
        
        // Ratio de liquidez (0-25 puntos)
        score += when {
            ratioLiquidez >= 1.5 -> 25
            ratioLiquidez >= 1.2 -> 20
            ratioLiquidez >= 1.0 -> 15
            ratioLiquidez >= 0.8 -> 10
            else -> 5
        }
        
        // Ratio de gastos fijos (0-25 puntos) - menor es mejor
        score += when {
            ratioGastosFijos <= 0.3 -> 25
            ratioGastosFijos <= 0.5 -> 20
            ratioGastosFijos <= 0.7 -> 15
            ratioGastosFijos <= 0.9 -> 10
            else -> 5
        }
        
        // Ratio de ahorro (0-25 puntos)
        score += when {
            ratioAhorro >= 0.2 -> 25
            ratioAhorro >= 0.15 -> 20
            ratioAhorro >= 0.1 -> 15
            ratioAhorro >= 0.05 -> 10
            else -> 5
        }
        
        // Índice de estabilidad (0-25 puntos)
        score += (indiceEstabilidad * 25).toInt()
        
        return score.coerceIn(0, 100)
    }
}
