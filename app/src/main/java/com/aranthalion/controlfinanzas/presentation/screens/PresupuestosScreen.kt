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
import com.aranthalion.controlfinanzas.presentation.components.PresupuestoProgressBar
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.ExperimentalFoundationApi

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
    val presupuestosCompletos by viewModel.presupuestosCompletos.collectAsState()
    val resumen by viewModel.resumen.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var presupuestoToEdit by remember { mutableStateOf<PresupuestoCategoriaEntity?>(null) }
    var showPeriodoSelector by remember { mutableStateOf(false) }
    
    // Cuando cambie el período seleccionado, aplicar lazy copy si es necesario antes de cargar los presupuestos
    LaunchedEffect(periodoSeleccionado) {
        viewModel.cargarPresupuestos(periodoSeleccionado)
    }
    
    // Cuando cambien las categorías, aplicar lazy copy solo si es necesario
    LaunchedEffect(categorias) {
        println("🔍 LAUNCHED_EFFECT: Categorías cambiaron - Total: ${categorias.size}")
        if (categorias.isNotEmpty()) {
            // Solo aplicar lazy copy si no hay presupuestos para el período actual
            val presupuestosActuales = presupuestosPorCategoria.values.toList()
            println("🔍 LAUNCHED_EFFECT: Presupuestos actuales en memoria: ${presupuestosActuales.size}")
            
            if (presupuestosActuales.isEmpty()) {
                println("🔍 LAUNCHED_EFFECT: Aplicando lazy copy para ${categorias.size} categorías")
                categorias.forEach { categoria ->
                    println("🔍 LAUNCHED_EFFECT: Procesando categoría: ${categoria.nombre} (ID: ${categoria.id})")
                    viewModel.lazyCopyPresupuestoSiNoExiste(categoria.id, periodoSeleccionado)
                }
                // Recargar presupuestos después de aplicar lazy copy
                kotlinx.coroutines.delay(200)
                println("🔍 LAUNCHED_EFFECT: Recargando presupuestos después de lazy copy")
                viewModel.cargarPresupuestos(periodoSeleccionado)
            } else {
                println("🔍 LAUNCHED_EFFECT: Ya existen presupuestos para el período, saltando lazy copy")
            }
        } else {
            println("🔍 LAUNCHED_EFFECT: No hay categorías disponibles")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header mejorado con diseño consistente
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Presupuestos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Gestiona los presupuestos por categoría",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPeriodoSelector = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Seleccionar período",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Período")
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Agregar presupuesto",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nuevo")
                    }
                }
            }
        }

        when (uiState) {
            is PresupuestosUiState.Loading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Cargando presupuestos...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            is PresupuestosUiState.Success -> {
                // Resumen de presupuestos mejorado
                if (resumen != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                "Resumen del Período",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Total Presupuestado",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        FormatUtils.formatMoneyCLP(resumen!!.totalPresupuestado),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Total Gastado",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        FormatUtils.formatMoneyCLP(resumen!!.totalGastado),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = (resumen!!.porcentajeGastado / 100.0).toFloat().coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth(),
                                color = when {
                                    resumen!!.porcentajeGastado <= 80 -> MaterialTheme.colorScheme.primary
                                    resumen!!.porcentajeGastado <= 90 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${resumen!!.porcentajeGastado.toInt()}% utilizado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Tabla de presupuestos mejorada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Presupuestos por Categoría (${categorias.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Período: $periodoSeleccionado",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (categorias.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "No hay categorías",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Agrega categorías para crear presupuestos",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            @OptIn(ExperimentalFoundationApi::class)
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(presupuestosCompletos, key = { it.categoria.id }) { presupuestoCompleto ->
                                    val presupuesto = presupuestosPorCategoria[presupuestoCompleto.categoria.id]
                                    PresupuestoItemCompleto(
                                        presupuestoCompleto = presupuestoCompleto,
                                        presupuesto = presupuesto,
                                        onEdit = { presupuestoToEdit = presupuesto },
                                        onDelete = {
                                            scope.launch {
                                                viewModel.eliminarPresupuesto(presupuestoCompleto.categoria.id, periodoSeleccionado)
                                            }
                                        },
                                        onSave = { monto ->
                                            scope.launch {
                                                viewModel.guardarPresupuesto(presupuestoCompleto.categoria.id, monto, periodoSeleccionado)
                                            }
                                        },
                                        modifier = Modifier.animateItemPlacement()
                                    )
                                }
                                
                                // Mostrar categorías sin presupuesto
                                items(categorias.filter { categoria ->
                                    presupuestosCompletos.none { it.categoria.id == categoria.id }
                                }) { categoria ->
                                    val presupuesto = presupuestosPorCategoria[categoria.id]
                                    PresupuestoItem(
                                        categoria = categoria,
                                        presupuesto = presupuesto,
                                        onEdit = { presupuestoToEdit = presupuesto },
                                        onDelete = {
                                            scope.launch {
                                                viewModel.eliminarPresupuesto(categoria.id, periodoSeleccionado)
                                            }
                                        },
                                        onSave = { monto ->
                                            scope.launch {
                                                viewModel.guardarPresupuesto(categoria.id, monto, periodoSeleccionado)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is PresupuestosUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Error al cargar presupuestos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = (uiState as PresupuestosUiState.Error).mensaje,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo para agregar presupuesto con animación
    if (showAddDialog) {
        PresupuestoDialog(
            categorias = categorias,
            periodoSeleccionado = periodoSeleccionado,
            categoriaPreseleccionada = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { categoriaId, monto ->
                viewModel.guardarPresupuesto(categoriaId, monto, periodoSeleccionado)
                showAddDialog = false
            }
        )
    }

    // Diálogo para editar presupuesto con animación
    AnimatedVisibility(
        visible = presupuestoToEdit != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        presupuestoToEdit?.let { presupuesto ->
            PresupuestoEditDialog(
                presupuesto = presupuesto,
                onDismiss = { presupuestoToEdit = null },
                onConfirm = { monto ->
                    viewModel.guardarPresupuesto(presupuesto.categoriaId, monto, periodoSeleccionado)
                    presupuestoToEdit = null
                }
            )
        }
    }

    // Diálogo para seleccionar período con animación
    AnimatedVisibility(
        visible = showPeriodoSelector,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        PresupuestosPeriodoSelectorDialog(
            periodos = periodosDisponibles,
            periodoSeleccionado = periodoSeleccionado,
            onDismiss = { showPeriodoSelector = false },
            onPeriodoSelected = { periodo ->
                periodoGlobalViewModel.cambiarPeriodo(periodo)
                showPeriodoSelector = false
            }
        )
    }
}

@Composable
fun PresupuestoItem(
    categoria: Categoria,
    presupuesto: PresupuestoCategoriaEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Double) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var montoTemporal by remember { mutableStateOf(presupuesto?.monto?.toString() ?: "") }
    val focusRequester = remember { FocusRequester() }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = categoria.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (presupuesto != null) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = formatNumberWithSeparators(montoTemporal),
                                onValueChange = {
                                    val cleaned = cleanNumberFormat(it)
                                    montoTemporal = cleaned
                                },
                                label = { Text("Monto presupuesto") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            Text(
                                text = "Presupuesto: ${FormatUtils.formatMoneyCLP(presupuesto.monto)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        if (isEditing) {
                            OutlinedTextField(
                                value = formatNumberWithSeparators(montoTemporal),
                                onValueChange = {
                                    val cleaned = cleanNumberFormat(it)
                                    montoTemporal = cleaned
                                },
                                label = { Text("Monto presupuesto") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            Text(
                                text = "Sin presupuesto",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Botones de acción
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (presupuesto != null) {
                        if (isEditing) {
                            IconButton(
                                onClick = {
                                    val monto = montoTemporal.toDoubleOrNull()
                                    if (monto != null && monto > 0) {
                                        onSave(monto)
                                        isEditing = false
                                    }
                                },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Guardar",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { 
                                    isEditing = false
                                    montoTemporal = presupuesto.monto.toString()
                                },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancelar",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { 
                                    isEditing = true
                                    montoTemporal = presupuesto.monto.toString()
                                },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { 
                                isEditing = true
                                montoTemporal = ""
                            },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Agregar presupuesto",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // Botones de acción cuando está editando y no hay presupuesto previo
            if (presupuesto == null && isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val monto = montoTemporal.toDoubleOrNull()
                            if (monto != null && monto > 0) {
                                onSave(monto)
                                isEditing = false
                            }
                        },
                        enabled = montoTemporal.toDoubleOrNull() ?: 0.0 > 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Guardar")
                    }
                    OutlinedButton(
                        onClick = { 
                            isEditing = false
                            montoTemporal = ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
    
    // Diálogo de confirmación de eliminación con animación
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
            title = { 
                Text(
                    "Eliminar presupuesto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { 
                Text(
                    "¿Estás seguro de que quieres eliminar el presupuesto de '${categoria.nombre}'? Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun PresupuestoItemCompleto(
    presupuestoCompleto: PresupuestoCategoria,
    presupuesto: PresupuestoCategoriaEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var montoTemporal by remember { mutableStateOf(presupuesto?.monto?.toString() ?: "") }
    val focusRequester = remember { FocusRequester() }
    
    val colorEstado = when (presupuestoCompleto.estado) {
        EstadoPresupuesto.NORMAL -> Color(0xFF4CAF50) // Verde
        EstadoPresupuesto.ADVERTENCIA -> Color(0xFFFF9800) // Naranja
        EstadoPresupuesto.CRITICO -> Color(0xFFFF5722) // Rojo
        EstadoPresupuesto.EXCEDIDO -> Color(0xFFD32F2F) // Rojo oscuro
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = presupuestoCompleto.categoria.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (presupuesto != null) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = formatNumberWithSeparators(montoTemporal),
                                onValueChange = {
                                    val cleaned = cleanNumberFormat(it)
                                    montoTemporal = cleaned
                                },
                                label = { Text("Monto presupuesto") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            Text(
                                text = "Presupuesto: ${FormatUtils.formatMoneyCLP(presupuesto.monto)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Gastado: ${FormatUtils.formatMoneyCLP(presupuestoCompleto.gastoActual)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (presupuestoCompleto.gastoActual < 0) {
                                Text(
                                    text = "💡 Incluye reversas/reembolsos que reducen el gasto",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        if (isEditing) {
                            OutlinedTextField(
                                value = formatNumberWithSeparators(montoTemporal),
                                onValueChange = {
                                    val cleaned = cleanNumberFormat(it)
                                    montoTemporal = cleaned
                                },
                                label = { Text("Monto presupuesto") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            Text(
                                text = "Sin presupuesto",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Porcentaje y botones de acción
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    if (!isEditing) {
                        Text(
                            text = "${String.format("%.1f", presupuestoCompleto.porcentajeGastado)}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorEstado
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (presupuesto != null) {
                            if (isEditing) {
                                IconButton(
                                    onClick = {
                                        val monto = montoTemporal.toDoubleOrNull()
                                        if (monto != null && monto > 0) {
                                            onSave(monto)
                                            isEditing = false
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Guardar",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { 
                                        isEditing = false
                                        montoTemporal = presupuesto.monto.toString()
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Cancelar",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { 
                                        isEditing = true
                                        montoTemporal = presupuesto.monto.toString()
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = { 
                                    isEditing = true
                                    montoTemporal = ""
                                },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Agregar presupuesto",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            // Gráfico de barras solo si no está editando
            if (!isEditing && presupuesto != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                PresupuestoProgressBar(
                    porcentaje = presupuestoCompleto.porcentajeGastado,
                    color = colorEstado,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Estado del presupuesto
                Text(
                    text = when (presupuestoCompleto.estado) {
                        EstadoPresupuesto.NORMAL -> "Normal"
                        EstadoPresupuesto.ADVERTENCIA -> "Advertencia"
                        EstadoPresupuesto.CRITICO -> "Crítico"
                        EstadoPresupuesto.EXCEDIDO -> "Excedido"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorEstado,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Botones de acción cuando está editando y no hay presupuesto previo
            if (presupuesto == null && isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val monto = montoTemporal.toDoubleOrNull()
                            if (monto != null && monto > 0) {
                                onSave(monto)
                                isEditing = false
                            }
                        },
                        enabled = montoTemporal.toDoubleOrNull() ?: 0.0 > 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Guardar")
                    }
                    OutlinedButton(
                        onClick = { 
                            isEditing = false
                            montoTemporal = ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
    
    // Diálogo de confirmación de eliminación con animación
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
            title = { 
                Text(
                    "Eliminar presupuesto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { 
                Text(
                    "¿Estás seguro de que quieres eliminar el presupuesto de '${presupuestoCompleto.categoria.nombre}'? Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresupuestoDialog(
    categorias: List<Categoria>,
    periodoSeleccionado: String,
    categoriaPreseleccionada: Categoria? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double) -> Unit
) {
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(categoriaPreseleccionada) }
    var monto by remember { mutableStateOf("") } // Asegura que el campo esté vacío por defecto
    var expandedCategoria by remember { mutableStateOf(false) }

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
            onDismissRequest = onDismiss,
            title = {
                Text(
                    "Nuevo Presupuesto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Selector de categoría
                    ExposedDropdownMenuBox(
                        expanded = expandedCategoria,
                        onExpandedChange = { if (categoriaPreseleccionada == null) expandedCategoria = !expandedCategoria }
                    ) {
                        OutlinedTextField(
                            value = categoriaSeleccionada?.nombre ?: "",
                            onValueChange = {},
                            label = { Text("Categoría") },
                            readOnly = true,
                            enabled = categoriaPreseleccionada == null,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { if (categoriaPreseleccionada == null) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        if (categoriaPreseleccionada == null) {
                        ExposedDropdownMenu(
                            expanded = expandedCategoria,
                            onDismissRequest = { expandedCategoria = false }
                        ) {
                            categorias.forEach { categoria ->
                                DropdownMenuItem(
                                    text = { Text(categoria.nombre) },
                                    onClick = {
                                        categoriaSeleccionada = categoria
                                        expandedCategoria = false
                                    }
                                )
                                }
                            }
                        }
                    }
                    
                    // Campo de monto
                    OutlinedTextField(
                        value = formatNumberWithSeparators(monto),
                        onValueChange = { 
                            val cleaned = cleanNumberFormat(it)
                            monto = cleaned
                        },
                        label = { Text("Monto") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    // Información del período
                    Text(
                        text = "Período: $periodoSeleccionado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val montoDouble = monto.toDoubleOrNull()
                        if (categoriaSeleccionada != null && montoDouble != null && montoDouble > 0) {
                            onConfirm(categoriaSeleccionada!!.id, montoDouble)
                        }
                    },
                    enabled = categoriaSeleccionada != null && monto.toDoubleOrNull() ?: 0.0 > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresupuestoEditDialog(
    presupuesto: PresupuestoCategoriaEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var monto by remember { mutableStateOf(presupuesto.monto.toString()) }

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
            onDismissRequest = onDismiss,
            title = {
                Text(
                    "Editar Presupuesto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = formatNumberWithSeparators(monto),
                        onValueChange = { 
                            val cleaned = cleanNumberFormat(it)
                            monto = cleaned
                        },
                        label = { Text("Monto") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val montoDouble = monto.toDoubleOrNull()
                        if (montoDouble != null && montoDouble > 0) {
                            onConfirm(montoDouble)
                        }
                    },
                    enabled = monto.toDoubleOrNull() ?: 0.0 > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun PresupuestosPeriodoSelectorDialog(
    periodos: List<String>,
    periodoSeleccionado: String,
    onDismiss: () -> Unit,
    onPeriodoSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Seleccionar Período",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
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
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Cerrar")
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun PresupuestosScreenPreview() {
    MaterialTheme {
        PresupuestosScreen(
            navController = rememberNavController(),
            viewModel = hiltViewModel(),
            periodoGlobalViewModel = hiltViewModel()
        )
    }
}

// Función para formatear números con separadores de miles
private fun formatNumberWithSeparators(value: String): String {
    return try {
        val number = value.toLongOrNull() ?: 0L
        NumberFormat.getNumberInstance(Locale("es", "CL")).format(number)
    } catch (e: Exception) {
        value
    }
}

// Función para limpiar formato y obtener solo números
private fun cleanNumberFormat(value: String): String {
    return value.replace(Regex("[^\\d]"), "")
} 