package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import javax.inject.Inject

class MovimientoRepository @Inject constructor(
    private val movimientoDao: MovimientoDao,
    private val categoriaDao: CategoriaDao
) {
    suspend fun obtenerMovimientos(): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientos()
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
} 