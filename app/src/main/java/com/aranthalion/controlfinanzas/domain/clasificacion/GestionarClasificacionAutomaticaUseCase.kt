package com.aranthalion.controlfinanzas.domain.clasificacion

import javax.inject.Inject

class GestionarClasificacionAutomaticaUseCase @Inject constructor(
    private val repository: ClasificacionAutomaticaRepository
) {
    
    /**
     * Registra un nuevo patrón o actualiza uno existente basado en la clasificación manual del usuario
     */
    suspend fun aprenderPatron(descripcion: String, categoriaId: Long) {
        val patrones = extraerPatrones(descripcion)
        patrones.forEach { patron ->
            repository.guardarPatron(patron, categoriaId)
        }
    }
    
    /**
     * Busca el patrón más relevante y devuelve una sugerencia de categoría con su nivel de confianza
     */
    suspend fun sugerirCategoria(descripcion: String): SugerenciaClasificacion? {
        return repository.obtenerSugerencia(descripcion)
    }
    
    /**
     * Carga los datos históricos para entrenar el sistema
     */
    suspend fun cargarDatosHistoricos() {
        repository.cargarDatosHistoricos()
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
        return repository.obtenerTodosLosPatrones()
    }
} 