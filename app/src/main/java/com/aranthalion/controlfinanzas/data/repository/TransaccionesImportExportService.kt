package com.aranthalion.controlfinanzas.data.repository

import android.content.Context
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.util.ExcelProcessor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransaccionesImportExportService(private val context: Context, private val movimientoRepository: MovimientoRepository) {
    private val gson = Gson()

    private fun obtenerFechasDePeriodo(periodo: String): Pair<Date, Date> {
        val formato = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val fechaInicio = formato.parse(periodo) ?: Date()
        val cal = Calendar.getInstance().apply { time = fechaInicio }
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DATE, -1)
        val fechaFin = cal.time
        return fechaInicio to fechaFin
    }

    /**
     * Exporta todas las transacciones de un periodo a un archivo JSON.
     * @param periodo El periodo a exportar (ej: "2024-07")
     * @param outputFile Archivo destino
     * @throws Exception si ocurre un error de IO
     */
    suspend fun exportarPorPeriodo(periodo: String, outputFile: File) {
        val (fechaInicio, fechaFin) = obtenerFechasDePeriodo(periodo)
        val movimientos = movimientoRepository.obtenerMovimientosPorPeriodo(fechaInicio, fechaFin)
        FileWriter(outputFile).use { writer ->
            gson.toJson(movimientos, writer)
        }
    }

    /**
     * Importa transacciones desde un archivo JSON, validando duplicados por idUnico.
     * @param periodo El periodo al que se asignarán las transacciones importadas
     * @param inputFile Archivo fuente
     * @throws Exception si ocurre un error de IO o formato
     */
    suspend fun importarPorPeriodo(periodo: String, inputFile: File) {
        val (fechaInicio, fechaFin) = obtenerFechasDePeriodo(periodo)
        val movimientosExistentes = movimientoRepository.obtenerMovimientosPorPeriodo(fechaInicio, fechaFin)
        val existentesIdUnico = movimientosExistentes.map { it.idUnico }.toSet()
        val type = object : TypeToken<List<MovimientoEntity>>() {}.type
        val nuevos: List<MovimientoEntity> = FileReader(inputFile).use { reader ->
            gson.fromJson(reader, type)
        }
        val nuevosFiltrados = nuevos.filter { it.idUnico !in existentesIdUnico }
        nuevosFiltrados.forEach { mov ->
            // Asignar el periodo importado
            val movConPeriodo = mov.copy(periodoFacturacion = periodo)
            movimientoRepository.agregarMovimiento(movConPeriodo)
        }
    }
} 