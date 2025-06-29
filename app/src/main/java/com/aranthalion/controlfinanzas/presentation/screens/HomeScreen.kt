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
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.presentation.screens.MovimientosViewModel
import com.aranthalion.controlfinanzas.presentation.screens.MovimientosUiState
import com.aranthalion.controlfinanzas.domain.usecase.EstadoPresupuesto
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
                val gastos = movimientos.filter { it.tipo == "GASTO" }
                val ingresos = movimientos.filter { it.tipo == "INGRESO" }
                val totalGastos = FormatUtils.roundToTwoDecimals(
                    gastos.sumOf { FormatUtils.normalizeAmount(it.monto) }
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        StatCard(
                            title = "Gasto Total",
                            value = totalGastos.toString(),
                            icon = Icons.Default.KeyboardArrowDown,
                            description = "Este mes",
                            trend = null,
                            isMonetary = true,
                            modifier = Modifier.height(if (isSmallScreen) 140.dp else 160.dp)
                        )
                    }
                    item {
                        StatCard(
                            title = "Cumplimiento Presupuesto",
                            value = "${resumenPresupuestos?.porcentajeGastado?.toInt() ?: 0}%",
                            icon = Icons.Default.Star,
                            description = "Basado en presupuestos",
                            trend = null,
                            isMonetary = false,
                            presupuestoInfo = presupuestoInfo,
                            modifier = Modifier.height(if (isSmallScreen) 140.dp else 160.dp)
                        )
                    }
                    item {
                        StatCard(
                            title = "Balance",
                            value = balance.toString(),
                            icon = Icons.Default.Add,
                            description = "Este mes",
                            trend = null,
                            isMonetary = true,
                            modifier = Modifier.height(if (isSmallScreen) 140.dp else 160.dp)
                        )
                    }
                    item {
                        StatCard(
                            title = "Categorías Activas",
                            value = categoriasActivas.toString(),
                            icon = Icons.Default.Person,
                            description = "En uso",
                            trend = null,
                            isMonetary = false,
                            modifier = Modifier.height(if (isSmallScreen) 140.dp else 160.dp)
                        )
                    }
                }
            }
            else -> {
                // Placeholder mientras carga - también responsive
                LazyVerticalGrid(
                    columns = GridCells.Fixed(statCardsColumns),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.height(if (isSmallScreen) 140.dp else 160.dp)
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
                    imageVector = Icons.Default.Info,
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