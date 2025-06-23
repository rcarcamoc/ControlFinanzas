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
            // Dashboard Stats
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
                    
                    // Tarjetas de estadísticas
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(240.dp)
                    ) {
                        item {
                            StatCard(
                                title = "Gasto Total",
                                value = totalGastos.toString(),
                                icon = Icons.Default.Add,
                                description = "Este mes",
                                isMonetary = true,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .height(110.dp)
                            )
                        }
                        item {
                            StatCard(
                                title = "Ingresos",
                                value = totalIngresos.toString(),
                                icon = Icons.Default.Add,
                                description = "Este mes",
                                isMonetary = true,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .height(110.dp)
                            )
                        }
                        item {
                            StatCard(
                                title = "Balance",
                                value = balance.toString(),
                                icon = Icons.Default.Add,
                                description = if (balance >= 0) "Positivo" else "Negativo",
                                isMonetary = true,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .height(110.dp)
                            )
                        }
                        item {
                            val movimientosSinCategoria = movimientos.count { it.categoriaId == null }
                            StatCard(
                                title = "Sin Clasificar",
                                value = movimientosSinCategoria.toString(),
                                icon = Icons.AutoMirrored.Filled.List,
                                description = "Pendientes de categoría",
                                isMonetary = false,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .height(110.dp)
                            )
                        }
                    }
                }
                else -> {
                    // Placeholder mientras carga
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(4) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
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
                        icon = Icons.AutoMirrored.Filled.List,
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
            }

            // KPIs adicionales
            val porcentajePresupuestoGastado = 78 // Simulado
            val top3Sobreconsumo = listOf(
                Triple("Alimentación", 120, 60000),
                Triple("Transporte", 110, 22000),
                Triple("Ocio", 105, 21000)
            )
            // Fila de KPIs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(
                    title = "% Presupuesto Gastado",
                    value = "$porcentajePresupuestoGastado%",
                    icon = Icons.Default.Warning,
                    description = "Este mes",
                    isMonetary = false
                )
            }
            // Transacciones pendientes de clasificación
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
                        Button(onClick = { navController.navigate("clasificacionPendiente") }) {
                            Text("Clasificar")
                        }
                    }
                }
            }
            // Top 3 categorías con sobreconsumo
            Text("Top 3 categorías con sobreconsumo proyectado", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                top3Sobreconsumo.forEach { (nombre, porcentaje, monto) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(nombre, modifier = Modifier.weight(1f))
                            Text("$porcentaje%", color = MaterialTheme.colorScheme.error)
                            Text("$monto CLP", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            // Acceso a análisis detallado
            Button(
                onClick = { navController.navigate("dashboardAnalisis") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver análisis detallado")
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