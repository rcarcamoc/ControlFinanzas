package com.aranthalion.controlfinanzas.presentation.screens.ml

import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase

/**
 * Servicio de clasificación automática con Machine Learning.
 * Separado de la lógica de UI para facilitar testing y reutilización.
 * 
 * RESPONSABILIDADES:
 * - Clasificar transacciones sin categoría
 * - Llamar al UseCase de ML
 * - Procesar resultados
 * - Manejo de errores específicos de ML
 */
class ClasificacionMLService(
    private val clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
) {
    /**
     * Obtiene una sugerencia de categoría para una transacción.
     * 
     * @param movimiento Transacción a clasificar
     * @return ID de la categoría sugerida o null si no hay sugerencia
     */
    suspend fun obtenerSugerenciaCategoria(movimiento: MovimientoEntity): Long? {
        return try {
            clasificacionUseCase(
                descripcion = movimiento.descripcion,
                monto = movimiento.monto,
                tipo = movimiento.tipo
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Clasifica múltiples transacciones sin categoría.
     * 
     * @param movimientos Lista de transacciones sin clasificar
     * @return Mapa de ID de transacción a ID de categoría sugerida
     */
    suspend fun clasificarMasivamente(
        movimientos: List<MovimientoEntity>
    ): Map<Long, Long> {
        val resultados = mutableMapOf<Long, Long>()
        
        movimientos.forEach { movimiento ->
            val categoriaId = obtenerSugerenciaCategoria(movimiento)
            if (categoriaId != null) {
                resultados[movimiento.id] = categoriaId
            }
        }
        
        return resultados
    }
    
    /**
     * Verifica si el modelo ML está disponible.
     */
    suspend fun esModeloDisponible(): Boolean {
        return try {
            // Prueba simple para verificar disponibilidad
            clasificacionUseCase(
                descripcion = "test",
                monto = 0.0,
                tipo = "GASTO"
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
