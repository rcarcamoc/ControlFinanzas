package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasViewModel
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import com.aranthalion.controlfinanzas.presentation.screens.components.*
import com.aranthalion.controlfinanzas.presentation.screens.state.rememberTransaccionesScreenState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesScreen(
    navController: NavHostController,
    viewModel: MovimientosViewModel = hiltViewModel(),
    periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel(),
    categoriasViewModel: CategoriasViewModel = hiltViewModel()
) {
    // Estados UI
    val uiState by viewModel.uiState.collectAsState()
    val totales by viewModel.totales.collectAsState()
    val categoriasUiState by categoriasViewModel.uiState.collectAsState()
    val periodoGlobal by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    
    // Estado persistente de pantalla
    val screenState = rememberTransaccionesScreenState()
    
    // Feedback visual
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Efecto: cargar movimientos cuando cambia el período global
    LaunchedEffect(periodoGlobal) {
        viewModel.cargarMovimientosPorPeriodo(periodoGlobal)
    }
    
    Scaffold(
        topBar = {
            TransaccionesTopAppBar(
                onFilterPressed = { screenState.mostrarFiltroDialog.value = true }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Barra de progreso de clasificación
                item {
                    ClasificacionProgressBar(uiState)
                }
                
                // Descripción
                item {
                    TransaccionesSubtitle()
                }
                
                // Barra de acciones
                item {
                    TransaccionesActionBar(
                        uiState = uiState,
                        viewModel = viewModel,
                        navController = navController,
                        screenState = screenState,
                        snackbarHostState = snackbarHostState,
                        scope = scope
                    )
                }
                
                // Campo de búsqueda
                item {
                    SearchTransactionField(
                        searchText = screenState.busquedaTexto.value,
                        onSearchChanged = { screenState.busquedaTexto.value = it }
                    )
                }
                
                // Contenido según estado
                when (uiState) {
                    is MovimientosUiState.Loading -> {
                        item { LoadingTransactionCard() }
                    }
                    is MovimientosUiState.Success -> {
                        val successState = uiState as MovimientosUiState.Success
                        val movimientosFiltrados = aplicarFiltros(
                            movimientos = successState.movimientos,
                            categorias = successState.categorias,
                            filtroTipo = screenState.filtroTipoSeleccionado.value,
                            filtroCategoria = screenState.filtroCategoriaSeleccionada.value,
                            filtroFecha = screenState.filtroFechaSeleccionada.value,
                            busquedaTexto = screenState.busquedaTexto.value
                        )
                        
                        item {
                            TransaccionesListCard(
                                movimientosFiltrados = movimientosFiltrados,
                                categorias = successState.categorias,
                                periodoGlobal = periodoGlobal,
                                onEditMovimiento = { screenState.movimientoAEditar.value = it },
                                onDeleteMovimiento = { viewModel.eliminarMovimiento(it) }
                            )
                        }
                    }
                    is MovimientosUiState.Error -> {
                        item {
                            ErrorTransactionCard(
                                errorMessage = (uiState as MovimientosUiState.Error).mensaje
                            )
                        }
                    }
                }
            }
        }
    )
    
    // Diálogos
    FiltroTransaccionesDialog(
        mostrar = screenState.mostrarFiltroDialog.value,
        onDismiss = { screenState.mostrarFiltroDialog.value = false },
        screenState = screenState,
        categoriasUiState = categoriasUiState
    )
    
    AgregarTransaccionDialog(
        mostrar = screenState.mostrarAddDialog.value,
        onDismiss = { screenState.mostrarAddDialog.value = false },
        onConfirm = { tipo, monto, descripcion, periodo, categoriaId ->
            val nuevoMovimiento = crearNuevoMovimiento(
                tipo, monto, descripcion, periodo, categoriaId
            )
            viewModel.agregarMovimiento(nuevoMovimiento)
            screenState.mostrarAddDialog.value = false
        },
        categoriasUiState = categoriasUiState,
        periodoGlobal = periodoGlobal
    )
    
    // Diálogo de edición
    screenState.movimientoAEditar.value?.let { movimiento ->
        EditarMovimientoDialog(
            movimiento = movimiento,
            categoriasUiState = categoriasUiState,
            onDismiss = { screenState.movimientoAEditar.value = null },
            onConfirm = { movimientoEditado ->
                viewModel.actualizarMovimiento(movimientoEditado)
                screenState.movimientoAEditar.value = null
            }
        )
    }
}

private fun aplicarFiltros(
    movimientos: List<MovimientoEntity>,
    categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>,
    filtroTipo: String,
    filtroCategoria: com.aranthalion.controlfinanzas.domain.categoria.Categoria?,
    filtroFecha: java.util.Date?,
    busquedaTexto: String
): List<MovimientoEntity> {
    return movimientos.filter { movimiento ->
        val cumpleTipo = when (filtroTipo) {
            \"Ingresos\" -> movimiento.tipo == \"INGRESO\"
            \"Gastos\" -> movimiento.tipo == \"GASTO\"
            \"Omitir\" -> movimiento.tipo == \"OMITIR\"
            else -> true
        }
        
        val cumpleCategoria = filtroCategoria?.let { categoria ->
            movimiento.categoriaId == categoria.id
        } ?: true
        
        val cumpleFecha = filtroFecha?.let { fecha ->
            val cal1 = java.util.Calendar.getInstance().apply { time = movimiento.fecha }
            val cal2 = java.util.Calendar.getInstance().apply { time = fecha }
            cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
            cal1.get(java.util.Calendar.MONTH) == cal2.get(java.util.Calendar.MONTH) &&
            cal1.get(java.util.Calendar.DAY_OF_MONTH) == cal2.get(java.util.Calendar.DAY_OF_MONTH)
        } ?: true
        
        val cumpleBusqueda = busquedaTexto.isEmpty() ||
            movimiento.descripcion.contains(busquedaTexto, ignoreCase = true) ||
            categorias.find { it.id == movimiento.categoriaId }
                ?.nombre?.contains(busquedaTexto, ignoreCase = true) ?: false
        
        cumpleTipo && cumpleCategoria && cumpleFecha && cumpleBusqueda
    }
}

private fun crearNuevoMovimiento(
    tipo: String,
    monto: Double,
    descripcion: String,
    periodo: String,
    categoriaId: Long?
): MovimientoEntity {
    val fecha = java.util.Date()
    return MovimientoEntity(
        tipo = tipo,
        monto = monto,
        descripcion = descripcion,
        fecha = fecha,
        periodoFacturacion = periodo,
        categoriaId = categoriaId,
        idUnico = com.aranthalion.controlfinanzas.data.util.ExcelProcessor.generarIdUnico(
            fecha, monto, descripcion
        )
    )
}