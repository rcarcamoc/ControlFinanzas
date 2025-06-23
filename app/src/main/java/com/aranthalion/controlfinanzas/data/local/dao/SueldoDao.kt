package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.local.entity.SueldoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SueldoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSueldo(sueldo: SueldoEntity)

    @Update
    suspend fun actualizarSueldo(sueldo: SueldoEntity)

    @Delete
    suspend fun eliminarSueldo(sueldo: SueldoEntity)

    @Query("SELECT * FROM sueldos WHERE periodo = :periodo ORDER BY nombrePersona ASC")
    suspend fun obtenerSueldosPorPeriodo(periodo: String): List<SueldoEntity>

    @Query("SELECT * FROM sueldos WHERE periodo = :periodo ORDER BY nombrePersona ASC")
    fun obtenerSueldosPorPeriodoFlow(periodo: String): Flow<List<SueldoEntity>>

    @Query("SELECT * FROM sueldos WHERE nombrePersona = :nombrePersona AND periodo = :periodo LIMIT 1")
    suspend fun obtenerSueldoPorPersonaYPeriodo(nombrePersona: String, periodo: String): SueldoEntity?

    @Query("SELECT * FROM sueldos ORDER BY periodo DESC, nombrePersona ASC")
    suspend fun obtenerTodosLosSueldos(): List<SueldoEntity>

    @Query("SELECT DISTINCT periodo FROM sueldos ORDER BY periodo DESC")
    suspend fun obtenerPeriodosDisponibles(): List<String>

    @Query("SELECT DISTINCT nombrePersona FROM sueldos ORDER BY nombrePersona ASC")
    suspend fun obtenerPersonasDisponibles(): List<String>
} 