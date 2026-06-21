package com.aranthalion.controlfinanzas.data.util

import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import java.util.*

data class ParDuplicadoSimilar(
    val nueva: ExcelTransaction,
    val existente: MovimientoEntity,
    val similitud: Double
)

data class ParDuplicadoMovimientos(
    val nueva: MovimientoEntity,
    val existente: MovimientoEntity,
    val similitud: Double
)

object DuplicateTransactionDetector {

    /**
     * Detecta duplicados basados en fecha y monto.
     * La descripción puede variar entre fuentes distintas (email, Excel, manual),
     * por lo que NO se usa como filtro, solo como dato informativo.
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
                // Condición principal: mismo monto y misma fecha → posible duplicado
                // La descripción se calcula solo como referencia informativa para el usuario
                if (sonCandidatosDuplicados(nueva, existente)) {
                    val similitudDescripcion = calcularSimilitudDescripcion(nueva.descripcion, existente.descripcion)
                    duplicadosSimilares.add(
                        ParDuplicadoSimilar(
                            nueva = nueva,
                            existente = existente,
                            similitud = similitudDescripcion // informativo, no es filtro
                        )
                    )
                }
            }
        }
        
        return duplicadosSimilares
    }

    /**
     * Verifica si dos transacciones son candidatos a duplicados (misma fecha y monto)
     */
    private fun sonCandidatosDuplicados(nueva: ExcelTransaction, existente: MovimientoEntity): Boolean {
        // Verificar que sea exactamente el mismo día calendario (año/mes/día)
        // No se usa tolerancia de días para evitar falsos positivos entre cobros recurrentes
        val fechaNueva = nueva.fecha ?: return false
        val calNueva = Calendar.getInstance().apply { time = fechaNueva }
        val calExistente = Calendar.getInstance().apply { time = existente.fecha }
        val mismodia = calNueva.get(Calendar.YEAR) == calExistente.get(Calendar.YEAR) &&
                       calNueva.get(Calendar.MONTH) == calExistente.get(Calendar.MONTH) &&
                       calNueva.get(Calendar.DAY_OF_MONTH) == calExistente.get(Calendar.DAY_OF_MONTH)
        if (!mismodia) return false

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

    /**
     * Fusiona dos movimientos, combinando su información
     */
    fun fusionarMovimientoConExcel(existente: MovimientoEntity, nueva: ExcelTransaction): MovimientoEntity {
        return existente.copy(
            categoriaId = existente.categoriaId ?: nueva.categoriaId,
            tipoTarjeta = existente.tipoTarjeta?.ifBlank { null } ?: nueva.tipoTarjeta?.ifBlank { null },
            descripcion = if (existente.descripcion.length >= nueva.descripcion.length) existente.descripcion else nueva.descripcion,
            fechaActualizacion = System.currentTimeMillis(),
            metodoActualizacion = "MERGE",
            daoResponsable = "DuplicateTransactionDetector"
        )
    }

    /**
     * Detecta duplicados entre movimientos ya existentes en la base de datos.
     * Criterio principal: mismo monto y misma fecha (tolerancia de 1 día).
     * La descripción puede diferir entre fuentes (email, Excel, manual) y no actúa como filtro.
     */
    fun detectarDuplicadosInternos(
        movimientos: List<MovimientoEntity>
    ): List<ParDuplicadoMovimientos> {
        val duplicados = mutableListOf<ParDuplicadoMovimientos>()
        val visitados = mutableSetOf<Long>()
        
        for (i in movimientos.indices) {
            val m1 = movimientos[i]
            if (m1.id in visitados) continue
            
            for (j in i + 1 until movimientos.size) {
                val m2 = movimientos[j]
                if (m2.id in visitados) continue
                
                // Condición principal: mismo monto y misma fecha → posible duplicado
                // La similitud de descripción se adjunta como dato informativo
                if (sonCandidatosDuplicadosInternos(m1, m2)) {
                    val similitudDescripcion = calcularSimilitudDescripcion(m1.descripcion, m2.descripcion)
                    duplicados.add(
                        ParDuplicadoMovimientos(
                            nueva = m2, // tratamos una como "nueva"
                            existente = m1,
                            similitud = similitudDescripcion // informativo, no es filtro
                        )
                    )
                    // Evitar procesar el mismo registro más de una vez como duplicado secundario
                    visitados.add(m2.id)
                }
            }
        }
        return duplicados
    }

    private fun sonCandidatosDuplicadosInternos(m1: MovimientoEntity, m2: MovimientoEntity): Boolean {
        // Verificar que sea exactamente el mismo día calendario (año/mes/día)
        val cal1 = Calendar.getInstance().apply { time = m1.fecha }
        val cal2 = Calendar.getInstance().apply { time = m2.fecha }
        val mismodia = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                       cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                       cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
        if (!mismodia) return false

        val diferenciaMonto = Math.abs(m1.monto - m2.monto)
        if (diferenciaMonto > 1.0) return false

        return true
    }

    /**
     * Fusiona dos movimientos existentes
     */
    fun fusionarMovimientosInternos(existente: MovimientoEntity, nueva: MovimientoEntity): MovimientoEntity {
        return existente.copy(
            categoriaId = existente.categoriaId ?: nueva.categoriaId,
            tipoTarjeta = existente.tipoTarjeta?.ifBlank { null } ?: nueva.tipoTarjeta?.ifBlank { null },
            descripcion = if (existente.descripcion.length >= nueva.descripcion.length) existente.descripcion else nueva.descripcion,
            fechaActualizacion = System.currentTimeMillis(),
            metodoActualizacion = "MERGE_DB",
            daoResponsable = "DuplicateTransactionDetector"
        )
    }
}

