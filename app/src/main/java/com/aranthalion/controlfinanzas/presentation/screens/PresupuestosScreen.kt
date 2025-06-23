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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresupuestosScreen(
    navController: NavHostController,
    viewModel: PresupuestosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val periodoSeleccionado by viewModel.periodoSeleccionado.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val presupuestosPorCategoria by viewModel.presupuestosPorCategoria.collectAsState()
    val resumen by viewModel.resumen.collectAsState()
    val scope = rememberCoroutineScope()

    var showPeriodoSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showPeriodoSelector = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Seleccionar período")
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
                        // Selector de período actual
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Período Actual",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = periodoSeleccionado,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Button(onClick = { showPeriodoSelector = true }) {
                                        Icon(Icons.Default.DateRange, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cambiar")
                                    }
                                }
                            }
                        }

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
                            val focusRequester = remember { FocusRequester() }
                            var fieldHasFocus by remember { mutableStateOf(false) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = categoria.nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (tienePresupuesto) {
                                            OutlinedTextField(
                                                value = montoPresupuesto,
                                                onValueChange = {
                                                    val filtered = it.filter { c -> c.isDigit() }
                                                    montoPresupuesto = filtered
                                                },
                                                label = { Text("Monto presupuesto") },
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                    keyboardType = KeyboardType.Number
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(focusRequester)
                                                    .onFocusChanged { focusState ->
                                                        val wasFocused = fieldHasFocus
                                                        fieldHasFocus = focusState.isFocused
                                                        if (wasFocused && !focusState.isFocused) {
                                                            val monto = montoPresupuesto.toLongOrNull()
                                                            if (monto != null && monto > 0) {
                                                                scope.launch {
                                                                    viewModel.guardarPresupuesto(categoria.id, monto.toDouble(), periodoSeleccionado)
                                                                }
                                                            }
                                                        }
                                                    },
                                                singleLine = true
                                            )
                                            LaunchedEffect(tienePresupuesto) {
                                                if (tienePresupuesto) {
                                                    focusRequester.requestFocus()
                                                }
                                            }
                                        }
                                    }
                                    Switch(
                                        checked = tienePresupuesto,
                                        onCheckedChange = { checked ->
                                            tienePresupuesto = checked
                                            if (!checked) {
                                                montoPresupuesto = ""
                                                scope.launch {
                                                    viewModel.eliminarPresupuesto(categoria.id, periodoSeleccionado)
                                                }
                                            } else {
                                                // Si se activa, poner un valor por defecto
                                                montoPresupuesto = ""
                                            }
                                        }
                                    )
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

        // Selector de período
        if (showPeriodoSelector) {
            PeriodoSelectorDialogPresupuesto(
                periodos = viewModel.periodosDisponibles,
                periodoSeleccionado = periodoSeleccionado,
                onDismiss = { showPeriodoSelector = false },
                onPeriodoSelected = { periodo ->
                    viewModel.cargarPresupuestos(periodo)
                    showPeriodoSelector = false
                }
            )
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