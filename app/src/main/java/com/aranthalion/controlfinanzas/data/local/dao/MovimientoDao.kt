package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface MovimientoDao {
    @Query("SELECT * FROM movimientos ORDER BY fecha DESC")
    suspend fun obtenerMovimientos(): List<MovimientoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun agregarMovimiento(movimiento: MovimientoEntity)

    @Update
    suspend fun actualizarMovimiento(movimiento: MovimientoEntity)

    // Métodos de auditoría
    @Query("UPDATE movimientos SET fechaActualizacion = :timestamp, metodoActualizacion = :metodo, daoResponsable = :dao WHERE id = :id")
    suspend fun actualizarAuditoria(id: Long, timestamp: Long, metodo: String, dao: String)
    
    @Query("SELECT * FROM movimientos ORDER BY fechaActualizacion DESC LIMIT 50")
    suspend fun obtenerMovimientosRecientes(): List<MovimientoEntity>
    
    @Query("SELECT * FROM movimientos WHERE metodoActualizacion = :metodo ORDER BY fechaActualizacion DESC")
    suspend fun obtenerMovimientosPorMetodo(metodo: String): List<MovimientoEntity>

    @Delete
    suspend fun eliminarMovimiento(movimiento: MovimientoEntity)

    @Query("SELECT * FROM movimientos WHERE tipo = :tipo ORDER BY fecha DESC")
    fun getMovimientosByTipo(tipo: String): Flow<List<MovimientoEntity>>

    @Query("SELECT * FROM movimientos WHERE fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY fecha DESC")
    suspend fun obtenerMovimientosPorPeriodo(fechaInicio: Date, fechaFin: Date): List<MovimientoEntity>

    @Query("DELETE FROM movimientos")
    suspend fun deleteAllMovimientos()

    @Query("SELECT idUnico FROM movimientos")
    suspend fun obtenerIdUnicos(): List<String>

    @Query("SELECT idUnico FROM movimientos WHERE periodoFacturacion = :periodo")
    suspend fun obtenerIdUnicosPorPeriodo(periodo: String?): List<String>

    @Query("SELECT idUnico, categoriaId FROM movimientos WHERE periodoFacturacion = :periodo")
    suspend fun obtenerCategoriasPorIdUnico(periodo: String?): List<IdUnicoCategoria>

    @Query("DELETE FROM movimientos WHERE periodoFacturacion = :periodo")
    suspend fun eliminarMovimientosPorPeriodo(periodo: String?)

    // Nuevo método para obtener movimientos que ya tienen categoría asignada
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NOT NULL ORDER BY fecha DESC")
    suspend fun obtenerMovimientosConCategoria(): List<MovimientoEntity>

    data class IdUnicoCategoria(
        val idUnico: String,
        val categoriaId: Long?
    )
} 