package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.local.entity.AuditoriaEntity

@Dao
interface AuditoriaDao {
    @Insert
    suspend fun insertarAuditoria(auditoria: AuditoriaEntity)
    
    @Query("SELECT * FROM auditoria ORDER BY timestamp DESC LIMIT 100")
    suspend fun obtenerAuditoriaReciente(): List<AuditoriaEntity>
    
    @Query("SELECT * FROM auditoria WHERE tabla = :tabla ORDER BY timestamp DESC LIMIT 50")
    suspend fun obtenerAuditoriaPorTabla(tabla: String): List<AuditoriaEntity>
    
    @Query("SELECT * FROM auditoria WHERE operacion = :operacion ORDER BY timestamp DESC LIMIT 50")
    suspend fun obtenerAuditoriaPorOperacion(operacion: String): List<AuditoriaEntity>
    
    @Query("SELECT * FROM auditoria WHERE timestamp >= :timestampInicio ORDER BY timestamp DESC")
    suspend fun obtenerAuditoriaDesde(timestampInicio: Long): List<AuditoriaEntity>
    
    @Query("DELETE FROM auditoria WHERE timestamp < :timestampLimite")
    suspend fun limpiarAuditoriaAntigua(timestampLimite: Long)
} 