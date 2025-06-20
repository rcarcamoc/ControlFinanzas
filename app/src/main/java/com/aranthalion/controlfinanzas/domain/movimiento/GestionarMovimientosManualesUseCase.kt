package com.aranthalion.controlfinanzas.domain.movimiento

import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class GestionarMovimientosManualesUseCase @Inject constructor(
    private val movimientoManualRepository: MovimientoManualRepository
) {
    fun getAllMovimientos(): Flow<List<MovimientoManual>> {
        return movimientoManualRepository.getAllMovimientos()
    }

    fun getMovimientosByTipo(tipo: TipoMovimiento): Flow<List<MovimientoManual>> {
        return movimientoManualRepository.getMovimientosByTipo(tipo)
    }

    fun getMovimientosByFecha(fechaInicio: Date, fechaFin: Date): Flow<List<MovimientoManual>> {
        return movimientoManualRepository.getMovimientosByFecha(fechaInicio, fechaFin)
    }

    fun getMovimientosByCategoria(categoriaId: Long): Flow<List<MovimientoManual>> {
        return movimientoManualRepository.getMovimientosByCategoria(categoriaId)
    }

    fun getTotalByTipo(tipo: TipoMovimiento): Flow<Double?> {
        return movimientoManualRepository.getTotalByTipo(tipo)
    }

    fun getTotalByCategoria(categoriaId: Long): Flow<Double?> {
        return movimientoManualRepository.getTotalByCategoria(categoriaId)
    }

    suspend fun insertMovimiento(movimiento: MovimientoManual): Long {
        return movimientoManualRepository.insertMovimiento(movimiento)
    }

    suspend fun updateMovimiento(movimiento: MovimientoManual) {
        movimientoManualRepository.updateMovimiento(movimiento)
    }

    suspend fun deleteMovimiento(movimiento: MovimientoManual) {
        movimientoManualRepository.deleteMovimiento(movimiento)
    }
} 