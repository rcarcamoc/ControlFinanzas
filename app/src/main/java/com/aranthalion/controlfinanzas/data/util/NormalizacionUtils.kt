package com.aranthalion.controlfinanzas.data.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidades para normalización de datos de movimientos (HITO 1.1)
 * Optimiza las consultas de clasificación automática
 */
object NormalizacionUtils {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val calendar = Calendar.getInstance()
    
    /**
     * Normaliza una descripción eliminando acentos, caracteres especiales y convirtiendo a minúsculas
     */
    fun normalizarDescripcion(descripcion: String): String {
        return descripcion
            .lowercase(Locale.getDefault())
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
            .replace("ü", "u")
            .replace(Regex("[^a-z0-9\\s]"), "") // Solo letras, números y espacios
            .replace(Regex("\\s+"), " ") // Múltiples espacios a uno solo
            .trim()
    }
    
    /**
     * Categoriza el monto en rangos para optimizar consultas
     */
    fun categorizarMonto(monto: Double): String {
        return when {
            monto <= 100 -> "0-100"
            monto <= 500 -> "100-500"
            monto <= 1000 -> "500-1000"
            monto <= 5000 -> "1000-5000"
            monto <= 10000 -> "5000-10000"
            else -> "10000+"
        }
    }
    
    /**
     * Extrae el mes de una fecha en formato "MM"
     */
    fun extraerMes(fecha: Date): String {
        calendar.time = fecha
        return String.format("%02d", calendar.get(Calendar.MONTH) + 1)
    }
    
    /**
     * Extrae el día de la semana en español
     */
    fun extraerDiaSemana(fecha: Date): String {
        calendar.time = fecha
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "DOMINGO"
            Calendar.MONDAY -> "LUNES"
            Calendar.TUESDAY -> "MARTES"
            Calendar.WEDNESDAY -> "MIERCOLES"
            Calendar.THURSDAY -> "JUEVES"
            Calendar.FRIDAY -> "VIERNES"
            Calendar.SATURDAY -> "SABADO"
            else -> "DESCONOCIDO"
        }
    }
    
    /**
     * Extrae el día del mes (1-31)
     */
    fun extraerDia(fecha: Date): Int {
        calendar.time = fecha
        return calendar.get(Calendar.DAY_OF_MONTH)
    }
    
    /**
     * Extrae el año de la fecha
     */
    fun extraerAnio(fecha: Date): Int {
        calendar.time = fecha
        return calendar.get(Calendar.YEAR)
    }
    
    /**
     * Normaliza completamente un movimiento con todos los campos optimizados
     */
    fun normalizarMovimiento(
        descripcion: String,
        monto: Double,
        fecha: Date
    ): Map<String, Any> {
        return mapOf(
            "descripcionNormalizada" to normalizarDescripcion(descripcion),
            "montoCategoria" to categorizarMonto(monto),
            "fechaMes" to extraerMes(fecha),
            "fechaDiaSemana" to extraerDiaSemana(fecha),
            "fechaDia" to extraerDia(fecha),
            "fechaAnio" to extraerAnio(fecha)
        )
    }
    
    /**
     * Calcula similitud entre dos descripciones normalizadas (más rápido que Levenshtein)
     */
    fun calcularSimilitudRapida(desc1: String, desc2: String): Double {
        if (desc1 == desc2) return 1.0
        if (desc1.isEmpty() || desc2.isEmpty()) return 0.0
        
        val palabras1 = desc1.split(" ").toSet()
        val palabras2 = desc2.split(" ").toSet()
        
        val interseccion = palabras1.intersect(palabras2).size
        val union = palabras1.union(palabras2).size
        
        return if (union > 0) interseccion.toDouble() / union else 0.0
    }
} 