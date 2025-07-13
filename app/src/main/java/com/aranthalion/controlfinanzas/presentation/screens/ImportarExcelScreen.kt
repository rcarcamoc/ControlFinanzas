package com.aranthalion.controlfinanzas.presentation.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.animation.*
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import com.aranthalion.controlfinanzas.di.ClasificacionUseCaseEntryPoint
import com.aranthalion.controlfinanzas.presentation.screens.TinderClasificacionScreen
import com.aranthalion.controlfinanzas.presentation.screens.TinderClasificacionViewModel

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
    var mostrarTinder by remember { mutableStateOf(false) }
    
    // ViewModel del Tinder de clasificación
    val tinderViewModel: TinderClasificacionViewModel = hiltViewModel()
    
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Archivo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedButton(
                    onClick = { launcher.launch("application/vnd.ms-excel") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (archivoNombre.isNotEmpty()) archivoNombre else "Seleccionar archivo Excel"
                    )
                }
                
                if (archivoNombre.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Archivo seleccionado",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
        
        // Configuración
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configuración",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
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
            }
        }
        
        // Botón de importar
        Button(
            onClick = {
                resultado = null
                error = null
                exito = false
                transaccionesConClasificacion = null
                if (archivoUri != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            context.contentResolver.openInputStream(archivoUri!!)?.use { inputStream ->
                                val transacciones = when (tipoArchivo) {
                                    "Estado de cierre" -> ExcelProcessor.importarEstadoDeCierreConSugerencias(inputStream, mesSeleccionado)
                                    "Últimos movimientos" -> ExcelProcessor.importarUltimosMovimientosConSugerencias(inputStream, mesSeleccionado)
                                    else -> emptyList()
                                }
                            
                            transaccionesConClasificacion = transacciones
                            
                            val movimientos = transacciones.filter {
                                it.descripcion.trim().uppercase() != "MONTO CANCELADO"
                            }.mapNotNull { t ->
                                if (t.fecha == null) return@mapNotNull null
                                // Todos los movimientos de tarjeta son gastos (incluyendo reversas)
                                val tipo = "GASTO"
                                // Mantener el monto original (negativo para reversas)
                                val montoFinal = t.monto
                                MovimientoEntity(
                                    tipo = tipo,
                                    monto = montoFinal,
                                    descripcion = t.descripcion,
                                    fecha = t.fecha,
                                    periodoFacturacion = t.periodoFacturacion ?: mesSeleccionado,
                                    categoriaId = null, // IMPORTANTE: SIEMPRE null - el usuario debe decidir
                                    tipoTarjeta = t.tipoTarjeta,
                                    idUnico = ExcelProcessor.generarIdUnico(t.fecha, t.monto, t.descripcion)
                                )
                            }
                            
                            resultado = movimientos
                            
                            val existentes = viewModel.obtenerIdUnicosExistentesPorPeriodo(mesSeleccionado)
                            val categoriasPrevias = viewModel.obtenerCategoriasPorIdUnico(mesSeleccionado)
                            
                            // Para "Estado de cierre", preservar clasificaciones existentes en lugar de eliminar todo
                            if (tipoArchivo == "Estado de cierre") {
                                // En lugar de eliminar todo el período, solo actualizar movimientos existentes
                                // y agregar los nuevos, preservando las clasificaciones manuales
                                println("🔄 IMPORTACIÓN: Preservando clasificaciones manuales para Estado de cierre")
                            }
                            
                            val nuevos = movimientos.filter { it.idUnico !in existentes }
                            val duplicados = movimientos.size - nuevos.size
                            
                            nuevos.forEach { mov ->
                                val categoriaAnterior = categoriasPrevias[mov.idUnico]
                                val movConCategoria = if (categoriaAnterior != null) {
                                    mov.copy(categoriaId = categoriaAnterior)
                                } else {
                                    mov
                                }
                                viewModel.agregarMovimiento(movConCategoria)
                            }
                            
                            // Aprender de las clasificaciones manuales preservadas
                            // Nota: Las clasificaciones se preservan automáticamente en el proceso de importación
                            
                            val montoTotal = nuevos.sumOf { it.monto.toDouble() }
                            val clasificadasAutomaticamente = 0 // IMPORTANTE: Ya no hay clasificación automática
                            val pendientesClasificacion = nuevos.size // Todas las transacciones necesitan clasificación manual
                            
                            resumenImportacion = ResumenImportacion(
                                totalProcesadas = movimientos.size,
                                nuevas = nuevos.size,
                                duplicadas = duplicados,
                                montoTotal = montoTotal.toLong(),
                                periodo = mesSeleccionado ?: "-",
                                clasificadasAutomaticamente = clasificadasAutomaticamente,
                                pendientesClasificacion = pendientesClasificacion
                            )
                            
                            // Procesar TODAS las transacciones para el Tinder de clasificación
                            // ya que ninguna tiene categoría asignada automáticamente
                            if (transacciones.isNotEmpty()) {
                                tinderViewModel.procesarTransaccionesParaTinder(transacciones)
                                mostrarTinder = true
                            }
                            
                            exito = true
                        }
                    } catch (e: Exception) {
                        error = e.message
                    }
                    }
                }
            },
            enabled = archivoUri != null && tipoArchivo.isNotEmpty() && mesSeleccionado.isNotEmpty(),
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
        
        // Resultados
        if (resultado != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Vista previa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Movimientos a importar: ${resultado!!.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    resultado!!.take(5).forEachIndexed { i, t ->
                        val clasificacionInfo = if (t.categoriaId != null) "✓ Clasificado" else "⚠ Pendiente"
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${i+1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = t.descripcion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${t.fecha} • ${FormatUtils.formatMoneyCLP(t.monto)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = clasificacionInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (t.categoriaId != null) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
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
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
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
    }
}

@Composable
private fun ResumenItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

data class ResumenImportacion(
    val totalProcesadas: Int,
    val nuevas: Int,
    val duplicadas: Int,
    val montoTotal: Long,
    val periodo: String,
    val clasificadasAutomaticamente: Int,
    val pendientesClasificacion: Int
) 