package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.movimiento.MovimientoManualEntity
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface MovimientoManualDao {
    @Query("SELECT * FROM movimientos_manuales ORDER BY fecha DESC")
    fun getAllMovimientos(): Flow<List<MovimientoManualEntity>>

    @Query("SELECT * FROM movimientos_manuales WHERE tipo = :tipo ORDER BY fecha DESC")
    fun getMovimientosByTipo(tipo: TipoMovimiento): Flow<List<MovimientoManualEntity>>

    @Query("SELECT * FROM movimientos_manuales WHERE fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY fecha DESC")
    fun getMovimientosByFecha(fechaInicio: Date, fechaFin: Date): Flow<List<MovimientoManualEntity>>

    @Query("SELECT * FROM movimientos_manuales WHERE categoriaId = :categoriaId ORDER BY fecha DESC")
    fun getMovimientosByCategoria(categoriaId: Long): Flow<List<MovimientoManualEntity>>

    @Query("SELECT SUM(monto) FROM movimientos_manuales WHERE tipo = :tipo")
    fun getTotalByTipo(tipo: TipoMovimiento): Flow<Double?>

    @Query("SELECT SUM(monto) FROM movimientos_manuales WHERE categoriaId = :categoriaId")
    fun getTotalByCategoria(categoriaId: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovimiento(movimiento: MovimientoManualEntity): Long

    @Update
    suspend fun updateMovimiento(movimiento: MovimientoManualEntity)

    @Delete
    suspend fun deleteMovimiento(movimiento: MovimientoManualEntity)
} 