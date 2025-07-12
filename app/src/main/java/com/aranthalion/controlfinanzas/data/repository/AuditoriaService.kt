package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.AuditoriaDao
import com.aranthalion.controlfinanzas.data.local.entity.AuditoriaEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditoriaService @Inject constructor(
    private val auditoriaDao: AuditoriaDao
) {
    suspend fun registrarOperacion(
        tabla: String,
        operacion: String,
        entidadId: Long? = null,
        detalles: String,
        daoResponsable: String
    ) {
        val auditoria = AuditoriaEntity(
            tabla = tabla,
            operacion = operacion,
            entidadId = entidadId,
            detalles = detalles,
            daoResponsable = daoResponsable
        )
        
        auditoriaDao.insertarAuditoria(auditoria)
        println("📝 AUDITORIA_SERVICE: $operacion en tabla $tabla - $detalles")
    }
    
    suspend fun obtenerAuditoriaReciente(): List<AuditoriaEntity> {
        return auditoriaDao.obtenerAuditoriaReciente()
    }
    
    suspend fun obtenerAuditoriaPorTabla(tabla: String): List<AuditoriaEntity> {
        return auditoriaDao.obtenerAuditoriaPorTabla(tabla)
    }
    
    suspend fun obtenerAuditoriaPorOperacion(operacion: String): List<AuditoriaEntity> {
        return auditoriaDao.obtenerAuditoriaPorOperacion(operacion)
    }
    
    suspend fun limpiarAuditoriaAntigua(diasAtras: Int = 30) {
        val timestampLimite = System.currentTimeMillis() - (diasAtras * 24 * 60 * 60 * 1000L)
        auditoriaDao.limpiarAuditoriaAntigua(timestampLimite)
        println("🧹 AUDITORIA_SERVICE: Limpiada auditoría anterior a ${diasAtras} días")
    }
} 