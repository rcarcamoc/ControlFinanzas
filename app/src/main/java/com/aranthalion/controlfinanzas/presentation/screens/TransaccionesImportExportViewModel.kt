package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.TransaccionesImportExportService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.io.File

@HiltViewModel
class TransaccionesImportExportViewModel @Inject constructor(
    private val movimientoRepository: MovimientoRepository
) : ViewModel() {
    suspend fun exportarPorPeriodo(context: android.content.Context, periodo: String, outputFile: File) =
        TransaccionesImportExportService(context, movimientoRepository).exportarPorPeriodo(periodo, outputFile)

    suspend fun importarPorPeriodo(context: android.content.Context, periodo: String, inputFile: File) =
        TransaccionesImportExportService(context, movimientoRepository).importarPorPeriodo(periodo, inputFile)
} 