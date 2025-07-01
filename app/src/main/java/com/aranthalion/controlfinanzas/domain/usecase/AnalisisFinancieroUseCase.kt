package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

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

class AnalisisFinancieroUseCase @Inject constructor(
    private val movimientoRepository: MovimientoRepository,
    private val presupuestoRepository: PresupuestoCategoriaRepository
) {
    
    suspend fun obtenerResumenFinanciero(fechaInicio: Date, fechaFin: Date): ResumenFinanciero {
        val movimientos = movimientoRepository.obtenerMovimientosPorPeriodo(fechaInicio, fechaFin)
        
        val ingresos = movimientos.filter { it.tipo == TipoMovimiento.INGRESO.name }.sumOf { it.monto }
        val gastos = movimientos.filter { it.tipo == TipoMovimiento.GASTO.name }.sumOf { abs(it.monto) }
        val balance = ingresos - gastos
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

    // Métodos auxiliares privados
    private suspend fun obtenerResumenFinancieroPorPeriodo(periodo: String): ResumenFinanciero {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val movimientosDelPeriodo = movimientos.filter { it.periodoFacturacion == periodo }
        
        val ingresos = movimientosDelPeriodo.filter { it.tipo == TipoMovimiento.INGRESO.name }.sumOf { it.monto }
        val gastos = movimientosDelPeriodo.filter { it.tipo == TipoMovimiento.GASTO.name }.sumOf { abs(it.monto) }
        val balance = ingresos - gastos
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
            
            historial.add(gastosCategoria.sumOf { abs(it.monto) })
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
}
