package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasViewModel
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasUiState
import java.text.SimpleDateFormat
import java.util.*
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosScreen(
    navController: NavHostController,
    viewModel: MovimientosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categoriasViewModel: CategoriasViewModel = hiltViewModel()
    val categoriasUiState by categoriasViewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var monto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(null) }
    var tipoSeleccionado by remember { mutableStateOf("GASTO") }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Movimientos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar movimiento")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar movimiento")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is MovimientosUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is MovimientosUiState.Success -> {
                    val movimientos = (uiState as MovimientosUiState.Success).movimientos
                    // Agrupar por periodo de facturación y ordenar descendente
                    val movimientosPorPeriodo = movimientos.groupBy { it.periodoFacturacion }
                        .toSortedMap(compareByDescending { it })
                    val periodos = movimientosPorPeriodo.keys.take(4)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        periodos.forEach { periodo ->
                            val lista = movimientosPorPeriodo[periodo] ?: emptyList()
                            item {
                                Text(
                                    text = "Período: $periodo",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(lista) { movimiento ->
                                MovimientoItemMovimientos(movimiento = movimiento, viewModel = viewModel)
                            }
                        }
                    }
                }
                is MovimientosUiState.Error -> {
                    Text(
                        text = (uiState as MovimientosUiState.Error).mensaje,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        val categorias = when (categoriasUiState) {
            is CategoriasUiState.Success -> {
                val domainCategorias = (categoriasUiState as CategoriasUiState.Success).categorias
                domainCategorias.map { domainCategoria ->
                    Categoria(
                        id = domainCategoria.id.toLong(),
                        nombre = domainCategoria.nombre,
                        descripcion = domainCategoria.descripcion,
                        tipo = "GASTO" // Por defecto
                    )
                }
            }
            else -> emptyList()
        }
        
        MovimientoDialog(
            showDialog = showAddDialog,
            onDismiss = { showAddDialog = false },
            onConfirm = { tipo, monto, descripcion, periodo, categoriaId ->
                val nuevoMovimiento = MovimientoEntity(
                    tipo = tipo,
                    monto = monto,
                    descripcion = descripcion,
                    fecha = Date(),
                    periodoFacturacion = periodo,
                    categoriaId = categoriaId
                )
                viewModel.agregarMovimiento(nuevoMovimiento)
                showAddDialog = false
            },
            categorias = categorias
        )
    }
}

@Composable
private fun MovimientoItemMovimientos(movimiento: MovimientoEntity, viewModel: MovimientosViewModel) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    // Formato personalizado para montos
    val formatearMonto = remember { { monto: Double ->
        // Mostrar directamente como string sin formato
        monto.toLong().toString()
    }}
    var showEditCategoria by remember { mutableStateOf(false) }
    val categoriasViewModel: CategoriasViewModel = hiltViewModel()
    val categoriasUiState by categoriasViewModel.uiState.collectAsState()
    val categorias = when (categoriasUiState) {
        is CategoriasUiState.Success -> (categoriasUiState as CategoriasUiState.Success).categorias
        else -> emptyList()
    }
    val categoriaNombre = categorias.find { it.id.toLong() == movimiento.categoriaId }?.nombre ?: "Sin categoría"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showEditCategoria = true },
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
            Column {
                Text(
                    text = movimiento.descripcion,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = categoriaNombre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = dateFormat.format(movimiento.fecha),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (!movimiento.tipoTarjeta.isNullOrBlank()) {
                    Text(
                        text = "Tarjeta: ${movimiento.tipoTarjeta}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "$${formatearMonto(movimiento.monto)}",
                style = MaterialTheme.typography.titleMedium,
                color = if (movimiento.tipo == "INGRESO") 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 80.dp)
            )
        }
    }
    if (showEditCategoria) {
        EditarCategoriaDialog(movimiento = movimiento, onDismiss = { showEditCategoria = false }, viewModel = viewModel)
    }
}

