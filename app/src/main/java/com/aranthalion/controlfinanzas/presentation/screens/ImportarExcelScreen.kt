package com.aranthalion.controlfinanzas.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.util.ExcelProcessor
import com.aranthalion.controlfinanzas.data.util.ExcelTransaction
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.data.util.ParDuplicadoSimilar
import com.aranthalion.controlfinanzas.di.ClasificacionUseCaseEntryPoint
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import com.aranthalion.controlfinanzas.presentation.screens.components.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val clasificacionUseCase = entryPoint.gestionarClasificacionAutomaticaUseCase()
    
    var archivoUri by remember { mutableStateOf<Uri?>(null) }
    var archivoNombre by remember { mutableStateOf("") }
    var tipoArchivo by remember { mutableStateOf("") }
    var expandedTipo by remember { mutableStateOf(false) }
    val periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
    val periodoGlobal by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    val periodosDisponibles by periodoGlobalViewModel.periodosDisponibles.collectAsState()
    val tipos = listOf("Estado de cierre", "Últimos movimientos")
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        archivoUri = uri
        archivoNombre = uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && nameIndex >= 0) c.getString(nameIndex) else ""
            } ?: ""
        } ?: ""
    }
    
    var resultado by remember { mutableStateOf<List<MovimientoEntity>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf(false) }
    var resumenImportacion by remember { mutableStateOf<ResumenImportacion?>(null) }
    var transaccionesConClasificacion by remember { mutableStateOf<List<ExcelTransaction>?>(null) }
    var mostrarTinder by remember { mutableStateOf(false) }
    var duplicadosSimilares by remember { mutableStateOf<List<ParDuplicadoSimilar>>(emptyList()) }
    var mostrarDialogoDuplicados by remember { mutableStateOf(false) }
    var duplicadoActual by remember { mutableStateOf<ParDuplicadoSimilar?>(null) }
    var indiceDuplicadoActual by remember { mutableStateOf(0) }
    var duplicadosOmitidos by remember { mutableStateOf<List<ParDuplicadoSimilar>>(emptyList()) }
    var duplicadosFusionados by remember { mutableStateOf<List<ParDuplicadoSimilar>>(emptyList()) }
    var duplicadosSobrescritos by remember { mutableStateOf<List<ParDuplicadoSimilar>>(emptyList()) }
    var duplicadosAmbos by remember { mutableStateOf<List<ParDuplicadoSimilar>>(emptyList()) }
    
    // Configurar el ExcelProcessor con el caso de uso de clasificación
    LaunchedEffect(clasificacionUseCase) {
        ExcelProcessor.setClasificacionUseCase(clasificacionUseCase)
    }
    
    // Función para continuar con la importación después de procesar duplicados
    fun continuarImportacion() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val movimientos = resultado ?: return@launch
                val existentes = viewModel.obtenerIdUnicosExistentesPorPeriodo(periodoGlobal)
                val categoriasPrevias = viewModel.obtenerCategoriasPorIdUnico(periodoGlobal)
                
                // Obtener IDs únicos de transacciones a omitir/fusionar/sobrescribir (ya procesadas localmente)
                val idsAOmitir = (duplicadosOmitidos + duplicadosFusionados + duplicadosSobrescritos).map {
                    ExcelProcessor.generarIdUnico(it.nueva.fecha, it.nueva.monto, it.nueva.descripcion)
                }.toSet()
                
                // Obtener IDs únicos de transacciones que se quieren conservar a pesar de ser duplicadas
                val idsAmbos = duplicadosAmbos.map {
                    ExcelProcessor.generarIdUnico(it.nueva.fecha, it.nueva.monto, it.nueva.descripcion)
                }.toSet()
                
                val movimientosFiltrados = movimientos.map { mov ->
                    if (mov.idUnico in idsAmbos) {
                        // Cambiar idUnico para evitar colisiones y asegurar inserción
                        mov.copy(idUnico = "${mov.idUnico}-f")
                    } else {
                        mov
                    }
                }.filter { it.idUnico !in idsAOmitir }
                
                val nuevos = movimientosFiltrados.filter { it.idUnico !in existentes }
                val duplicados = movimientos.size - nuevos.size
                
                println("🔄 IMPORTACIÓN: Nuevos movimientos: ${nuevos.size}, Duplicados: $duplicados")
                
                nuevos.forEach { mov ->
                    val categoriaAnterior = categoriasPrevias[mov.idUnico]
                    val movConCategoria = if (categoriaAnterior != null) {
                        mov.copy(categoriaId = categoriaAnterior)
                    } else {
                        mov
                    }
                    println("🔄 IMPORTACIÓN: Procesando movimiento: ${mov.descripcion}, idUnico: ${mov.idUnico}")
                    viewModel.agregarMovimiento(movConCategoria)
                    println("💾 IMPORTACIÓN: Intento de guardar movimiento: ${movConCategoria.descripcion}, idUnico: ${movConCategoria.idUnico}")
                }
                
                val montoTotal = nuevos.sumOf { it.monto.toDouble() }
                val clasificadasAutomaticamente = 0
                val pendientesClasificacion = nuevos.size
                
                resumenImportacion = ResumenImportacion(
                    totalProcesadas = movimientos.size,
                    nuevas = nuevos.size,
                    duplicadas = duplicados,
                    montoTotal = montoTotal.toLong(),
                    periodo = periodoGlobal ?: "-",
                    clasificadasAutomaticamente = clasificadasAutomaticamente,
                    pendientesClasificacion = pendientesClasificacion
                )
                
                exito = true
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Importar Excel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Importa transacciones desde archivos Excel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Selector de archivo
        PanelSelectorArchivo(
            archivoNombre = archivoNombre,
            onSeleccionarArchivo = { launcher.launch("application/vnd.ms-excel") }
        )
        
        // Configuración
        PanelConfiguracionImportacion(
            tipoArchivo = tipoArchivo,
            tipos = tipos,
            expandedTipo = expandedTipo,
            onExpandedTipoChange = { expandedTipo = it },
            onTipoArchivoChange = { tipoArchivo = it }
        )
        
        // Botón de importar
        Button(
            onClick = {
                resultado = null
                error = null
                exito = false
                transaccionesConClasificacion = null
                duplicadosOmitidos = emptyList()
                duplicadosFusionados = emptyList()
                duplicadosSobrescritos = emptyList()
                duplicadosAmbos = emptyList()
                if (archivoUri != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            context.contentResolver.openInputStream(archivoUri!!)?.use { inputStream ->
                                val transacciones = when (tipoArchivo) {
                                    "Estado de cierre" -> ExcelProcessor.importarEstadoDeCierreConSugerencias(inputStream, periodoGlobal)
                                    "Últimos movimientos" -> ExcelProcessor.importarUltimosMovimientosConSugerencias(inputStream, periodoGlobal)
                                    else -> emptyList()
                                }
                            
                                transaccionesConClasificacion = transacciones
                            
                                val movimientos = transacciones.filter {
                                    it.descripcion.trim().uppercase() != "MONTO CANCELADO"
                                }.mapNotNull { t ->
                                    if (t.fecha == null) return@mapNotNull null
                                    val tipo = "GASTO"
                                    val montoFinal = t.monto
                                    val periodoCalculado = t.fecha?.let {
                                        com.aranthalion.controlfinanzas.data.util.BillingPeriodHelper.obtenerPeriodoParaFecha(it, viewModel.customPeriodConfigs)
                                    } ?: t.periodoFacturacion ?: periodoGlobal

                                    MovimientoEntity(
                                        tipo = tipo,
                                        monto = montoFinal,
                                        descripcion = t.descripcion,
                                        fecha = t.fecha,
                                        periodoFacturacion = periodoCalculado,
                                        categoriaId = null,
                                        tipoTarjeta = t.tipoTarjeta,
                                        idUnico = ExcelProcessor.generarIdUnico(t.fecha, t.monto, t.descripcion)
                                    )
                                }
                            
                                resultado = movimientos
                            
                                val existentes = viewModel.obtenerIdUnicosExistentesPorPeriodo(periodoGlobal)
                                val categoriasPrevias = viewModel.obtenerCategoriasPorIdUnico(periodoGlobal)
                                val movimientosExistentes = viewModel.obtenerMovimientosPorPeriodo(periodoGlobal)
                            
                                // Detectar duplicados similares
                                val duplicadosSimilaresDetectados = ExcelProcessor.detectarDuplicadosSimilares(
                                    transacciones, movimientosExistentes
                                )
                            
                                if (duplicadosSimilaresDetectados.isNotEmpty()) {
                                    duplicadosSimilares = duplicadosSimilaresDetectados
                                    mostrarDialogoDuplicados = true
                                    duplicadoActual = duplicadosSimilaresDetectados.first()
                                    indiceDuplicadoActual = 0
                                    return@launch
                                }
                            
                                val nuevos = movimientos.filter { it.idUnico !in existentes }
                                val duplicados = movimientos.size - nuevos.size
                            
                                println("🔄 IMPORTACIÓN: Nuevos movimientos: ${nuevos.size}, Duplicados: $duplicados")
                            
                                nuevos.forEach { mov ->
                                    val categoriaAnterior = categoriasPrevias[mov.idUnico]
                                    val movConCategoria = if (categoriaAnterior != null) {
                                        mov.copy(categoriaId = categoriaAnterior)
                                    } else {
                                        mov
                                    }
                                    println("🔄 IMPORTACIÓN: Procesando movimiento: ${mov.descripcion}, idUnico: ${mov.idUnico}")
                                    viewModel.agregarMovimiento(movConCategoria)
                                    println("💾 IMPORTACIÓN: Intento de guardar movimiento: ${movConCategoria.descripcion}, idUnico: ${movConCategoria.idUnico}")
                                }
                            
                                val montoTotal = nuevos.sumOf { it.monto.toDouble() }
                                val clasificadasAutomaticamente = 0
                                val pendientesClasificacion = nuevos.size
                            
                                resumenImportacion = ResumenImportacion(
                                    totalProcesadas = movimientos.size,
                                    nuevas = nuevos.size,
                                    duplicadas = duplicados,
                                    montoTotal = montoTotal.toLong(),
                                    periodo = periodoGlobal ?: "-",
                                    clasificadasAutomaticamente = clasificadasAutomaticamente,
                                    pendientesClasificacion = pendientesClasificacion
                                )
                            
                                exito = true
                            }
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }
            },
            enabled = archivoUri != null && tipoArchivo.isNotEmpty() && periodoGlobal.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Procesar archivo e importar",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Vista previa de resultados
        if (resultado != null) {
            PanelVistaPreviaImportacion(resultado!!)
        }
        
        if (exito) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "¡Importación completada exitosamente!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        if (error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        resumenImportacion?.let { resumen ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(200, easing = FastOutLinearInEasing)
                )
            ) {
                AlertDialog(
                    onDismissRequest = { resumenImportacion = null },
                    title = { 
                        Text(
                            "Resumen de Importación",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResumenItem("Total procesadas", "${resumen.totalProcesadas}")
                            ResumenItem("Nuevas importadas", "${resumen.nuevas}")
                            ResumenItem("Duplicadas (omitidas)", "${resumen.duplicadas}")
                            ResumenItem("Clasificadas automáticamente", "${resumen.clasificadasAutomaticamente}")
                            ResumenItem("Pendientes de clasificación", "${resumen.pendientesClasificacion}")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ResumenItem("Monto total importado", FormatUtils.formatMoneyCLP(resumen.montoTotal.toDouble()))
                            ResumenItem("Periodo", resumen.periodo)
                        }
                    },
                    confirmButton = {
                        Button(onClick = { resumenImportacion = null }) {
                            Text("Aceptar")
                        }
                    }
                )
            }
        }
        
        // Tinder de clasificación
        if (mostrarTinder) {
            TinderClasificacionScreen(
                onDismiss = {
                    mostrarTinder = false
                }
            )
        }
        
        // Diálogo de confirmación de duplicados similares
        if (mostrarDialogoDuplicados && duplicadoActual != null) {
            val avanzarDuplicado = {
                if (indiceDuplicadoActual + 1 >= duplicadosSimilares.size) {
                    mostrarDialogoDuplicados = false
                    continuarImportacion()
                } else {
                    indiceDuplicadoActual++
                    duplicadoActual = duplicadosSimilares[indiceDuplicadoActual]
                }
            }
            
            DialogoConfirmacionDuplicados(
                duplicado = duplicadoActual!!,
                indice = indiceDuplicadoActual,
                total = duplicadosSimilares.size,
                onOmitirNueva = {
                    duplicadosOmitidos = duplicadosOmitidos + duplicadoActual!!
                    avanzarDuplicado()
                },
                onFusionar = {
                    val current = duplicadoActual!!
                    duplicadosFusionados = duplicadosFusionados + current
                    CoroutineScope(Dispatchers.IO).launch {
                        val merged = com.aranthalion.controlfinanzas.data.util.DuplicateTransactionDetector.fusionarMovimientoConExcel(
                            current.existente, current.nueva
                        )
                        viewModel.actualizarMovimiento(merged)
                    }
                    avanzarDuplicado()
                },
                onSobrescribir = {
                    val current = duplicadoActual!!
                    duplicadosSobrescritos = duplicadosSobrescritos + current
                    CoroutineScope(Dispatchers.IO).launch {
                        val updated = current.existente.copy(
                            monto = current.nueva.monto,
                            descripcion = current.nueva.descripcion,
                            fecha = current.nueva.fecha ?: current.existente.fecha,
                            tipoTarjeta = current.nueva.tipoTarjeta?.ifBlank { null } ?: current.existente.tipoTarjeta,
                            fechaActualizacion = System.currentTimeMillis(),
                            metodoActualizacion = "OVERWRITE"
                        )
                        viewModel.actualizarMovimiento(updated)
                    }
                    avanzarDuplicado()
                },
                onConservarAmbas = {
                    duplicadosAmbos = duplicadosAmbos + duplicadoActual!!
                    avanzarDuplicado()
                },
                onCancelar = {
                    mostrarDialogoDuplicados = false
                    duplicadosSimilares = emptyList()
                }
            )
        }
    }
}