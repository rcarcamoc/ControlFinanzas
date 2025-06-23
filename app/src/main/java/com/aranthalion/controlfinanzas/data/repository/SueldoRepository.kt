package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.entity.SueldoEntity
import kotlinx.coroutines.flow.Flow

interface SueldoRepository {
    suspend fun insertarSueldo(sueldo: SueldoEntity)
    suspend fun actualizarSueldo(sueldo: SueldoEntity)
    suspend fun eliminarSueldo(sueldo: SueldoEntity)
    suspend fun obtenerSueldosPorPeriodo(periodo: String): List<SueldoEntity>
    fun obtenerSueldosPorPeriodoFlow(periodo: String): Flow<List<SueldoEntity>>
    suspend fun obtenerSueldoPorPersonaYPeriodo(nombrePersona: String, periodo: String): SueldoEntity?
    suspend fun obtenerTodosLosSueldos(): List<SueldoEntity>
    suspend fun obtenerPeriodosDisponibles(): List<String>
    suspend fun obtenerPersonasDisponibles(): List<String>
} 