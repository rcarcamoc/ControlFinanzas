package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.SueldoDao
import com.aranthalion.controlfinanzas.data.local.entity.SueldoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SueldoRepositoryImpl @Inject constructor(
    private val dao: SueldoDao
) : SueldoRepository {
    
    override suspend fun insertarSueldo(sueldo: SueldoEntity) =
        dao.insertarSueldo(sueldo)

    override suspend fun actualizarSueldo(sueldo: SueldoEntity) =
        dao.actualizarSueldo(sueldo)

    override suspend fun eliminarSueldo(sueldo: SueldoEntity) =
        dao.eliminarSueldo(sueldo)

    override suspend fun obtenerSueldosPorPeriodo(periodo: String): List<SueldoEntity> =
        dao.obtenerSueldosPorPeriodo(periodo)

    override fun obtenerSueldosPorPeriodoFlow(periodo: String): Flow<List<SueldoEntity>> =
        dao.obtenerSueldosPorPeriodoFlow(periodo)

    override suspend fun obtenerSueldoPorPersonaYPeriodo(nombrePersona: String, periodo: String): SueldoEntity? =
        dao.obtenerSueldoPorPersonaYPeriodo(nombrePersona, periodo)

    override suspend fun obtenerTodosLosSueldos(): List<SueldoEntity> =
        dao.obtenerTodosLosSueldos()

    override suspend fun obtenerPeriodosDisponibles(): List<String> =
        dao.obtenerPeriodosDisponibles()

    override suspend fun obtenerPersonasDisponibles(): List<String> =
        dao.obtenerPersonasDisponibles()
} 