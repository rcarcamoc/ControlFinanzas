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
     * Carga los datos históricos para entrenar el sistema
     */
    suspend fun cargarDatosHistoricos() {
        Log.d("ClasificacionUseCase", "📚 Iniciando carga de datos históricos...")
        repository.cargarDatosHistoricos()
        Log.d("ClasificacionUseCase", "✅ Datos históricos cargados")
    }
    
    /**
     * Extrae patrones clave de una descripción para mejorar la clasificación
     */
    private fun extraerPatrones(descripcion: String): List<String> {
        val descripcionNormalizada = descripcion.trim().lowercase()
        val patrones = mutableListOf<String>()
        
        // Patrón completo
        patrones.add(descripcionNormalizada)
        
        // Patrones por palabras clave
        val palabras = descripcionNormalizada.split(" ").filter { it.length > 2 }
        palabras.forEach { palabra ->
            if (palabra.length > 3) {
                patrones.add(palabra)
            }
        }
        
        // Patrones por prefijos comunes
        val prefijosComunes = listOf("COPEC", "SHELL", "UNIRED", "FALABELLA", "GOOGLE", "PAYSCAN", "MERPAGO")
        prefijosComunes.forEach { prefijo ->
            if (descripcionNormalizada.contains(prefijo.lowercase())) {
                patrones.add(prefijo.lowercase())
            }
        }
        
        return patrones.distinct()
    }
    
    /**
     * Obtiene todos los patrones almacenados
     */
    suspend fun obtenerTodosLosPatrones(): List<ClasificacionAutomatica> {
        Log.d("ClasificacionUseCase", "📋 Obteniendo todos los patrones almacenados...")
        val patrones = repository.obtenerTodosLosPatrones()
        Log.d("ClasificacionUseCase", "📊 Total de patrones: ${patrones.size}")
        return patrones
    }
} 