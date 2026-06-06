package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio de cache en memoria para optimizar consultas frecuentes (HITO 1.3)
 * Implementa cache para categorías, configuraciones y transacciones frecuentes
 */
@Singleton
class CacheService @Inject constructor() {
    
    private val mutex = Mutex()
    private val cache = ConcurrentHashMap<String, Any>()
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    private val cacheExpiration = 5 * 60 * 1000L // 5 minutos
    
    // Cache para categorías
    private var categoriasCache: List<Categoria>? = null
    private var categoriasCacheTimestamp: Long = 0
    
    // Cache para movimientos sin categoría
    private var movimientosSinCategoriaCache: List<MovimientoEntity>? = null
    private var movimientosSinCategoriaCacheTimestamp: Long = 0
    
    // Cache para estadísticas
    private var estadisticasCache: Map<String, Int>? = null
    private var estadisticasCacheTimestamp: Long = 0
    
    // Cache para movimientos por período
    private val movimientosPorPeriodoCache = ConcurrentHashMap<String, List<MovimientoEntity>>()
    private val movimientosPorPeriodoCacheTimestamp = ConcurrentHashMap<String, Long>()
    
    /**
     * Obtiene categorías del cache o las carga si es necesario
     */
    suspend fun getCategorias(
        loadFunction: suspend () -> List<Categoria>
    ): List<Categoria> = mutex.withLock {
        val now = System.currentTimeMillis()
        
        if (categoriasCache == null || (now - categoriasCacheTimestamp) > cacheExpiration) {
            categoriasCache = loadFunction()
            categoriasCacheTimestamp = now
            println("🔄 HITO 1.3: Cache de categorías actualizado")
        }
        
        return categoriasCache!!
    }
    
    /**
     * Obtiene movimientos sin categoría del cache
     */
    suspend fun getMovimientosSinCategoria(
        loadFunction: suspend () -> List<MovimientoEntity>
    ): List<MovimientoEntity> = mutex.withLock {
        val now = System.currentTimeMillis()
        
        if (movimientosSinCategoriaCache == null || (now - movimientosSinCategoriaCacheTimestamp) > cacheExpiration) {
            movimientosSinCategoriaCache = loadFunction()
            movimientosSinCategoriaCacheTimestamp = now
            println("🔄 HITO 1.3: Cache de movimientos sin categoría actualizado")
        }
        
        return movimientosSinCategoriaCache!!
    }
    
    /**
     * Obtiene movimientos por período del cache
     */
    suspend fun getMovimientosPorPeriodo(
        periodo: String,
        loadFunction: suspend () -> List<MovimientoEntity>
    ): List<MovimientoEntity> = mutex.withLock {
        val now = System.currentTimeMillis()
        val timestamp = movimientosPorPeriodoCacheTimestamp[periodo] ?: 0
        
        if (!movimientosPorPeriodoCache.containsKey(periodo) || (now - timestamp) > cacheExpiration) {
            val movimientos = loadFunction()
            movimientosPorPeriodoCache[periodo] = movimientos
            movimientosPorPeriodoCacheTimestamp[periodo] = now
            println("🔄 HITO 1.3: Cache de movimientos por período '$periodo' actualizado")
        }
        
        return movimientosPorPeriodoCache[periodo]!!
    }
    
    /**
     * Obtiene estadísticas del cache
     */
    suspend fun getEstadisticas(
        loadFunction: suspend () -> Map<String, Int>
    ): Map<String, Int> = mutex.withLock {
        val now = System.currentTimeMillis()
        
        if (estadisticasCache == null || (now - estadisticasCacheTimestamp) > cacheExpiration) {
            estadisticasCache = loadFunction()
            estadisticasCacheTimestamp = now
            println("🔄 HITO 1.3: Cache de estadísticas actualizado")
        }
        
        return estadisticasCache!!
    }
    
    /**
     * Invalida el cache de movimientos por período
     */
    suspend fun invalidarCacheMovimientosPorPeriodo() = mutex.withLock {
        movimientosPorPeriodoCache.clear()
        movimientosPorPeriodoCacheTimestamp.clear()
        println("🗑️ HITO 1.3: Cache de movimientos por período invalidado")
    }
    
    /**
     * Invalida el cache de categorías
     */
    suspend fun invalidarCacheCategorias() = mutex.withLock {
        categoriasCache = null
        categoriasCacheTimestamp = 0
        println("🗑️ HITO 1.3: Cache de categorías invalidado")
    }
    
    /**
     * Invalida el cache de movimientos sin categoría
     */
    suspend fun invalidarCacheMovimientosSinCategoria() = mutex.withLock {
        movimientosSinCategoriaCache = null
        movimientosSinCategoriaCacheTimestamp = 0
        println("🗑️ HITO 1.3: Cache de movimientos sin categoría invalidado")
    }
    
    /**
     * Invalida el cache de estadísticas
     */
    suspend fun invalidarCacheEstadisticas() = mutex.withLock {
        estadisticasCache = null
        estadisticasCacheTimestamp = 0
        println("🗑️ HITO 1.3: Cache de estadísticas invalidado")
    }
    
    /**
     * Invalida todo el cache
     */
    suspend fun invalidarTodoCache() = mutex.withLock {
        categoriasCache = null
        categoriasCacheTimestamp = 0
        movimientosSinCategoriaCache = null
        movimientosSinCategoriaCacheTimestamp = 0
        estadisticasCache = null
        estadisticasCacheTimestamp = 0
        cache.clear()
        cacheTimestamps.clear()
        println("🗑️ HITO 1.3: Todo el cache invalidado")
    }
    
    /**
     * Obtiene información del estado del cache
     */
    fun getCacheInfo(): Map<String, Any> {
        val now = System.currentTimeMillis()
        return mapOf(
            "categoriasCache" to (categoriasCache != null && (now - categoriasCacheTimestamp) <= cacheExpiration),
            "movimientosSinCategoriaCache" to (movimientosSinCategoriaCache != null && (now - movimientosSinCategoriaCacheTimestamp) <= cacheExpiration),
            "estadisticasCache" to (estadisticasCache != null && (now - estadisticasCacheTimestamp) <= cacheExpiration),
            "cacheSize" to cache.size,
            "cacheExpiration" to cacheExpiration
        )
    }
} 