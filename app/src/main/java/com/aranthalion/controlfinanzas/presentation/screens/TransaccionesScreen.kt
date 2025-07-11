package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorGlobal
import com.aranthalion.controlfinanzas.presentation.components.StatCard
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.aranthalion.controlfinanzas.domain.categoria.Categoria as DomainCategoria
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.aranthalion.controlfinanzas.di.ClasificacionUseCaseEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesScreen(
    navController: NavHostController,
    viewModel: MovimientosViewModel = hiltViewModel(),
    periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totales by viewModel.totales.collectAsState()
    val categoriasViewModel: CategoriasViewModel = hiltViewModel()
    val categoriasUiState by categoriasViewModel.uiState.collectAsState()
    val periodoGlobal by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showFiltroDialog by remember { mutableStateOf(false) }
    var filtroTipoSeleccionado by remember { mutableStateOf("Todos") }
    var filtroCategoriaSeleccionada by remember { mutableStateOf<DomainCategoria?>(null) }
    var filtroFechaSeleccionada by remember { mutableStateOf<Date?>(null) }
    var busquedaTexto by remember { mutableStateOf("") }
    var movimientoAEditar by remember { mutableStateOf<MovimientoEntity?>(null) }
    var expandedPeriodo by remember { mutableStateOf(false) }
    var expandedTipo by remember { mutableStateOf(false) }
    var expandedCategoria by remember { mutableStateOf(false) }
    val tipos = listOf("Todos", "Ingresos", "Gastos", "Omitir")
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, 2)
    val periodos = (0..12).map { offset ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.MONTH, -offset)
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        "$year-$month"
    }.toMutableList().apply { add(0, "Todos") }
    
    // Actualizar filtro de período cuando cambie el período global
    LaunchedEffect(periodoGlobal) {
        viewModel.cargarMovimientosPorPeriodo(periodoGlobal)
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
                        "Transacciones",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Gestiona tus ingresos y gastos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón de filtros mejorado
                    OutlinedButton(
                        onClick = { showFiltroDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Filtrar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Filtros")
                    }
                    
                    // Botón de agregar mejorado
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp
                        ),
                        modifier = Modifier.width(200.dp) // Ancho fijo más compacto
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Agregar transacción",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Nueva Transacción",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Campo de búsqueda
        if (uiState is MovimientosUiState.Success) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                OutlinedTextField(
                    value = busquedaTexto,
                    onValueChange = { busquedaTexto = it },
                    label = { Text("Buscar transacciones...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (busquedaTexto.isNotEmpty()) {
                            IconButton(
                                onClick = { busquedaTexto = "" }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        when (uiState) {
            is MovimientosUiState.Loading -> {
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
                                "Cargando transacciones...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            is MovimientosUiState.Success -> {
                val movimientos = (uiState as MovimientosUiState.Success).movimientos
                val categorias = (uiState as MovimientosUiState.Success).categorias
                
                // Aplicar filtros
                val movimientosFiltrados = movimientos.filter { movimiento ->
                    val cumpleTipo = when (filtroTipoSeleccionado) {
                        "Ingresos" -> movimiento.tipo == "INGRESO"
                        "Gastos" -> movimiento.tipo == "GASTO"
                        "Omitir" -> movimiento.tipo == "OMITIR"
                        else -> true
                    }
                    val cumpleCategoria = filtroCategoriaSeleccionada?.let { categoria ->
                        movimiento.categoriaId == categoria.id
                    } ?: true
                    val cumpleFecha = filtroFechaSeleccionada?.let { fecha ->
                        val movimientoDate = Calendar.getInstance().apply { time = movimiento.fecha }
                        val filtroDate = Calendar.getInstance().apply { time = fecha }
                        movimientoDate.get(Calendar.YEAR) == filtroDate.get(Calendar.YEAR) &&
                        movimientoDate.get(Calendar.MONTH) == filtroDate.get(Calendar.MONTH) &&
                        movimientoDate.get(Calendar.DAY_OF_MONTH) == filtroDate.get(Calendar.DAY_OF_MONTH)
                    } ?: true
                    val cumpleBusqueda = busquedaTexto.isEmpty() || 
                        movimiento.descripcion.contains(busquedaTexto, ignoreCase = true) ||
                        (movimiento.categoriaId?.let { catId ->
                            categorias.find { it.id == catId }?.nombre?.contains(busquedaTexto, ignoreCase = true)
                        } ?: false)
                    
                    cumpleTipo && cumpleCategoria && cumpleFecha && cumpleBusqueda
                }

                // Eliminar el bloque de StatCard de ingresos, gastos y balance
                // ... aquí continúa la lista de transacciones ...

                // Lista de transacciones mejorada
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
                                text = "Transacciones (${movimientosFiltrados.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (movimientosFiltrados.isNotEmpty()) {
                                Text(
                                    text = "Período: $periodoGlobal",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (movimientosFiltrados.isEmpty()) {
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
                                        Icons.Default.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "No hay transacciones",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Agrega tu primera transacción para comenzar",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
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
                    }
                }
            }
            is MovimientosUiState.Error -> {
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
                                text = "Error al cargar transacciones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = (uiState as MovimientosUiState.Error).mensaje,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de filtros mejorado
    AnimatedVisibility(
        visible = showFiltroDialog,
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
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Filtro por tipo
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tipo de transacción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        tipos.forEach { tipo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { filtroTipoSeleccionado = tipo }
                                    .padding(vertical = 8.dp),
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
                    }
                    
                    Divider()
                    
                    // Filtro por categoría
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    )
                    {
                        Text(
                            text = "Categoría",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        when (categoriasUiState) {
                            is CategoriasUiState.Success -> {
                                val domainCategorias = (categoriasUiState as CategoriasUiState.Success).categorias
                                domainCategorias.forEach { categoria ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { filtroCategoriaSeleccionada = categoria }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = filtroCategoriaSeleccionada == categoria,
                                            onClick = { filtroCategoriaSeleccionada = categoria }
                                        )
                                        Text(
                                            text = categoria.nombre,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = "Categorías no disponibles",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Filtro por fecha
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Fecha",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = filtroFechaSeleccionada?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                                onValueChange = { },
                                label = { Text("Desde") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Text),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            OutlinedTextField(
                                value = filtroFechaSeleccionada?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                                onValueChange = { },
                                label = { Text("Hasta") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Text),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showFiltroDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        filtroTipoSeleccionado = "Todos"
                        filtroCategoriaSeleccionada = null
                        filtroFechaSeleccionada = null
                        showFiltroDialog = false 
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Limpiar")
                }
            }
        )
    }

    // Diálogo para agregar nueva transacción
    AnimatedVisibility(
        visible = showAddDialog,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(200, easing = FastOutLinearInEasing)
        )
    ) {
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

    // Diálogo para editar transacción con animación
    AnimatedVisibility(
        visible = movimientoAEditar != null,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(200, easing = FastOutLinearInEasing)
        )
    ) {
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

    // Snackbar global
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        ClasificacionUseCaseEntryPoint::class.java
    )
    val clasificacionUseCase = entryPoint.gestionarClasificacionAutomaticaUseCase()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    SnackbarHost(hostState = snackbarHostState)
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
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (movimiento.categoriaId == null) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de tipo de transacción mejorado
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = if (movimiento.tipo == "INGRESO") 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (movimiento.tipo == "INGRESO") 
                            Icons.Default.KeyboardArrowUp 
                        else 
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (movimiento.tipo == "INGRESO") 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Información de la transacción mejorada
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            if (movimiento.categoriaId == null) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "Sin clasificar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (movimiento.descripcion.isNotEmpty()) {
                            Text(
                                text = movimiento.descripcion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Mostrar período de facturación
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Período: ${movimiento.periodoFacturacion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Monto mejorado
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = FormatUtils.formatMoneyCLP(movimiento.monto),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (movimiento.tipo == "INGRESO") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (movimiento.tipo == "INGRESO") 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else 
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = movimiento.tipo,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (movimiento.tipo == "INGRESO") 
                                    MaterialTheme.colorScheme.primary
                                else 
                                    MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // Botones de acción mejorados
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onEdit(movimiento) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
        }
    }
    
    // Diálogo de confirmación de eliminación mejorado con animación
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
                    "Eliminar transacción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { 
                Text(
                    "¿Estás seguro de que quieres eliminar esta transacción? Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(movimiento)
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
    calendar.add(Calendar.MONTH, 2)
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Tipo de transacción mejorado
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Tipo de transacción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { tipoSeleccionado = "GASTO" }
                                .padding(12.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    if (tipoSeleccionado == "GASTO") 
                                        MaterialTheme.colorScheme.errorContainer 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tipoSeleccionado == "GASTO",
                                onClick = { tipoSeleccionado = "GASTO" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.error
                                )
                            )
                            Text(
                                "Gasto",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (tipoSeleccionado == "GASTO") 
                                    MaterialTheme.colorScheme.onErrorContainer 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { tipoSeleccionado = "INGRESO" }
                                .padding(12.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    if (tipoSeleccionado == "INGRESO") 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tipoSeleccionado == "INGRESO",
                                onClick = { tipoSeleccionado = "INGRESO" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                "Ingreso",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (tipoSeleccionado == "INGRESO") 
                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Fila adicional para el tipo "Omitir"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tipoSeleccionado = "OMITIR" }
                            .padding(12.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (tipoSeleccionado == "OMITIR") 
                                    MaterialTheme.colorScheme.tertiaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tipoSeleccionado == "OMITIR",
                            onClick = { tipoSeleccionado = "OMITIR" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                        Text(
                            "Omitir (no afecta cálculos)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (tipoSeleccionado == "OMITIR") 
                                MaterialTheme.colorScheme.onTertiaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                                    // Monto mejorado
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = monto,
                            onValueChange = { monto = it },
                            label = { Text("Monto") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        if (tipoSeleccionado == "GASTO") {
                            Text(
                                text = "💡 Para reversas o reembolsos, ingresa el monto como negativo (ej: -50000)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                
                // Descripción mejorada
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                // Periodo de facturación mejorado
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriodo) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
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
                
                // Categoría (opcional) mejorada
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
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
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
                    Button(
                        onClick = {
                            val montoDouble = monto.toDoubleOrNull() ?: 0.0
                            val isValidAmount = when (tipoSeleccionado) {
                                "GASTO" -> FormatUtils.isValidAmountForGastos(monto)
                                "INGRESO" -> FormatUtils.isValidAmountForIngresos(monto)
                                "OMITIR" -> true // No hay validación específica para omitir
                                else -> false
                            }
                            if (isValidAmount && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()) {
                                onConfirm(
                                    tipoSeleccionado,
                                    montoDouble,
                                    descripcion,
                                    periodoSeleccionado,
                                    categoriaSeleccionada?.id
                                )
                            }
                        },
                        enabled = {
                            val montoDouble = monto.toDoubleOrNull() ?: 0.0
                            val isValidAmount = when (tipoSeleccionado) {
                                "GASTO" -> FormatUtils.isValidAmountForGastos(monto)
                                "INGRESO" -> FormatUtils.isValidAmountForIngresos(monto)
                                "OMITIR" -> true // No hay validación específica para omitir
                                else -> false
                            }
                            isValidAmount && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()
                        }(),
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
    calendar.add(Calendar.MONTH, 2)
    val periodos = (0..12).map { offset ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.MONTH, -offset)
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        "$year-$month"
    }

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
                    "Editar Transacción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Tipo de transacción mejorado
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tipo de transacción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { tipoSeleccionado = "GASTO" }
                                    .padding(12.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        if (tipoSeleccionado == "GASTO") 
                                            MaterialTheme.colorScheme.errorContainer 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = tipoSeleccionado == "GASTO",
                                    onClick = { tipoSeleccionado = "GASTO" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.error
                                    )
                                )
                                Text(
                                    "Gasto",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (tipoSeleccionado == "GASTO") 
                                        MaterialTheme.colorScheme.onErrorContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { tipoSeleccionado = "INGRESO" }
                                    .padding(12.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        if (tipoSeleccionado == "INGRESO") 
                                            MaterialTheme.colorScheme.primaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = tipoSeleccionado == "INGRESO",
                                    onClick = { tipoSeleccionado = "INGRESO" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    "Ingreso",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (tipoSeleccionado == "INGRESO") 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                                    // Monto mejorado
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = monto,
                        onValueChange = { monto = it },
                        label = { Text("Monto") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    if (tipoSeleccionado == "GASTO") {
                        Text(
                            text = "💡 Para reversas o reembolsos, ingresa el monto como negativo (ej: -50000)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                    
                    // Descripción mejorada
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    // Periodo de facturación mejorado
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
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriodo) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
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
                    
                    // Categoría mejorada
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
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
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
                Button(
                    onClick = {
                        val montoDouble = monto.toDoubleOrNull() ?: 0.0
                        val isValidAmount = when (tipoSeleccionado) {
                            "GASTO" -> FormatUtils.isValidAmountForGastos(monto)
                            "INGRESO" -> FormatUtils.isValidAmountForIngresos(monto)
                            "OMITIR" -> true // No hay validación específica para omitir
                            else -> false
                        }
                        if (isValidAmount && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()) {
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
                    enabled = {
                        val montoDouble = monto.toDoubleOrNull() ?: 0.0
                        val isValidAmount = when (tipoSeleccionado) {
                            "GASTO" -> FormatUtils.isValidAmountForGastos(monto)
                            "INGRESO" -> FormatUtils.isValidAmountForIngresos(monto)
                            "OMITIR" -> true // No hay validación específica para omitir
                            else -> false
                        }
                        isValidAmount && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()
                    }(),
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
fun EditarMovimientoDialogConSugerencia(
    movimiento: MovimientoEntity,
    categorias: List<Categoria>,
    onDismiss: () -> Unit,
    onConfirm: (MovimientoEntity) -> Unit,
    clasificacionUseCase: GestionarClasificacionAutomaticaUseCase,
    snackbarHostState: SnackbarHostState
) {
    var monto by remember { mutableStateOf(movimiento.monto.toString()) }
    var descripcion by remember { mutableStateOf(movimiento.descripcion) }
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(categorias.find { it.id == movimiento.categoriaId }) }
    var tipoSeleccionado by remember { mutableStateOf(movimiento.tipo) }
    var expandedPeriodo by remember { mutableStateOf(false) }
    var periodoSeleccionado by remember { mutableStateOf(movimiento.periodoFacturacion) }
    val scope = rememberCoroutineScope()
    
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, 2)
    val periodos = (0..12).map { offset ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.MONTH, -offset)
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        "$year-$month"
    }
    val context = LocalContext.current
    // Sugerencia de categoría
    var sugerenciaCategoria by remember { mutableStateOf<Categoria?>(null) }
    var mostrarSugerencia by remember { mutableStateOf(false) }
    // Consultar sugerencia solo si no hay categoría
    LaunchedEffect(movimiento.id) {
        if (movimiento.categoriaId == null) {
            val sugerencia = clasificacionUseCase.sugerirCategoria(movimiento.descripcion)
            if (sugerencia != null) {
                val categoriaSugerida = categorias.find { it.id == sugerencia.categoriaId }
                if (categoriaSugerida != null) {
                    sugerenciaCategoria = categoriaSugerida
                    mostrarSugerencia = true
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "¿Asignar la categoría sugerida: ${categoriaSugerida.nombre}?",
                            actionLabel = "Aceptar",
                            withDismissAction = true
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            categoriaSeleccionada = categoriaSugerida
                        }
                        mostrarSugerencia = false
                    }
                }
            }
        }
    }

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
                    "Editar Transacción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Tipo de transacción mejorado
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tipo de transacción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { tipoSeleccionado = "GASTO" }
                                    .padding(12.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        if (tipoSeleccionado == "GASTO") 
                                            MaterialTheme.colorScheme.errorContainer 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = tipoSeleccionado == "GASTO",
                                    onClick = { tipoSeleccionado = "GASTO" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.error
                                    )
                                )
                                Text(
                                    "Gasto",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (tipoSeleccionado == "GASTO") 
                                        MaterialTheme.colorScheme.onErrorContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { tipoSeleccionado = "INGRESO" }
                                    .padding(12.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        if (tipoSeleccionado == "INGRESO") 
                                            MaterialTheme.colorScheme.primaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = tipoSeleccionado == "INGRESO",
                                    onClick = { tipoSeleccionado = "INGRESO" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    "Ingreso",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (tipoSeleccionado == "INGRESO") 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                                    // Monto mejorado
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = monto,
                        onValueChange = { monto = it },
                        label = { Text("Monto") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    if (tipoSeleccionado == "GASTO") {
                        Text(
                            text = "💡 Para reversas o reembolsos, ingresa el monto como negativo (ej: -50000)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                    
                    // Descripción mejorada
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    // Periodo de facturación mejorado
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
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriodo) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
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
                    
                    // Categoría mejorada
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
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
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
                Button(
                    onClick = {
                        val montoDouble = monto.toDoubleOrNull() ?: 0.0
                        val isValidAmount = when (tipoSeleccionado) {
                            "GASTO" -> FormatUtils.isValidAmountForGastos(monto)
                            "INGRESO" -> FormatUtils.isValidAmountForIngresos(monto)
                            "OMITIR" -> true // No hay validación específica para omitir
                            else -> false
                        }
                        if (isValidAmount && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()) {
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
                    enabled = {
                        val montoDouble = monto.toDoubleOrNull() ?: 0.0
                        val isValidAmount = when (tipoSeleccionado) {
                            "GASTO" -> FormatUtils.isValidAmountForGastos(monto)
                            "INGRESO" -> FormatUtils.isValidAmountForIngresos(monto)
                            "OMITIR" -> true // No hay validación específica para omitir
                            else -> false
                        }
                        isValidAmount && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()
                    }(),
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

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun TransaccionesScreenPreview() {
    MaterialTheme {
        TransaccionesScreen(
            navController = rememberNavController(),
            viewModel = hiltViewModel(),
            periodoGlobalViewModel = hiltViewModel()
        )
    }
} 