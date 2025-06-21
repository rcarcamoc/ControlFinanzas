package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.domain.movimiento.MovimientoManual
import com.aranthalion.controlfinanzas.domain.movimiento.MovimientoManualRepository
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject

data class ResumenFinanciero(
    val ingresos: Double,
    val gastos: Double,
    val balance: Double,
    val cantidadTransacciones: Int
)

data class MovimientoPorCategoria(
    val categoriaId: Long,
    val categoriaNombre: String,
    val tipo: String, // "Ingreso" o "Gasto"
    val total: Double,
    val cantidadTransacciones: Int
)

data class TendenciaMensual(
    val mes: String, // Formato: "2024-01"
    val ingresos: Double,
    val gastos: Double,
    val balance: Double
)

class AnalisisFinancieroUseCase @Inject constructor(
    private val movimientoRepository: MovimientoRepository,
    private val movimientoManualRepository: MovimientoManualRepository
) {
    
    /**
     * Calcula el total de ingresos, gastos y balance neto para un período
     */
    suspend fun obtenerResumenFinanciero(fechaInicio: Date, fechaFin: Date): ResumenFinanciero {
        // Obtener movimientos importados del período
        val movimientosImportados = movimientoRepository.obtenerMovimientosPorPeriodo(fechaInicio, fechaFin)
        
        // Obtener movimientos manuales del período
        val movimientosManuales = movimientoManualRepository.getMovimientosByFecha(fechaInicio, fechaFin).first()
        
        // Calcular totales de movimientos importados
        val ingresosImportados = movimientosImportados.filter { it.monto > 0 }.sumOf { it.monto }
        val gastosImportados = movimientosImportados.filter { it.monto < 0 }.sumOf { -it.monto }
        
        // Calcular totales de movimientos manuales
        val ingresosManuales = movimientosManuales.filter { it.tipo == TipoMovimiento.INGRESO }.sumOf { it.monto }
        val gastosManuales = movimientosManuales.filter { it.tipo == TipoMovimiento.GASTO }.sumOf { it.monto }
        
        // Consolidar totales
        val ingresosTotal = ingresosImportados + ingresosManuales
        val gastosTotal = gastosImportados + gastosManuales
        val balance = ingresosTotal - gastosTotal
        val cantidadTransacciones = movimientosImportados.size + movimientosManuales.size
        
        return ResumenFinanciero(
            ingresos = ingresosTotal,
            gastos = gastosTotal,
            balance = balance,
            cantidadTransacciones = cantidadTransacciones
        )
    }
    
    /**
     * Agrupa y suma movimientos por categoría para un período
     */
    suspend fun obtenerMovimientosPorCategoria(fechaInicio: Date, fechaFin: Date): List<MovimientoPorCategoria> {
        // Obtener movimientos importados del período
        val movimientosImportados = movimientoRepository.obtenerMovimientosPorPeriodo(fechaInicio, fechaFin)
        
        // Obtener movimientos manuales del período
        val movimientosManuales = movimientoManualRepository.getMovimientosByFecha(fechaInicio, fechaFin).first()
        
        // Agrupar movimientos importados por categoría
        val movimientosPorCategoria = mutableMapOf<Long, MutableList<MovimientoEntity>>()
        movimientosImportados.forEach { movimiento ->
            val categoriaId = movimiento.categoriaId ?: 0L
            movimientosPorCategoria.getOrPut(categoriaId) { mutableListOf() }.add(movimiento)
        }
        
        // Agrupar movimientos manuales por categoría
        val movimientosManualesPorCategoria = mutableMapOf<Long, MutableList<MovimientoManual>>()
        movimientosManuales.forEach { movimiento ->
            val categoriaId = movimiento.categoriaId ?: 0L
            movimientosManualesPorCategoria.getOrPut(categoriaId) { mutableListOf() }.add(movimiento)
        }
        
        // Consolidar resultados
        val resultado = mutableListOf<MovimientoPorCategoria>()
        
        // Procesar movimientos importados
        movimientosPorCategoria.forEach { (categoriaId, movimientos) ->
            val ingresos = movimientos.filter { it.monto > 0 }.sumOf { it.monto }
            val gastos = movimientos.filter { it.monto < 0 }.sumOf { -it.monto }
            
            if (ingresos > 0) {
                resultado.add(MovimientoPorCategoria(
                    categoriaId = categoriaId,
                    categoriaNombre = "Categoría $categoriaId", // TODO: Obtener nombre real
                    tipo = "Ingreso",
                    total = ingresos,
                    cantidadTransacciones = movimientos.count { it.monto > 0 }
                ))
            }
            
            if (gastos > 0) {
                resultado.add(MovimientoPorCategoria(
                    categoriaId = categoriaId,
                    categoriaNombre = "Categoría $categoriaId", // TODO: Obtener nombre real
                    tipo = "Gasto",
                    total = gastos,
                    cantidadTransacciones = movimientos.count { it.monto < 0 }
                ))
            }
        }
        
        // Procesar movimientos manuales
        movimientosManualesPorCategoria.forEach { (categoriaId, movimientos) ->
            val ingresos = movimientos.filter { it.tipo == TipoMovimiento.INGRESO }.sumOf { it.monto }
            val gastos = movimientos.filter { it.tipo == TipoMovimiento.GASTO }.sumOf { it.monto }
            
            if (ingresos > 0) {
                resultado.add(MovimientoPorCategoria(
                    categoriaId = categoriaId,
                    categoriaNombre = "Categoría $categoriaId", // TODO: Obtener nombre real
                    tipo = "Ingreso",
                    total = ingresos,
                    cantidadTransacciones = movimientos.count { it.tipo == TipoMovimiento.INGRESO }
                ))
            }
            
            if (gastos > 0) {
                resultado.add(MovimientoPorCategoria(
                    categoriaId = categoriaId,
                    categoriaNombre = "Categoría $categoriaId", // TODO: Obtener nombre real
                    tipo = "Gasto",
                    total = gastos,
                    cantidadTransacciones = movimientos.count { it.tipo == TipoMovimiento.GASTO }
                ))
            }
        }
        
        return resultado.sortedByDescending { it.total }
    }
    
    /**
     * Calcula ingresos, gastos y balance para los últimos N meses
     */
    suspend fun obtenerTendenciaMensual(cantidadMeses: Int): List<TendenciaMensual> {
        val tendencias = mutableListOf<TendenciaMensual>()
        val calendar = Calendar.getInstance()
        
        for (i in 0 until cantidadMeses) {
            // Calcular fechas del mes
            calendar.add(Calendar.MONTH, -i)
            val finMes = calendar.time
            
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val inicioMes = calendar.time
            
            // Obtener resumen del mes
            val resumen = obtenerResumenFinanciero(inicioMes, finMes)
            
            // Formatear mes
            val mes = String.format("%04d-%02d", 
                calendar.get(Calendar.YEAR), 
                calendar.get(Calendar.MONTH) + 1
            )
            
            tendencias.add(TendenciaMensual(
                mes = mes,
                ingresos = resumen.ingresos,
                gastos = resumen.gastos,
                balance = resumen.balance
            ))
            
            // Restaurar fecha original
            calendar.add(Calendar.MONTH, i)
        }
        
        return tendencias.reversed() // Ordenar cronológicamente
    }
} 