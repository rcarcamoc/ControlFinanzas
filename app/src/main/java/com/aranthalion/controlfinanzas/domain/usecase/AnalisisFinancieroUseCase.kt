package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

data class ResumenFinanciero(
    val ingresos: Double,
    val gastos: Double,
    val balance: Double,
    val cantidadTransacciones: Int,
    val tasaAhorro: Double
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
}
