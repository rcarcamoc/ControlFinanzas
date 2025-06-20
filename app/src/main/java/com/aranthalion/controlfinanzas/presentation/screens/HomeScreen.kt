package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aranthalion.controlfinanzas.presentation.components.StatCard
import com.aranthalion.controlfinanzas.presentation.movimientos.MovimientosViewModel
import com.aranthalion.controlfinanzas.presentation.movimientos.MovimientosUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: MovimientosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totales by viewModel.totales.collectAsState()
    
    val currentTime = remember { 
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                    val totalGastos = gastos.sumOf { it.monto }
                    val totalIngresos = ingresos.sumOf { it.monto }
                    val balance = totalIngresos - totalGastos
                    
                    // Tarjetas de estadísticas
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(200.dp)
                    ) {
                        item {
                            StatCard(
                                title = "Gasto Total",
                                value = "$${totalGastos.toLong()}",
                                icon = Icons.Default.Remove,
                                description = "Este mes"
                            )
                        }
                        item {
                            StatCard(
                                title = "Ingresos",
                                value = "$${totalIngresos.toLong()}",
                                icon = Icons.Default.Add,
                                description = "Este mes"
                            )
                        }
                        item {
                            StatCard(
                                title = "Balance",
                                value = "$${balance.toLong()}",
                                icon = Icons.Default.AccountBalance,
                                description = if (balance >= 0) "Positivo" else "Negativo"
                            )
                        }
                        item {
                            StatCard(
                                title = "Transacciones",
                                value = "${movimientos.size}",
                                icon = Icons.Default.List,
                                description = "Total registradas"
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
                        title = "Movimientos",
                        icon = Icons.Default.Add,
                        description = "Registra ingresos y gastos",
                        onClick = { navController.navigate("movimientos_manuales") }
                    )
                }
                item {
                    MenuCard(
                        title = "Historial",
                        icon = Icons.Default.List,
                        description = "Consulta transacciones",
                        onClick = { navController.navigate("historial") }
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
                        icon = Icons.Default.Upload,
                        description = "Carga extractos bancarios",
                        onClick = { navController.navigate("importar_excel") }
                    )
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