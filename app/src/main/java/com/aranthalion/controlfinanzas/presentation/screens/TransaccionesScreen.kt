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
import androidx.compose.ui.platform.LocalConfiguration
import com.aranthalion.controlfinanzas.presentation.screens.TinderClasificacionScreen
import com.aranthalion.controlfinanzas.presentation.screens.TinderClasificacionViewModel
import com.aranthalion.controlfinanzas.data.util.ExcelTransaction
import com.aranthalion.controlfinanzas.presentation.components.ClasificacionStatsCard
import com.aranthalion.controlfinanzas.presentation.components.ClasificacionStats
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.saveable.rememberSaveable

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
    var mostrarTinderClasificacion by rememberSaveable { mutableStateOf(false) }
    
    // ViewModel del Tinder de clasificación
    val tinderViewModel: TinderClasificacionViewModel = hiltViewModel()
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

    // Snackbar y scope global para feedback visual
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Actualizar filtro de período cuando cambie el período global
    LaunchedEffect(periodoGlobal) {
        viewModel.cargarMovimientosPorPeriodo(periodoGlobal)
    }
    
    // Función para procesar transacciones sin categoría para el Tinder
    fun procesarTransaccionesSinCategoria() {
        println("[LOG_TINDER_CLASIFICACION 1] Botón 'Juguemos a clasificar' presionado")
        println("[LOG_TINDER_CLASIFICACION 2] Iniciando función procesarTransaccionesSinCategoria")
        if (uiState is MovimientosUiState.Success) {
            println("[LOG_TINDER_CLASIFICACION 3] Estado UI: Success")
            val movimientos = (uiState as MovimientosUiState.Success).movimientos
            val transaccionesSinCategoria = movimientos.filter { it.categoriaId == null }
            println("[LOG_TINDER_CLASIFICACION 4] Transacciones sin categoría encontradas: ${transaccionesSinCategoria.size}")
            if (transaccionesSinCategoria.isNotEmpty()) {
                scope.launch {
                    snackbarHostState.showSnackbar("Procesando ${transaccionesSinCategoria.size} transacciones sin clasificar")
                }
                println("[LOG_TINDER_CLASIFICACION 5] Navegando a pantalla TinderClasificacionScreen")
                // Navegar a la nueva pantalla dedicada
                navController.navigate("tinder_clasificacion")
            } else {
                println("[LOG_TINDER_CLASIFICACION 6] No hay transacciones pendientes de clasificación")
                scope.launch {
                    snackbarHostState.showSnackbar("No hay transacciones pendientes de clasificación")
                }
            }
        } else {
            println("[LOG_TINDER_CLASIFICACION 7] Estado UI no es Success")
            scope.launch {
                snackbarHostState.showSnackbar("No se pudo obtener el estado de las transacciones")
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 600.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                        Text(
                            "Transacciones",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                },
                actions = {
                    IconButton(onClick = { showFiltroDialog = true }) {
                                Icon(
                            Icons.Default.List,
                                    contentDescription = "Filtrar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        content = { innerPadding ->
            val categoriasForDialog = when (categoriasUiState) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Barra de progreso de clasificación
                    if (uiState is MovimientosUiState.Success) {
                        val movimientos = (uiState as MovimientosUiState.Success).movimientos
                        val clasificadas = movimientos.count { it.categoriaId != null }
                        val porcentaje = if (movimientos.isNotEmpty()) clasificadas.toFloat() / movimientos.size else 0f
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = porcentaje,
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "${(porcentaje * 100).toInt()}% transacciones clasificadas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                    // Subtítulo
                    item {
                        Text(
                            "Gestiona tus ingresos y gastos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    // Barra de acciones
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar transacción", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nueva transacción")
                            }
                            Button(
                                onClick = {
                                    println("[LOG_TINDER_CLASIFICACION 1] Botón 'Juguemos a clasificar' presionado")
                                    procesarTransaccionesSinCategoria()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Jugar a clasificar")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Juguemos a clasificar")
                            }
                            Button(
                                onClick = {
        if (uiState is MovimientosUiState.Success) {
                val movimientos = (uiState as MovimientosUiState.Success).movimientos
                                        val unicos = mutableSetOf<String>()
                                        val duplicados = mutableListOf<MovimientoEntity>()
                                        for (mov in movimientos) {
                                            val idUnico = com.aranthalion.controlfinanzas.data.util.ExcelProcessor.generarIdUnico(mov.fecha, mov.monto, mov.descripcion)
                                            if (idUnico in unicos) {
                                                duplicados.add(mov)
                                            } else {
                                                unicos.add(idUnico)
                                            }
                                        }
                                        for (dup in duplicados) {
                                            viewModel.eliminarMovimiento(dup)
                                        }
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Se eliminaron ${duplicados.size} transacciones duplicadas")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar duplicados")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Eliminar duplicados")
                            }
                        }
                    }
                    // Búsqueda
                    item {
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
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
        }

        when (uiState) {
            is MovimientosUiState.Loading -> {
                item {
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
            }
            is MovimientosUiState.Success -> {
                item {
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

                    // Lista de transacciones mejorada
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
                                                    .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                Column(
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    movimientosFiltrados.forEach { movimiento ->
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
            }
            is MovimientosUiState.Error -> {
                item {
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
        
    }
            }
            // Diálogo de filtros
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
                    categorias = categoriasForDialog
        )
    }

            // Diálogo para editar transacción
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
            val context = LocalContext.current
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                ClasificacionUseCaseEntryPoint::class.java
            )
            val clasificacionUseCase = entryPoint.gestionarClasificacionAutomaticaUseCase()
            EditarMovimientoDialogConSugerencia(
                movimiento = movimiento,
                categorias = (uiState as? MovimientosUiState.Success)?.categorias ?: emptyList(),
                onDismiss = { movimientoAEditar = null },
                onConfirm = { movimientoEditado ->
                    viewModel.actualizarMovimiento(movimientoEditado)
                    movimientoAEditar = null
                },
                clasificacionUseCase = clasificacionUseCase,
                snackbarHostState = snackbarHostState
            )
        }
    }

    // Snackbar global
    SnackbarHost(hostState = snackbarHostState)
        }     )
    }
    


@Composable
private fun TransaccionItem(
    movimiento: MovimientoEntity,
    categorias: List<Categoria>,
    onEdit: (MovimientoEntity) -> Unit,
    onDelete: (MovimientoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val categoria = categorias.find { it.id == movimiento.categoriaId }
    val formattedDate = remember(movimiento.fecha) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(movimiento.fecha)
    }
    var deletePressed by remember { mutableStateOf(false) }
    val deleteScale by animateFloatAsState(if (deletePressed) 0.92f else 1f, animationSpec = spring())
    var deleteConfirmCountdown by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit(movimiento) }
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .background(
                when {
                    movimiento.categoriaId == null -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (movimiento.categoriaId == null) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(4.dp))
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
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = categoria?.nombre ?: "Sin categoría",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (movimiento.categoriaId == null) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (movimiento.categoriaId == null) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "Sin clasificar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        if (movimiento.descripcion.isNotEmpty()) {
                            Text(
                                text = movimiento.descripcion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Fecha de la transacción (solo mostrar, no editable)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Mostrar período de facturación
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            color = when (movimiento.tipo) {
                                "INGRESO" -> MaterialTheme.colorScheme.primary
                                "GASTO" -> MaterialTheme.colorScheme.error
                                "OMITIR" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when (movimiento.tipo) {
                                "INGRESO" -> MaterialTheme.colorScheme.primaryContainer
                                "GASTO" -> MaterialTheme.colorScheme.errorContainer
                                "OMITIR" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = movimiento.tipo,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (movimiento.tipo) {
                                    "INGRESO" -> MaterialTheme.colorScheme.onPrimaryContainer
                                    "GASTO" -> MaterialTheme.colorScheme.onErrorContainer
                                    "OMITIR" -> MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
                        tint = MaterialTheme.colorScheme.primary // Mejor visibilidad
                    )
                }
                IconButton(
                    onClick = {
                        if (deleteConfirmCountdown == 0) {
                            deleteConfirmCountdown = 2
                            scope.launch {
                                repeat(2) {
                                    kotlinx.coroutines.delay(500)
                                    deleteConfirmCountdown--
                                }
                            }
                        } else if (deleteConfirmCountdown == 1) {
                            onDelete(movimiento)
                            deleteConfirmCountdown = 0
                        }
                    },
                    modifier = Modifier.scale(deleteScale)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = if (deleteConfirmCountdown > 0) "Toca de nuevo para confirmar" else "Eliminar transacción",
                        tint = if (deleteConfirmCountdown > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
        if (deleteConfirmCountdown > 0) {
            Text(
                "Toca de nuevo para confirmar eliminación",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
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
                    
                    // Categoría mejorada
                    if (categorias.isNotEmpty()) {
                        var expandedCategoria by remember { mutableStateOf(false) }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                            
                            // Botón para sugerencia automática si no hay categoría
                            if (categoriaSeleccionada == null) {
                                OutlinedButton(
                                    onClick = {
                                        // TODO: Implementar sugerencia automática
                                        // Por ahora solo muestra un mensaje
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Sugerir categoría",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sugerir categoría automáticamente")
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
    var fechaSeleccionada by remember { mutableStateOf(movimiento.fecha) }
    var showDatePicker by remember { mutableStateOf(false) }
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
            // TODO: Implementar sugerencia automática
            // Por ahora no se hace nada
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
                    
                    // Fecha de la transacción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fechaSeleccionada),
                            onValueChange = {},
                            label = { Text("Fecha") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showDatePicker = true }
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Seleccionar fecha",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (showDatePicker) {
                        val calendar = Calendar.getInstance()
                        calendar.time = fechaSeleccionada
                        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = calendar.timeInMillis)
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            fechaSeleccionada = Date(it)
                                        }
                                        showDatePicker = false
                                    }
                                ) {
                                    Text("OK")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showDatePicker = false }
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        ) {
                            DatePicker(
                                state = datePickerState,
                                showModeToggle = false
                            )
                        }
                    }
                    
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
                                fecha = fechaSeleccionada,
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