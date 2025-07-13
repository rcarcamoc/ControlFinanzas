package com.aranthalion.controlfinanzas.data.util

import android.util.Log
import java.util.*

/**
 * Sistema de normalización y fuzzy matching para clasificación automática
 * Basado en las mejores prácticas de las opiniones recibidas
 */
object ClasificacionNormalizer {
    
    // Umbrales configurables
    private const val UMBRAL_CONFIANZA_MINIMA = 0.6 // 60% - No mostrar sugerencias con menos confianza
    private const val UMBRAL_COINCIDENCIA_EXACTA = 1.0 // 100%
    private const val UMBRAL_COINCIDENCIA_PARCIAL = 0.9 // 90%
    private const val UMBRAL_FUZZY_ALTA = 0.8 // 80%
    private const val UMBRAL_FUZZY_MEDIA = 0.6 // 60%
    
    // Stop words para eliminar
    private val STOP_WORDS = setOf(
        "tienda", "servicio", "online", "web", "com", "cl", "santiago", "chile",
        "compra", "pago", "transferencia", "debito", "credito", "tarjeta",
        "sucursal", "local", "centro", "mall", "plaza", "avenida", "calle"
    )
    
    // Sinónimos de comercios conocidos
    private val SINONIMOS_COMERCIOS = mapOf(
        "starbucks" to listOf("starbucks sc", "starbucks av. libertad", "starbucks*", "starbucks coffee"),
        "netflix" to listOf("netflix.com", "nfx*", "netflix streaming"),
        "spotify" to listOf("spotify", "spotify premium", "spotify*"),
        "uber" to listOf("uber", "uber eats", "uber*"),
        "copec" to listOf("copec app", "copec santiago", "copec*"),
        "shell" to listOf("shell", "shell.fuel", "shell*"),
        "falabella" to listOf("falabella", "falabella.com", "falabella*"),
        "amazon" to listOf("amazon", "amazon.com", "amazon web services", "aws"),
        "google" to listOf("google", "google play", "google*"),
        "youtube" to listOf("youtube", "youtube premium", "youtube*"),
        "walmart" to listOf("walmart", "lider", "lider.cl"),
        "jumbo" to listOf("jumbo", "jumbo.cl"),
        "santa isabel" to listOf("santa isabel", "santaisabel"),
        "unimarc" to listOf("unimarc", "unimarc.cl"),
        "sodimac" to listOf("sodimac", "sodimac.cl"),
        "easy" to listOf("easy", "easy.cl"),
        "homecenter" to listOf("homecenter", "homecenter.cl"),
        "paris" to listOf("paris", "paris.cl"),
        "ripley" to listOf("ripley", "ripley.cl"),
        "hites" to listOf("hites", "hites.cl"),
        "la polar" to listOf("la polar", "lapolar.cl"),
        "alcampo" to listOf("alcampo", "alcampo.cl"),
        "oxxo" to listOf("oxxo", "oxxo*"),
        "santa emiliana" to listOf("santa emiliana", "santaemiliana"),
        "concha y toro" to listOf("concha y toro", "conchaytoro"),
        "undurraga" to listOf("undurraga", "undurraga.cl"),
        "santa rita" to listOf("santa rita", "santarita"),
        "santa carolina" to listOf("santa carolina", "santacarolina"),
        "santa emiliana" to listOf("santa emiliana", "santaemiliana"),
        "concha y toro" to listOf("concha y toro", "conchaytoro"),
        "undurraga" to listOf("undurraga", "undurraga.cl"),
        "santa rita" to listOf("santa rita", "santarita"),
        "santa carolina" to listOf("santa carolina", "santacarolina")
    )
    
    /**
     * Normaliza una descripción de transacción para mejorar la clasificación
     */
    fun normalizarDescripcion(descripcion: String): String {
        var normalizada = descripcion.trim().lowercase()
        
        // Eliminar caracteres especiales pero mantener espacios
        normalizada = normalizada.replace(Regex("[^a-z0-9\\s]"), " ")
        
        // Eliminar múltiples espacios
        normalizada = normalizada.replace(Regex("\\s+"), " ").trim()
        
        // Eliminar stop words
        val palabras = normalizada.split(" ").filter { it.isNotBlank() && it !in STOP_WORDS }
        normalizada = palabras.joinToString(" ")
        
        // Aplicar sinónimos
        normalizada = aplicarSinonimos(normalizada)
        
        Log.d("ClasificacionNormalizer", "🔧 Normalización: '$descripcion' -> '$normalizada'")
        return normalizada
    }
    
    /**
     * Aplica sinónimos de comercios conocidos
     */
    private fun aplicarSinonimos(descripcion: String): String {
        var resultado = descripcion
        for ((canonico, sinonimos) in SINONIMOS_COMERCIOS) {
            for (sinonimo in sinonimos) {
                if (resultado.contains(sinonimo)) {
                    resultado = resultado.replace(sinonimo, canonico)
                    break
                }
            }
        }
        return resultado
    }
    
