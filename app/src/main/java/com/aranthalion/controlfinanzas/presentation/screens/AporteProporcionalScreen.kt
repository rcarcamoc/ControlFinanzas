package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.*
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
import com.aranthalion.controlfinanzas.data.local.entity.SueldoEntity
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.domain.usecase.AporteProporcional
import com.aranthalion.controlfinanzas.domain.usecase.ResumenAporteProporcional
import com.aranthalion.controlfinanzas.presentation.components.HistorialAportesCharts
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorDialog
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AporteProporcionalScreen(
    navController: NavHostController,
    viewModel: AporteProporcionalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
    val periodoGlobal by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    var periodoSeleccionado by remember { mutableStateOf(periodoGlobal) }
    val periodosDisponibles by viewModel.periodosDisponibles.collectAsState()
    val personasDisponibles by viewModel.personasDisponibles.collectAsState()
    val sueldosActuales by viewModel.sueldosActuales.collectAsState()
    
    var showAddSueldoDialog by remember { mutableStateOf(false) }
    var showPeriodoSelector by remember { mutableStateOf(false) }
    var showHistorialDialog by remember { mutableStateOf(false) }
    var sueldoToEdit by remember { mutableStateOf<SueldoEntity?>(null) }

    DisposableEffect(periodoGlobal) {
        periodoSeleccionado = periodoGlobal
        onDispose { }
    }

    LaunchedEffect(periodoSeleccionado) {
        viewModel.calcularAporteProporcional(periodoSeleccionado)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aporte Proporcional") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showPeriodoSelector = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Seleccionar período")
                    }
                    IconButton(onClick = { showHistorialDialog = true }) {
                        Icon(Icons.Default.List, contentDescription = "Ver historial")
                    }
                    IconButton(onClick = { showAddSueldoDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar sueldo")
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
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            when (uiState) {
                is AporteProporcionalUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is AporteProporcionalUiState.Success -> {
                    val resumen = (uiState as AporteProporcionalUiState.Success).resumen
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

                        // Botones de diagnóstico y corrección
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.diagnosticarMovimientosSueldo() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Diagnosticar")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.corregirMovimientosSueldoSinCategoria() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Corregir")
                                }
                            }
                        }

                        // Resumen del período
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Resumen del Período",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Total Gastos: ${FormatUtils.formatMoneyCLP(resumen.totalGastos)}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Total Tarjeta Titular: ${FormatUtils.formatMoneyCLP(resumen.totalTarjetaTitular)}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Total Gastos Distribuibles: ${FormatUtils.formatMoneyCLP(resumen.totalADistribuir)}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Total Sueldos: ${FormatUtils.formatMoneyCLP(resumen.totalSueldos)}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (resumen.totalSueldos > 0) {
                                        Text(
                                            text = "Porcentaje de gastos vs sueldos: ${String.format("%.1f", (resumen.totalADistribuir / resumen.totalSueldos) * 100)}%",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Lista de sueldos registrados
                        if (sueldosActuales.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Sueldos Registrados",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            items(sueldosActuales) { sueldo ->
                                SueldoItem(
                                    sueldo = sueldo,
                                    onEdit = { sueldoToEdit = sueldo },
                                    onDelete = { viewModel.eliminarSueldo(sueldo) }
                                )
                            }
                        }

                        // Lista de aportes calculados
                        if (resumen.aportes.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Aportes Proporcionales",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            items(resumen.aportes) { aporte ->
                                AporteItem(aporte = aporte)
                            }
                        }

                        // Separador y Papá + Tarjeta Titular
                        if (resumen.aportePapaConTarjetaTitular != null) {
                            item {
                                Divider(modifier = Modifier.padding(vertical = 16.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Papá + Tarjeta Titular",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Total a pagar: ${FormatUtils.formatMoneyCLP(resumen.aportePapaConTarjetaTitular)}",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Mensaje si no hay datos
                        if (resumen.aportes.isEmpty() && sueldosActuales.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No hay sueldos registrados para este período",
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { showAddSueldoDialog = true }) {
                                            Text("Agregar sueldo")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is AporteProporcionalUiState.Error -> {
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
                                text = (uiState as AporteProporcionalUiState.Error).mensaje,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                is AporteProporcionalUiState.Diagnostico -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Diagnóstico de Movimientos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (uiState as AporteProporcionalUiState.Diagnostico).diagnostico,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.corregirMovimientosSueldoSinCategoria() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Corregir")
                                }
                                Button(
                                    onClick = { viewModel.calcularAporteProporcional(periodoSeleccionado) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Actualizar")
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        // Diálogo para agregar/editar sueldo
        AnimatedVisibility(
            visible = showAddSueldoDialog || sueldoToEdit != null,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200, easing = FastOutLinearInEasing)
            )
        ) {
            AgregarSueldoDialog(
                sueldo = sueldoToEdit,
                onDismiss = { 
                    showAddSueldoDialog = false
                    sueldoToEdit = null
                },
                onConfirm = { nombrePersona, periodo, sueldo ->
                    if (sueldoToEdit != null) {
                        viewModel.actualizarSueldo(sueldoToEdit!!.copy(
                            nombrePersona = nombrePersona,
                            periodo = periodo,
                            sueldo = sueldo
                        ))
                        sueldoToEdit = null
                    } else {
                        viewModel.guardarSueldo(nombrePersona, periodo, sueldo)
                    }
                    showAddSueldoDialog = false
                },
                personasDisponibles = personasDisponibles,
                periodosDisponibles = periodosDisponibles,
                periodoActual = periodoSeleccionado
            )
        }

        // Selector de período
        AnimatedVisibility(
            visible = showPeriodoSelector,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200, easing = FastOutLinearInEasing)
            )
        ) {
            PeriodoSelectorDialog(
                isVisible = true,
                onDismiss = { showPeriodoSelector = false },
                onConfirm = { startDate, endDate ->
                    // Convertir fechas a formato de período YYYY-MM
                    val periodo = startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                    periodoSeleccionado = periodo
                }
            )
        }

        // Diálogo de historial con gráficas
        AnimatedVisibility(
            visible = showHistorialDialog,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200, easing = FastOutLinearInEasing)
            )
        ) {
            HistorialAportesDialog(
                onDismiss = { showHistorialDialog = false },
                onPeriodoSelected = { periodo ->
                    periodoSeleccionado = periodo
                }
            )
        }
    }
}

