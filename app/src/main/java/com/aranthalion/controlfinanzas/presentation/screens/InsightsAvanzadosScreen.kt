package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aranthalion.controlfinanzas.presentation.components.*
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import com.aranthalion.controlfinanzas.domain.usecase.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsAvanzadosScreen(
    navController: NavController,
    viewModel: InsightsAvanzadosViewModel = hiltViewModel(),
    periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
) {
    val periodoSeleccionado by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(periodoSeleccionado) {
        viewModel.cargarInsights(periodoSeleccionado)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights y Agrupaciones") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.cargarInsights(periodoSeleccionado, force = true) }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar insights")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PeriodoSelectorGlobal()
            }
            
            when (uiState) {
                is InsightsAvanzadosUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                is InsightsAvanzadosUiState.Success -> {
                    val data = (uiState as InsightsAvanzadosUiState.Success)
                    
                    // Score de Comportamiento
                    item {
                        ScoreComportamientoCard(
                            score = data.resumen.scoreComportamiento,
                            areasMejora = data.resumen.areasMejora,
                            fortalezas = data.resumen.fortalezas
                        )
                    }
                    
                    // Insights Críticos
                    val insightsCriticos = viewModel.obtenerInsightsCriticos()
                    if (insightsCriticos.isNotEmpty()) {
                        item {
                            Text(
                                "Insights Críticos",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            InsightsAvanzadosCard(
                                insights = insightsCriticos,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Insights Positivos
                    val insightsPositivos = viewModel.obtenerInsightsPositivos()
                    if (insightsPositivos.isNotEmpty()) {
                        item {
                            Text(
                                "Insights Positivos",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            InsightsAvanzadosCard(
                                insights = insightsPositivos,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Agrupaciones Más Relevantes
                    val agrupacionesRelevantes = viewModel.obtenerAgrupacionesMasRelevantes()
                    if (agrupacionesRelevantes.isNotEmpty()) {
                        item {
                            Text(
                                "Agrupaciones Más Relevantes",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            AgrupacionesCard(
                                agrupaciones = agrupacionesRelevantes,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Recomendaciones de Alta Prioridad
                    val recomendacionesAlta = viewModel.obtenerRecomendacionesAltaPrioridad()
                    if (recomendacionesAlta.isNotEmpty()) {
                        item {
                            Text(
                                "Recomendaciones Prioritarias",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            RecomendacionesCard(
                                recomendaciones = recomendacionesAlta,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Patrones Temporales
                    val patronesFrecuentes = viewModel.obtenerPatronesMasFrecuentes()
                    if (patronesFrecuentes.isNotEmpty()) {
                        item {
                            Text(
                                "Patrones Temporales",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            PatronesTemporalesCard(
                                patrones = patronesFrecuentes,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Resumen Estadístico
                    item {
                        ResumenEstadisticoCard(
                            resumen = data.resumen,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                is InsightsAvanzadosUiState.Error -> {
                    item {
                        ErrorCard(
                            error = (uiState as InsightsAvanzadosUiState.Error).mensaje,
                            onDismiss = { (uiState as InsightsAvanzadosUiState.Error).onRetry() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreComportamientoCard(
    score: Int,
    areasMejora: List<String>,
    fortalezas: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Score de Comportamiento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // Score Circular
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.size(120.dp),
                    strokeWidth = 8.dp,
                    color = when {
                        score >= 80 -> Color(0xFF4CAF50) // Verde
                        score >= 60 -> Color(0xFFFF9800) // Amarillo
                        else -> Color(0xFFF44336) // Rojo
                    }
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // Áreas de Mejora
            if (areasMejora.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Áreas de Mejora",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    areasMejora.forEach { area ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = area,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // Fortalezas
            if (fortalezas.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Fortalezas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    fortalezas.forEach { fortaleza ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fortaleza,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatronesTemporalesCard(
    patrones: List<AnalisisPatronTemporal>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Patrones Temporales",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                patrones.forEach { patron ->
                    PatronTemporalItem(patron = patron)
                }
            }
        }
    }
}

@Composable
private fun PatronTemporalItem(patron: AnalisisPatronTemporal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = patron.patron,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PatronStat(
                    label = "Frecuencia",
                    value = "${patron.frecuencia}",
                    icon = Icons.Default.Refresh
                )
                PatronStat(
                    label = "Promedio",
                    value = "${patron.montoPromedio.toInt()}",
                    icon = Icons.Default.Info
                )
                PatronStat(
                    label = "Tendencia",
                    value = patron.tendencia,
                    icon = Icons.Default.Info
                )
            }
            
            patron.categoriaNombre?.let { categoria ->
                AssistChip(
                    onClick = { },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    label = {
                        Text(
                            text = categoria,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PatronStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResumenEstadisticoCard(
    resumen: ResumenInsights,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Resumen Estadístico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResumenStat(
                    label = "Insights",
                    value = "${resumen.insightsGenerados}",
                    icon = Icons.Default.Info
                )
                ResumenStat(
                    label = "Críticos",
                    value = "${resumen.insightsCriticos}",
                    icon = Icons.Default.Warning
                )
                ResumenStat(
                    label = "Agrupaciones",
                    value = "${resumen.agrupacionesEncontradas}",
                    icon = Icons.Default.List
                )
                ResumenStat(
                    label = "Recomendaciones",
                    value = "${resumen.recomendacionesGeneradas}",
                    icon = Icons.Default.Star
                )
            }
        }
    }
}

@Composable
private fun ResumenStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
} 