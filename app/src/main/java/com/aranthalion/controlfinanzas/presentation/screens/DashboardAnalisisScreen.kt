package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorGlobal
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import com.aranthalion.controlfinanzas.data.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAnalisisScreen(
    navController: androidx.navigation.NavController,
    viewModel: DashboardAnalisisViewModel = hiltViewModel(),
    periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
) {
    val periodoSeleccionado by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(periodoSeleccionado) {
        viewModel.cargarAnalisis(periodoSeleccionado)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis Financiero") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            item {
                PeriodoSelectorGlobal()
            }
            
            when (uiState) {
                is DashboardAnalisisUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                is DashboardAnalisisUiState.Success -> {
                    val data = (uiState as DashboardAnalisisUiState.Success)
                    
                    // Predicción de Cierre del Período
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Predicción de Cierre - ${periodoSeleccionado}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Progreso del período
                                val progresoPeriodo = calcularProgresoPeriodo(periodoSeleccionado)
                                Text(
                                    "Progreso del período: ${progresoPeriodo.toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                LinearProgressIndicator(
                                    progress = (progresoPeriodo / 100f).toFloat(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                                )
                                
                                // Predicción de gasto total
                                val prediccionTotal = data.predicciones.sumOf { (it as PrediccionPrueba).prediccion }
                                Text(
                                    "Gasto Predicho: ${FormatUtils.formatMoneyCLP(prediccionTotal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    // Resumen de KPIs
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Resumen Ejecutivo",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    "KPIs Generados: ${data.kpis.size}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Tendencias: ${data.tendencias.size}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Categorías Analizadas: ${data.analisisCategorias.size}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Predicciones: ${data.predicciones.size}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    // Top 5 Categorías con Mayor Gasto
                    if (data.analisisCategorias.isNotEmpty()) {
                        item {
                            Text(
                                "Top 5 Categorías",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        val topCategorias = data.analisisCategorias.take(5)
                        topCategorias.forEach { categoria ->
                            item {
                                val cat = categoria as CategoriaPrueba
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
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
                                                cat.nombreCategoria,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "${cat.porcentajeDelTotal.toInt()}% del total",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Promedio diario: ${FormatUtils.formatMoneyCLP(cat.promedioDiario)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        Column(
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                FormatUtils.formatMoneyCLP(cat.totalGastado),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = when (cat.tendencia) {
                                                    "AUMENTO" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                                    "DISMINUCION" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            ) {
                                                Text(
                                                    cat.tendencia,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when (cat.tendencia) {
                                                        "AUMENTO" -> MaterialTheme.colorScheme.error
                                                        "DISMINUCION" -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Predicciones para Próximo Mes
                    if (data.predicciones.isNotEmpty()) {
                        item {
                            Text(
                                "Predicciones para Próximo Mes",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        val topPredicciones = data.predicciones.take(5)
                        topPredicciones.forEach { prediccion ->
                            item {
                                val pred = prediccion as PrediccionPrueba
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                pred.nombreCategoria,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                FormatUtils.formatMoneyCLP(pred.prediccion),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            "Intervalo: ${FormatUtils.formatMoneyCLP(pred.intervaloConfianza.first)} - ${FormatUtils.formatMoneyCLP(pred.intervaloConfianza.second)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        LinearProgressIndicator(
                                            progress = pred.confiabilidad.toFloat(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        
                                        Text(
                                            "Confiabilidad: ${(pred.confiabilidad * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                is DashboardAnalisisUiState.Error -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    (uiState as DashboardAnalisisUiState.Error).mensaje,
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

@Composable
private fun calcularProgresoPeriodo(periodo: String): Double {
    val calendar = java.util.Calendar.getInstance()
    val currentYear = calendar.get(java.util.Calendar.YEAR)
    val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
    
    val periodoYear = periodo.substring(0, 4).toInt()
    val periodoMonth = periodo.substring(5, 7).toInt()
    
    return if (currentYear == periodoYear && currentMonth == periodoMonth) {
        val dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        (dayOfMonth.toDouble() / daysInMonth) * 100
    } else if (currentYear > periodoYear || (currentYear == periodoYear && currentMonth > periodoMonth)) {
        100.0
    } else {
        0.0
    }
}

sealed class DashboardAnalisisUiState {
    object Loading : DashboardAnalisisUiState()
    data class Success(
        val kpis: List<Any>,
        val tendencias: List<Any>,
        val analisisCategorias: List<Any>,
        val predicciones: List<Any>
    ) : DashboardAnalisisUiState()
    data class Error(val mensaje: String) : DashboardAnalisisUiState()
} 