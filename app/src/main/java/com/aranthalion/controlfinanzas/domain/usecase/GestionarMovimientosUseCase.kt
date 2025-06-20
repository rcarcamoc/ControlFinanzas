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

    suspend fun obtenerCategorias(): List<Categoria> {
        return movimientoRepository.obtenerCategorias()
    }

    suspend fun agregarMovimiento(movimiento: MovimientoEntity) {
        movimientoRepository.agregarMovimiento(movimiento)
    }

    suspend fun actualizarMovimiento(movimiento: MovimientoEntity) {
        movimientoRepository.actualizarMovimiento(movimiento)
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
} 