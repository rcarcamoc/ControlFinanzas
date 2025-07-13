package com.aranthalion.controlfinanzas.domain.clasificacion

import android.util.Log
import javax.inject.Inject

class GestionarClasificacionAutomaticaUseCase @Inject constructor(
    private val repository: ClasificacionAutomaticaRepository
) {
    
    /**
     * Registra un nuevo patrón o actualiza uno existente basado en la clasificación manual del usuario
     */
    suspend fun aprenderPatron(descripcion: String, categoriaId: Long) {
        Log.d("ClasificacionUseCase", "🔄 Aprendiendo patrón: '$descripcion' -> Categoría ID: $categoriaId")
        val patrones = extraerPatrones(descripcion)
        Log.d("ClasificacionUseCase", "📝 Patrones extraídos: $patrones")
        patrones.forEach { patron ->
            repository.guardarPatron(patron, categoriaId)
        }
        Log.d("ClasificacionUseCase", "✅ Patrón aprendido exitosamente")
    }
    
    /**
     * Busca el patrón más relevante y devuelve una sugerencia de categoría con su nivel de confianza
     * MÉTODO LEGACY - Usar obtenerSugerenciaMejorada() en su lugar
     */
    suspend fun sugerirCategoria(descripcion: String): SugerenciaClasificacion? {
        Log.d("ClasificacionUseCase", "🔍 Buscando sugerencia para: '$descripcion'")
        val sugerencia = repository.obtenerSugerencia(descripcion)
        if (sugerencia != null) {
            Log.d("ClasificacionUseCase", "✅ Sugerencia encontrada: Categoría ID ${sugerencia.categoriaId}, Confianza: ${sugerencia.nivelConfianza}, Patrón: '${sugerencia.patron}'")
        } else {
            Log.d("ClasificacionUseCase", "❌ No se encontró sugerencia para: '$descripcion'")
        }
        return sugerencia
    }
    
    /**
     * NUEVO MÉTODO: Sistema mejorado de clasificación automática
     * Usa cache, fuzzy matching y umbrales configurables
     */
    suspend fun obtenerSugerenciaMejorada(descripcion: String): ResultadoClasificacion {
        Log.d("ClasificacionUseCase", "🔍 Buscando sugerencia mejorada para: '$descripcion'")
        val resultado = repository.obtenerSugerenciaMejorada(descripcion)
        
        when (resultado) {
            is ResultadoClasificacion.AltaConfianza -> {
                Log.d("ClasificacionUseCase", "✅ Alta confianza: Categoría ID ${resultado.categoriaId}, Confianza: ${(resultado.confianza * 100).toInt()}%, Tipo: ${resultado.tipoCoincidencia}")
            }
            is ResultadoClasificacion.BajaConfianza -> {
                Log.d("ClasificacionUseCase", "⚠️ Baja confianza: ${resultado.sugerencias.size} sugerencias, Máxima: ${(resultado.confianzaMaxima * 100).toInt()}%")
            }
            is ResultadoClasificacion.SinCoincidencias -> {
                Log.d("ClasificacionUseCase", "❌ Sin coincidencias: ${resultado.razon}")
            }
        }
        
        return resultado
    }
    
    /**
     * Carga los datos históricos para entrenar el sistema
     */
    suspend fun cargarDatosHistoricos() {
        Log.d("ClasificacionUseCase", "📚 Iniciando carga de datos históricos...")
        repository.cargarDatosHistoricos()
        Log.d("ClasificacionUseCase", "✅ Datos históricos cargados")
    }
    
    /**
     * Actualiza el cache de clasificaciones
     */
    suspend fun actualizarCacheClasificaciones() {
        Log.d("ClasificacionUseCase", "🔄 Actualizando cache de clasificaciones...")
        repository.actualizarCacheClasificaciones()
        Log.d("ClasificacionUseCase", "✅ Cache actualizado")
    }
    
    /**
     * Obtiene estadísticas del sistema de clasificación
     */
    suspend fun obtenerEstadisticasClasificacion(): EstadisticasClasificacion {
        Log.d("ClasificacionUseCase", "📊 Obteniendo estadísticas de clasificación...")
        val estadisticas = repository.obtenerEstadisticasClasificacion()
        Log.d("ClasificacionUseCase", "✅ Estadísticas obtenidas: ${estadisticas.totalTransacciones} transacciones totales")
        return estadisticas
    }
    
    /**
     * Extrae patrones de una descripción para mejorar el aprendizaje
     */
    private fun extraerPatrones(descripcion: String): List<String> {
        val patrones = mutableListOf<String>()
        
        // Patrón completo
        patrones.add(descripcion.trim())
        
        // Palabras individuales (si tienen más de 3 caracteres)
        val palabras = descripcion.split(" ").filter { it.length > 3 }
        patrones.addAll(palabras)
        
        // Combinaciones de 2 palabras
        if (palabras.size >= 2) {
            for (i in 0 until palabras.size - 1) {
                patrones.add("${palabras[i]} ${palabras[i + 1]}")
            }
        }
        
        return patrones.distinct()
    }
    
    /**
     * Verifica si una sugerencia tiene confianza suficiente para ser mostrada
     */
    fun esConfianzaSuficiente(confianza: Double): Boolean {
        return confianza >= 0.6 // 60% de confianza mínima
    }
    
    /**
     * Obtiene todos los patrones aprendidos
     */
    suspend fun obtenerTodosLosPatrones(): List<ClasificacionAutomatica> {
        Log.d("ClasificacionUseCase", "📋 Obteniendo todos los patrones...")
        val patrones = repository.obtenerTodosLosPatrones()
        Log.d("ClasificacionUseCase", "✅ Patrones obtenidos: ${patrones.size}")
        return patrones
    }
    
    /**
     * Actualiza la confianza de un patrón específico
     */
    suspend fun actualizarConfianza(patron: String, categoriaId: Long, nuevaConfianza: Double) {
        Log.d("ClasificacionUseCase", "🔄 Actualizando confianza: '$patron' -> $nuevaConfianza")
        repository.actualizarConfianza(patron, categoriaId, nuevaConfianza)
        Log.d("ClasificacionUseCase", "✅ Confianza actualizada")
    }
} 