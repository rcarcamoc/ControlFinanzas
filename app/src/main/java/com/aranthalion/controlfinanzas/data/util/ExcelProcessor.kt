package com.aranthalion.controlfinanzas.data.util

import android.content.Context
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import java.io.FileInputStream
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import java.security.MessageDigest

// Modelo simple para transacción importada
data class ExcelTransaction(
    val fecha: Date?,
    val codigoReferencia: String?,
    val ciudad: String?,
    val descripcion: String,
    val tipoTarjeta: String?,
    val monto: Double,
    val periodoFacturacion: String?,
    val categoria: String? // Siempre null al importar
)

object ExcelProcessor {
    /**
     * Procesa un archivo Excel y extrae una lista de transacciones.
     * Soporta .xls y .xlsx. Lanza excepción si el archivo es inválido.
     */
    fun procesarArchivo(inputStream: InputStream): List<ExcelTransaction> {
        val workbook = WorkbookFactory.create(inputStream)
        val transacciones = mutableListOf<ExcelTransaction>()
        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            // Asumimos que la primera fila es encabezado
            val header = sheet.getRow(0)
            if (header == null) continue
            val colMap = mutableMapOf<String, Int>()
            for (cellIndex in 0 until header.physicalNumberOfCells) {
                val cellValue = header.getCell(cellIndex)?.toString()?.trim()?.lowercase() ?: ""
                colMap[cellValue] = cellIndex
            }
            // Buscamos columnas relevantes
            val idxFecha = colMap.entries.find { it.key.contains("fecha") }?.value
            val idxDesc = colMap.entries.find { it.key.contains("descripcion") || it.key.contains("detalle") }?.value
            val idxMonto = colMap.entries.find { it.key.contains("monto") || it.key.contains("importe") }?.value
            val idxTarjeta = colMap.entries.find { it.key.contains("tarjeta") }?.value
            val idxRef = colMap.entries.find { it.key.contains("referencia") || it.key.contains("codigo") }?.value
            val idxPeriodo = colMap.entries.find { it.key.contains("periodo") || it.key.contains("mes") }?.value
            // Procesamos filas
            for (rowIndex in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                val fecha = idxFecha?.let { parseFecha(row.getCell(it)?.toString()) }
                val descripcion = idxDesc?.let { row.getCell(it)?.toString() } ?: ""
                val monto = idxMonto?.let { row.getCell(it)?.toString()?.replace(",", ".")?.toDoubleOrNull() } ?: 0.0
                val tipoTarjeta = idxTarjeta?.let { row.getCell(it)?.toString() }
                val codigoReferencia = idxRef?.let { row.getCell(it)?.toString() }
                val periodoFacturacion = idxPeriodo?.let { row.getCell(it)?.toString() }
                if (descripcion.isNotBlank() && monto != 0.0) {
                    transacciones.add(
                        ExcelTransaction(
                            fecha,
                            codigoReferencia,
                            null,
                            descripcion,
                            tipoTarjeta,
                            monto,
                            periodoFacturacion,
                            null
                        )
                    )
                }
            }
        }
        workbook.close()
        return transacciones
    }

    /**
     * Importa transacciones desde un archivo de 'estado de cierre'.
     * @param periodoFacturacion El periodo seleccionado en la UI
     */
    fun importarEstadoDeCierre(inputStream: InputStream, periodoFacturacion: String?): List<ExcelTransaction> {
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0)
        val transacciones = mutableListOf<ExcelTransaction>()
        // Encabezados en la fila 12 (índice 11), datos desde la fila 13 (índice 12)
        for (rowIndex in 12..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            val fecha = getCellDateFlexible(row, 0)
            val codigoReferencia = getCellString(row, 1)
            val ciudad = getCellString(row, 2)
            val descripcion = getCellString(row, 3) ?: ""
            val tipoTarjeta = getCellString(row, 4)
            val monto = getCellDoubleFlexible(row, 5)
            if (descripcion.isBlank() || monto == 0.0) continue
            transacciones.add(
                ExcelTransaction(
                    fecha = fecha,
                    codigoReferencia = codigoReferencia,
                    ciudad = ciudad,
                    descripcion = descripcion,
                    tipoTarjeta = tipoTarjeta,
                    monto = monto,
                    periodoFacturacion = periodoFacturacion,
                    categoria = null
                )
            )
        }
        workbook.close()
        return transacciones
    }

    /**
     * Importa transacciones desde un archivo de 'últimos movimientos'.
     * @param periodoFacturacion El periodo seleccionado en la UI
     */
    fun importarUltimosMovimientos(inputStream: InputStream, periodoFacturacion: String?): List<ExcelTransaction> {
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0)
        val transacciones = mutableListOf<ExcelTransaction>()
        // Encabezados en la fila 13 (índice 12), datos desde la fila 14 (índice 13)
        for (rowIndex in 13..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            val fecha = getCellDateFlexible(row, 0)
            val codigoReferencia = getCellString(row, 1)
            val ciudad = getCellString(row, 2)
            val descripcion = getCellString(row, 3) ?: ""
            val tipoTarjeta = getCellString(row, 4)
            val monto = getCellDoubleFlexible(row, 5)
            if (descripcion.isBlank() || monto == 0.0) continue
            transacciones.add(
                ExcelTransaction(
                    fecha = fecha,
                    codigoReferencia = codigoReferencia,
                    ciudad = ciudad,
                    descripcion = descripcion,
                    tipoTarjeta = tipoTarjeta,
                    monto = monto,
                    periodoFacturacion = periodoFacturacion,
                    categoria = null
                )
            )
        }
        workbook.close()
        return transacciones
    }

    private fun getCellString(row: Row, index: Int): String? {
        val cell = row.getCell(index)
        return when (cell?.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> cell.numericCellValue.toLong().toString()
            else -> null
        }
    }

    private fun getCellDouble(row: Row, index: Int): Double {
        val cell = row.getCell(index)
        val value = when (cell?.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.replace(",", ".").toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        return value.toInt().toDouble() // Trunca a entero
    }

    private fun getCellDate(row: Row, index: Int): Date? {
        val cell = row.getCell(index)
        return when (cell?.cellType) {
            CellType.NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue
                } else {
                    // Intentar convertir cualquier serial numérico razonable
                    val serial = cell.numericCellValue
                    if (serial > 10000 && serial < 100000) {
                        org.apache.poi.ss.usermodel.DateUtil.getJavaDate(serial)
                    } else {
                        null
                    }
                }
            }
            CellType.STRING -> parseFecha(cell.stringCellValue)
            else -> null
        }
    }

    private fun parseFecha(valor: String?): Date? {
        if (valor == null) return null
        val formatos = listOf("dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy")
        for (formato in formatos) {
            try {
                return SimpleDateFormat(formato, Locale.getDefault()).parse(valor)
            } catch (_: Exception) {}
        }
        return null
    }

    // Mejorar el parseo de fecha: acepta formato dd-MM-yyyy, dd/MM/yyyy, serial Excel
    private fun getCellDateFlexible(row: Row, index: Int): Date? {
        val cell = row.getCell(index)
        return when (cell?.cellType) {
            CellType.NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue
                } else {
                    val serial = cell.numericCellValue
                    if (serial > 10000 && serial < 100000) {
                        org.apache.poi.ss.usermodel.DateUtil.getJavaDate(serial)
                    } else {
                        null
                    }
                }
            }
            CellType.STRING -> parseFechaFlexible(cell.stringCellValue)
            else -> null
        }
    }

    // Acepta fechas en formato dd-MM-yyyy, dd/MM/yyyy, yyyy-MM-dd
    private fun parseFechaFlexible(valor: String?): Date? {
        if (valor == null) return null
        val formatos = listOf("dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy")
        for (formato in formatos) {
            try {
                return SimpleDateFormat(formato, Locale.getDefault()).parse(valor)
            } catch (_: Exception) {}
        }
        return null
    }

    // Mejorar el parseo de monto: elimina separadores de miles y acepta punto o coma decimal
    private fun getCellDoubleFlexible(row: Row, index: Int): Double {
        val cell = row.getCell(index)
        return when (cell?.cellType) {
            CellType.NUMERIC -> {
                // Para valores numéricos, usar directamente el valor
                val value = cell.numericCellValue
                Log.d("ExcelParse", "Valor numérico original: $value")
                // Si el valor es muy pequeño (menos de 1), puede ser un porcentaje o error
                if (value > 0 && value < 1) {
                    Log.d("ExcelParse", "Valor pequeño detectado, multiplicando por 1000: ${value * 1000}")
                    FormatUtils.roundToTwoDecimals(value * 1000) // Convertir a valor completo y redondear
                } else {
                    Log.d("ExcelParse", "Valor numérico final: $value")
                    FormatUtils.roundToTwoDecimals(value)
                }
            }
            CellType.STRING -> {
                val raw = cell.stringCellValue.trim()
                Log.d("ExcelParse", "Valor string original: '$raw'")
                val sinEspacios = raw.replace("\u00A0", "").replace("'", "")
                Log.d("ExcelParse", "Valor sin espacios: '$sinEspacios'")
                
                // Primero intentar parsear directamente
                val directParse = sinEspacios.toDoubleOrNull()
                Log.d("ExcelParse", "Parseo directo: $directParse")
                if (directParse != null) return FormatUtils.roundToTwoDecimals(directParse)
                
                // Si falla, intentar con separadores
                val normalized = sinEspacios
                    .replace(".", "") // Eliminar puntos (separadores de miles)
                    .replace(",", ".") // Reemplazar coma decimal por punto
                Log.d("ExcelParse", "Valor normalizado: '$normalized'")
                
                val finalValue = normalized.toDoubleOrNull() ?: 0.0
                Log.d("ExcelParse", "Valor final string: $finalValue")
                FormatUtils.roundToTwoDecimals(finalValue)
            }
            else -> {
                Log.d("ExcelParse", "Tipo de celda no soportado: ${cell?.cellType}")
                0.0
            }
        }
    }

    /**
     * Prueba local: lee ambos archivos de ejemplo y muestra en el log los resultados.
     */
    fun pruebaProcesamientoArchivosExcel(context: Context) {
        try {
            val pathBase = "/home/rick/AndroidStudioProjects/ControlFinanzas/Archivos/excel/"
            val archivoCierre = FileInputStream(pathBase + "estado de cierre.xls")
            val archivoUltimos = FileInputStream(pathBase + "ultimosMovimientos.xls")
            val cierre = importarEstadoDeCierre(archivoCierre, null)
            val ultimos = importarUltimosMovimientos(archivoUltimos, null)
            Log.i("ExcelTest", "Estado de cierre: ${cierre.size} transacciones")
            cierre.take(5).forEachIndexed { i, t ->
                Log.i("ExcelTest", "Cierre #$i: ${t.fecha} | ${t.descripcion} | ${t.monto}")
            }
            Log.i("ExcelTest", "Ultimos movimientos: ${ultimos.size} transacciones")
            ultimos.take(5).forEachIndexed { i, t ->
                Log.i("ExcelTest", "Ultimo #$i: ${t.fecha} | ${t.descripcion} | ${t.monto}")
            }
        } catch (e: Exception) {
            Log.e("ExcelTest", "Error al procesar archivos Excel: ${e.message}")
        }
    }

    fun generarIdUnico(fecha: Date?, monto: Double, descripcion: String): String {
        val input = "${fecha?.time ?: 0}-$monto-$descripcion"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
} 