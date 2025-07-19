package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import javax.inject.Inject

class GestionarMovimientosUseCase @Inject constructor(
    private val movimientoRepository: MovimientoRepository
) {
    suspend fun obtenerMovimientos(): List<MovimientoEntity> {
        return movimientoRepository.obtenerMovimientos()
    }

    // Métodos optimizados del HITO 1
    suspend fun obtenerMovimientosOptimizado(): List<MovimientoEntity> {
        return movimientoRepository.obtenerMovimientos()
    }
    
    suspend fun obtenerMovimientosPorPeriodoOptimizado(periodo: String): List<MovimientoEntity> {
        return movimientoRepository.obtenerMovimientosPorPeriodo(
            obtenerFechaInicioPeriodo(periodo),
            obtenerFechaFinPeriodo(periodo)
        )
    }

    suspend fun obtenerCategorias(): List<Categoria> {
        return movimientoRepository.obtenerCategorias()
    }
    
    suspend fun obtenerCategoriasOptimizado(): List<Categoria> {
        return movimientoRepository.obtenerCategorias()
    }

    suspend fun agregarMovimiento(movimiento: MovimientoEntity, metodo: String = "INSERT", dao: String = "MovimientoDao") {
        movimientoRepository.agregarMovimiento(movimiento, metodo, dao)
    }

    suspend fun actualizarMovimiento(movimiento: MovimientoEntity, metodo: String = "UPDATE", dao: String = "MovimientoDao") {
        movimientoRepository.actualizarMovimiento(movimiento, metodo, dao)
    }

    suspend fun eliminarMovimiento(movimiento: MovimientoEntity) {
        movimientoRepository.eliminarMovimiento(movimiento)
    }

    suspend fun obtenerIdUnicos(): Set<String> {
        return movimientoRepository.obtenerIdUnicos()
    }

    suspend fun obtenerIdUnicosPorPeriodo(periodo: String?): Set<String> {
        return movimientoRepository.obtenerIdUnicosPorPeriodo(periodo)
    }

    suspend fun obtenerCategoriasPorIdUnico(periodo: String?): Map<String, Long?> {
        return movimientoRepository.obtenerCategoriasPorIdUnico(periodo)
    }

    suspend fun eliminarMovimientosPorPeriodo(periodo: String?) {
        movimientoRepository.eliminarMovimientosPorPeriodo(periodo)
    }
    
    // Funciones auxiliares para optimización
    private fun obtenerFechaInicioPeriodo(periodo: String): java.util.Date {
        val (anio, mes) = periodo.split("-")
        val calendar = java.util.Calendar.getInstance()
        calendar.set(anio.toInt(), mes.toInt() - 1, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    private fun obtenerFechaFinPeriodo(periodo: String): java.util.Date {
        val (anio, mes) = periodo.split("-")
        val calendar = java.util.Calendar.getInstance()
        calendar.set(anio.toInt(), mes.toInt() - 1, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.add(java.util.Calendar.MILLISECOND, -1)
        return calendar.time
    }
} 