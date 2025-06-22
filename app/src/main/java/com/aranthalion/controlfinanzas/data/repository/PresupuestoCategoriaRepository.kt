package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity

interface PresupuestoCategoriaRepository {
    suspend fun insertarPresupuesto(presupuesto: PresupuestoCategoriaEntity)
    suspend fun actualizarPresupuesto(presupuesto: PresupuestoCategoriaEntity)
    suspend fun eliminarPresupuesto(presupuesto: PresupuestoCategoriaEntity)
    suspend fun obtenerPresupuestosPorPeriodo(periodo: String): List<PresupuestoCategoriaEntity>
    suspend fun obtenerPresupuestoPorCategoriaYPeriodo(categoriaId: Long, periodo: String): PresupuestoCategoriaEntity?
    suspend fun obtenerSumaTotalPresupuesto(periodo: String): Double?
} 