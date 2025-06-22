package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.presentation.components.StatCard
import com.aranthalion.controlfinanzas.presentation.components.BarChart
import com.aranthalion.controlfinanzas.presentation.components.BarChartData
import com.aranthalion.controlfinanzas.presentation.components.PieChart
import com.aranthalion.controlfinanzas.presentation.components.PieChartData
import com.aranthalion.controlfinanzas.ui.theme.Chart1
import com.aranthalion.controlfinanzas.ui.theme.Chart2
import com.aranthalion.controlfinanzas.ui.theme.Chart3
import com.aranthalion.controlfinanzas.ui.theme.Chart4
import com.aranthalion.controlfinanzas.ui.theme.Chart5
import java.text.NumberFormat
import java.util.*
import com.aranthalion.controlfinanzas.domain.usecase.GestionarPresupuestosUseCase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAnalisisScreen(
    viewModel: DashboardAnalisisViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedPieSlice by remember { mutableStateOf<PieChartData?>(null) }
    val scope = rememberCoroutineScope()

    // Obtener periodo actual para presupuestos
    val calendar = Calendar.getInstance()
    val periodoActual = String.format("%04d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    var presupuestosPorCategoria by remember { mutableStateOf(emptyList<com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity>()) }
    var presupuestoTotal by remember { mutableStateOf(0.0) }

    // Cargar presupuestos al iniciar - por ahora comentamos esta funcionalidad
    // La funcionalidad de presupuestos se implementará más adelante
    /*
    LaunchedEffect(periodoActual) {
        presupuestosPorCategoria = gestionarPresupuestosUseCase.obtenerPresupuestosPorPeriodo(periodoActual)
        presupuestoTotal = gestionarPresupuestosUseCase.obtenerSumaTotalPresupuesto(periodoActual)
    }
    */

    // Calcular gasto total del mes
    val gastoTotal = uiState.resumenFinanciero?.gastos ?: 0.0
    val porcentajeConsumido = if (presupuestoTotal > 0) (gastoTotal / presupuestoTotal).coerceAtMost(1.0) else 0.0
    val superoPresupuesto = presupuestoTotal > 0 && gastoTotal > presupuestoTotal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis Financiero") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filtros de período
            item {
                FiltrosPeriodo(
                    periodoSeleccionado = uiState.periodoSeleccionado,
                    onPeriodoChanged = { viewModel.cambiarPeriodo(it) }
                )
            }

            // Estado de carga
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Error
            uiState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Resumen financiero
            uiState.resumenFinanciero?.let { resumen ->
                item {
                    ResumenFinancieroCard(resumen = resumen)
                }
            }

            // Resumen de presupuesto global
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (superoPresupuesto) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Presupuesto mensual total",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = porcentajeConsumido.toFloat(),
                            color = if (superoPresupuesto) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gastado: ${NumberFormat.getCurrencyInstance(Locale("es", "CL")).format(gastoTotal)} / ${NumberFormat.getCurrencyInstance(Locale("es", "CL")).format(presupuestoTotal)}",
                            fontWeight = FontWeight.Medium
                        )
                        if (superoPresupuesto) {
                            Text(
                                text = "¡Has superado tu presupuesto mensual!",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (presupuestoTotal > 0) {
                            val restante = presupuestoTotal - gastoTotal
                            Text(
                                text = if (restante > presupuestoTotal * 0.3) "¡Vas bien este mes!" else "Atento, te queda poco presupuesto",
                                color = if (restante > presupuestoTotal * 0.3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Gráfico de barras Gasto vs Presupuesto por categoría
            if (uiState.movimientosPorCategoria.isNotEmpty() && presupuestosPorCategoria.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val barData = uiState.movimientosPorCategoria.mapNotNull { mov ->
                            val presupuesto = presupuestosPorCategoria.find { it.categoriaId == mov.categoriaId }?.monto
                            if (presupuesto != null) {
                                BarChartData(
                                    label = mov.categoriaNombre,
                                    value = mov.total.toFloat(),
                                    color = when {
                                        mov.total > presupuesto -> MaterialTheme.colorScheme.error
                                        mov.total > presupuesto * 0.8 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    budgetValue = presupuesto.toFloat()
                                )
                            } else null
                        }
                        BarChart(
                            data = barData,
                            title = "Gasto vs Presupuesto por Categoría",
                            modifier = Modifier.fillMaxWidth(),
                            showBudget = true
                        )
                    }
                }
            }

            // Alertas por categoría
            if (uiState.movimientosPorCategoria.isNotEmpty() && presupuestosPorCategoria.isNotEmpty()) {
                items(uiState.movimientosPorCategoria) { mov ->
                    val presupuesto = presupuestosPorCategoria.find { it.categoriaId == mov.categoriaId }?.monto
                    if (presupuesto != null && mov.total > presupuesto) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = "¡La categoría '${mov.categoriaNombre}' ha superado su presupuesto!",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            // Gráfico de tendencia mensual
            if (uiState.tendenciaMensual.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val chartData = uiState.tendenciaMensual.map { tendencia ->
                            BarChartData(
                                label = tendencia.mes,
                                value = tendencia.gastos.toFloat(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        BarChart(
                            data = chartData,
                            title = "Tendencia de Gastos Mensual",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Gráfico de torta por categorías
            if (uiState.movimientosPorCategoria.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val pieData = uiState.movimientosPorCategoria
                            .filter { it.tipo == "Gasto" }
                            .take(5)
                            .mapIndexed { index, movimiento ->
                                val colors = listOf(Chart1, Chart2, Chart3, Chart4, Chart5)
                                PieChartData(
                                    label = movimiento.categoriaNombre,
                                    value = movimiento.total.toFloat(),
                                    color = colors.getOrElse(index) { Chart1 },
                                    id = movimiento.categoriaId.toString()
                                )
                            }
                        
                        PieChart(
                            data = pieData,
                            title = "Gastos por Categoría",
                            onSliceClick = { slice ->
                                selectedPieSlice = if (selectedPieSlice?.id == slice.id) null else slice
                            }
                        )
                    }
                }
            }

            // Análisis por categorías (lista detallada)
            if (uiState.movimientosPorCategoria.isNotEmpty()) {
                item {
                    Text(
                        text = "Análisis Detallado por Categorías",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(uiState.movimientosPorCategoria.take(10)) { movimiento ->
                    MovimientoPorCategoriaItem(
                        movimiento = movimiento,
                        isSelected = selectedCategory == movimiento.categoriaId.toString(),
                        onClick = {
                            selectedCategory = if (selectedCategory == movimiento.categoriaId.toString()) {
                                null
                            } else {
                                movimiento.categoriaId.toString()
                            }
                        }
                    )
                }
            }

            // Detalle de comercios (si se selecciona una categoría del pie chart)
            selectedPieSlice?.let { slice ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Comercios en ${slice.label}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // Aquí se mostrarían los comercios específicos
                            // Por ahora mostramos un placeholder
                            Text(
                                text = "Detalle de transacciones para ${slice.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FiltrosPeriodo(
    periodoSeleccionado: PeriodoAnalisis,
    onPeriodoChanged: (PeriodoAnalisis) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Período de Análisis",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PeriodoAnalisis.values().forEach { periodo ->
                    FilterChip(
                        onClick = { onPeriodoChanged(periodo) },
                        label = { Text(periodo.descripcion) },
                        selected = periodoSeleccionado == periodo
                    )
                }
            }
        }
    }
}

@Composable
fun ResumenFinancieroCard(resumen: com.aranthalion.controlfinanzas.domain.usecase.ResumenFinanciero) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Resumen Financiero",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(
                    title = "Ingresos",
                    value = NumberFormat.getCurrencyInstance().format(resumen.ingresos),
                    icon = androidx.compose.material.icons.Icons.Default.ArrowBack,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                StatCard(
                    title = "Gastos",
                    value = NumberFormat.getCurrencyInstance().format(resumen.gastos),
                    icon = androidx.compose.material.icons.Icons.Default.ArrowBack,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                StatCard(
                    title = "Balance",
                    value = NumberFormat.getCurrencyInstance().format(resumen.balance),
                    icon = androidx.compose.material.icons.Icons.Default.ArrowBack,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Text(
                text = "Total transacciones: ${resumen.cantidadTransacciones}",
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun MovimientoPorCategoriaItem(
    movimiento: com.aranthalion.controlfinanzas.domain.usecase.MovimientoPorCategoria,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
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
                    text = movimiento.categoriaNombre,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${movimiento.tipo} • ${movimiento.cantidadTransacciones} transacciones",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = NumberFormat.getCurrencyInstance().format(movimiento.total),
                fontWeight = FontWeight.Bold,
                color = if (movimiento.tipo == "Ingreso") 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
        }
    }
} 