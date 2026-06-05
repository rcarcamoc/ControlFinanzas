package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aranthalion.controlfinanzas.presentation.components.StatCard
import com.aranthalion.controlfinanzas.presentation.components.PresupuestoInfo
import com.aranthalion.controlfinanzas.presentation.components.BarChart
import com.aranthalion.controlfinanzas.presentation.components.BarChartData
import com.aranthalion.controlfinanzas.presentation.components.procesarDatosParaGrafico
import com.aranthalion.controlfinanzas.presentation.components.PieChart
import com.aranthalion.controlfinanzas.presentation.components.PieChartData
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.presentation.screens.MovimientosViewModel
import com.aranthalion.controlfinanzas.presentation.screens.MovimientosUiState
import com.aranthalion.controlfinanzas.domain.usecase.EstadoPresupuesto
import com.aranthalion.controlfinanzas.presentation.components.CustomIcons
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorGlobal
import com.aranthalion.controlfinanzas.presentation.screens.components.GastoMensualCard
import com.aranthalion.controlfinanzas.presentation.screens.components.EstadoPresupuestoCard
import com.aranthalion.controlfinanzas.presentation.screens.components.GastoPorCategoriaCard
import com.aranthalion.controlfinanzas.presentation.screens.components.TransaccionesSinClasificarCard
import com.aranthalion.controlfinanzas.presentation.screens.components.UltimaActualizacionCard
import kotlin.math.abs
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: MovimientosViewModel = hiltViewModel(),
    presupuestosViewModel: PresupuestosViewModel = hiltViewModel(),
    periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totales by viewModel.totales.collectAsState()
    val resumenPresupuestos by presupuestosViewModel.resumen.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val periodoGlobal by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    val periodosDisponibles by periodoGlobalViewModel.periodosDisponibles.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val currentTime = remember { 
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    }
    
    // Configuración responsive
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 600.dp
    val isMediumScreen = screenWidth >= 600.dp && screenWidth < 840.dp
    
    // Determinar número de columnas para StatCards
    val statCardsColumns = when {
        isSmallScreen -> 2
        isMediumScreen -> 3
        else -> 4
    }
    
    // Refrescar al volver a primer plano
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.cargarMovimientosPorPeriodo(periodoGlobal)
                presupuestosViewModel.cargarPresupuestos(periodoGlobal)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Observar cambios en el período global y actualizar los ViewModels
    LaunchedEffect(periodoGlobal) {
        println("🏠 HOME: Cargando datos para período: $periodoGlobal")
        viewModel.cargarMovimientosPorPeriodo(periodoGlobal)
        presupuestosViewModel.cargarPresupuestos(periodoGlobal)
    }
    // Forzar carga inicial si el estado es Loading
    LaunchedEffect(Unit) {
        if (uiState is MovimientosUiState.Loading) {
            println("🏠 HOME: Carga inicial forzada...")
            viewModel.cargarMovimientosPorPeriodo(periodoGlobal)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dashboard Stats con datos reales - Layout responsive
        when (uiState) {
            is MovimientosUiState.Success -> {
                val movimientos = (uiState as MovimientosUiState.Success).movimientos
                val periodoActual = obtenerPeriodoActual()
                println("🏠 HOME: Estado Success - Movimientos: ${movimientos.size}, Categorías: ${(uiState as MovimientosUiState.Success).categorias.size}")
                if (movimientos.isEmpty()) {
                    Text(
                        text = "No hay movimientos para mostrar en este período.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Filtrar movimientos por período y excluir transacciones omitidas
                val movimientosFiltrados = movimientos.filter { 
                    (periodoGlobal == "Todos" || it.periodoFacturacion == periodoGlobal) &&
                    it.tipo != "OMITIR" // Excluir transacciones omitidas
                }
                
                val gastos = movimientosFiltrados.filter { it.tipo == "GASTO" }
                val ingresos = movimientosFiltrados.filter { it.tipo == "INGRESO" }
                // Para gastos, sumamos todos los valores (positivos y negativos)
                // Los negativos representan reversas y reducen el gasto total
                val totalGastos = FormatUtils.roundToTwoDecimals(
                    abs(gastos.sumOf { FormatUtils.normalizeAmount(it.monto) })
                )
                val totalIngresos = FormatUtils.roundToTwoDecimals(
                    ingresos.sumOf { FormatUtils.normalizeAmount(it.monto) }
                )
                val balance = FormatUtils.roundToTwoDecimals(totalIngresos - totalGastos)
                val movimientosSinCategoria = movimientos.count { it.categoriaId == null }
                val categoriasActivas = (uiState as MovimientosUiState.Success).categorias.size

                // Información de presupuesto
                val presupuestoInfo = resumenPresupuestos?.let { resumen ->
                    PresupuestoInfo(
                        porcentajeGastado = resumen.porcentajeGastado,
                        presupuestoRestante = resumen.totalPresupuestado - resumen.totalGastado,
                        estado = when {
                            resumen.porcentajeGastado <= 80 -> EstadoPresupuesto.NORMAL
                            resumen.porcentajeGastado <= 90 -> EstadoPresupuesto.ADVERTENCIA
                            resumen.porcentajeGastado <= 100 -> EstadoPresupuesto.CRITICO
                            else -> EstadoPresupuesto.EXCEDIDO
                        }
                    )
                }

                // StatCards en grid responsive
                LazyVerticalGrid(
                    columns = GridCells.Fixed(statCardsColumns),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(if (isSmallScreen) 320.dp else 160.dp)
                ) {
                    item {
                        StatCard(
                            title = "Gasto Total",
                            value = totalGastos.toString(),
                            icon = CustomIcons.KeyboardArrowDown,
                            description = "Este mes",
                            trend = null,
                            isMonetary = true,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                    item {
                        StatCard(
                            title = "Cumplimiento Presupuesto",
                            value = "${resumenPresupuestos?.porcentajeGastado?.toInt() ?: 0}%",
                            icon = CustomIcons.Star,
                            description = "Basado en presupuestos",
                            trend = null,
                            isMonetary = false,
                            presupuestoInfo = presupuestoInfo,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                    item {
                        StatCard(
                            title = "Balance",
                            value = balance.toString(),
                            icon = CustomIcons.Add,
                            description = "Este mes",
                            trend = null,
                            isMonetary = true,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                    item {
                        StatCard(
                            title = "Categorías Activas",
                            value = categoriasActivas.toString(),
                            icon = CustomIcons.Person,
                            description = "En uso",
                            trend = null,
                            isMonetary = false,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }

                // Grid de 2 columnas para contenido principal (Dashboard completo)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isSmallScreen) 1 else 2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Tarjeta: "Tendencia de Gasto Mensual" (con BarChart)
                    item {
                        GastoMensualCard(gastos = gastos)
                    }

                    // Tarjeta: "Estado del Presupuesto" (con ProgressBar)
                    item {
                        EstadoPresupuestoCard(
                            resumenPresupuestos = resumenPresupuestos,
                            navController = navController
                        )
                    }

                    // Tarjeta: "Gasto por Categoría" (con PieChart + drill-down)
                    item {
                        GastoPorCategoriaCard(
                            categorias = (uiState as MovimientosUiState.Success).categorias,
                            gastos = gastos,
                            navController = navController
                        )
                    }
                }
            }
            is MovimientosUiState.Loading -> {
                println("🏠 HOME: Estado Loading - Mostrando placeholder")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(statCardsColumns),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(if (isSmallScreen) 320.dp else 160.dp)
                ) {
                    repeat(4) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
            is MovimientosUiState.Error -> {
                println("🏠 HOME: Estado Error - ${(uiState as MovimientosUiState.Error).mensaje}")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error al cargar datos",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = (uiState as MovimientosUiState.Error).mensaje,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = { viewModel.cargarMovimientosPorPeriodo(periodoGlobal) }
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }

        // KPIs simplificados
        val movimientosSinCategoria = (uiState as? MovimientosUiState.Success)?.movimientos?.count { it.categoriaId == null } ?: 0
        TransaccionesSinClasificarCard(
            movimientosSinCategoria = movimientosSinCategoria,
            navController = navController
        )

        // Información adicional
        UltimaActualizacionCard(currentTime = currentTime)
    }
}

// Helpers para obtener el período actual y generar la lista
fun obtenerPeriodoActual(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    return String.format("%04d-%02d", year, month)
}

fun generarPeriodosDisponibles(): List<String> {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val periodos = mutableListOf<String>()
    for (i in -11..2) {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        cal.add(Calendar.MONTH, i)
        val periodo = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        periodos.add(periodo)
    }
    return periodos.sortedDescending()
} 