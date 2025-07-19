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
        val movimientos = movimientoRepository.obtenerMovimientos()
        
        println("🔍 ANALISIS DEBUG: Período actual: $periodoActual")
        println("🔍 ANALISIS DEBUG: Período anterior: $periodoAnterior")
        println("🔍 ANALISIS DEBUG: Total movimientos: ${movimientos.size}")
        
        // Mostrar períodos disponibles
        val periodosDisponibles = movimientos.map { it.periodoFacturacion }.distinct().sorted()
        println("🔍 ANALISIS DEBUG: Períodos disponibles: $periodosDisponibles")
        
        // Mostrar algunos movimientos de ejemplo
        movimientos.take(5).forEach { movimiento ->
            println("🔍 ANALISIS DEBUG: Movimiento - ID: ${movimiento.id}, Descripción: ${movimiento.descripcion}, Período: ${movimiento.periodoFacturacion}, Categoría: ${movimiento.categoriaId}")
        }
        
        // Mostrar estadísticas de categorías
        val movimientosConCategoria = movimientos.count { it.categoriaId != null }
        val movimientosSinCategoria = movimientos.count { it.categoriaId == null }
        println("🔍 ANALISIS DEBUG: Movimientos con categoría: $movimientosConCategoria")
        println("🔍 ANALISIS DEBUG: Movimientos sin categoría: $movimientosSinCategoria")
        
        // Mostrar algunos movimientos sin categoría
        val ejemplosSinCategoria = movimientos.filter { it.categoriaId == null }.take(3)
        ejemplosSinCategoria.forEach { movimiento ->
            println("🔍 ANALISIS DEBUG: Sin categoría - ID: ${movimiento.id}, Descripción: ${movimiento.descripcion}, Período: ${movimiento.periodoFacturacion}, Tipo: ${movimiento.tipo}")
        }
        
        val lista = categorias.mapNotNull { categoria ->
            val presupuesto = presupuestos.find { it.categoriaId == categoria.id }?.monto ?: 0.0
            val movimientosCategoria = movimientos.filter {
                it.categoriaId == categoria.id &&
                it.periodoFacturacion == periodoActual &&
                it.tipo == TipoMovimiento.GASTO.name &&
                it.tipo != TipoMovimiento.OMITIR.name
            }
            val gastoActual = movimientosCategoria.sumOf { it.monto }
            
            println("🔍 ANALISIS DEBUG: Categoría '${categoria.nombre}' - Movimientos en período $periodoActual: ${movimientosCategoria.size}, Gasto: $gastoActual")
            
            if (gastoActual <= 0) return@mapNotNull null
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
        }.toMutableList()

        // Agregar fila especial para movimientos sin categoría
        val sinCategoriaGastos = movimientos.filter {
            it.categoriaId == null &&
            it.periodoFacturacion == periodoActual &&
            it.tipo == TipoMovimiento.GASTO.name &&
            it.tipo != TipoMovimiento.OMITIR.name
        }
        val totalSinCategoria = sinCategoriaGastos.sumOf { it.monto }
        println("🔍 ANALISIS DEBUG: Movimientos sin categoría en período $periodoActual: ${sinCategoriaGastos.size}, Total: $totalSinCategoria")
        
        // Debug detallado de filtros
        val todosSinCategoria = movimientos.filter { it.categoriaId == null }
        println("🔍 ANALISIS DEBUG: Total sin categoría (sin filtros): ${todosSinCategoria.size}")
        
        val sinCategoriaPeriodo = todosSinCategoria.filter { it.periodoFacturacion == periodoActual }
        println("🔍 ANALISIS DEBUG: Sin categoría en período $periodoActual: ${sinCategoriaPeriodo.size}")
        
        val sinCategoriaGastosTodos = sinCategoriaPeriodo.filter { it.tipo == TipoMovimiento.GASTO.name }
        println("🔍 ANALISIS DEBUG: Sin categoría + gastos en período $periodoActual: ${sinCategoriaGastosTodos.size}")
        
        val sinCategoriaFinal = sinCategoriaGastosTodos.filter { it.tipo != TipoMovimiento.OMITIR.name }
        println("🔍 ANALISIS DEBUG: Sin categoría + gastos + no omitidos en período $periodoActual: ${sinCategoriaFinal.size}")
        
        // Mostrar algunos ejemplos de movimientos sin categoría de otros períodos
        val otrosPeriodosSinCategoria = todosSinCategoria.filter { it.periodoFacturacion != periodoActual }.take(3)
        otrosPeriodosSinCategoria.forEach { movimiento ->
            println("🔍 ANALISIS DEBUG: Otro período sin categoría - ID: ${movimiento.id}, Descripción: ${movimiento.descripcion}, Período: ${movimiento.periodoFacturacion}, Tipo: ${movimiento.tipo}")
        }
        
        // Mostrar distribución por períodos
        val sinCategoriaPorPeriodo = sinCategoriaGastos.groupBy { it.periodoFacturacion }
        sinCategoriaPorPeriodo.forEach { (periodo, movs) ->
            println("🔍 ANALISIS DEBUG: Período $periodo - ${movs.size} movimientos sin categoría, Total: ${movs.sumOf { it.monto }}")
        }
        
        if (totalSinCategoria > 0) {
            val categoriaSin = Categoria(
                id = -1,
                nombre = "Sin Categoría",
                descripcion = "Movimientos no clasificados",
                tipo = "Gasto"
            )
            lista.add(
                AnalisisGastoCategoria(
                    categoria = categoriaSin,
                    gastoActual = totalSinCategoria,
                    presupuesto = 0.0,
                    porcentajeGastado = 0.0,
                    gastoPeriodoAnterior = 0.0,
                    porcentajeGastoAnterior = 0.0,
                    desviacion = 0.0,
                    proyeccionCierreMes = 0.0,
                    porcentajeProyeccion = 0.0,
                    estado = EstadoAnalisis.NORMAL
                )
            )
        }
        
        println("🔍 ANALISIS DEBUG: Total categorías con datos: ${lista.size}")
        return lista.sortedByDescending { it.porcentajeGastado }
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
        return movimientos
            .filter { 
                it.categoriaId == categoriaId && 
                it.periodoFacturacion == periodo &&
                it.tipo == TipoMovimiento.GASTO.name &&
                it.tipo != TipoMovimiento.OMITIR.name // Excluir transacciones omitidas
            }
            .sumOf { it.monto }
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
     * Determina el estado del análisis basado en el porcentaje de gasto
     * Verde: < 90%, Amarillo: 90-100%, Rojo: > 100%
     */
    private fun determinarEstadoAnalisis(
        porcentajeGastado: Double,
        desviacion: Double,
        porcentajeProyeccion: Double
    ): EstadoAnalisis {
        return when {
            porcentajeGastado < 90 -> EstadoAnalisis.EXCELENTE  // Verde
            porcentajeGastado <= 100 -> EstadoAnalisis.ADVERTENCIA  // Amarillo
            else -> EstadoAnalisis.CRITICO  // Rojo
        }
    }
} 