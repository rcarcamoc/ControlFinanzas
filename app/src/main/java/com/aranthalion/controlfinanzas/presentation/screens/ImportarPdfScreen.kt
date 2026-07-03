package com.aranthalion.controlfinanzas.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.data.util.ParDuplicadoSimilar
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import com.aranthalion.controlfinanzas.presentation.screens.components.DialogoConfirmacionDuplicados
import com.aranthalion.controlfinanzas.presentation.screens.components.PanelSelectorArchivo
import com.aranthalion.controlfinanzas.presentation.screens.components.PanelVistaPreviaImportacion
import com.aranthalion.controlfinanzas.presentation.screens.components.ResumenItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportarPdfScreen(
    viewModel: PdfImportViewModel = hiltViewModel(),
    periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val importState by viewModel.importState.collectAsState()
    val periodoGlobal by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    val periodosDisponibles by periodoGlobalViewModel.periodosDisponibles.collectAsState()
    var periodoSeleccionado by remember(periodoGlobal) { mutableStateOf(periodoGlobal) }

    var archivoUri by remember { mutableStateOf<Uri?>(null) }
    var archivoNombre by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("2851") } // Contraseña predeterminada 2851 para Lider PDF

    // Resolviendo duplicados
    var duplicadosOmitidos by remember { mutableStateOf<List<ParDuplicadoSimilar>>(emptyList()) }
    var duplicadosFusionados by remember { mutableStateOf<List<ParDuplicadoSimilar>>(emptyList()) }
    var duplicadosSobrescritos by remember { mutableStateOf<List<ParDuplicadoSimilar>>(emptyList()) }
    var duplicadosAmbos by remember { mutableStateOf<List<ParDuplicadoSimilar>>(emptyList()) }

    var duplicadoActualIndex by remember { mutableStateOf(0) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        archivoUri = uri
        archivoNombre = uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && nameIndex >= 0) c.getString(nameIndex) else ""
            } ?: ""
        } ?: ""
    }

    // Limpiar estados al salir o entrar
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetState()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera
        item {
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
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Importar PDF Estado Cuenta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Importa transacciones desde PDFs encriptados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Selección de archivo
        item {
            PanelSelectorArchivo(
                archivoNombre = archivoNombre,
                onSeleccionarArchivo = { fileLauncher.launch("application/pdf") }
            )
        }

        // Selección de período de destino
        item {
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
                        text = "Período de facturación de destino",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    var dropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = periodoSeleccionado,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selecciona el período para los movimientos") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .clickable { dropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            periodosDisponibles.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    onClick = {
                                        periodoSeleccionado = p
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Configuración y Contraseña
        item {
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
                        text = "Configuración de seguridad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña del PDF") },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Acciones e importación
        item {
            if (importState is PdfImportUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else {
                Button(
                    onClick = {
                        if (archivoUri != null) {
                            viewModel.importPdfFile(
                                context = context,
                                fileUri = archivoUri!!,
                                passwordStr = password,
                                defaultPeriodo = periodoSeleccionado
                            )
                        }
                    },
                    enabled = archivoUri != null && periodoSeleccionado.isNotEmpty(),
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
                        text = "Enviar y procesar PDF",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Estado del proceso
        item {
            when (val state = importState) {
                is PdfImportUiState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "¡Importación exitosa!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ResumenItem("Total transacciones", "${state.totalProcesadas}")
                            ResumenItem("Nuevas registradas", "${state.nuevas}")
                            ResumenItem("Duplicados resueltos", "${state.duplicadas}")
                            ResumenItem("Monto total", FormatUtils.formatMoneyCLP(state.montoTotal.toDouble()))
                            ResumenItem("Periodo facturación", state.periodo)
                        }
                    }
                }
                is PdfImportUiState.Error -> {
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
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                is PdfImportUiState.PendingDuplicateResolution -> {
                    PanelVistaPreviaImportacion(movimientos = state.movimientos)
                }
                else -> {}
            }
        }
    }

    // Mostrar diálogos de resolución de duplicados secuencialmente
    val state = importState
    if (state is PdfImportUiState.PendingDuplicateResolution) {
        val duplicados = state.duplicados
        if (duplicadoActualIndex < duplicados.size) {
            val duplicadoActual = duplicados[duplicadoActualIndex]

            val avanzar = {
                if (duplicadoActualIndex + 1 >= duplicados.size) {
                    // Finalizar y enviar todas las decisiones al ViewModel
                    viewModel.resolveDuplicatesAndImport(
                        movimientos = state.movimientos,
                        duplicadosOmitidos = duplicadosOmitidos,
                        duplicadosFusionados = duplicadosFusionados,
                        duplicadosSobrescritos = duplicadosSobrescritos,
                        duplicadosAmbos = duplicadosAmbos,
                        billingPeriod = state.billingPeriod
                    )
                    duplicadoActualIndex = 0
                } else {
                    duplicadoActualIndex++
                }
            }

            DialogoConfirmacionDuplicados(
                duplicado = duplicadoActual,
                indice = duplicadoActualIndex,
                total = duplicados.size,
                onOmitirNueva = {
                    duplicadosOmitidos = duplicadosOmitidos + duplicadoActual
                    avanzar()
                },
                onFusionar = {
                    duplicadosFusionados = duplicadosFusionados + duplicadoActual
                    avanzar()
                },
                onSobrescribir = {
                    duplicadosSobrescritos = duplicadosSobrescritos + duplicadoActual
                    avanzar()
                },
                onConservarAmbas = {
                    duplicadosAmbos = duplicadosAmbos + duplicadoActual
                    avanzar()
                },
                onCancelar = {
                    viewModel.resetState()
                    duplicadoActualIndex = 0
                }
            )
        }
    }
}