@Composable
fun EditarCategoriaDialog(movimiento: MovimientoEntity, onDismiss: () -> Unit, viewModel: MovimientosViewModel) {
    val categoriasViewModel: CategoriasViewModel = hiltViewModel()
    val categoriasUiState by categoriasViewModel.uiState.collectAsState()
    var categoriaSeleccionada by remember { mutableStateOf<Long?>(movimiento.categoriaId) }
    val categorias = when (categoriasUiState) {
        is CategoriasUiState.Success -> (categoriasUiState as CategoriasUiState.Success).categorias
        else -> emptyList()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Categoría") },
        text = {
            Box(modifier = Modifier.heightIn(max = 300.dp)) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(categorias) { categoria ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoriaSeleccionada = categoria.id.toLong() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = categoriaSeleccionada == categoria.id.toLong(),
                                onClick = { categoriaSeleccionada = categoria.id.toLong() }
                            )
                            Text(text = categoria.nombre)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.actualizarMovimiento(
                    movimiento.copy(categoriaId = categoriaSeleccionada)
                )
                onDismiss()
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientoDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, Long?) -> Unit,
    categorias: List<Categoria>
) {
    if (showDialog) {
        var tipoSeleccionado by remember { mutableStateOf("GASTO") }
        var monto by remember { mutableStateOf("") }
        var descripcion by remember { mutableStateOf("") }
        var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(null) }
        var showCategoriaDialog by remember { mutableStateOf(false) }
        var periodoFacturacion by remember { mutableStateOf("") }
        // Calcular los últimos 3 meses
        val calendar = Calendar.getInstance()
        val periodos = (0..2).map { offset ->
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.MONTH, -offset)
            val year = cal.get(Calendar.YEAR)
            val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            "$year-$month"
        }
        var expandedPeriodo by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    "Nuevo Movimiento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Selector de tipo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = tipoSeleccionado == "GASTO",
                            onClick = { tipoSeleccionado = "GASTO" }
                        )
                        Text("Gasto")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = tipoSeleccionado == "INGRESO",
                            onClick = { tipoSeleccionado = "INGRESO" }
                        )
                        Text("Ingreso")
                    }

                    // Campo de monto
                    OutlinedTextField(
                        value = monto,
                        onValueChange = { monto = it },
                        label = { Text("Monto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Campo de descripción
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Selector de período de facturación
                    ExposedDropdownMenuBox(
                        expanded = expandedPeriodo,
                        onExpandedChange = { expandedPeriodo = !expandedPeriodo }
                    ) {
                        OutlinedTextField(
                            value = periodoFacturacion,
                            onValueChange = { },
                            label = { Text("Período de Facturación") },
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriodo) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPeriodo,
                            onDismissRequest = { expandedPeriodo = false }
                        ) {
                            periodos.forEach { periodo ->
                                DropdownMenuItem(
                                    text = { Text(periodo) },
                                    onClick = {
                                        periodoFacturacion = periodo
                                        expandedPeriodo = false
                                    }
                                )
                            }
                        }
                    }

                    // Selector de categoría
                    OutlinedTextField(
                        value = categoriaSeleccionada?.nombre ?: "Seleccionar categoría",
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showCategoriaDialog = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Seleccionar categoría")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val montoDouble = monto.toDoubleOrNull() ?: 0.0
                        onConfirm(
                            tipoSeleccionado,
                            montoDouble,
                            descripcion,
                            periodoFacturacion,
                            categoriaSeleccionada?.id
                        )
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )

        // Diálogo para seleccionar categoría
        if (showCategoriaDialog) {
            AlertDialog(
                onDismissRequest = { showCategoriaDialog = false },
                title = {
                    Text(
                        "Seleccionar Categoría",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    LazyColumn {
                        items(categorias) { categoria ->
                            ListItem(
                                headlineContent = { Text(categoria.nombre) },
                                leadingContent = {
                                    Icon(
                                        imageVector = if (categoria.tipo == "GASTO") 
                                            Icons.Filled.Delete else Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = if (categoria.tipo == "GASTO") 
                                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.clickable {
                                    categoriaSeleccionada = categoria
                                    showCategoriaDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCategoriaDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

enum class TipoMovimiento {
    INGRESO, GASTO
}

data class Movimiento(
    val monto: Double,
    val descripcion: String,
    val categoria: String,
    val tipo: TipoMovimiento,
    val fecha: Date
) 