package com.aranthalion.controlfinanzas.presentation.screens

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.remote.ai.PdfImportService
import com.aranthalion.controlfinanzas.data.util.ExcelProcessor
import com.aranthalion.controlfinanzas.data.util.ExcelTransaction
import com.aranthalion.controlfinanzas.data.util.ParDuplicadoSimilar
import com.aranthalion.controlfinanzas.domain.usecase.GestionarMovimientosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PdfImportViewModel @Inject constructor(
    private val pdfImportService: PdfImportService,
    private val gestionarMovimientosUseCase: GestionarMovimientosUseCase,
    private val configuracionPreferences: ConfiguracionPreferences
) : ViewModel() {

    private val _importState = MutableStateFlow<PdfImportUiState>(PdfImportUiState.Idle)
    val importState: StateFlow<PdfImportUiState> = _importState.asStateFlow()

    val customPeriodConfigs: Map<String, com.aranthalion.controlfinanzas.data.util.BillingPeriodConfig>
        get() = configuracionPreferences.obtenerPeriodoDatesMap()

    fun resetState() {
        _importState.value = PdfImportUiState.Idle
    }

    fun importPdfFile(context: Context, fileUri: Uri, passwordStr: String, defaultPeriodo: String) {
        viewModelScope.launch {
            _importState.value = PdfImportUiState.Loading

            try {
                // 1. Read bytes and convert to Base64
                val base64Pdf = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                        ?: throw Exception("No se pudo leer el archivo seleccionado")
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                }

                // 2. Call Service
                val result = pdfImportService.importPdf(base64Pdf, passwordStr)

                result.onSuccess { importResult ->
                    if (!importResult.success) {
                        _importState.value = PdfImportUiState.Error("El servidor no pudo procesar el PDF correctamente")
                        return@launch
                    }

                    val billingPeriod = defaultPeriodo
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                    // Convert to ExcelTransaction list to use similar duplicate detection logic
                    val excelTransactions = importResult.transactions.map { tx ->
                        val parsedDate = try {
                            dateFormat.parse(tx.date)
                        } catch (e: Exception) {
                            null
                        }
                        
                        ExcelTransaction(
                            fecha = parsedDate,
                            codigoReferencia = null,
                            ciudad = null,
                            descripcion = tx.description,
                            tipoTarjeta = tx.cardType ?: "LIDER",
                            monto = tx.amount,
                            periodoFacturacion = billingPeriod,
                            categoria = tx.suggestedCategoryName
                        )
                    }

                    // Convert to MovimientoEntity list for final insertion
                    val movimientos = excelTransactions.mapNotNull { t ->
                        if (t.fecha == null) return@mapNotNull null
                        val tipo = if (t.monto < 0) "GASTO" else "INGRESO"
                        val montoAbs = Math.abs(t.monto)
                        
                        val periodoCalculado = defaultPeriodo

                        MovimientoEntity(
                            tipo = tipo,
                            monto = montoAbs, // El sistema almacena montos positivos para gastos/ingresos
                            descripcion = t.descripcion,
                            fecha = t.fecha,
                            periodoFacturacion = periodoCalculado,
                            categoriaId = null,
                            tipoTarjeta = t.tipoTarjeta,
                            idUnico = ExcelProcessor.generarIdUnico(t.fecha, montoAbs, t.descripcion)
                        )
                    }

                    // Detect duplicates
                    val existentes = withContext(Dispatchers.IO) {
                        gestionarMovimientosUseCase.obtenerMovimientosPorPeriodoOptimizado(billingPeriod)
                    }
                    val idsExistentes = existentes.map { it.idUnico }.toSet()

                    val duplicadosSimilaresDetectados = ExcelProcessor.detectarDuplicadosSimilares(
                        excelTransactions, existentes
                    )

                    if (duplicadosSimilaresDetectados.isNotEmpty()) {
                        _importState.value = PdfImportUiState.PendingDuplicateResolution(
                            movimientos = movimientos,
                            duplicados = duplicadosSimilaresDetectados,
                            billingPeriod = billingPeriod
                        )
                    } else {
                        // Directly import non-duplicates
                        importMovimientosDirectamente(movimientos, idsExistentes, billingPeriod)
                    }

                }.onFailure { error ->
                    _importState.value = PdfImportUiState.Error(error.message ?: "Error al importar PDF")
                }

            } catch (e: Exception) {
                _importState.value = PdfImportUiState.Error(e.message ?: "Error inesperado")
            }
        }
    }

    private fun importMovimientosDirectamente(
        movimientos: List<MovimientoEntity>,
        existentes: Set<String>,
        periodo: String
    ) {
        viewModelScope.launch {
            try {
                val categoriasPrevias = withContext(Dispatchers.IO) {
                    gestionarMovimientosUseCase.obtenerCategoriasPorIdUnico(periodo)
                }

                val nuevos = movimientos.filter { it.idUnico !in existentes }
                val duplicadosCount = movimientos.size - nuevos.size

                withContext(Dispatchers.IO) {
                    nuevos.forEach { mov ->
                        val categoriaAnterior = categoriasPrevias[mov.idUnico]
                        val movConCategoria = if (categoriaAnterior != null) {
                            mov.copy(categoriaId = categoriaAnterior)
                        } else {
                            mov
                        }
                        gestionarMovimientosUseCase.agregarMovimiento(movConCategoria)
                    }
                }

                val totalMonto = nuevos.sumOf { it.monto }
                _importState.value = PdfImportUiState.Success(
                    totalProcesadas = movimientos.size,
                    nuevas = nuevos.size,
                    duplicadas = duplicadosCount,
                    montoTotal = totalMonto.toLong(),
                    periodo = periodo
                )
            } catch (e: Exception) {
                _importState.value = PdfImportUiState.Error(e.message ?: "Error al guardar movimientos")
            }
        }
    }

    fun resolveDuplicatesAndImport(
        movimientos: List<MovimientoEntity>,
        duplicadosOmitidos: List<ParDuplicadoSimilar>,
        duplicadosFusionados: List<ParDuplicadoSimilar>,
        duplicadosSobrescritos: List<ParDuplicadoSimilar>,
        duplicadosAmbos: List<ParDuplicadoSimilar>,
        billingPeriod: String
    ) {
        viewModelScope.launch {
            _importState.value = PdfImportUiState.Loading
            try {
                val existentes = withContext(Dispatchers.IO) {
                    gestionarMovimientosUseCase.obtenerIdUnicosPorPeriodo(billingPeriod)
                }
                val categoriasPrevias = withContext(Dispatchers.IO) {
                    gestionarMovimientosUseCase.obtenerCategoriasPorIdUnico(billingPeriod)
                }

                // Process merged and overwritten ones in background
                withContext(Dispatchers.IO) {
                    duplicadosFusionados.forEach { p ->
                        val merged = com.aranthalion.controlfinanzas.data.util.DuplicateTransactionDetector.fusionarMovimientoConExcel(
                            p.existente, p.nueva
                        )
                        gestionarMovimientosUseCase.actualizarMovimiento(merged)
                    }

                    duplicadosSobrescritos.forEach { p ->
                        val updated = p.existente.copy(
                            monto = p.nueva.monto,
                            descripcion = p.nueva.descripcion,
                            fecha = p.nueva.fecha ?: p.existente.fecha,
                            tipoTarjeta = p.nueva.tipoTarjeta?.ifBlank { null } ?: p.existente.tipoTarjeta,
                            fechaActualizacion = System.currentTimeMillis(),
                            metodoActualizacion = "OVERWRITE"
                        )
                        gestionarMovimientosUseCase.actualizarMovimiento(updated)
                    }
                }

                // Gather list of IDs that are processed or skipped
                val idsAOmitir = (duplicadosOmitidos + duplicadosFusionados + duplicadosSobrescritos).map {
                    ExcelProcessor.generarIdUnico(it.nueva.fecha, it.nueva.monto, it.nueva.descripcion)
                }.toSet()

                val idsAmbos = duplicadosAmbos.map {
                    ExcelProcessor.generarIdUnico(it.nueva.fecha, it.nueva.monto, it.nueva.descripcion)
                }.toSet()

                val movimientosFiltrados = movimientos.map { mov ->
                    if (mov.idUnico in idsAmbos) {
                        mov.copy(idUnico = "${mov.idUnico}-f")
                    } else {
                        mov
                    }
                }.filter { it.idUnico !in idsAOmitir }

                val nuevos = movimientosFiltrados.filter { it.idUnico !in existentes }
                val duplicadosCount = movimientos.size - nuevos.size

                withContext(Dispatchers.IO) {
                    nuevos.forEach { mov ->
                        val categoriaAnterior = categoriasPrevias[mov.idUnico]
                        val movConCategoria = if (categoriaAnterior != null) {
                            mov.copy(categoriaId = categoriaAnterior)
                        } else {
                            mov
                        }
                        gestionarMovimientosUseCase.agregarMovimiento(movConCategoria)
                    }
                }

                val totalMonto = nuevos.sumOf { it.monto }
                _importState.value = PdfImportUiState.Success(
                    totalProcesadas = movimientos.size,
                    nuevas = nuevos.size,
                    duplicadas = duplicadosCount,
                    montoTotal = totalMonto.toLong(),
                    periodo = billingPeriod
                )

            } catch (e: Exception) {
                _importState.value = PdfImportUiState.Error(e.message ?: "Error al finalizar importación")
            }
        }
    }
}

sealed class PdfImportUiState {
    object Idle : PdfImportUiState()
    object Loading : PdfImportUiState()
    data class Success(
        val totalProcesadas: Int,
        val nuevas: Int,
        val duplicadas: Int,
        val montoTotal: Long,
        val periodo: String
    ) : PdfImportUiState()
    data class PendingDuplicateResolution(
        val movimientos: List<MovimientoEntity>,
        val duplicados: List<ParDuplicadoSimilar>,
        val billingPeriod: String
    ) : PdfImportUiState()
    data class Error(val message: String) : PdfImportUiState()
}
