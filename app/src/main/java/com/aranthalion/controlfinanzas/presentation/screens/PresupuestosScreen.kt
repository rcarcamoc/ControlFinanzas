package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.domain.usecase.GestionarPresupuestosUseCase
import com.aranthalion.controlfinanzas.domain.usecase.PresupuestoCategoria
import com.aranthalion.controlfinanzas.domain.usecase.EstadoPresupuesto
import com.aranthalion.controlfinanzas.presentation.components.PresupuestoCard
import com.aranthalion.controlfinanzas.presentation.components.ResumenPresupuestosCard
import java.text.SimpleDateFormat
import java.util.*
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresupuestosScreen(
    navController: NavHostController,
    viewModel: PresupuestosViewModel = hiltViewModel(),
    periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val periodoSeleccionado by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    val periodosDisponibles by periodoGlobalViewModel.periodosDisponibles.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val presupuestosPorCategoria by viewModel.presupuestosPorCategoria.collectAsState()
    val resumen by viewModel.resumen.collectAsState()
    val scope = rememberCoroutineScope()
    var showPeriodoSelector by remember { mutableStateOf(false) }
    // Cuando cambie el período seleccionado, aplicar lazy copy si es necesario antes de cargar los presupuestos
    LaunchedEffect(periodoSeleccionado, categorias) {
        categorias.forEach { categoria ->
            viewModel.lazyCopyPresupuestoSiNoExiste(categoria.id, periodoSeleccionado)
        }
        // Esperar un poco para asegurar que los presupuestos se creen antes de cargar
        kotlinx.coroutines.delay(100)
        viewModel.cargarPresupuestos(periodoSeleccionado)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is PresupuestosUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is PresupuestosUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Resumen de presupuestos
                        if (resumen != null) {
                            item {
                                ResumenPresupuestosCard(
                                    resumen = resumen!!,
                                    onVerDetalle = { /* TODO: Navegar a detalle */ }
                                )
                            }
                        }

                        // Lista editable de presupuestos por categoría
                        items(categorias.distinctBy { it.nombre.trim().lowercase() }) { categoria: Categoria ->
                            val presupuesto = presupuestosPorCategoria[categoria.id]
                            var tienePresupuesto by remember(presupuesto) { mutableStateOf(presupuesto != null) }
                            var montoPresupuesto by remember(presupuesto) { mutableStateOf(presupuesto?.monto?.toLong()?.toString() ?: "") }
                            var montoTemporal by remember { mutableStateOf(montoPresupuesto) }
                            var isEditing by remember { mutableStateOf(false) }
                            val focusRequester = remember { FocusRequester() }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = categoria.nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (tienePresupuesto) {
                                                if (isEditing) {
                                            OutlinedTextField(
                                                        value = montoTemporal,
                                                onValueChange = {
                                                    val filtered = it.filter { c -> c.isDigit() }
                                                            montoTemporal = filtered
                                                },
                                                label = { Text("Monto presupuesto") },
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                    keyboardType = KeyboardType.Number
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                            .focusRequester(focusRequester),
                                                singleLine = true
                                            )
                                                    LaunchedEffect(Unit) {
                                                    focusRequester.requestFocus()
                                                    }
                                                } else {
                                                    Text(
                                                        text = "Presupuesto: $${montoPresupuesto}",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                            }
                                        }
                                    }
                                    Switch(
                                        checked = tienePresupuesto,
                                        onCheckedChange = { checked ->
                                            tienePresupuesto = checked
                                            if (!checked) {
                                                montoPresupuesto = ""
                                                    montoTemporal = ""
                                                    isEditing = false
                                                scope.launch {
                                                    viewModel.eliminarPresupuesto(categoria.id, periodoSeleccionado)
                                                }
                                            } else {
                                                montoPresupuesto = ""
                                                    montoTemporal = ""
                                                    isEditing = true
                                                }
                                            }
                                        )
                                    }
                                    
                                    // Botones de acción cuando está editando
                                    if (tienePresupuesto && isEditing) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val monto = montoTemporal.toLongOrNull()
                                                    if (monto != null && monto > 0) {
                                                        montoPresupuesto = montoTemporal
                                                        isEditing = false
                                                        scope.launch {
                                                            viewModel.guardarPresupuesto(categoria.id, monto.toDouble(), periodoSeleccionado)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                enabled = montoTemporal.isNotEmpty() && montoTemporal.toLongOrNull() != null && montoTemporal.toLongOrNull()!! > 0
                                            ) {
                                                Text("Guardar")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    montoTemporal = montoPresupuesto
                                                    isEditing = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Cancelar")
                                            }
                                        }
                                    } else if (tienePresupuesto && !isEditing) {
                                        // Botón para editar cuando no está en modo edición
                                        Button(
                                            onClick = {
                                                montoTemporal = montoPresupuesto
                                                isEditing = true
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        ) {
                                            Text("Editar Presupuesto")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is PresupuestosUiState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (uiState as PresupuestosUiState.Error).mensaje,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarPresupuestoDialog(
    presupuesto: PresupuestoCategoria?,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double?) -> Unit
) {
    var presupuestoValue by remember { mutableStateOf(presupuesto?.presupuesto?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (presupuesto == null) "Configurar Presupuesto" else "Editar Presupuesto") },
        text = {
            Column {
                if (presupuesto != null) {
                    Text(
                        text = "Categoría: ${presupuesto.categoria.nombre}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedTextField(
                    value = presupuestoValue,
                    onValueChange = { presupuestoValue = it },
                    label = { Text("Presupuesto mensual") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Deja vacío para eliminar el presupuesto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val presupuestoDouble = presupuestoValue.toDoubleOrNull()
                    if (presupuesto != null) {
                        onConfirm(presupuesto.categoria.id, presupuestoDouble)
                    }
                },
                enabled = presupuesto != null
            ) {
                Text(if (presupuesto == null) "Guardar" else "Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun PeriodoSelectorDialogPresupuesto(
    periodos: List<String>,
    periodoSeleccionado: String,
    onDismiss: () -> Unit,
    onPeriodoSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Período") },
        text = {
            Column {
                periodos.forEach { periodo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = periodoSeleccionado == periodo,
                            onClick = { onPeriodoSelected(periodo) }
                        )
                        Text(periodo)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
} 