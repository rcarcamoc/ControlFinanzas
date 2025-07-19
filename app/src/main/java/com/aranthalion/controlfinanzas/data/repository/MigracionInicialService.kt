package com.aranthalion.controlfinanzas.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para manejar la migración inicial de datos (HITO 1.1)
 * Se ejecuta automáticamente cuando se detecta que la base de datos se actualizó
 */
@Singleton
class MigracionInicialService @Inject constructor(
    private val normalizacionService: NormalizacionService
) {
    
    /**
     * Inicia la migración de datos en segundo plano
     */
    fun iniciarMigracionEnBackground() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("🚀 HITO 1.1: Iniciando migración de datos en background...")
                val resultado = normalizacionService.migrarDatosExistentes()
                
                if (resultado) {
                    println("✅ HITO 1.1: Migración completada exitosamente")
                } else {
                    println("❌ HITO 1.1: Error en la migración")
                }
            } catch (e: Exception) {
                println("❌ HITO 1.1: Excepción durante migración: ${e.message}")
            }
        }
    }
    
    /**
     * Verifica si es necesario migrar datos
     */
    suspend fun verificarNecesidadMigracion(): Boolean {
        // Por ahora, siempre ejecutamos la migración
        // En el futuro, podríamos verificar si ya existen campos normalizados
        return true
    }
} 