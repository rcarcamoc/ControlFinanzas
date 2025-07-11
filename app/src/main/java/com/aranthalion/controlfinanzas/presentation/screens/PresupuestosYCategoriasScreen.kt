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
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorDialog
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
    
    var selectedTab by remember { mutableStateOf(0) }
    var showAddPresupuestoDialog by remember { mutableStateOf(false) }
    var showAddCategoriaDialog by remember { mutableStateOf(false) }
    var presupuestoToEdit by remember { mutableStateOf<PresupuestoCategoria?>(null) }
    var categoriaToEdit by remember { mutableStateOf<Categoria?>(null) }
    var showPeriodoSelector by remember { mutableStateOf(false) }
    var showConfirmacionHistorica by remember { mutableStateOf(false) }
    var categoriaPendienteConfirmacion by remember { mutableStateOf<Categoria?>(null) }
    var aplicarAHistorico by remember { mutableStateOf(false) }
    
    // Cuando cambie el período seleccionado, aplicar lazy copy si es necesario antes de cargar los presupuestos
    LaunchedEffect(periodoSeleccionado) {
        presupuestosViewModel.cargarPresupuestos(periodoSeleccionado)
    }
    
    // Cuando cambien las categorías, aplicar lazy copy
    LaunchedEffect(categorias) {
        if (categorias.isNotEmpty()) {
            categorias.forEach { categoria ->
                presupuestosViewModel.lazyCopyPresupuestoSiNoExiste(categoria.id, periodoSeleccionado)
            }
            // Recargar presupuestos después de aplicar lazy copy
            kotlinx.coroutines.delay(200)
            presupuestosViewModel.cargarPresupuestos(periodoSeleccionado)
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
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Presupuestos y Categorías",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Gestiona presupuestos y categorías en una sola vista",
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
                            onClick = { 
                                if (selectedTab == 0) {
                                    showAddPresupuestoDialog = true
                                } else {
                                    showAddCategoriaDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = "Agregar",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedTab == 0) "Nuevo Presupuesto" else "Nueva Categoría")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pestañas
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Presupuestos") },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Categorías") },
                        icon = { Icon(Icons.Default.List, contentDescription = null) }
                    )
                }
            }
        }

        // Contenido según la pestaña seleccionada
        when (selectedTab) {
            0 -> {
                // Pestaña de Presupuestos
                when (presupuestosUiState) {
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
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                "Presupuesto Total",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                FormatUtils.formatMoneyCLP(resumen!!.totalPresupuestado),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Column {
                                            Text(
                                                "Gastado",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                FormatUtils.formatMoneyCLP(resumen!!.totalGastado),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Column {
                                            Text(
                                                "Restante",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                FormatUtils.formatMoneyCLP(resumen!!.totalPresupuestado - resumen!!.totalGastado),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    PresupuestoProgressBar(
                                        porcentaje = if (resumen!!.totalPresupuestado > 0) 
                                            (resumen!!.totalGastado / resumen!!.totalPresupuestado) * 100 
                                        else 0.0,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // Lista de presupuestos
                        if (presupuestosCompletos.isNotEmpty()) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(presupuestosCompletos) { presupuesto ->
                                    PresupuestoCard(
                                        presupuesto = presupuesto,
                                        onEditPresupuesto = { categoriaId, presupuestoValue -> 
                                            presupuestoToEdit = presupuesto
                                        }
                                    )
                                }
                            }
                        } else {
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
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "No hay presupuestos",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Crea tu primer presupuesto para controlar tus gastos",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { showAddPresupuestoDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("Crear Presupuesto")
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
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    "Error al cargar presupuestos",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    (presupuestosUiState as PresupuestosUiState.Error).mensaje,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                // Pestaña de Categorías
                when (categoriasUiState) {
                    is CategoriasUiState.Loading -> {
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
                                        "Cargando categorías...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    is CategoriasUiState.Success -> {
                        val categoriasList = (categoriasUiState as CategoriasUiState.Success).categorias
                        
                        if (categoriasList.isEmpty()) {
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
                                        Icons.Default.List,
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
                                        "Crea tu primera categoría para organizar tus gastos",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { showAddCategoriaDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("Crear Categoría")
                                    }
                                }
                            }
                        } else {
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
                                        text = "Categorías (${categoriasList.size})",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(categoriasList) { categoria ->
                                            CategoriaRow(
                                                categoria = categoria,
                                                onEdit = { categoriaToEdit = categoria },
                                                onDelete = { 
                                                    scope.launch {
                                                        categoriasViewModel.eliminarCategoria(categoria)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is CategoriasUiState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    "Error al cargar categorías",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    (categoriasUiState as CategoriasUiState.Error).mensaje,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogos
    if (showAddPresupuestoDialog) {
        PresupuestoDialog(
            categorias = categorias,
            periodoSeleccionado = periodoSeleccionado,
            onDismiss = { showAddPresupuestoDialog = false },
            onConfirm = { categoriaId, monto ->
                scope.launch {
                    presupuestosViewModel.guardarPresupuesto(categoriaId, monto, periodoSeleccionado)
                    showAddPresupuestoDialog = false
                }
            }
        )
    }

    if (showAddCategoriaDialog) {
        CategoriaDialog(
            onDismiss = { showAddCategoriaDialog = false },
            onConfirm = { nombre, tipo, presupuesto, activarPresupuesto ->
                scope.launch {
                    categoriasViewModel.agregarCategoria(nombre, tipo)
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
                        categoriasViewModel.eliminarCategoria(categoria)
                        categoriasViewModel.agregarCategoria(nombre, tipo)
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
                // Convertir fechas a formato de período YYYY-MM
                val nuevoPeriodo = startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                scope.launch {
                    periodoGlobalViewModel.cambiarPeriodo(nuevoPeriodo)
                    showPeriodoSelector = false
                }
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

 