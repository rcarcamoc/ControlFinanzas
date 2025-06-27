package com.aranthalion.controlfinanzas.presentation.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import com.aranthalion.controlfinanzas.data.util.ExcelProcessor
import com.aranthalion.controlfinanzas.data.util.ExcelTransaction
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import kotlinx.coroutines.withContext
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import androidx.compose.ui.window.DialogProperties
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import javax.inject.Inject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.runtime.DisposableEffect
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ClasificacionUseCaseEntryPoint {
    fun clasificacionUseCase(): GestionarClasificacionAutomaticaUseCase
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportarExcelScreen(
    viewModel: MovimientosViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        ClasificacionUseCaseEntryPoint::class.java
    )
    val clasificacionUseCase = entryPoint.clasificacionUseCase()
    
    var archivoUri by remember { mutableStateOf<Uri?>(null) }
    var archivoNombre by remember { mutableStateOf("") }
    var tipoArchivo by remember { mutableStateOf("") }
    var expandedTipo by remember { mutableStateOf(false) }
    var expandedMes by remember { mutableStateOf(false) }
    val periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
    val periodoGlobal by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    var mesSeleccionado by remember { mutableStateOf(periodoGlobal) }
    val tipos = listOf("Estado de cierre", "Últimos movimientos")
    val calendar = Calendar.getInstance()
    val meses = (0..11).map { offset ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.MONTH, -offset)
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        "$year-$month"
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        archivoUri = uri
        archivoNombre = uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && nameIndex >= 0) c.getString(nameIndex) else ""
            } ?: ""
        } ?: ""
    }
    var resultado by remember { mutableStateOf<List<MovimientoEntity>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf(false) }
    var resumenImportacion by remember { mutableStateOf<ResumenImportacion?>(null) }
    var transaccionesConClasificacion by remember { mutableStateOf<List<ExcelTransaction>?>(null) }
    
    // Configurar el ExcelProcessor con el caso de uso de clasificación
    LaunchedEffect(clasificacionUseCase) {
        ExcelProcessor.setClasificacionUseCase(clasificacionUseCase)
    }
    
    // Sincronizar mesSeleccionado con periodoGlobal
    LaunchedEffect(periodoGlobal) {
        if (mesSeleccionado != periodoGlobal) mesSeleccionado = periodoGlobal
    }
    
    // Cuando el usuario cambie mesSeleccionado, actualizar periodoGlobalViewModel.cambiarPeriodo(mesSeleccionado)
    LaunchedEffect(mesSeleccionado) {
        periodoGlobalViewModel.cambiarPeriodo(mesSeleccionado)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Importar archivo Excel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Button(onClick = { launcher.launch("application/vnd.ms-excel") }) {
            Text(if (archivoNombre.isNotEmpty()) "Archivo: $archivoNombre" else "Seleccionar archivo Excel")
        }
        ExposedDropdownMenuBox(expanded = expandedTipo, onExpandedChange = { expandedTipo = !expandedTipo }) {
            OutlinedTextField(
                value = tipoArchivo,
                onValueChange = {},
                label = { Text("Tipo de archivo") },
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) }
            )
            ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                tipos.forEach { tipo ->
                    DropdownMenuItem(
                        text = { Text(tipo) },
                        onClick = {
                            tipoArchivo = tipo
                            expandedTipo = false
                        }
                    )
                }
            }
        }
        ExposedDropdownMenuBox(expanded = expandedMes, onExpandedChange = { expandedMes = !expandedMes }) {
            OutlinedTextField(
                value = mesSeleccionado,
                onValueChange = {},
                label = { Text("Mes de ciclo de facturación") },
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMes) }
            )
            ExposedDropdownMenu(expanded = expandedMes, onDismissRequest = { expandedMes = false }) {
                meses.forEach { mes ->
                    DropdownMenuItem(
                        text = { Text(mes) },
                        onClick = {
                            mesSeleccionado = mes
                            expandedMes = false
                            periodoGlobalViewModel.cambiarPeriodo(mes)
                        }
                    )
                }
            }
        }
        Button(
            onClick = {
                resultado = null
                error = null
                exito = false
                transaccionesConClasificacion = null
                if (archivoUri != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val inputStream = context.contentResolver.openInputStream(archivoUri!!)
                            val transacciones = when (tipoArchivo) {
                                "Estado de cierre" -> ExcelProcessor.importarEstadoDeCierreConClasificacion(inputStream!!, mesSeleccionado)
                                "Últimos movimientos" -> ExcelProcessor.importarUltimosMovimientosConClasificacion(inputStream!!, mesSeleccionado)
                                else -> emptyList()
                            }
                            
                            transaccionesConClasificacion = transacciones
                            
                            val movimientos = transacciones.filter {
                                it.descripcion.trim().uppercase() != "MONTO CANCELADO"
                            }.mapNotNull { t ->
                                if (t.fecha == null) return@mapNotNull null
                                MovimientoEntity(
                                    tipo = "GASTO",
                                    monto = t.monto,
                                    descripcion = t.descripcion,
                                    fecha = t.fecha,
                                    periodoFacturacion = mesSeleccionado,
                                    categoriaId = t.categoriaId, // Usar la categoría sugerida automáticamente
                                    tipoTarjeta = t.tipoTarjeta,
                                    idUnico = ExcelProcessor.generarIdUnico(t.fecha, t.monto, t.descripcion)
                                )
                            }
                            
                            resultado = movimientos
                            
                            val existentes = viewModel.obtenerIdUnicosExistentesPorPeriodo(mesSeleccionado)
                            val categoriasPrevias = viewModel.obtenerCategoriasPorIdUnico(mesSeleccionado)
                            if (tipoArchivo == "Estado de cierre") {
                                viewModel.eliminarMovimientosPorPeriodo(mesSeleccionado)
                            }
                            val nuevos = movimientos.filter { it.idUnico !in existentes }
                            val duplicados = movimientos.size - nuevos.size
                            
                            nuevos.forEach { mov ->
                                val categoriaAnterior = categoriasPrevias[mov.idUnico]
                                val movConCategoria = if (categoriaAnterior != null) {
                                    mov.copy(categoriaId = categoriaAnterior)
                                } else {
                                    mov // Ya tiene la categoría sugerida automáticamente
                                }
                                viewModel.agregarMovimiento(movConCategoria)
                            }
                            
                            val montoTotal = nuevos.sumOf { it.monto }
                            val clasificadasAutomaticamente = nuevos.count { it.categoriaId != null }
                            val pendientesClasificacion = nuevos.size - clasificadasAutomaticamente
                            
                            resumenImportacion = ResumenImportacion(
                                totalProcesadas = movimientos.size,
                                nuevas = nuevos.size,
                                duplicadas = duplicados,
                                montoTotal = montoTotal,
                                periodo = mesSeleccionado ?: "-",
                                clasificadasAutomaticamente = clasificadasAutomaticamente,
                                pendientesClasificacion = pendientesClasificacion
                            )
                            exito = true
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }
            },
            enabled = archivoUri != null && tipoArchivo.isNotEmpty() && mesSeleccionado.isNotEmpty()
        ) {
            Text("Procesar archivo e importar")
        }
        
        if (resultado != null) {
            Text("Movimientos a importar: ${resultado!!.size}", fontWeight = FontWeight.Bold)
            resultado!!.take(5).forEachIndexed { i, t ->
                val clasificacionInfo = if (t.categoriaId != null) "✓ Clasificado" else "⚠ Pendiente"
                Text("#${i+1}: ${t.fecha} | ${t.descripcion} | ${t.monto} | $clasificacionInfo")
            }
        }
        
        if (exito) {
            Text("¡Importación completada!", color = MaterialTheme.colorScheme.primary)
        }
        
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        
        if (resumenImportacion != null) {
            AlertDialog(
                onDismissRequest = { resumenImportacion = null },
                title = { Text("Resumen de Importación") },
                text = {
                    Column {
                        Text("Total procesadas: ${resumenImportacion!!.totalProcesadas}")
                        Text("Nuevas importadas: ${resumenImportacion!!.nuevas}")
                        Text("Duplicadas (omitidas): ${resumenImportacion!!.duplicadas}")
                        Text("Clasificadas automáticamente: ${resumenImportacion!!.clasificadasAutomaticamente}")
                        Text("Pendientes de clasificación: ${resumenImportacion!!.pendientesClasificacion}")
                        Text("Monto total importado: ${FormatUtils.formatMoneyCLP(resumenImportacion!!.montoTotal)}")
                        Text("Periodo: ${resumenImportacion!!.periodo}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { resumenImportacion = null }) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }
}

data class ResumenImportacion(
    val totalProcesadas: Int,
    val nuevas: Int,
    val duplicadas: Int,
    val montoTotal: Double,
    val periodo: String,
    val clasificadasAutomaticamente: Int = 0,
    val pendientesClasificacion: Int = 0
) 