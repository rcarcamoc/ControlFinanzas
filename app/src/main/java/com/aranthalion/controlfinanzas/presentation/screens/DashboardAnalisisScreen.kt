package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorGlobal
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import com.aranthalion.controlfinanzas.presentation.screens.components.AlertaBanner
import com.aranthalion.controlfinanzas.presentation.screens.components.CardResumenIa
import com.aranthalion.controlfinanzas.presentation.screens.components.CardRitmoGasto
import com.aranthalion.controlfinanzas.presentation.screens.components.CardGastosHormiga
import com.aranthalion.controlfinanzas.presentation.screens.components.CardDistribucionGasto
import com.aranthalion.controlfinanzas.presentation.screens.components.CardEstadoPresupuesto
import com.aranthalion.controlfinanzas.presentation.screens.components.CardTendencias

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAnalisisScreen(
    navController: NavController,
    viewModel: DashboardAnalisisViewModel = hiltViewModel(),
    periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
) {
    val periodoSeleccionado by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    val scopeSeleccionado by periodoGlobalViewModel.scopeSeleccionado.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(periodoSeleccionado, scopeSeleccionado) {
        viewModel.cargarAnalisis(periodoSeleccionado)
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is DashboardAnalisisUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Calculando análisis financiero...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is DashboardAnalisisUiState.Success -> {
                    val data = uiState as DashboardAnalisisUiState.Success
                    DashboardContent(
                        data = data,
                        viewModel = viewModel,
                        navController = navController
                    )
                }

                is DashboardAnalisisUiState.Error -> {
                    val errorState = uiState as DashboardAnalisisUiState.Error
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = errorState.mensaje,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { errorState.onRetry() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Reintentar", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    data: DashboardAnalisisUiState.Success,
    viewModel: DashboardAnalisisViewModel,
    navController: NavController
) {
    var expandedCategoryId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Selector de período global
        item {
            Spacer(modifier = Modifier.height(8.dp))
            PeriodoSelectorGlobal()
        }

        // 1. Alertas (si existen)
        if (data.alertas.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.alertas.forEach { alerta ->
                        AlertaBanner(alerta = alerta)
                    }
                }
            }
        }

        // 2. Resumen IA del mes
        item {
            CardResumenIa(data = data)
        }

        // 3. Dónde Gasto (Distribución de categorías con drill-down)
        item {
            CardDistribucionGasto(
                data = data,
                expandedCategoryId = expandedCategoryId,
                onCategoryClick = { catId ->
                    expandedCategoryId = if (expandedCategoryId == catId) null else catId
                },
                navController = navController
            )
        }

        // 4. Estado del Presupuesto (Comparación con límites)
        item {
            CardEstadoPresupuesto(data = data, navController = navController)
        }

        // 5. Ritmo de Gasto (Pace indicator + proyección)
        item {
            CardRitmoGasto(data = data)
        }

        // 6. Gastos Hormiga
        if (data.gastosHormiga.isNotEmpty()) {
            item {
                CardGastosHormiga(data = data)
            }
        }

        // 7. Tendencia Mensual vs Semanal (con selector de granularidad)
        item {
            CardTendencias(
                data = data,
                onGranularidadChange = { viewModel.cambiarGranularidadTendencia(it) }
            )
        }
    }
}