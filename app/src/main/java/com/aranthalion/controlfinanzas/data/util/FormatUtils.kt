package com.aranthalion.controlfinanzas.data.util

import java.text.NumberFormat
import java.util.*

object FormatUtils {
    
    /**
     * Formatea un monto como moneda con separadores de miles y 2 decimales
     * @param amount El monto a formatear
     * @return String formateado (ej: "$1,500.75")
     */
    fun formatMoney(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
    }
    
    /**
     * Formatea un monto como moneda chilena (CLP) sin decimales
     * @param amount El monto a formatear
     * @return String formateado (ej: "$1.500")
     */
    fun formatMoneyCLP(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
        return formatter.format(amount)
    }
    
    /**
     * Formatea un monto como número con separadores de miles y 2 decimales
     * @param amount El monto a formatear
     * @return String formateado (ej: "1,500.75")
     */
    fun formatNumber(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 2
        return formatter.format(amount)
    }
    
    /**
     * Formatea un monto como número entero con separadores de miles
     * @param amount El monto a formatear
     * @return String formateado (ej: "1,500")
     */
    fun formatInteger(amount: Double): String {
        val formatter = NumberFormat.getIntegerInstance(Locale.US)
        return formatter.format(amount.toLong())
    }
    
    /**
     * Normaliza un valor numérico asegurando que sea un Double válido
     * @param value El valor a normalizar
     * @return Double normalizado
     */
    fun normalizeAmount(value: Any?): Double {
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is String -> {
                try {
                    value.replace(",", "").toDouble()
                } catch (e: NumberFormatException) {
                    0.0
                }
            }
            else -> 0.0
        }
    }
    
    /**
     * Redondea un monto a 2 decimales
     * @param amount El monto a redondear
     * @return Double redondeado
     */
    fun roundToTwoDecimals(amount: Double): Double {
        return kotlin.math.round(amount * 100) / 100
    }
    
    /**
     * Valida si un string representa un monto válido
     * @param amountString El string a validar
     * @return true si es válido, false en caso contrario
     */
    fun isValidAmount(amountString: String): Boolean {
        return try {
            amountString.replace(",", "").toDouble() >= 0
        } catch (e: NumberFormatException) {
            false
        }
    }
} 