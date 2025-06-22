package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.clickable
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
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasViewModel
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasUiState
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.*
import java.text.NumberFormat
import com.aranthalion.controlfinanzas.data.util.ExcelProcessor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesScreen(
    navController: NavHostController,
    viewModel: MovimientosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totales by viewModel.totales.collectAsState()
    val categoriasViewModel: CategoriasViewModel = hiltViewModel()
    val categoriasUiState by categoriasViewModel.uiState.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showFiltroDialog by remember { mutableStateOf(false) }
    var filtroTipoSeleccionado by remember { mutableStateOf("Todos") }
    var filtroPeriodoSeleccionado by remember { mutableStateOf("Todos") }
    var filtroFechaSeleccionada by remember { mutableStateOf<Date?>(null) }
    var movimientoAEditar by remember { mutableStateOf<MovimientoEntity?>(null) }
    var expandedPeriodo by remember { mutableStateOf(false) }
    var expandedTipo by remember { mutableStateOf(false) }

    val tipos = listOf("Todos", "Ingresos", "Gastos")
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, 1)
    val periodos = (0..12).map { offset ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.MONTH, -offset)
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        "$year-$month"
    }.toMutableList().apply { add(0, "Todos") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Transacciones",
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
                    IconButton(onClick = { showFiltroDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Filtrar")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar transacción")
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
                Icon(Icons.Default.Add, contentDescription = "Agregar transacción")
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
                    val categorias = (uiState as MovimientosUiState.Success).categorias
                    
                    // Aplicar filtros
                    val movimientosFiltrados = movimientos.filter { movimiento ->
                        val cumpleTipo = when (filtroTipoSeleccionado) {
                            "Ingresos" -> movimiento.tipo == "INGRESO"
                            "Gastos" -> movimiento.tipo == "GASTO"
                            else -> true
                        }
                        
                        val cumplePeriodo = if (filtroPeriodoSeleccionado != "Todos") {
                            movimiento.periodoFacturacion == filtroPeriodoSeleccionado
                        } else true
                        
                        val cumpleFecha = filtroFechaSeleccionada?.let { fecha ->
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            dateFormat.format(movimiento.fecha) == dateFormat.format(fecha)
                        } ?: true
                        
                        cumpleTipo && cumplePeriodo && cumpleFecha
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Resumen de filtros aplicados
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
                                        text = "Resumen",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Total Ingresos: ${FormatUtils.formatMoneyCLP(totales.ingresos)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Total Gastos: ${FormatUtils.formatMoneyCLP(totales.gastos)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Balance: ${FormatUtils.formatMoneyCLP(totales.balance)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Transacciones mostradas: ${movimientosFiltrados.size}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Filtros activos
                        if (filtroTipoSeleccionado != "Todos" || filtroPeriodoSeleccionado != "Todos" || filtroFechaSeleccionada != null) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = "Filtros activos:",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (filtroTipoSeleccionado != "Todos") {
                                                AssistChip(
                                                    onClick = { filtroTipoSeleccionado = "Todos" },
                                                    label = { Text(filtroTipoSeleccionado) },
                                                    trailingIcon = { Icon(Icons.Default.Close, null) }
                                                )
                                            }
                                            if (filtroPeriodoSeleccionado != "Todos") {
                                                AssistChip(
                                                    onClick = { filtroPeriodoSeleccionado = "Todos" },
                                                    label = { Text(filtroPeriodoSeleccionado) },
                                                    trailingIcon = { Icon(Icons.Default.Close, null) }
                                                )
                                            }
                                            if (filtroFechaSeleccionada != null) {
                                                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                                AssistChip(
                                                    onClick = { filtroFechaSeleccionada = null },
                                                    label = { Text(dateFormat.format(filtroFechaSeleccionada!!)) },
                                                    trailingIcon = { Icon(Icons.Default.Close, null) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        items(movimientosFiltrados) { movimiento ->
                            TransaccionItem(
                                movimiento = movimiento,
                                categorias = categorias,
                                onEdit = { movimientoAEditar = it },
                                onDelete = { viewModel.eliminarMovimiento(it) }
                            )
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

    // Diálogo de filtros
    if (showFiltroDialog) {
        AlertDialog(
            onDismissRequest = { showFiltroDialog = false },
            title = {
                Text(
                    "Filtrar Transacciones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Filtro por tipo
                    Text(
                        text = "Tipo de transacción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    tipos.forEach { tipo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = filtroTipoSeleccionado == tipo,
                                onClick = { filtroTipoSeleccionado = tipo }
                            )
                            Text(
                                text = tipo,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    
                    Divider()
                    
                    // Filtro por periodo
                    Text(
                        text = "Periodo de facturación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    periodos.take(6).forEach { periodo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = filtroPeriodoSeleccionado == periodo,
                                onClick = { filtroPeriodoSeleccionado = periodo }
                            )
                            Text(
                                text = periodo,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFiltroDialog = false }) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    filtroTipoSeleccionado = "Todos"
                    filtroPeriodoSeleccionado = "Todos"
                    filtroFechaSeleccionada = null
                    showFiltroDialog = false 
                }) {
                    Text("Limpiar")
                }
            }
        )
    }

    // Diálogo para agregar nueva transacción
    if (showAddDialog) {
        val categorias = when (categoriasUiState) {
            is CategoriasUiState.Success -> {
                val domainCategorias = (categoriasUiState as CategoriasUiState.Success).categorias
                domainCategorias.map { domainCategoria ->
                    Categoria(
                        id = domainCategoria.id.toLong(),
                        nombre = domainCategoria.nombre,
                        descripcion = domainCategoria.descripcion,
                        tipo = "GASTO"
                    )
                }
            }
            else -> emptyList()
        }
        
        TransaccionDialog(
            showDialog = showAddDialog,
            onDismiss = { showAddDialog = false },
            onConfirm = { tipo, monto, descripcion, periodo, categoriaId ->
                val fecha = Date()
                val nuevoMovimiento = MovimientoEntity(
                    tipo = tipo,
                    monto = monto,
                    descripcion = descripcion,
                    fecha = fecha,
                    periodoFacturacion = periodo,
                    categoriaId = categoriaId,
                    idUnico = ExcelProcessor.generarIdUnico(fecha, monto, descripcion)
                )
                viewModel.agregarMovimiento(nuevoMovimiento)
                showAddDialog = false
            },
            categorias = categorias
        )
    }

    // Diálogo para editar transacción
    movimientoAEditar?.let { movimiento ->
        TransaccionEditDialog(
            movimiento = movimiento,
            categorias = (uiState as? MovimientosUiState.Success)?.categorias ?: emptyList(),
            onDismiss = { movimientoAEditar = null },
            onConfirm = { movimientoEditado ->
                viewModel.actualizarMovimiento(movimientoEditado)
                movimientoAEditar = null
            }
        )
    }
}

@Composable
private fun TransaccionItem(
    movimiento: MovimientoEntity,
    categorias: List<Categoria>,
    onEdit: (MovimientoEntity) -> Unit,
    onDelete: (MovimientoEntity) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val categoria = categorias.find { it.id == movimiento.categoriaId }
    val formattedDate = remember(movimiento.fecha) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(movimiento.fecha)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(movimiento) },
        colors = CardDefaults.cardColors(
            containerColor = if (movimiento.categoriaId == null) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
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
                        text = categoria?.nombre ?: "Sin categoría",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (movimiento.categoriaId == null) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    if (movimiento.descripcion.isNotEmpty()) {
                        Text(
                            text = movimiento.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "• ${movimiento.periodoFacturacion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (movimiento.tipoTarjeta != null) {
                            Text(
                                text = "• ${movimiento.tipoTarjeta}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = FormatUtils.formatMoneyCLP(movimiento.monto),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (movimiento.tipo == "INGRESO") 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        modifier = Modifier.widthIn(min = 80.dp)
                    )
                    
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Eliminar Transacción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que quieres eliminar esta transacción?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(movimiento)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransaccionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, Long?) -> Unit,
    categorias: List<Categoria>
) {
    if (!showDialog) return
    
    var monto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(null) }
    var tipoSeleccionado by remember { mutableStateOf("GASTO") }
    var expandedPeriodo by remember { mutableStateOf(false) }
    var periodoSeleccionado by remember { mutableStateOf("") }
    
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, 1)
    val periodos = (0..12).map { offset ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.MONTH, -offset)
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        "$year-$month"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Nueva Transacción",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tipo de transacción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tipoSeleccionado == "GASTO",
                            onClick = { tipoSeleccionado = "GASTO" }
                        )
                        Text("Gasto")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tipoSeleccionado == "INGRESO",
                            onClick = { tipoSeleccionado = "INGRESO" }
                        )
                        Text("Ingreso")
                    }
                }
                
                // Monto
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Periodo de facturación
                ExposedDropdownMenuBox(
                    expanded = expandedPeriodo,
                    onExpandedChange = { expandedPeriodo = !expandedPeriodo }
                ) {
                    OutlinedTextField(
                        value = periodoSeleccionado,
                        onValueChange = {},
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
                                    periodoSeleccionado = periodo
                                    expandedPeriodo = false
                                }
                            )
                        }
                    }
                }
                
                // Categoría (opcional)
                if (categorias.isNotEmpty()) {
                    var expandedCategoria by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCategoria,
                        onExpandedChange = { expandedCategoria = !expandedCategoria }
                    ) {
                        OutlinedTextField(
                            value = categoriaSeleccionada?.nombre ?: "Sin categoría",
                            onValueChange = {},
                            label = { Text("Categoría (opcional)") },
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategoria,
                            onDismissRequest = { expandedCategoria = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sin categoría") },
                                onClick = {
                                    categoriaSeleccionada = null
                                    expandedCategoria = false
                                }
                            )
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val montoDouble = monto.toDoubleOrNull() ?: 0.0
                    if (montoDouble > 0 && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()) {
                        onConfirm(
                            tipoSeleccionado,
                            montoDouble,
                            descripcion,
                            periodoSeleccionado,
                            categoriaSeleccionada?.id
                        )
                    }
                },
                enabled = monto.toDoubleOrNull() ?: 0.0 > 0 && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransaccionEditDialog(
    movimiento: MovimientoEntity,
    categorias: List<Categoria>,
    onDismiss: () -> Unit,
    onConfirm: (MovimientoEntity) -> Unit
) {
    var monto by remember { mutableStateOf(movimiento.monto.toString()) }
    var descripcion by remember { mutableStateOf(movimiento.descripcion) }
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(categorias.find { it.id == movimiento.categoriaId }) }
    var tipoSeleccionado by remember { mutableStateOf(movimiento.tipo) }
    var expandedPeriodo by remember { mutableStateOf(false) }
    var periodoSeleccionado by remember { mutableStateOf(movimiento.periodoFacturacion) }
    
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, 1)
    val periodos = (0..12).map { offset ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.MONTH, -offset)
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        "$year-$month"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Editar Transacción",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tipo de transacción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tipoSeleccionado == "GASTO",
                            onClick = { tipoSeleccionado = "GASTO" }
                        )
                        Text("Gasto")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tipoSeleccionado == "INGRESO",
                            onClick = { tipoSeleccionado = "INGRESO" }
                        )
                        Text("Ingreso")
                    }
                }
                
                // Monto
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Periodo de facturación
                ExposedDropdownMenuBox(
                    expanded = expandedPeriodo,
                    onExpandedChange = { expandedPeriodo = !expandedPeriodo }
                ) {
                    OutlinedTextField(
                        value = periodoSeleccionado,
                        onValueChange = {},
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
                                    periodoSeleccionado = periodo
                                    expandedPeriodo = false
                                }
                            )
                        }
                    }
                }
                
                // Categoría
                if (categorias.isNotEmpty()) {
                    var expandedCategoria by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCategoria,
                        onExpandedChange = { expandedCategoria = !expandedCategoria }
                    ) {
                        OutlinedTextField(
                            value = categoriaSeleccionada?.nombre ?: "Sin categoría",
                            onValueChange = {},
                            label = { Text("Categoría") },
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategoria,
                            onDismissRequest = { expandedCategoria = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sin categoría") },
                                onClick = {
                                    categoriaSeleccionada = null
                                    expandedCategoria = false
                                }
                            )
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val montoDouble = monto.toDoubleOrNull() ?: 0.0
                    if (montoDouble > 0 && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()) {
                        val movimientoEditado = movimiento.copy(
                            tipo = tipoSeleccionado,
                            monto = montoDouble,
                            descripcion = descripcion,
                            periodoFacturacion = periodoSeleccionado,
                            categoriaId = categoriaSeleccionada?.id
                        )
                        onConfirm(movimientoEditado)
                    }
                },
                enabled = monto.toDoubleOrNull() ?: 0.0 > 0 && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()
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
} 