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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aranthalion.controlfinanzas.presentation.components.StatCard
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.presentation.screens.MovimientosViewModel
import com.aranthalion.controlfinanzas.presentation.screens.MovimientosUiState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: MovimientosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totales by viewModel.totales.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val currentTime = remember { 
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    }

    // Estado global de período
    var periodoGlobal by rememberSaveable { mutableStateOf(obtenerPeriodoActual()) }
    val periodosDisponibles = remember { generarPeriodosDisponibles() }
    var expanded by remember { mutableStateOf(false) }

    // Refrescar al volver a primer plano
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.cargarMovimientos()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "FinaVision",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Control de Finanzas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    // Espacio para mantener el título centrado
                },
                actions = {
                    IconButton(onClick = { navController.navigate("configuracion") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Selector de período/ciclo de facturación
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = periodoGlobal,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Período/Ciclo de facturación") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().clickable { expanded = true }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    periodosDisponibles.forEach { periodo ->
                        DropdownMenuItem(
                            text = { Text(periodo) },
                            onClick = {
                                periodoGlobal = periodo
                                expanded = false
                                // Aquí deberías propagar el cambio a los demás ViewModels
                            }
                        )
                    }
                }
            }

            // Dashboard Stats simplificado
            when (uiState) {
                is MovimientosUiState.Success -> {
                    val movimientos = (uiState as MovimientosUiState.Success).movimientos
                    val gastos = movimientos.filter { it.tipo == "GASTO" }
                    val ingresos = movimientos.filter { it.tipo == "INGRESO" }
                    
                    // Normalizar y redondear montos
                    val totalGastos = FormatUtils.roundToTwoDecimals(
                        gastos.sumOf { FormatUtils.normalizeAmount(it.monto) }
                    )
                    val totalIngresos = FormatUtils.roundToTwoDecimals(
                        ingresos.sumOf { FormatUtils.normalizeAmount(it.monto) }
                    )
                    val balance = FormatUtils.roundToTwoDecimals(totalIngresos - totalGastos)
                    
                    // Tarjetas de estadísticas simplificadas (solo 3)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(120.dp)
                    ) {
                        item {
                            StatCard(
                                title = "Gastos",
                                value = FormatUtils.formatMoneyCLP(totalGastos),
                                icon = Icons.Default.KeyboardArrowDown,
                                description = "Este mes",
                                isMonetary = false,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .height(100.dp)
                            )
                        }
                        item {
                            StatCard(
                                title = "Ingresos",
                                value = FormatUtils.formatMoneyCLP(totalIngresos),
                                icon = Icons.Default.Add,
                                description = "Este mes",
                                isMonetary = false,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .height(100.dp)
                            )
                        }
                        item {
                            val movimientosSinCategoria = movimientos.count { it.categoriaId == null }
                            StatCard(
                                title = "Pendientes",
                                value = movimientosSinCategoria.toString(),
                                icon = Icons.AutoMirrored.Filled.List,
                                description = "Sin clasificar",
                                isMonetary = false,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .height(100.dp)
                            )
                        }
                    }
                }
                else -> {
                    // Placeholder mientras carga
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(120.dp)
                    ) {
                        items(3) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }

            // Navegación principal
            Text(
                text = "Funciones Principales",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MenuCard(
                        title = "Transacciones",
                        icon = Icons.Default.Add,
                        description = "Gestiona ingresos y gastos",
                        onClick = { navController.navigate("transacciones") }
                    )
                }
                item {
                    MenuCard(
                        title = "Categorías",
                        icon = Icons.Default.List,
                        description = "Administra categorías",
                        onClick = { navController.navigate("categorias") }
                    )
                }
                item {
                    MenuCard(
                        title = "Importar Excel",
                        icon = Icons.Default.Add,
                        description = "Carga extractos bancarios",
                        onClick = { navController.navigate("importar_excel") }
                    )
                }
                item {
                    MenuCard(
                        title = "Clasificación",
                        icon = Icons.Default.Edit,
                        description = "Revisar transacciones pendientes",
                        onClick = { navController.navigate("clasificacion_pendiente") }
                    )
                }
                item {
                    MenuCard(
                        title = "Análisis",
                        icon = Icons.Default.List,
                        description = "Dashboard y reportes",
                        onClick = { navController.navigate("dashboardAnalisis") }
                    )
                }
                item {
                    MenuCard(
                        title = "Aporte Proporcional",
                        icon = Icons.Default.Person,
                        description = "Cálculo de aportes en pareja",
                        onClick = { navController.navigate("aporte_proporcional") }
                    )
                }
                item {
                    MenuCard(
                        title = "Presupuestos",
                        icon = Icons.Default.List,
                        description = "Control de presupuestos",
                        onClick = { navController.navigate("presupuestos") }
                    )
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
}

@Composable
private fun MenuCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
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