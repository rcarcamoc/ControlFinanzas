package com.aranthalion.controlfinanzas.domain.movimiento

import kotlinx.coroutines.flow.Flow
import java.util.Date

interface MovimientoManualRepository {
    fun getAllMovimientos(): Flow<List<MovimientoManual>>
    fun getMovimientosByTipo(tipo: TipoMovimiento): Flow<List<MovimientoManual>>
    fun getMovimientosByFecha(fechaInicio: Date, fechaFin: Date): Flow<List<MovimientoManual>>
    fun getMovimientosByCategoria(categoriaId: Long): Flow<List<MovimientoManual>>
    fun getTotalByTipo(tipo: TipoMovimiento): Flow<Double?>
    fun getTotalByCategoria(categoriaId: Long): Flow<Double?>
    suspend fun insertMovimiento(movimiento: MovimientoManual): Long
    suspend fun updateMovimiento(movimiento: MovimientoManual)
    suspend fun deleteMovimiento(movimiento: MovimientoManual)
} 