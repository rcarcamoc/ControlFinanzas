package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import javax.inject.Inject

class GestionarPresupuestosUseCase @Inject constructor(
    private val repository: PresupuestoCategoriaRepository
) {
    suspend fun guardarPresupuesto(presupuesto: PresupuestoCategoriaEntity) =
        repository.insertarPresupuesto(presupuesto)

    suspend fun actualizarPresupuesto(presupuesto: PresupuestoCategoriaEntity) =
        repository.actualizarPresupuesto(presupuesto)

    suspend fun eliminarPresupuesto(presupuesto: PresupuestoCategoriaEntity) =
        repository.eliminarPresupuesto(presupuesto)

    suspend fun obtenerPresupuestosPorPeriodo(periodo: String): List<PresupuestoCategoriaEntity> =
        repository.obtenerPresupuestosPorPeriodo(periodo)

    suspend fun obtenerPresupuestoPorCategoriaYPeriodo(categoriaId: Long, periodo: String): PresupuestoCategoriaEntity? =
        repository.obtenerPresupuestoPorCategoriaYPeriodo(categoriaId, periodo)

    suspend fun obtenerSumaTotalPresupuesto(periodo: String): Double =
        repository.obtenerSumaTotalPresupuesto(periodo) ?: 0.0
} 