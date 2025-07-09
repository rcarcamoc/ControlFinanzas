package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

data class AnalisisGastoCategoria(
    val categoria: Categoria,
    val gastoActual: Double,
    val presupuesto: Double,
    val porcentajeGastado: Double,
    val gastoPeriodoAnterior: Double,
    val porcentajeGastoAnterior: Double,
    val desviacion: Double,
    val proyeccionCierreMes: Double,
    val porcentajeProyeccion: Double,
    val estado: EstadoAnalisis
)

enum class EstadoAnalisis {
    NORMAL,      // Dentro del presupuesto y estable
    ADVERTENCIA, // Cerca del límite o aumentando
    CRITICO,     // Excedido o tendencia negativa
    EXCELENTE    // Muy por debajo del presupuesto
}

data class ResumenAnalisisGasto(
    val totalCategorias: Int,
    val categoriasAnalizadas: Int,
    val categoriasConDesviacion: Int,
    val promedioDesviacion: Double,
    val categoriasCriticas: Int,
    val categoriasExcelentes: Int
)

class AnalisisGastoPorCategoriaUseCase @Inject constructor(
    private val movimientoRepository: MovimientoRepository,
    private val presupuestoRepository: PresupuestoCategoriaRepository,
    private val categoriaRepository: CategoriaRepository
) {
    
    /**
     * Obtiene el análisis completo de gasto por categoría para el período actual
     */
    suspend fun obtenerAnalisisGastoPorCategoria(periodoActual: String): List<AnalisisGastoCategoria> {
        val categorias = categoriaRepository.obtenerCategorias()
        val presupuestos = presupuestoRepository.obtenerPresupuestosPorPeriodo(periodoActual)
        val periodoAnterior = obtenerPeriodoAnterior(periodoActual)
        
        return categorias.map { categoria ->
            val presupuesto = presupuestos.find { it.categoriaId == categoria.id }?.monto ?: 0.0
            val gastoActual = calcularGastoCategoria(categoria.id, periodoActual)
            val gastoAnterior = calcularGastoCategoria(categoria.id, periodoAnterior)
            
            val porcentajeGastado = if (presupuesto > 0) (gastoActual / presupuesto) * 100 else 0.0
            val porcentajeGastoAnterior = if (presupuesto > 0) (gastoAnterior / presupuesto) * 100 else 0.0
            
            val desviacion = porcentajeGastado - porcentajeGastoAnterior
            val proyeccionCierreMes = calcularProyeccionCierreMes(gastoActual, periodoActual)
            val porcentajeProyeccion = if (presupuesto > 0) (proyeccionCierreMes / presupuesto) * 100 else 0.0
            
            val estado = determinarEstadoAnalisis(porcentajeGastado, desviacion, porcentajeProyeccion)
            
            AnalisisGastoCategoria(
                categoria = categoria,
                gastoActual = gastoActual,
                presupuesto = presupuesto,
                porcentajeGastado = porcentajeGastado,
                gastoPeriodoAnterior = gastoAnterior,
                porcentajeGastoAnterior = porcentajeGastoAnterior,
                desviacion = desviacion,
                proyeccionCierreMes = proyeccionCierreMes,
                porcentajeProyeccion = porcentajeProyeccion,
                estado = estado
            )
        }.sortedByDescending { it.porcentajeGastado }
    }
    
    /**
     * Obtiene un resumen del análisis de gasto
     */
    suspend fun obtenerResumenAnalisis(periodoActual: String): ResumenAnalisisGasto {
        val analisis = obtenerAnalisisGastoPorCategoria(periodoActual)
        val categoriasConDesviacion = analisis.count { abs(it.desviacion) > 5.0 } // Más de 5% de desviación
        val promedioDesviacion = analisis.map { it.desviacion }.average()
        val categoriasCriticas = analisis.count { it.estado == EstadoAnalisis.CRITICO }
        val categoriasExcelentes = analisis.count { it.estado == EstadoAnalisis.EXCELENTE }
        
        return ResumenAnalisisGasto(
            totalCategorias = analisis.size,
            categoriasAnalizadas = analisis.count { it.presupuesto > 0 },
            categoriasConDesviacion = categoriasConDesviacion,
            promedioDesviacion = promedioDesviacion,
            categoriasCriticas = categoriasCriticas,
            categoriasExcelentes = categoriasExcelentes
        )
    }
    
    /**
     * Obtiene categorías que requieren atención especial
     */
    suspend fun obtenerCategoriasConAlerta(periodoActual: String): List<AnalisisGastoCategoria> {
        return obtenerAnalisisGastoPorCategoria(periodoActual).filter { 
            it.estado == EstadoAnalisis.CRITICO || it.estado == EstadoAnalisis.ADVERTENCIA 
        }
    }
    
    /**
     * Calcula el gasto de una categoría en un período específico
     */
    private suspend fun calcularGastoCategoria(categoriaId: Long, periodo: String): Double {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val movimientosCategoria = movimientos.filter {
            it.categoriaId == categoriaId && 
            it.tipo == TipoMovimiento.GASTO.name &&
            it.periodoFacturacion == periodo
        }
        return abs(movimientosCategoria.sumOf { it.monto })
    }
    
    /**
     * Calcula la proyección de gasto al cierre del mes
     */
    private fun calcularProyeccionCierreMes(gastoActual: Double, periodo: String): Double {
        val calendar = Calendar.getInstance()
        val hoy = calendar.get(Calendar.DAY_OF_MONTH)
        val diasEnMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Proyección lineal basada en el gasto diario promedio
        val gastoDiarioPromedio = gastoActual / hoy
        return gastoDiarioPromedio * diasEnMes
    }
    
    /**
     * Obtiene el período anterior en formato YYYY-MM
     */
    private fun obtenerPeriodoAnterior(periodoActual: String): String {
        val partes = periodoActual.split("-")
        val year = partes[0].toInt()
        val month = partes[1].toInt()
        
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        calendar.add(Calendar.MONTH, -1)
        
        val yearAnterior = calendar.get(Calendar.YEAR)
        val monthAnterior = calendar.get(Calendar.MONTH) + 1
        
        return String.format("%04d-%02d", yearAnterior, monthAnterior)
    }
    
    /**
     * Determina el estado del análisis basado en múltiples factores
     */
    private fun determinarEstadoAnalisis(
        porcentajeGastado: Double,
        desviacion: Double,
        porcentajeProyeccion: Double
    ): EstadoAnalisis {
        return when {
            porcentajeProyeccion <= 70 && desviacion <= 0 -> EstadoAnalisis.EXCELENTE
            porcentajeProyeccion <= 100 && desviacion <= 10 -> EstadoAnalisis.NORMAL
            porcentajeProyeccion > 100 || desviacion > 10 -> EstadoAnalisis.CRITICO
            else -> EstadoAnalisis.ADVERTENCIA
        }
    }
} 