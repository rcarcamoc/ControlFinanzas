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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorGlobal
import kotlin.math.abs
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues

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
        viewModel.cargarMovimientosPorPeriodo(periodoGlobal)
        presupuestosViewModel.cargarPresupuestos(periodoGlobal)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Dashboard Stats con datos reales - Layout responsive
        when (uiState) {
            is MovimientosUiState.Success -> {
                val movimientos = (uiState as MovimientosUiState.Success).movimientos
                val periodoActual = obtenerPeriodoActual()
                
                // Filtrar movimientos por período y excluir transacciones omitidas
                val movimientosFiltrados = movimientos.filter { 
                    it.periodoFacturacion == periodoActual &&
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tarjeta: "Tendencia de Gasto Mensual" (con BarChart)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tendencia de Gasto Mensual",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = CustomIcons.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // Procesar datos reales para el gráfico
                                val chartData = procesarDatosParaGrafico(gastos, MaterialTheme.colorScheme.primary)
                                
                                BarChart(
                                    data = chartData,
                                    title = "Tendencia de Gasto Mensual",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                        }
                    }

                    // Tarjeta: "Estado del Presupuesto" (con ProgressBar)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Estado del Presupuesto",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = CustomIcons.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // Información detallada del presupuesto
                                resumenPresupuestos?.let { resumen ->
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Barra de progreso principal
                                        LinearProgressIndicator(
                                            progress = (resumen.porcentajeGastado.toFloat() / 100f).coerceIn(0f, 1f),
                                            modifier = Modifier.fillMaxWidth(),
                                            color = when {
                                                resumen.porcentajeGastado <= 80 -> MaterialTheme.colorScheme.primary
                                                resumen.porcentajeGastado <= 90 -> MaterialTheme.colorScheme.tertiary
                                                else -> MaterialTheme.colorScheme.error
                                            },
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        
                                        // Estadísticas del presupuesto
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Gastado",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = FormatUtils.formatMoneyCLP(resumen.totalGastado),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Presupuestado",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = FormatUtils.formatMoneyCLP(resumen.totalPresupuestado),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        // Botón para ver detalles
                                        Button(
                                            onClick = { navController.navigate("presupuestos") },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Ver Detalles del Presupuesto")
                                        }
                                    }
                                } ?: run {
                                    Text(
                                        text = "No hay presupuestos configurados",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Tarjeta: "Gasto por Categoría" (con PieChart + drill-down)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Gasto por Categoría",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = CustomIcons.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // Datos de ejemplo para el gráfico circular
                                val categorias = (uiState as MovimientosUiState.Success).categorias
                                val gastosPorCategoria = gastos.groupBy { it.categoriaId }
                                    .mapValues { (_, movimientos) -> 
                                        movimientos.sumOf { FormatUtils.normalizeAmount(it.monto) }
                                    }
                                
                                val pieChartData = categorias.mapNotNull { categoria ->
                                    val gasto = gastosPorCategoria[categoria.id] ?: 0.0
                                    if (gasto > 0) PieChartData(categoria.nombre, gasto.toFloat(), MaterialTheme.colorScheme.primary, categoria.id.toString()) else null
                                }.take(5) // Top 5 categorías
                                
                                if (pieChartData.isNotEmpty()) {
                                    // PieChart(
                                    //     data = pieChartData,
                                    //     modifier = Modifier
                                    //         .fillMaxWidth()
                                    //         .height(200.dp)
                                    // )
                                    Text(
                                        text = "Gráfico de gastos por categoría",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Text(
                                        text = "No hay gastos categorizados",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                
                                // Botón para drill-down
                                Button(
                                    onClick = { navController.navigate("dashboardAnalisis") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Ver Análisis Detallado")
                                }
                            }
                        }
                    }

                    // Tarjeta: "Proyecciones y Perspectivas"
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Proyecciones y Perspectivas",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = CustomIcons.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // Información de proyecciones
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Proyección mensual",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = FormatUtils.formatMoneyCLP(totalGastos * 1.1),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Ahorro potencial",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = FormatUtils.formatMoneyCLP(totalGastos * 0.2),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    
                                    // Botón condicional "Ver Resumen General"
                                    if (totalGastos > 1000) {
                                        Button(
                                            onClick = { navController.navigate("dashboardAnalisis") },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Ver Resumen General")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // Placeholder mientras carga - también responsive
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
        }

        // KPIs simplificados
        val movimientosSinCategoria = (uiState as? MovimientosUiState.Success)?.movimientos?.count { it.categoriaId == null } ?: 0
        if (movimientosSinCategoria > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$movimientosSinCategoria transacciones sin clasificar", color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(onClick = { navController.navigate("clasificacion_pendiente") }) {
                        Text("Clasificar")
                    }
                }
            }
        }

        // Información adicional
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = CustomIcons.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Última actualización",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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