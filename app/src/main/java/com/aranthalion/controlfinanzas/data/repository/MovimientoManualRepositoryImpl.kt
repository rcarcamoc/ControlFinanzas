package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.MovimientoManualDao
import com.aranthalion.controlfinanzas.data.movimiento.MovimientoManualEntity
import com.aranthalion.controlfinanzas.data.util.MovimientoManualMapper
import com.aranthalion.controlfinanzas.domain.movimiento.MovimientoManual
import com.aranthalion.controlfinanzas.domain.movimiento.MovimientoManualRepository
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

class MovimientoManualRepositoryImpl @Inject constructor(
    private val movimientoManualDao: MovimientoManualDao,
    private val mapper: MovimientoManualMapper
) : MovimientoManualRepository {

    override fun getAllMovimientos(): Flow<List<MovimientoManual>> {
        return movimientoManualDao.getAllMovimientos().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun getMovimientosByTipo(tipo: TipoMovimiento): Flow<List<MovimientoManual>> {
        return movimientoManualDao.getMovimientosByTipo(tipo).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun getMovimientosByFecha(fechaInicio: Date, fechaFin: Date): Flow<List<MovimientoManual>> {
        return movimientoManualDao.getMovimientosByFecha(fechaInicio, fechaFin).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun getMovimientosByCategoria(categoriaId: Long): Flow<List<MovimientoManual>> {
        return movimientoManualDao.getMovimientosByCategoria(categoriaId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun getTotalByTipo(tipo: TipoMovimiento): Flow<Double?> {
        return movimientoManualDao.getTotalByTipo(tipo)
    }

    override fun getTotalByCategoria(categoriaId: Long): Flow<Double?> {
        return movimientoManualDao.getTotalByCategoria(categoriaId)
    }

    override suspend fun insertMovimiento(movimiento: MovimientoManual): Long {
        val entity = mapper.toEntity(movimiento)
        return movimientoManualDao.insertMovimiento(entity)
    }

    override suspend fun updateMovimiento(movimiento: MovimientoManual) {
        val entity = mapper.toEntity(movimiento)
        movimientoManualDao.updateMovimiento(entity)
    }

    override suspend fun deleteMovimiento(movimiento: MovimientoManual) {
        val entity = mapper.toEntity(movimiento)
        movimientoManualDao.deleteMovimiento(entity)
    }
} 