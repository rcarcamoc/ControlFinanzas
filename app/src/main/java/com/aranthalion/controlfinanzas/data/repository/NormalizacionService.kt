package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.util.NormalizacionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para normalización de datos de movimientos (HITO 1.1)
 * Maneja la migración de datos existentes y la normalización de nuevos movimientos
 */
@Singleton
class NormalizacionService @Inject constructor(
    private val movimientoDao: MovimientoDao
) {
    
    /**
     * Migra todos los datos existentes para agregar campos normalizados
     */
    suspend fun migrarDatosExistentes(): Boolean = withContext(Dispatchers.IO) {
        try {
            val movimientos = movimientoDao.obtenerMovimientos()
            var procesados = 0
            
            movimientos.forEach { movimiento ->
                val camposNormalizados = NormalizacionUtils.normalizarMovimiento(
                    movimiento.descripcion,
                    movimiento.monto,
                    movimiento.fecha
                )
                
                movimientoDao.actualizarCamposNormalizados(
                    id = movimiento.id,
                    descripcionNormalizada = camposNormalizados["descripcionNormalizada"] as String,
                    montoCategoria = camposNormalizados["montoCategoria"] as String,
                    fechaMes = camposNormalizados["fechaMes"] as String,
                    fechaDiaSemana = camposNormalizados["fechaDiaSemana"] as String,
                    fechaDia = camposNormalizados["fechaDia"] as Int,
                    fechaAnio = camposNormalizados["fechaAnio"] as Int
                )
                
                procesados++
            }
            
            println("✅ HITO 1.1: Migración completada. $procesados movimientos normalizados.")
            true
        } catch (e: Exception) {
            println("❌ HITO 1.1: Error en migración: ${e.message}")
            false
        }
    }
    
    /**
     * Normaliza un movimiento antes de guardarlo
     */
    fun normalizarMovimientoParaGuardar(movimiento: MovimientoEntity): MovimientoEntity {
        val camposNormalizados = NormalizacionUtils.normalizarMovimiento(
            movimiento.descripcion,
            movimiento.monto,
            movimiento.fecha
        )
        
        return movimiento.copy(
            descripcionNormalizada = camposNormalizados["descripcionNormalizada"] as String,
            montoCategoria = camposNormalizados["montoCategoria"] as String,
            fechaMes = camposNormalizados["fechaMes"] as String,
            fechaDiaSemana = camposNormalizados["fechaDiaSemana"] as String,
            fechaDia = camposNormalizados["fechaDia"] as Int,
            fechaAnio = camposNormalizados["fechaAnio"] as Int
        )
    }
    
    /**
     * Obtiene movimientos sin categoría usando consultas optimizadas
     */
    suspend fun obtenerMovimientosSinCategoriaOptimizado(limit: Int = 50): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientosSinCategoriaOptimizado(limit)
    }
    
    /**
     * Obtiene movimientos similares usando campos normalizados
     */
    suspend fun obtenerMovimientosSimilaresOptimizado(
        descripcion: String,
        limit: Int = 10
    ): List<MovimientoEntity> {
        val descripcionNormalizada = NormalizacionUtils.normalizarDescripcion(descripcion)
        return movimientoDao.obtenerMovimientosSimilaresExactos(descripcionNormalizada, limit)
    }
    
    /**
     * Obtiene movimientos similares por monto y patrón
     */
    suspend fun obtenerMovimientosSimilaresPorMontoOptimizado(
        descripcion: String,
        monto: Double,
        limit: Int = 10
    ): List<MovimientoEntity> {
        val descripcionNormalizada = NormalizacionUtils.normalizarDescripcion(descripcion)
        val categoriaMonto = NormalizacionUtils.categorizarMonto(monto)
        
        return movimientoDao.obtenerMovimientosSimilaresPorMonto(
            descripcionNormalizada,
            categoriaMonto,
            limit
        )
    }
    
    /**
     * Calcula similitud rápida entre dos descripciones
     */
    fun calcularSimilitudRapida(descripcion1: String, descripcion2: String): Double {
        val normalizada1 = NormalizacionUtils.normalizarDescripcion(descripcion1)
        val normalizada2 = NormalizacionUtils.normalizarDescripcion(descripcion2)
        return NormalizacionUtils.calcularSimilitudRapida(normalizada1, normalizada2)
    }
    
    /**
     * Obtiene movimientos por categoría de monto optimizado
     */
    suspend fun obtenerMovimientosPorCategoriaMonto(categoriaMonto: String, limit: Int = 20): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientosPorCategoriaMonto(categoriaMonto, limit)
    }
    
    /**
     * Obtiene movimientos por mes optimizado
     */
    suspend fun obtenerMovimientosPorMes(mes: String, limit: Int = 20): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientosPorMes(mes, limit)
    }
    
    /**
     * Cuenta movimientos sin categoría
     */
    suspend fun contarMovimientosSinCategoria(): Int {
        return movimientoDao.contarMovimientosSinCategoria()
    }
    
    /**
     * Cuenta movimientos con categoría
     */
    suspend fun contarMovimientosConCategoria(): Int {
        return movimientoDao.contarMovimientosConCategoria()
    }
    
    /**
     * Obtiene estadísticas de clasificación
     */
    suspend fun obtenerEstadisticasClasificacion(): Map<String, Int> {
        val sinCategoria = contarMovimientosSinCategoria()
        val conCategoria = contarMovimientosConCategoria()
        val total = sinCategoria + conCategoria
        
        return mapOf(
            "sinCategoria" to sinCategoria,
            "conCategoria" to conCategoria,
            "total" to total,
            "porcentajeClasificado" to if (total > 0) ((conCategoria * 100) / total) else 0
        )
    }
} 