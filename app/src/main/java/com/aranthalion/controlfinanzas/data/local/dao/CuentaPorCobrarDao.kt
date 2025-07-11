package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.local.entity.CuentaPorCobrarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CuentaPorCobrarDao {
    @Query("SELECT * FROM cuentas_por_cobrar ORDER BY fechaCreacion DESC")
    suspend fun obtenerTodasLasCuentas(): List<CuentaPorCobrarEntity>

    @Query("SELECT * FROM cuentas_por_cobrar WHERE estado = 'PENDIENTE' ORDER BY fechaCreacion DESC")
    suspend fun obtenerCuentasPendientes(): List<CuentaPorCobrarEntity>

    @Query("SELECT * FROM cuentas_por_cobrar WHERE usuarioId = :usuarioId ORDER BY fechaCreacion DESC")
    suspend fun obtenerCuentasPorUsuario(usuarioId: Long): List<CuentaPorCobrarEntity>

    @Query("SELECT * FROM cuentas_por_cobrar WHERE periodoCobro = :periodo ORDER BY fechaCreacion DESC")
    suspend fun obtenerCuentasPorPeriodo(periodo: String): List<CuentaPorCobrarEntity>

    @Query("SELECT * FROM cuentas_por_cobrar WHERE id = :id LIMIT 1")
    suspend fun obtenerCuentaPorId(id: Long): CuentaPorCobrarEntity?

    @Query("SELECT * FROM cuentas_por_cobrar WHERE motivo LIKE '%' || :motivo || '%'")
    suspend fun buscarCuentasPorMotivo(motivo: String): List<CuentaPorCobrarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCuenta(cuenta: CuentaPorCobrarEntity): Long

    @Update
    suspend fun actualizarCuenta(cuenta: CuentaPorCobrarEntity)

    @Delete
    suspend fun eliminarCuenta(cuenta: CuentaPorCobrarEntity)

    @Query("UPDATE cuentas_por_cobrar SET estado = :estado WHERE id = :id")
    suspend fun cambiarEstadoCuenta(id: Long, estado: String)

    @Query("SELECT SUM(monto) FROM cuentas_por_cobrar WHERE estado = 'PENDIENTE'")
    suspend fun obtenerTotalPendiente(): Double?

    @Query("SELECT SUM(monto) FROM cuentas_por_cobrar WHERE usuarioId = :usuarioId AND estado = 'PENDIENTE'")
    suspend fun obtenerTotalPendientePorUsuario(usuarioId: Long): Double?

    @Query("SELECT COUNT(*) FROM cuentas_por_cobrar WHERE estado = 'PENDIENTE'")
    suspend fun obtenerCantidadCuentasPendientes(): Int
} 