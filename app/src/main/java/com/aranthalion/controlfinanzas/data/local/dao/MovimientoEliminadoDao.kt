package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEliminadoEntity

@Dao
interface MovimientoEliminadoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEliminado(entity: MovimientoEliminadoEntity)

    @Query("SELECT * FROM movimientos_eliminados WHERE syncPending = 1")
    suspend fun obtenerPendientes(): List<MovimientoEliminadoEntity>

    @Query("UPDATE movimientos_eliminados SET syncPending = 0 WHERE id IN (:ids)")
    suspend fun marcarComoSincronizados(ids: List<Long>)

    @Query("DELETE FROM movimientos_eliminados WHERE syncPending = 0")
    suspend fun purgarSincronizados()
}
