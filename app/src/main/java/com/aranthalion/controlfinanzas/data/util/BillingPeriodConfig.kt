package com.aranthalion.controlfinanzas.data.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BillingPeriodConfig(
    val startDateStr: String, // "yyyy-MM-dd"
    val endDateStr: String    // "yyyy-MM-dd"
) {
    fun contains(date: Date): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val start = sdf.parse(startDateStr) ?: return false
            val end = sdf.parse(endDateStr) ?: return false
            // Ajustar fechas para comparar solo año-mes-día a las 00:00:00 vs 23:59:59
            val cal = Calendar.getInstance()
            
            cal.time = start
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startClean = cal.time

            cal.time = end
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endClean = cal.time

            !date.before(startClean) && !date.after(endClean)
        } catch (e: Exception) {
            false
        }
    }
}

object BillingPeriodHelper {
    fun obtenerPeriodoParaFecha(date: Date, configs: Map<String, BillingPeriodConfig>): String {
        for ((periodo, config) in configs) {
            if (config.contains(date)) {
                return periodo
            }
        }
        val calendar = Calendar.getInstance()
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }
}
