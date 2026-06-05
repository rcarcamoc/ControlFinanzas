package com.aranthalion.controlfinanzas.domain.usecase

import java.util.*

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

// Estructuras para análisis avanzado
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

// Modelo para análisis histórico
data class ResumenHistoricoCategoria(
    val categoriaId: Long,
    val nombreCategoria: String,
    val gastoPromedioMensual: Double,
    val desviacionEstandar: Double,
    val gastoMaximo: Double,
    val gastoMinimo: Double,
    val mesesAnalizados: Int,
    val tendencia: String // "AUMENTO", "DISMINUCION", "ESTABLE"
)

// Modelo para presupuesto con análisis de brecha
data class PresupuestoConBrecha(
    val categoriaId: Long,
    val nombreCategoria: String,
    val presupuesto: Double,
    val gastoActual: Double,
    val porcentajeGastado: Double,
    val brechaPresupuesto: Double, // Diferencia vs presupuesto
    val ritmoHistorico: Double, // % vs promedio histórico
    val diasRestantes: Int, // Días restantes en el mes
    val proyeccionMensual: Double // Proyección basada en ritmo actual
)
