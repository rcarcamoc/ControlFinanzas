package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import java.util.*
import javax.inject.Inject

class MovimientoRepository @Inject constructor(
    private val movimientoDao: MovimientoDao,
    private val categoriaDao: CategoriaDao
) {
    suspend fun obtenerMovimientos(): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientos()
    }

    suspend fun obtenerMovimientosPorPeriodo(fechaInicio: Date, fechaFin: Date): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientosPorPeriodo(fechaInicio, fechaFin)
    }

    suspend fun obtenerCategorias(): List<Categoria> {
        return categoriaDao.obtenerCategorias()
    }

    suspend fun agregarMovimiento(movimiento: MovimientoEntity) {
        movimientoDao.agregarMovimiento(movimiento)
    }

    suspend fun actualizarMovimiento(movimiento: MovimientoEntity) {
        movimientoDao.actualizarMovimiento(movimiento)
    }

    suspend fun eliminarMovimiento(movimiento: MovimientoEntity) {
        movimientoDao.eliminarMovimiento(movimiento)
    }

    suspend fun obtenerIdUnicos(): Set<String> {
        return movimientoDao.obtenerIdUnicos().toSet()
    }

    suspend fun obtenerIdUnicosPorPeriodo(periodo: String?): Set<String> {
        return movimientoDao.obtenerIdUnicosPorPeriodo(periodo).toSet()
    }

    suspend fun obtenerCategoriasPorIdUnico(periodo: String?): Map<String, Long?> {
        return movimientoDao.obtenerCategoriasPorIdUnico(periodo).associate { it.idUnico to it.categoriaId }
    }

    suspend fun eliminarMovimientosPorPeriodo(periodo: String?) {
        movimientoDao.eliminarMovimientosPorPeriodo(periodo)
    }
} 