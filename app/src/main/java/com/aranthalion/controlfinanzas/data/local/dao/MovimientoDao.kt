package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {
    @Query("SELECT * FROM movimientos ORDER BY fecha DESC")
    suspend fun obtenerMovimientos(): List<MovimientoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun agregarMovimiento(movimiento: MovimientoEntity)

    @Update
    suspend fun actualizarMovimiento(movimiento: MovimientoEntity)

    @Delete
    suspend fun eliminarMovimiento(movimiento: MovimientoEntity)

    @Query("SELECT * FROM movimientos WHERE tipo = :tipo ORDER BY fecha DESC")
    fun getMovimientosByTipo(tipo: String): Flow<List<MovimientoEntity>>

    @Query("DELETE FROM movimientos")
    suspend fun deleteAllMovimientos()
} 