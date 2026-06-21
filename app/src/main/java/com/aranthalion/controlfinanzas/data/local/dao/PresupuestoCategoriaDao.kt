package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity

@Dao
interface PresupuestoCategoriaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarPresupuesto(presupuesto: PresupuestoCategoriaEntity)

    @Update
    suspend fun actualizarPresupuesto(presupuesto: PresupuestoCategoriaEntity)

    @Delete
    suspend fun eliminarPresupuesto(presupuesto: PresupuestoCategoriaEntity)

    @Query("SELECT * FROM presupuesto_categoria WHERE periodo = :periodo")
    suspend fun obtenerPresupuestosPorPeriodo(periodo: String): List<PresupuestoCategoriaEntity>

    @Query("SELECT * FROM presupuesto_categoria WHERE periodo = :periodo AND scope = :scope")
    suspend fun obtenerPresupuestosPorPeriodoYScope(periodo: String, scope: String): List<PresupuestoCategoriaEntity>

    @Query("SELECT * FROM presupuesto_categoria WHERE categoriaId = :categoriaId AND periodo = :periodo LIMIT 1")
    suspend fun obtenerPresupuestoPorCategoriaYPeriodo(categoriaId: Long, periodo: String): PresupuestoCategoriaEntity?

    @Query("SELECT * FROM presupuesto_categoria WHERE categoriaId = :categoriaId AND periodo = :periodo AND scope = :scope LIMIT 1")
    suspend fun obtenerPresupuestoPorCategoriaYPeriodoYScope(categoriaId: Long, periodo: String, scope: String): PresupuestoCategoriaEntity?

    @Query("SELECT SUM(monto) FROM presupuesto_categoria WHERE periodo = :periodo")
    suspend fun obtenerSumaTotalPresupuesto(periodo: String): Double?

    @Query("SELECT SUM(monto) FROM presupuesto_categoria WHERE periodo = :periodo AND scope = :scope")
    suspend fun obtenerSumaTotalPresupuestoYScope(periodo: String, scope: String): Double?

    @Query("SELECT * FROM presupuesto_categoria ORDER BY periodo DESC")
    suspend fun obtenerTodosLosPresupuestos(): List<PresupuestoCategoriaEntity>
} 