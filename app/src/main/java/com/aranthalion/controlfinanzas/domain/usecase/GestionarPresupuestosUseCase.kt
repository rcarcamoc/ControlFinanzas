package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

data class PresupuestoCategoria(
    val categoria: Categoria,
    val presupuesto: Double,
    val gastoActual: Double,
    val porcentajeGastado: Double,
    val estado: EstadoPresupuesto
)

enum class EstadoPresupuesto {
    NORMAL,     // 0-80%
    ADVERTENCIA, // 80-90%
    CRITICO,    // 90-100%
    EXCEDIDO    // >100%
}

class GestionarPresupuestosUseCase @Inject constructor(
    private val repository: PresupuestoCategoriaRepository,
    private val categoriaRepository: CategoriaRepository,
    private val movimientoRepository: MovimientoRepository
) {
    suspend fun guardarPresupuesto(presupuesto: PresupuestoCategoriaEntity) =
        repository.insertarPresupuesto(presupuesto)

    suspend fun actualizarPresupuesto(presupuesto: PresupuestoCategoriaEntity) =
        repository.actualizarPresupuesto(presupuesto)

    suspend fun eliminarPresupuesto(presupuesto: PresupuestoCategoriaEntity) =
        repository.eliminarPresupuesto(presupuesto)

    suspend fun eliminarPresupuestosPorPeriodo(periodo: String) {
        val presupuestos = repository.obtenerPresupuestosPorPeriodo(periodo)
        presupuestos.forEach { presupuesto ->
            repository.eliminarPresupuesto(presupuesto)
        }
    }

    suspend fun obtenerPresupuestosPorPeriodo(periodo: String): List<PresupuestoCategoriaEntity> {
        return repository.obtenerPresupuestosPorPeriodo(periodo)
    }

    suspend fun obtenerPresupuestoPorCategoriaYPeriodo(categoriaId: Long, periodo: String): PresupuestoCategoriaEntity? =
        repository.obtenerPresupuestoPorCategoriaYPeriodo(categoriaId, periodo)

    suspend fun obtenerSumaTotalPresupuesto(periodo: String): Double =
        repository.obtenerSumaTotalPresupuesto(periodo) ?: 0.0

    /**
     * Obtiene el estado de presupuestos para todas las categorías en un período
     */
    suspend fun obtenerEstadoPresupuestos(periodo: String): List<PresupuestoCategoria> {
        val categorias = categoriaRepository.obtenerCategorias()
        val presupuestosEntity = repository.obtenerPresupuestosPorPeriodo(periodo)
        val presupuestos = mutableListOf<PresupuestoCategoria>()
        
        println("🔍 ESTADO: Buscando presupuestos para periodo $periodo")
        println("🔍 ESTADO: Categorías encontradas: ${categorias.size}")
        println("🔍 ESTADO: Presupuestos en tabla: ${presupuestosEntity.size}")
        
        for (categoria in categorias) {
            // Buscar presupuesto en la tabla PresupuestoCategoriaEntity
            val presupuestoEntity = presupuestosEntity.find { it.categoriaId == categoria.id }
            
            if (presupuestoEntity != null) {
                val gastoActual = calcularGastoCategoria(categoria.id, periodo)
                val porcentajeGastado = (gastoActual / presupuestoEntity.monto) * 100
                val estado = determinarEstadoPresupuesto(porcentajeGastado)
                
                presupuestos.add(
                    PresupuestoCategoria(
                        categoria = categoria,
                        presupuesto = presupuestoEntity.monto,
                        gastoActual = gastoActual,
                        porcentajeGastado = porcentajeGastado,
                        estado = estado
                    )
                )
                println("🔍 ESTADO: Presupuesto encontrado para ${categoria.nombre}: ${presupuestoEntity.monto}")
            }
        }
        
        println("🔍 ESTADO: Total presupuestos procesados: ${presupuestos.size}")
        return presupuestos.sortedByDescending { it.porcentajeGastado }
    }
    
    /**
     * Actualiza el presupuesto de una categoría
     */
    suspend fun actualizarPresupuestoCategoria(categoriaId: Long, presupuesto: Double?) {
        val categoria = categoriaRepository.obtenerCategorias().find { it.id == categoriaId }
        categoria?.let {
            val categoriaActualizada = it.copy(presupuestoMensual = presupuesto)
            categoriaRepository.updateCategoria(categoriaActualizada)
        }
    }
    
    /**
     * Obtiene categorías que están cerca o han excedido su presupuesto
     */
    suspend fun obtenerCategoriasConAlerta(periodo: String): List<PresupuestoCategoria> {
        return obtenerEstadoPresupuestos(periodo).filter { 
            it.estado != EstadoPresupuesto.NORMAL 
        }
    }
    
    /**
     * Calcula el gasto total de una categoría en un período
     */
    private suspend fun calcularGastoCategoria(categoriaId: Long, periodo: String): Double {
        // Obtener todos los movimientos y filtrar por período de facturación
        val todosLosMovimientos = movimientoRepository.obtenerMovimientos()
        println("🔍 GASTO: Total movimientos obtenidos: ${todosLosMovimientos.size}")
        todosLosMovimientos.forEach { m ->
            println("🔍 GASTO: Movimiento: id=${m.id}, fecha=${m.fecha}, periodoFacturacion=${m.periodoFacturacion}, categoriaId=${m.categoriaId}, tipo=${m.tipo}, monto=${m.monto}")
        }
        
        // Filtrar por período de facturación en lugar de fecha
        val movimientosDelPeriodo = todosLosMovimientos.filter { movimiento ->
            movimiento.periodoFacturacion == periodo
        }
        println("🔍 GASTO: Movimientos en periodo de facturación ($periodo): ${movimientosDelPeriodo.size}")
        movimientosDelPeriodo.forEach { m ->
            println("🔍 GASTO: [Periodo] id=${m.id}, fecha=${m.fecha}, periodoFacturacion=${m.periodoFacturacion}, categoriaId=${m.categoriaId}, tipo=${m.tipo}, monto=${m.monto}")
        }
        
        val movimientosCategoria = movimientosDelPeriodo.filter {
            it.categoriaId == categoriaId && it.tipo == TipoMovimiento.GASTO.name
        }
        println("🔍 GASTO: Movimientos de la categoría $categoriaId y tipo GASTO: ${movimientosCategoria.size}")
        movimientosCategoria.forEach { m ->
            println("🔍 GASTO: [Categoria] id=${m.id}, fecha=${m.fecha}, periodoFacturacion=${m.periodoFacturacion}, categoriaId=${m.categoriaId}, tipo=${m.tipo}, monto=${m.monto}")
        }
        val gasto = movimientosCategoria.sumOf { abs(it.monto) }
        println("🔍 GASTO: Categoría $categoriaId, periodo $periodo, gasto calculado: $gasto")
        return gasto
    }
    
    /**
     * Determina el estado del presupuesto basado en el porcentaje gastado
     */
    private fun determinarEstadoPresupuesto(porcentajeGastado: Double): EstadoPresupuesto {
        return when {
            porcentajeGastado <= 80 -> EstadoPresupuesto.NORMAL
            porcentajeGastado <= 90 -> EstadoPresupuesto.ADVERTENCIA
            porcentajeGastado <= 100 -> EstadoPresupuesto.CRITICO
            else -> EstadoPresupuesto.EXCEDIDO
        }
    }
    
    /**
     * Obtiene un resumen de presupuestos para el período actual
     */
    suspend fun obtenerResumenPresupuestos(periodo: String): ResumenPresupuestos {
        val presupuestos = obtenerEstadoPresupuestos(periodo)
        val totalPresupuestado = presupuestos.sumOf { it.presupuesto }
        val totalGastado = presupuestos.sumOf { it.gastoActual }
        val porcentajeTotalGastado = if (totalPresupuestado > 0) {
            (totalGastado / totalPresupuestado) * 100
        } else 0.0
        println("🔍 RESUMEN: periodo=$periodo, presupuestos=${presupuestos.size}, totalPresupuestado=$totalPresupuestado, totalGastado=$totalGastado, porcentaje=$porcentajeTotalGastado")
        return ResumenPresupuestos(
            totalPresupuestado = totalPresupuestado,
            totalGastado = totalGastado,
            porcentajeGastado = porcentajeTotalGastado,
            categoriasConAlerta = presupuestos.count { it.estado != EstadoPresupuesto.NORMAL },
            categoriasExcedidas = presupuestos.count { it.estado == EstadoPresupuesto.EXCEDIDO }
        )
    }
}

data class ResumenPresupuestos(
    val totalPresupuestado: Double,
    val totalGastado: Double,
    val porcentajeGastado: Double,
    val categoriasConAlerta: Int,
    val categoriasExcedidas: Int
) 