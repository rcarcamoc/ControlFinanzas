package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorDialog
import com.aranthalion.controlfinanzas.presentation.components.CustomIcons
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
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasViewModel
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasUiState
import com.aranthalion.controlfinanzas.presentation.screens.PresupuestosViewModel
import com.aranthalion.controlfinanzas.presentation.screens.PresupuestosUiState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresupuestosYCategoriasScreen(
    navController: NavHostController,
    presupuestosViewModel: PresupuestosViewModel = hiltViewModel(),
    categoriasViewModel: CategoriasViewModel = hiltViewModel(),
    periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
) {
    val presupuestosUiState by presupuestosViewModel.uiState.collectAsState()
    val categoriasUiState by categoriasViewModel.uiState.collectAsState()
    val periodoSeleccionado by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    val periodosDisponibles by periodoGlobalViewModel.periodosDisponibles.collectAsState()
    val categorias by presupuestosViewModel.categorias.collectAsState()
    val presupuestosPorCategoria by presupuestosViewModel.presupuestosPorCategoria.collectAsState()
    val presupuestosCompletos by presupuestosViewModel.presupuestosCompletos.collectAsState()
    val resumen by presupuestosViewModel.resumen.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showAddPresupuestoDialog by remember { mutableStateOf(false) }
    var showAddCategoriaDialog by remember { mutableStateOf(false) }
    var presupuestoToEdit by remember { mutableStateOf<PresupuestoCategoria?>(null) }
    var categoriaToEdit by remember { mutableStateOf<Categoria?>(null) }
    var showPeriodoSelector by remember { mutableStateOf(false) }
    var showConfirmacionHistorica by remember { mutableStateOf(false) }
    var categoriaPendienteConfirmacion by remember { mutableStateOf<Categoria?>(null) }
    var aplicarAHistorico by remember { mutableStateOf(false) }
    var categoriaParaPresupuesto by remember { mutableStateOf<Categoria?>(null) }
    
    // Cuando cambie el período seleccionado, aplicar lazy copy si es necesario antes de cargar los presupuestos
    LaunchedEffect(periodoSeleccionado) {
        presupuestosViewModel.cargarPresupuestos(periodoSeleccionado)
    }
    
    // Cuando cambien las categorías, aplicar lazy copy solo si es necesario
    LaunchedEffect(categorias, periodoSeleccionado) {
        println("🔍 LAUNCHED_EFFECT: Categorías o período cambiaron - Categorías: ${categorias.size}, Período: $periodoSeleccionado")
        
        // Solo ejecutar si hay categorías y no se ha ejecutado recientemente
        if (categorias.isNotEmpty()) {
            val presupuestosActuales = presupuestosPorCategoria.values.toList()
            println("🔍 LAUNCHED_EFFECT: Presupuestos actuales en memoria: ${presupuestosActuales.size}")
            
            // Solo aplicar lazy copy si realmente no hay presupuestos para el período actual
            if (presupuestosActuales.isEmpty()) {
                println("🔍 LAUNCHED_EFFECT: Aplicando lazy copy para ${categorias.size} categorías en período $periodoSeleccionado")
                categorias.forEach { categoria ->
                    println("🔍 LAUNCHED_EFFECT: Procesando categoría: ${categoria.nombre} (ID: ${categoria.id})")
                    presupuestosViewModel.lazyCopyPresupuestoSiNoExiste(categoria.id, periodoSeleccionado)
                }
                // Recargar presupuestos después de aplicar lazy copy
                kotlinx.coroutines.delay(200)
                println("🔍 LAUNCHED_EFFECT: Recargando presupuestos después de lazy copy")
                presupuestosViewModel.cargarPresupuestos(periodoSeleccionado)
            } else {
                println("🔍 LAUNCHED_EFFECT: Ya existen presupuestos para el período $periodoSeleccionado, saltando lazy copy")
            }
        } else {
            println("🔍 LAUNCHED_EFFECT: No hay categorías disponibles")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header unificado
        item {
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
                    Text(
                        "Presupuestos y Categorías",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Gestiona todo en una sola vista",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Botones de acción responsive
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp.dp
                    val isSmallScreen = screenWidth < 600.dp
                    
                    if (isSmallScreen) {
                        // En pantallas pequeñas, apilar verticalmente
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPeriodoSelector = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    CustomIcons.DateRange,
                                    contentDescription = "Período",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Período", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = { showAddCategoriaDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    CustomIcons.Add,
                                    contentDescription = "Nueva categoría",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Categoría", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        // En pantallas grandes, alinear horizontalmente
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPeriodoSelector = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    CustomIcons.DateRange,
                                    contentDescription = "Período",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Período", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = { showAddCategoriaDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    CustomIcons.Add,
                                    contentDescription = "Nueva categoría",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Categoría", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // Resumen de presupuestos
        if (resumen != null) {
            item {
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
                        
                        // Grid de métricas
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(120.dp) // Altura fija para evitar scroll anidado
                        ) {
                            item {
                                MetricCard(
                                    title = "Presupuesto",
                                    value = FormatUtils.formatMoneyCLP(resumen!!.totalPresupuestado),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            item {
                                MetricCard(
                                    title = "Gastado",
                                    value = FormatUtils.formatMoneyCLP(resumen!!.totalGastado),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            item {
                                MetricCard(
                                    title = "Restante",
                                    value = FormatUtils.formatMoneyCLP(resumen!!.totalPresupuestado - resumen!!.totalGastado),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        PresupuestoProgressBar(
                            porcentaje = if (resumen!!.totalPresupuestado > 0) 
                                (resumen!!.totalGastado / resumen!!.totalPresupuestado) * 100 
                            else 0.0,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Contenido principal - Grid de categorías con presupuestos
        item {
            when {
                presupuestosUiState is PresupuestosUiState.Loading || categoriasUiState is CategoriasUiState.Loading -> {
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
                                    "Cargando...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                presupuestosUiState is PresupuestosUiState.Success && categoriasUiState is CategoriasUiState.Success -> {
                    val categoriasList = (categoriasUiState as CategoriasUiState.Success).categorias
                    
                    if (categoriasList.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.List,
                            title = "No hay categorías",
                            description = "Crea tu primera categoría para organizar tus gastos",
                            actionText = "Crear Categoría",
                            onAction = { showAddCategoriaDialog = true }
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 280.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .navigationBarsPadding()
                                .heightIn(max = 800.dp) // Limitar altura para evitar scroll anidado
                        ) {
                            items(categoriasList) { categoria ->
                                CategoriaPresupuestoCard(
                                    categoria = categoria,
                                    presupuesto = presupuestosCompletos.find { it.categoria.id == categoria.id },
                                    onEditCategoria = { categoriaToEdit = categoria },
                                    onDeleteCategoria = { 
                                        scope.launch {
                                            categoriasViewModel.eliminarCategoria(categoria)
                                        }
                                    },
                                    onEditPresupuesto = { presupuesto ->
                                        presupuestoToEdit = presupuesto
                                    },
                                    onAddPresupuesto = { 
                                        categoriaParaPresupuesto = categoria
                                        showAddPresupuestoDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
                presupuestosUiState is PresupuestosUiState.Error -> {
                    ErrorCard(
                        title = "Error al cargar presupuestos",
                        message = (presupuestosUiState as PresupuestosUiState.Error).mensaje
                    )
                }
                categoriasUiState is CategoriasUiState.Error -> {
                    ErrorCard(
                        title = "Error al cargar categorías",
                        message = (categoriasUiState as CategoriasUiState.Error).mensaje
                    )
                }
            }
        }
    }

    // Diálogos
    if (showAddPresupuestoDialog) {
        PresupuestoDialog(
            categorias = categorias,
            periodoSeleccionado = periodoSeleccionado,
            categoriaPreseleccionada = categoriaParaPresupuesto,
            onDismiss = {
                showAddPresupuestoDialog = false
                categoriaParaPresupuesto = null
            },
            onConfirm = { categoriaId, monto ->
                scope.launch {
                    presupuestosViewModel.guardarPresupuesto(categoriaId, monto, periodoSeleccionado)
                    showAddPresupuestoDialog = false
                    categoriaParaPresupuesto = null
                }
            }
        )
    }

    if (showAddCategoriaDialog) {
        CategoriaDialog(
            onDismiss = { showAddCategoriaDialog = false },
            onConfirm = { nombre, tipo, presupuesto, activarPresupuesto ->
                scope.launch {
                    // Crear la categoría
                    categoriasViewModel.agregarCategoria(nombre, tipo)
                    
                    // Si se activó asignar presupuesto, crear automáticamente el presupuesto para esta categoría
                    if (activarPresupuesto && presupuesto > 0) {
                        // Esperar un poco para que la categoría se cree antes de asignar el presupuesto
                        kotlinx.coroutines.delay(200)
                        
                        // Buscar la categoría recién creada para obtener su ID
                        val categoriasActuales = (categoriasUiState as? CategoriasUiState.Success)?.categorias ?: emptyList()
                        val categoriaCreada = categoriasActuales.find { it.nombre == nombre.trim().lowercase() }
                        
                        categoriaCreada?.let { categoria ->
                            presupuestosViewModel.guardarPresupuesto(categoria.id, presupuesto, periodoSeleccionado)
                        }
                    }
                    
                    showAddCategoriaDialog = false
                }
            }
        )
    }

    if (presupuestoToEdit != null) {
        PresupuestoEditDialog(
            presupuesto = PresupuestoCategoriaEntity(
                id = presupuestoToEdit!!.categoria.id,
                categoriaId = presupuestoToEdit!!.categoria.id,
                monto = presupuestoToEdit!!.presupuesto,
                periodo = periodoSeleccionado
            ),
            onDismiss = { presupuestoToEdit = null },
            onConfirm = { monto ->
                scope.launch {
                    presupuestoToEdit?.let { presupuesto ->
                        presupuestosViewModel.guardarPresupuesto(presupuesto.categoria.id, monto, periodoSeleccionado)
                    }
                    presupuestoToEdit = null
                }
            }
        )
    }

    if (categoriaToEdit != null) {
        CategoriaDialog(
            categoria = categoriaToEdit,
            onDismiss = { categoriaToEdit = null },
            onConfirm = { nombre, tipo, presupuesto, activarPresupuesto ->
                scope.launch {
                    categoriaToEdit?.let { categoria ->
                        val categoriaActualizada = categoria.copy(nombre = nombre, tipo = tipo)
                        categoriasViewModel.actualizarCategoria(categoriaActualizada)
                        // Si se activa presupuesto, actualizar o crear presupuesto
                        if (activarPresupuesto && presupuesto > 0) {
                            presupuestosViewModel.guardarPresupuesto(categoriaActualizada.id, presupuesto, periodoSeleccionado)
                        }
                    }
                    categoriaToEdit = null
                }
            }
        )
    }

    if (showPeriodoSelector) {
        PeriodoSelectorDialog(
            isVisible = true,
            onDismiss = { showPeriodoSelector = false },
            onConfirm = { startDate, endDate ->
                // Convertir las fechas a formato de período (YYYY-MM)
                val periodo = "${startDate.year}-${startDate.monthValue.toString().padStart(2, '0')}"
                periodoGlobalViewModel.cambiarPeriodo(periodo)
                showPeriodoSelector = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaRow(
    categoria: Categoria,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = categoria.tipo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaDialog(
    categoria: Categoria? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Boolean) -> Unit
) {
    var nombre by remember { mutableStateOf(categoria?.nombre ?: "") }
    var tipoSeleccionado by remember { mutableStateOf(categoria?.tipo ?: "Gasto") }
    var expandedTipo by remember { mutableStateOf(false) }
    var presupuesto by remember { mutableStateOf(0.0) }
    var activarPresupuesto by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (categoria == null) "Nueva Categoría" else "Editar Categoría",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la categoría") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = tipoSeleccionado,
                        onValueChange = {},
                        label = { Text("Tipo") },
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        listOf("Gasto", "Ingreso").forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo) },
                                onClick = {
                                    tipoSeleccionado = tipo
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }
                
                // Campo de presupuesto opcional
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = activarPresupuesto, onCheckedChange = { activarPresupuesto = it })
                    Text("Asignar presupuesto mensual", modifier = Modifier.padding(start = 8.dp))
                }
                if (activarPresupuesto) {
                    OutlinedTextField(
                        value = if (presupuesto == 0.0) "" else presupuesto.toString(),
                        onValueChange = { presupuesto = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("Presupuesto mensual") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nombre, tipoSeleccionado, presupuesto, activarPresupuesto) },
                enabled = nombre.isNotEmpty()
            ) {
                Text(if (categoria == null) "Crear" else "Actualizar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun ErrorCard(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaPresupuestoCard(
    categoria: Categoria,
    presupuesto: PresupuestoCategoria?,
    onEditCategoria: () -> Unit,
    onDeleteCategoria: () -> Unit,
    onEditPresupuesto: (PresupuestoCategoria) -> Unit,
    onAddPresupuesto: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header de la categoría
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoria.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (categoria.descripcion.isNotEmpty()) {
                        Text(
                            text = categoria.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Botones de acción de categoría
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditCategoria,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            CustomIcons.Edit,
                            contentDescription = "Editar categoría",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDeleteCategoria,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            CustomIcons.Delete,
                            contentDescription = "Eliminar categoría",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Información del presupuesto
            if (presupuesto != null) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Presupuesto",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                FormatUtils.formatMoneyCLP(presupuesto.presupuesto),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        IconButton(
                            onClick = { onEditPresupuesto(presupuesto) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                CustomIcons.Edit,
                                contentDescription = "Editar presupuesto",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Barra de progreso del presupuesto
                    LinearProgressIndicator(
                        progress = if (presupuesto.presupuesto > 0) {
                            (presupuesto.gastoActual / presupuesto.presupuesto).toFloat().coerceIn(0f, 1f)
                        } else 0f,
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            presupuesto.gastoActual <= presupuesto.presupuesto * 0.8 -> MaterialTheme.colorScheme.primary
                            presupuesto.gastoActual <= presupuesto.presupuesto * 0.9 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        "Gastado: ${FormatUtils.formatMoneyCLP(presupuesto.gastoActual)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Sin presupuesto asignado
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onAddPresupuesto,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            CustomIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Asignar Presupuesto")
                    }
                }
            }
        }
    }
}

 