package com.aranthalion.controlfinanzas.data.util

import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import java.util.*

data class ParDuplicadoSimilar(
    val nueva: ExcelTransaction,
    val existente: MovimientoEntity,
    val similitud: Double
)

object DuplicateTransactionDetector {

    /**
     * Detecta duplicados similares basados en fecha, monto y similitud de descripción
     * @param transaccionesNuevas Lista de transacciones a importar
     * @param transaccionesExistentes Lista de transacciones ya existentes
     * @return Lista de pares de transacciones que podrían ser duplicados
     */
    fun detectarDuplicadosSimilares(
        transaccionesNuevas: List<ExcelTransaction>,
        transaccionesExistentes: List<MovimientoEntity>
    ): List<ParDuplicadoSimilar> {
        val duplicadosSimilares = mutableListOf<ParDuplicadoSimilar>()
        
        for (nueva in transaccionesNuevas) {
            for (existente in transaccionesExistentes) {
                // Verificar si son candidatos a duplicados (misma fecha y monto)
                if (sonCandidatosDuplicados(nueva, existente)) {
                    val similitud = calcularSimilitudDescripcion(nueva.descripcion, existente.descripcion)
                    if (similitud >= 0.7) { // Umbral de similitud del 70%
                        duplicadosSimilares.add(
                            ParDuplicadoSimilar(
                                nueva = nueva,
                                existente = existente,
                                similitud = similitud
                            )
                        )
                    }
                }
            }
        }
        
        return duplicadosSimilares
    }

    /**
     * Verifica si dos transacciones son candidatos a duplicados (misma fecha y monto)
     */
    private fun sonCandidatosDuplicados(nueva: ExcelTransaction, existente: MovimientoEntity): Boolean {
        // Verificar que tengan la misma fecha (con tolerancia de 1 día)
        val fechaNueva = nueva.fecha ?: return false
        val fechaExistente = existente.fecha
        val diferenciaDias = Math.abs(fechaNueva.time - fechaExistente.time) / (1000 * 60 * 60 * 24)
        if (diferenciaDias > 1) return false
        
        // Verificar que tengan el mismo monto (con tolerancia de 1 peso)
        val diferenciaMonto = Math.abs(nueva.monto - existente.monto)
        if (diferenciaMonto > 1.0) return false
        
        return true
    }

    /**
     * Calcula la similitud entre dos descripciones usando el algoritmo de Levenshtein
     */
    private fun calcularSimilitudDescripcion(desc1: String, desc2: String): Double {
        val desc1Normalizada = normalizarDescripcion(desc1)
        val desc2Normalizada = normalizarDescripcion(desc2)
        
        val distancia = calcularDistanciaLevenshtein(desc1Normalizada, desc2Normalizada)
        val longitudMaxima = maxOf(desc1Normalizada.length, desc2Normalizada.length)
        
        return if (longitudMaxima == 0) 1.0 else (longitudMaxima - distancia) / longitudMaxima.toDouble()
    }

    /**
     * Normaliza una descripción para comparación (elimina espacios extra, convierte a minúsculas, etc.)
     */
    private fun normalizarDescripcion(descripcion: String): String {
        return descripcion
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ") // Reemplaza múltiples espacios con uno solo
            .replace(Regex("[^a-z0-9\\s]"), "") // Elimina caracteres especiales excepto espacios
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
                    matrix[i - 1][j] + 1, // eliminación
                    matrix[i][j - 1] + 1, // inserción
                    matrix[i - 1][j - 1] + cost // sustitución
                )
            }
        }
        
        return matrix[str1.length][str2.length]
    }
}