@Composable
fun SueldoItem(
    sueldo: SueldoEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sueldo.nombrePersona,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Período: ${sueldo.periodo}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Sueldo: ${FormatUtils.formatMoneyCLP(sueldo.sueldo)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showDeleteDialog,
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
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Sueldo") },
            text = { Text("¿Estás seguro de que quieres eliminar el sueldo de ${sueldo.nombrePersona} para el período ${sueldo.periodo}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AporteItem(aporte: AporteProporcional) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = aporte.nombrePersona,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Sueldo: ${FormatUtils.formatMoneyCLP(aporte.sueldo)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Porcentaje: ${String.format("%.1f", aporte.porcentajeAporte)}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Aporte:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = FormatUtils.formatMoneyCLP(aporte.montoAporte),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarSueldoDialog(
    sueldo: SueldoEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit,
    personasDisponibles: List<String>,
    periodosDisponibles: List<String>,
    periodoActual: String
) {
    var nombrePersona by remember { mutableStateOf(sueldo?.nombrePersona ?: "") }
    var periodoSeleccionado by remember { mutableStateOf(sueldo?.periodo ?: periodoActual) }
    var sueldoValue by remember { mutableStateOf(sueldo?.sueldo?.toString() ?: "") }
    var showPersonaInput by remember { mutableStateOf(false) }
    var showPeriodoInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (sueldo == null) "Agregar Sueldo" else "Editar Sueldo") },
        text = {
            Column {
                // Selector de persona
                Text("Persona:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (personasDisponibles.isNotEmpty() && !showPersonaInput) {
                    personasDisponibles.forEach { persona ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = nombrePersona == persona,
                                onClick = { nombrePersona = persona }
                            )
                            Text(persona)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = showPersonaInput,
                            onClick = { showPersonaInput = true }
                        )
                        Text("Nueva persona")
                    }
                } else {
                    showPersonaInput = true
                }
                
                if (showPersonaInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nombrePersona,
                        onValueChange = { nombrePersona = it },
                        label = { Text("Nombre de la persona") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de período
                Text("Período:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (periodosDisponibles.isNotEmpty() && !showPeriodoInput) {
                    periodosDisponibles.take(5).forEach { periodo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = periodoSeleccionado == periodo,
                                onClick = { periodoSeleccionado = periodo }
                            )
                            Text(periodo)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = showPeriodoInput,
                            onClick = { showPeriodoInput = true }
                        )
                        Text("Nuevo período")
                    }
                } else {
                    showPeriodoInput = true
                }
                
                if (showPeriodoInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = periodoSeleccionado,
                        onValueChange = { periodoSeleccionado = it },
                        label = { Text("Período (YYYY-MM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Campo de sueldo
                OutlinedTextField(
                    value = if (sueldoValue.isNotEmpty()) FormatUtils.formatMoneyCLP(sueldoValue.replace(".", "").replace("$", "").toDoubleOrNull() ?: 0.0) else "",
                    onValueChange = {
                        val cleaned = it.replace("[^\\d]".toRegex(), "")
                        sueldoValue = cleaned
                    },
                    label = { Text("Sueldo") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sueldoDouble = sueldoValue.toDoubleOrNull() ?: 0.0
                    if (nombrePersona.isNotBlank() && periodoSeleccionado.isNotBlank() && sueldoDouble > 0) {
                        onConfirm(nombrePersona, periodoSeleccionado, sueldoDouble)
                    }
                },
                enabled = nombrePersona.isNotBlank() && periodoSeleccionado.isNotBlank() && sueldoValue.toDoubleOrNull() ?: 0.0 > 0
            ) {
                Text(if (sueldo == null) "Guardar" else "Actualizar")
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
fun HistorialAportesDialog(
    onDismiss: () -> Unit,
    onPeriodoSelected: (String) -> Unit
) {
    var historial by remember { mutableStateOf<List<ResumenAporteProporcional>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Cargar historial cuando se abre el diálogo
    LaunchedEffect(Unit) {
        // Por ahora simulamos datos, después conectaremos con el ViewModel
        isLoading = false
        historial = emptyList() // Aquí se cargarían los datos reales
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial de Aportes") },
        text = { 
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (historial.isEmpty()) {
                    Text("No hay datos históricos disponibles. Agrega sueldos en diferentes períodos para ver el historial.")
                } else {
                    HistorialAportesCharts(
                        historial = historial,
                        onPeriodoSelected = onPeriodoSelected
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        modifier = Modifier.fillMaxWidth(0.95f)
    )
} 