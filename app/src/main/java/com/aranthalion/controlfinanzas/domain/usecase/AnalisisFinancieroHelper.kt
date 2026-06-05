package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object AnalisisFinancieroHelper {

    fun calcularResumen(movimientosFiltrados: List<MovimientoEntity>): ResumenFinanciero {
        val ingresos = movimientosFiltrados.filter { it.tipo == TipoMovimiento.INGRESO.name }.sumOf { it.monto }
        val gastos = movimientosFiltrados.filter { it.tipo == TipoMovimiento.GASTO.name }.sumOf { it.monto }
        val balance = ingresos - abs(gastos)
        val cantidadTransacciones = movimientosFiltrados.size
        val tasaAhorro = if (ingresos > 0) (balance / ingresos) * 100 else 0.0
        return ResumenFinanciero(ingresos, gastos, balance, cantidadTransacciones, tasaAhorro)
    }

    fun extraerHistorialCategoria(
        movimientos: List<MovimientoEntity>,
        categoriaId: Long,
        meses: Int
    ): List<Double> {
        val historial = mutableListOf<Double>()
        val calendar = Calendar.getInstance()
        
        for (i in 0 until meses) {
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.MONTH, -i)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val periodo = String.format("%04d-%02d", year, month)
            
            val gastosCategoria = movimientos.filter { 
                it.categoriaId == categoriaId && 
                it.tipo == TipoMovimiento.GASTO.name && 
                it.periodoFacturacion == periodo &&
                it.tipo != TipoMovimiento.OMITIR.name
            }
            
            val gastoTotal = gastosCategoria.sumOf { it.monto }
            historial.add(abs(gastoTotal))
        }
        
        return historial.reversed()
    }

    fun extraerGastoCategoriaPeriodo(
        movimientos: List<MovimientoEntity>,
        categoriaId: Long,
        periodo: String
    ): Double {
        val gastosCategoria = movimientos.filter { 
            it.categoriaId == categoriaId && 
            it.tipo == TipoMovimiento.GASTO.name && 
            it.periodoFacturacion == periodo &&
            it.tipo != TipoMovimiento.OMITIR.name
        }
        return abs(gastosCategoria.sumOf { it.monto })
    }

    fun calcularPrediccionLineal(historial: List<Double>): Double {
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

    fun calcularIntervaloConfianza(historial: List<Double>, prediccion: Double): Pair<Double, Double> {
        val desviacion = sqrt(historial.map { (it - historial.average()).pow(2.0) }.average())
        val margen = desviacion * 1.96 // 95% de confianza
        return Pair(prediccion - margen, prediccion + margen)
    }

    fun calcularConfiabilidad(historial: List<Double>): Double {
        if (historial.size < 2) return 0.0
        
        val promedio = historial.average()
        val variabilidad = historial.map { abs(it - promedio) / promedio }.average()
        return (1 - variabilidad).coerceIn(0.0, 1.0)
    }

    fun calcularTendencia(valores: List<Double>): String {
        if (valores.size < 2) return "ESTABLE"
        
        val primerValor = valores.first()
        val ultimoValor = valores.last()
        val cambio = if (primerValor > 0) ((ultimoValor - primerValor) / primerValor) * 100 else 0.0
        
        return when {
            cambio > 10 -> "AUMENTO"
            cambio < -10 -> "DISMINUCION"
            else -> "ESTABLE"
        }
    }

    fun calcularMovingAverage(valores: List<Double>, ventana: Int): List<Double> {
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

    fun calcularVolatilidad(valores: List<Double>): Double {
        if (valores.size < 2) return 0.0
        val promedio = valores.average()
        val varianza = valores.map { (it - promedio).pow(2) }.average()
        return sqrt(varianza)
    }

    fun detectarOutliers(valores: List<Double>): Set<Int> {
        if (valores.size < 3) return emptySet()
        
        val promedio = valores.average()
        val desviacion = calcularDesviacionEstandar(valores)
        val outliers = mutableSetOf<Int>()
        
        valores.forEachIndexed { index, valor ->
            val zScore = if (desviacion > 0) abs(valor - promedio) / desviacion else 0.0
            if (zScore > 2.0) { // Más de 2 desviaciones estándar
                outliers.add(index)
            }
        }
        
        return outliers
    }

    fun calcularDesviacionEstandar(valores: List<Double>): Double {
        if (valores.size < 2) return 0.0
        val promedio = valores.average()
        val varianza = valores.map { (it - promedio).pow(2) }.average()
        return sqrt(varianza)
    }

    fun calcularCambioPorcentual(valorAnterior: Double, valorActual: Double): Double {
        return if (valorAnterior > 0) {
            ((valorActual - valorAnterior) / valorAnterior) * 100
        } else 0.0
    }

    fun calcularScoreFinanciero(
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

    fun calcularPeriodoAnterior(periodoActual: String, mesesAtras: Int): String {
        val formato = java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val fecha = formato.parse(periodoActual) ?: Date()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = fecha
        calendar.add(java.util.Calendar.MONTH, -mesesAtras)
        return formato.format(calendar.time)
    }

    fun calcularTendenciaHistorica(gastos: List<Double>): String {
        if (gastos.size < 2) return "ESTABLE"
        
        val promedioPrimeraMitad = gastos.take(gastos.size / 2).average()
        val promedioSegundaMitad = gastos.takeLast(gastos.size / 2).average()
        
        val diferencia = if (promedioPrimeraMitad > 0) {
            ((promedioSegundaMitad - promedioPrimeraMitad) / promedioPrimeraMitad) * 100
        } else 0.0
        
        return when {
            diferencia > 10 -> "AUMENTO"
            diferencia < -10 -> "DISMINUCION"
            else -> "ESTABLE"
        }
    }

    fun calcularDiasRestantes(periodo: String): Int {
        val formato = java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val fecha = formato.parse(periodo) ?: Date()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = fecha
        val ultimoDia = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val diaActual = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        return maxOf(0, ultimoDia - diaActual)
    }
}
