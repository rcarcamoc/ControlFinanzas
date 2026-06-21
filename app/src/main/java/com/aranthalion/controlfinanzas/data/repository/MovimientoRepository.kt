package com.aranthalion.controlfinanzas.data.repository

import android.content.Context
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoEliminadoDao
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEliminadoEntity
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.aranthalion.controlfinanzas.data.repository.AuditoriaService

class MovimientoRepository @Inject constructor(
    private val movimientoDao: MovimientoDao,
    private val categoriaDao: CategoriaDao,
    private val movimientoEliminadoDao: MovimientoEliminadoDao,
    private val context: Context,
    private val auditoriaService: AuditoriaService,
    private val normalizacionService: NormalizacionService,
    private val cacheService: CacheService,
    private val configuracionPreferences: com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
) {
    suspend fun obtenerMovimientos(): List<MovimientoEntity> {
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        val movimientos = movimientoDao.obtenerMovimientos().filter { it.scope == activeScope }
        println("🔍 DEBUG: MovimientoRepository.obtenerMovimientos() - Scope: $activeScope - Total: ${movimientos.size}")
        movimientos.take(3).forEach { movimiento ->
            println("  - ${movimiento.descripcion}: ${movimiento.fecha} (tipo: ${movimiento.tipo})")
        }
        return movimientos
    }

    /**
     * Obtiene TODOS los movimientos sin filtro de scope.
     * Usado por el EmailSyncWorker para detectar duplicados independientemente del scope activo.
     */
    suspend fun obtenerTodosLosMovimientos(): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientos()
    }

    /**
     * Obtiene un movimiento por su ID sin filtro de scope.
     * Usado para lookups en clasificación y edición donde el scope puede cambiar.
     */
    suspend fun obtenerMovimientoPorId(id: Long): MovimientoEntity? {
        return movimientoDao.obtenerMovimientoPorId(id)
    }

    /**
     * Obtiene todos los movimientos sin categoría de TODOS los scopes.
     * Usado por el asistente de clasificación para mostrar TODAS las pendientes.
     */
    suspend fun obtenerMovimientosSinCategoriaTodosScopes(): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientosSinCategoriaTodosScopes()
    }

    // Métodos optimizados del HITO 1
    suspend fun obtenerMovimientosOptimizado(): List<MovimientoEntity> {
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        return movimientoDao.obtenerMovimientos().filter { it.scope == activeScope }
    }
    
    suspend fun obtenerMovimientosPorPeriodoOptimizado(periodo: String): List<MovimientoEntity> {
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        val movimientos = cacheService.getMovimientosPorPeriodo(periodo) {
            movimientoDao.obtenerMovimientosPorPeriodoFacturacion(periodo)
        }
        return movimientos.filter { it.scope == activeScope }
    }

    suspend fun obtenerMovimientosPorPeriodo(fechaInicio: Date, fechaFin: Date): List<MovimientoEntity> {
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        return movimientoDao.obtenerMovimientosPorPeriodo(fechaInicio, fechaFin).filter { it.scope == activeScope }
    }

    suspend fun obtenerCategorias(): List<Categoria> {
        return cacheService.getCategorias {
            categoriaDao.obtenerCategorias()
        }
    }
    
    // Método optimizado para categorías (HITO 1.3)
    suspend fun obtenerCategoriasOptimizado(): List<Categoria> {
        return cacheService.getCategorias {
            categoriaDao.obtenerCategorias()
        }
    }

    suspend fun agregarMovimiento(movimiento: MovimientoEntity, metodo: String = "INSERT", dao: String = "MovimientoDao") {
        // Buscar si ya existe un movimiento con el mismo idUnico
        val idUnico = movimiento.idUnico
        val existentes = movimientoDao.obtenerMovimientos().filter { it.idUnico == idUnico }
        val timestamp = System.currentTimeMillis()
        if (existentes.isNotEmpty()) {
            // Ya existe, actualizar — pero preservar campos clasificados manualmente
            val existente = existentes.first()
            val movimientoActualizado = movimiento.copy(
                id = existente.id,
                // Preservar categoría si el nuevo no trae ninguna (evita que el correo borre clasificaciones manuales)
                categoriaId = movimiento.categoriaId ?: existente.categoriaId,
                // Preservar scope y userId del existente si el nuevo no los define
                scope = movimiento.scope.ifEmpty { existente.scope },
                userId_internal = movimiento.userId_internal ?: existente.userId_internal,
                // Preservar tipo si el existente fue marcado como OMITIR
                tipo = if (existente.tipo == "OMITIR") "OMITIR" else movimiento.tipo,
                fechaActualizacion = timestamp,
                metodoActualizacion = "UPDATE_POR_DUPLICADO",
                daoResponsable = dao
            )
            movimientoDao.actualizarMovimiento(movimientoActualizado)
            auditoriaService.registrarOperacion(
                tabla = "movimientos",
                operacion = "UPDATE_POR_DUPLICADO",
                entidadId = movimientoActualizado.id,
                detalles = "Movimiento actualizado por duplicado: ${movimientoActualizado.descripcion} - Monto: ${movimientoActualizado.monto} - Tipo: ${movimientoActualizado.tipo}",
                daoResponsable = dao
            )
            cacheService.invalidarCacheMovimientosSinCategoria()
            cacheService.invalidarCacheEstadisticas()
            cacheService.invalidarCacheMovimientosPorPeriodo()
            println("⚠️ DUPLICADO: Movimiento actualizado - ID: ${movimientoActualizado.id}, idUnico: $idUnico, Timestamp: $timestamp")
        } else {
            // No existe, insertar normalmente
            val movimientoNormalizado = normalizacionService.normalizarMovimientoParaGuardar(movimiento)
        val movimientoConAuditoria = movimientoNormalizado.copy(
            fechaCreacion = timestamp,
            fechaActualizacion = timestamp,
            metodoActualizacion = metodo,
            daoResponsable = dao
        )
        movimientoDao.agregarMovimiento(movimientoConAuditoria)
        auditoriaService.registrarOperacion(
            tabla = "movimientos",
            operacion = "INSERT",
            entidadId = movimientoConAuditoria.id,
            detalles = "Movimiento agregado: ${movimientoConAuditoria.descripcion} - Monto: ${movimientoConAuditoria.monto} - Tipo: ${movimientoConAuditoria.tipo}",
            daoResponsable = dao
        )
        cacheService.invalidarCacheMovimientosSinCategoria()
        cacheService.invalidarCacheEstadisticas()
        cacheService.invalidarCacheMovimientosPorPeriodo()
            println("📝 AUDITORÍA: Movimiento agregado - ID: ${movimiento.id}, idUnico: $idUnico, Método: $metodo, DAO: $dao, Timestamp: $timestamp")
        }
    }

    suspend fun actualizarMovimiento(movimiento: MovimientoEntity, metodo: String = "UPDATE", dao: String = "MovimientoDao") {
        val timestamp = System.currentTimeMillis()
        val movimientoConAuditoria = movimiento.copy(
            fechaActualizacion = timestamp,
            metodoActualizacion = metodo,
            daoResponsable = dao
        )
        movimientoDao.actualizarMovimiento(movimientoConAuditoria)
        
        // Registrar auditoría
        auditoriaService.registrarOperacion(
            tabla = "movimientos",
            operacion = "UPDATE",
            entidadId = movimiento.id,
            detalles = "Movimiento actualizado: ${movimiento.descripcion} - Monto: ${movimiento.monto} - Categoría: ${movimiento.categoriaId}",
            daoResponsable = dao
        )
        
        // Invalidar cache relacionado
        cacheService.invalidarCacheMovimientosSinCategoria()
        cacheService.invalidarCacheEstadisticas()
        cacheService.invalidarCacheMovimientosPorPeriodo()
        
        println("📝 AUDITORÍA: Movimiento actualizado - ID: ${movimiento.id}, Método: $metodo, DAO: $dao, Timestamp: $timestamp")
    }

    suspend fun eliminarMovimiento(movimiento: MovimientoEntity) {
        val timestamp = System.currentTimeMillis()
        println("📝 AUDITORÍA: Eliminando movimiento individual - ID: ${movimiento.id}, Descripción: ${movimiento.descripcion}")
        
        // Registrar auditoría antes de eliminar
        auditoriaService.registrarOperacion(
            tabla = "movimientos",
            operacion = "DELETE_INDIVIDUAL",
            entidadId = movimiento.id,
            detalles = "Movimiento eliminado: ${movimiento.descripcion} - Monto: ${movimiento.monto} - Tipo: ${movimiento.tipo}",
            daoResponsable = "MovimientoDao"
        )
        
        // Registrar tombstone para sincronización web (solo si tiene idUnico no vacío)
        if (movimiento.idUnico.isNotEmpty()) {
            movimientoEliminadoDao.insertarEliminado(
                MovimientoEliminadoEntity(
                    idUnico = movimiento.idUnico,
                    deletedAt = timestamp,
                    syncPending = true
                )
            )
            println("🗑️ SYNC: Tombstone registrada para idUnico: ${movimiento.idUnico}")
        }
        
        // Ahora eliminar el movimiento
        movimientoDao.eliminarMovimiento(movimiento)
        
        // Invalidar cache relacionado
        cacheService.invalidarCacheMovimientosSinCategoria()
        cacheService.invalidarCacheEstadisticas()
        cacheService.invalidarCacheMovimientosPorPeriodo()
        
        println("✅ AUDITORÍA: Movimiento eliminado exitosamente")
    }

    suspend fun obtenerIdUnicos(): Set<String> {
        return movimientoDao.obtenerIdUnicos().toSet()
    }

    suspend fun obtenerIdUnicosPorPeriodo(periodo: String?): Set<String> {
        return movimientoDao.obtenerIdUnicosPorPeriodo(periodo).toSet()
    }

    suspend fun obtenerCategoriasPorIdUnico(periodo: String?): Map<String, Long?> {
        return movimientoDao.obtenerCategoriasPorIdUnico(periodo).associate { it.idUnico to it.categoriaId }
    }

    suspend fun eliminarMovimientosPorPeriodo(periodo: String?) {
        val timestamp = System.currentTimeMillis()
        println("📝 AUDITORÍA: Eliminando movimientos por período - Período: $periodo, Timestamp: $timestamp")
        
        // Obtener los movimientos que se van a eliminar para registrar auditoría y registrar tombstones
        val movimientosAEliminar = movimientoDao.obtenerMovimientos().filter { 
            it.periodoFacturacion == periodo 
        }
        
        println("📝 AUDITORÍA: Movimientos a eliminar: ${movimientosAEliminar.size}")
        
        // Registrar auditoría y tombstone para cada movimiento antes de eliminarlo
        movimientosAEliminar.forEach { movimiento ->
            auditoriaService.registrarOperacion(
                tabla = "movimientos",
                operacion = "DELETE_PERIODO",
                entidadId = movimiento.id,
                detalles = "Movimiento eliminado por período $periodo: ${movimiento.descripcion} - Monto: ${movimiento.monto}",
                daoResponsable = "MovimientoDao"
            )
            if (movimiento.idUnico.isNotEmpty()) {
                movimientoEliminadoDao.insertarEliminado(
                    MovimientoEliminadoEntity(
                        idUnico = movimiento.idUnico,
                        deletedAt = timestamp,
                        syncPending = true
                    )
                )
            }
            println("📝 AUDITORÍA: Registrada eliminación y tombstone para movimiento ID: ${movimiento.id}, Descripción: ${movimiento.descripcion}")
        }
        
        // Ahora eliminar los movimientos
        movimientoDao.eliminarMovimientosPorPeriodo(periodo)
        
        // Invalidar cache relacionado
        cacheService.invalidarCacheMovimientosSinCategoria()
        cacheService.invalidarCacheEstadisticas()
        cacheService.invalidarCacheMovimientosPorPeriodo()
        
        println("✅ AUDITORÍA: Eliminación completada para período: $periodo")
    }
    
    // Métodos de auditoría
    suspend fun obtenerMovimientosRecientes(): List<MovimientoEntity> {
        val movimientos = movimientoDao.obtenerMovimientosRecientes()
        println("🔍 AUDITORIA_REPO: Movimientos recientes obtenidos: ${movimientos.size}")
        movimientos.take(5).forEach { movimiento ->
            println("  - ID: ${movimiento.id}, Descripción: ${movimiento.descripcion}, Método: ${movimiento.metodoActualizacion}, DAO: ${movimiento.daoResponsable}")
        }
        return movimientos
    }
    
    suspend fun obtenerMovimientosPorMetodo(metodo: String): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientosPorMetodo(metodo)
    }
    
    suspend fun actualizarAuditoria(id: Long, metodo: String, dao: String) {
        val timestamp = System.currentTimeMillis()
        movimientoDao.actualizarAuditoria(id, timestamp, metodo, dao)
        println("📝 AUDITORÍA: Actualizando auditoría - ID: $id, Método: $metodo, DAO: $dao, Timestamp: $timestamp")
    }

    /**
     * Limpia todos los datos históricos de la base de datos
     * Útil para instalaciones completamente limpias
     */
    suspend fun limpiarTodosLosDatos() {
        try {
            println("🧹 Limpiando todos los datos históricos...")
            
            // Obtener todos los movimientos
            val movimientosExistentes = movimientoDao.obtenerMovimientos()
            
            if (movimientosExistentes.isNotEmpty()) {
                // Eliminar todos los movimientos
                movimientosExistentes.forEach { movimiento ->
                    movimientoDao.eliminarMovimiento(movimiento)
                }
                println("🗑️ Eliminados ${movimientosExistentes.size} movimientos de la base de datos")
            } else {
                println("ℹ️ No hay movimientos para eliminar")
            }
            
            // Invalidar cache
            cacheService.invalidarTodoCache()
            
            println("✅ Base de datos limpiada completamente")
            
        } catch (e: Exception) {
            println("❌ Error al limpiar datos: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Carga datos históricos hardcodeados (solo una vez)
     */
    suspend fun cargarDatosHistoricos() {
        DatosHistoricosPredefinidos.cargarDatosHistoricos(movimientoDao, categoriaDao)
    }
    
    // Métodos optimizados usando campos normalizados (HITO 1.1)
    
    /**
     * Obtiene movimientos sin categoría usando consultas optimizadas
     */
    suspend fun obtenerMovimientosSinCategoriaOptimizado(limit: Int = 50): List<MovimientoEntity> {
        return cacheService.getMovimientosSinCategoria {
            normalizacionService.obtenerMovimientosSinCategoriaOptimizado(limit)
        }
    }
    
    /**
     * Obtiene movimientos similares usando campos normalizados
     */
    suspend fun obtenerMovimientosSimilaresOptimizado(
        descripcion: String,
        limit: Int = 10
    ): List<MovimientoEntity> {
        return normalizacionService.obtenerMovimientosSimilaresOptimizado(descripcion, limit)
    }
    
    /**
     * Obtiene movimientos similares por monto y patrón
     */
    suspend fun obtenerMovimientosSimilaresPorMontoOptimizado(
        descripcion: String,
        monto: Double,
        limit: Int = 10
    ): List<MovimientoEntity> {
        return normalizacionService.obtenerMovimientosSimilaresPorMontoOptimizado(descripcion, monto, limit)
    }
    
    /**
     * Calcula similitud rápida entre dos descripciones
     */
    fun calcularSimilitudRapida(descripcion1: String, descripcion2: String): Double {
        return normalizacionService.calcularSimilitudRapida(descripcion1, descripcion2)
    }
    
    /**
     * Migra datos existentes para agregar campos normalizados
     */
    suspend fun migrarDatosExistentes(): Boolean {
        return normalizacionService.migrarDatosExistentes()
    }
    
    /**
     * Obtiene estadísticas de clasificación optimizadas
     */
    suspend fun obtenerEstadisticasClasificacion(): Map<String, Int> {
        return cacheService.getEstadisticas {
            normalizacionService.obtenerEstadisticasClasificacion()
        }
    }
    
    /**
     * Obtiene movimientos por categoría de monto optimizado
     */
    suspend fun obtenerMovimientosPorCategoriaMonto(categoriaMonto: String, limit: Int = 20): List<MovimientoEntity> {
        return normalizacionService.obtenerMovimientosPorCategoriaMonto(categoriaMonto, limit)
    }
    
    /**
     * Obtiene movimientos por mes optimizado
     */
    suspend fun obtenerMovimientosPorMes(mes: String, limit: Int = 20): List<MovimientoEntity> {
        return normalizacionService.obtenerMovimientosPorMes(mes, limit)
    }
    
    /**
     * Función auxiliar para obtener fechas de inicio y fin de un período
     */
    private fun obtenerFechasDePeriodo(periodo: String): Pair<Date, Date> {
        val (anio, mes) = periodo.split("-")
        val calendar = java.util.Calendar.getInstance()
        
        // Fecha de inicio: primer día del mes
        calendar.set(anio.toInt(), mes.toInt() - 1, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val fechaInicio = calendar.time
        
        // Fecha de fin: último día del mes
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.add(java.util.Calendar.MILLISECOND, -1)
        val fechaFin = calendar.time
        
        return Pair(fechaInicio, fechaFin)
    }
}