    /**
     * Calcula la similitud entre dos strings usando algoritmo de Levenshtein
     */
    fun calcularSimilitud(str1: String, str2: String): Double {
        if (str1 == str2) return 1.0
        if (str1.isEmpty() || str2.isEmpty()) return 0.0
        
        val distancia = calcularDistanciaLevenshtein(str1, str2)
        val maxLength = maxOf(str1.length, str2.length)
        return 1.0 - (distancia.toDouble() / maxLength.toDouble())
    }
    
    /**
     * Calcula la distancia de Levenshtein entre dos strings
     */
    private fun calcularDistanciaLevenshtein(str1: String, str2: String): Int {
        val matrix = Array(str1.length + 1) { IntArray(str2.length + 1) }
        
        for (i in 0..str1.length) {
            matrix[i][0] = i
        }
        for (j in 0..str2.length) {
            matrix[0][j] = j
        }
        
        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // eliminación
                    matrix[i][j - 1] + 1,      // inserción
                    matrix[i - 1][j - 1] + cost // sustitución
                )
            }
        }
        
        return matrix[str1.length][str2.length]
    }
    
    /**
     * Busca coincidencias exactas en el historial
     */
    fun buscarCoincidenciaExacta(descripcion: String, historial: Map<String, Long>): Pair<Long, Double>? {
        val normalizada = normalizarDescripcion(descripcion)
        return historial[normalizada]?.let { categoriaId ->
            categoriaId to UMBRAL_COINCIDENCIA_EXACTA
        }
    }
    
    /**
     * Busca coincidencias parciales en el historial
     */
    fun buscarCoincidenciaParcial(descripcion: String, historial: Map<String, Long>): Pair<Long, Double>? {
        val normalizada = normalizarDescripcion(descripcion)
        
        // Buscar si la descripción contiene algún patrón del historial
        for ((patron, categoriaId) in historial) {
            if (normalizada.contains(patron) || patron.contains(normalizada)) {
                Log.d("ClasificacionNormalizer", "🎯 Coincidencia parcial: '$descripcion' contiene '$patron'")
                return categoriaId to UMBRAL_COINCIDENCIA_PARCIAL
            }
        }
        
        return null
    }
    
    /**
     * Busca coincidencias fuzzy en el historial
     */
    fun buscarCoincidenciaFuzzy(descripcion: String, historial: Map<String, Long>): Pair<Long, Double>? {
        val normalizada = normalizarDescripcion(descripcion)
        var mejorCoincidencia: Pair<Long, Double>? = null
        var mejorSimilitud = 0.0
        
        for ((patron, categoriaId) in historial) {
            val similitud = calcularSimilitud(normalizada, patron)
            if (similitud > mejorSimilitud && similitud >= UMBRAL_FUZZY_MEDIA) {
                mejorSimilitud = similitud
                mejorCoincidencia = categoriaId to similitud
                Log.d("ClasificacionNormalizer", "🔍 Coincidencia fuzzy: '$descripcion' ~ '$patron' (${(similitud * 100).toInt()}%)")
            }
        }
        
        return mejorCoincidencia
    }
    
    /**
     * Determina el tipo de coincidencia basado en la confianza
     */
    fun determinarTipoCoincidencia(confianza: Double): TipoCoincidencia {
        return when {
            confianza >= UMBRAL_COINCIDENCIA_EXACTA -> TipoCoincidencia.EXACTA
            confianza >= UMBRAL_COINCIDENCIA_PARCIAL -> TipoCoincidencia.PARCIAL
            confianza >= UMBRAL_FUZZY_ALTA -> TipoCoincidencia.FUZZY_ALTA
            confianza >= UMBRAL_FUZZY_MEDIA -> TipoCoincidencia.FUZZY_MEDIA
            else -> TipoCoincidencia.PATRON
        }
    }
    
    /**
     * Verifica si la confianza es suficiente para mostrar una sugerencia
     */
    fun esConfianzaSuficiente(confianza: Double): Boolean {
        return confianza >= UMBRAL_CONFIANZA_MINIMA
    }
    
    /**
     * Extrae palabras clave de una descripción
     */
    fun extraerPalabrasClave(descripcion: String): List<String> {
        val normalizada = normalizarDescripcion(descripcion)
        return normalizada.split(" ")
            .filter { it.length > 2 } // Filtrar palabras muy cortas
            .distinct()
    }
    
    /**
     * Genera variaciones de una descripción para mejorar la búsqueda
     */
    fun generarVariaciones(descripcion: String): List<String> {
        val normalizada = normalizarDescripcion(descripcion)
        val variaciones = mutableListOf(normalizada)
        
        // Agregar variaciones sin palabras específicas
        val palabras = normalizada.split(" ")
        if (palabras.size > 1) {
            // Variación sin la primera palabra
            variaciones.add(palabras.drop(1).joinToString(" "))
            // Variación sin la última palabra
            variaciones.add(palabras.dropLast(1).joinToString(" "))
        }
        
        return variaciones.distinct()
    }
}

enum class TipoCoincidencia {
    EXACTA,           // 100% confianza
    PARCIAL,          // 90% confianza
    FUZZY_ALTA,       // 80% confianza
    FUZZY_MEDIA,      // 60% confianza
    HISTORICO,        // Basado en historial
    PATRON            // Basado en patrones aprendidos
} 