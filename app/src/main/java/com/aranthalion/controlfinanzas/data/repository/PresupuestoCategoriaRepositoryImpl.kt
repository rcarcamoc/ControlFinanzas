package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.PresupuestoCategoriaDao
import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity
import javax.inject.Inject

class PresupuestoCategoriaRepositoryImpl @Inject constructor(
    private val dao: PresupuestoCategoriaDao
) : PresupuestoCategoriaRepository {
    override suspend fun insertarPresupuesto(presupuesto: PresupuestoCategoriaEntity) =
        dao.insertarPresupuesto(presupuesto)

    override suspend fun actualizarPresupuesto(presupuesto: PresupuestoCategoriaEntity) =
        dao.actualizarPresupuesto(presupuesto)

    override suspend fun eliminarPresupuesto(presupuesto: PresupuestoCategoriaEntity) =
        dao.eliminarPresupuesto(presupuesto)

    override suspend fun obtenerPresupuestosPorPeriodo(periodo: String): List<PresupuestoCategoriaEntity> =
        dao.obtenerPresupuestosPorPeriodo(periodo)

    override suspend fun obtenerPresupuestoPorCategoriaYPeriodo(categoriaId: Long, periodo: String): PresupuestoCategoriaEntity? =
        dao.obtenerPresupuestoPorCategoriaYPeriodo(categoriaId, periodo)

    override suspend fun obtenerSumaTotalPresupuesto(periodo: String): Double? =
        dao.obtenerSumaTotalPresupuesto(periodo)
} 