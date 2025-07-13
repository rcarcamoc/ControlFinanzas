package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.domain.usecase.ResumenAporteProporcional
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistorialAportesCharts(
    historial: List<ResumenAporteProporcional>,
    onPeriodoSelected: (String) -> Unit
) {
    if (historial.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No hay datos históricos disponibles",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 96.dp // Extra padding para navegación
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gráfico de evolución de gastos distribuibles
        item {
            EvolucionGastosChart(historial = historial)
        }

        // Gráfico de evolución de sueldos totales
        item {
            EvolucionSueldosChart(historial = historial)
        }

        // Gráfico de evolución de aportes por persona
        item {
            EvolucionAportesPorPersonaChart(historial = historial)
        }

        // Tabla resumen histórica
        item {
            TablaResumenHistorica(historial = historial, onPeriodoSelected = onPeriodoSelected)
        }
    }
}

@Composable
fun EvolucionGastosChart(historial: List<ResumenAporteProporcional>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Evolución de Gastos Distribuibles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Gráfico simple de barras usando texto
            historial.sortedBy { it.periodo }.forEach { resumen ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = resumen.periodo,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = FormatUtils.formatMoneyCLP(resumen.totalADistribuir),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Barra visual simple
                val maxGastos = historial.maxOfOrNull { it.totalADistribuir } ?: 1.0
                val progress = if (maxGastos > 0) (resumen.totalADistribuir / maxGastos).toFloat() else 0f
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        }
    }
}

@Composable
fun EvolucionSueldosChart(historial: List<ResumenAporteProporcional>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Evolución de Sueldos Totales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            historial.sortedBy { it.periodo }.forEach { resumen ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = resumen.periodo,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = FormatUtils.formatMoneyCLP(resumen.totalSueldos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                val maxSueldos = historial.maxOfOrNull { it.totalSueldos } ?: 1.0
                val progress = if (maxSueldos > 0) (resumen.totalSueldos / maxSueldos).toFloat() else 0f
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun EvolucionAportesPorPersonaChart(historial: List<ResumenAporteProporcional>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Evolución de Aportes por Persona",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Obtener todas las personas únicas
            val personas = historial.flatMap { it.aportes.map { aporte -> aporte.nombrePersona } }.distinct()
            
            personas.forEach { persona ->
                Text(
                    text = persona,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                historial.sortedBy { it.periodo }.forEach { resumen ->
                    val aporte = resumen.aportes.find { it.nombrePersona == persona }
                    if (aporte != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = resumen.periodo,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = FormatUtils.formatMoneyCLP(aporte.montoAporte),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TablaResumenHistorica(
    historial: List<ResumenAporteProporcional>,
    onPeriodoSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Resumen Histórico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            historial.sortedByDescending { it.periodo }.forEach { resumen ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = resumen.periodo,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { onPeriodoSelected(resumen.periodo) }) {
                                Text("Ver Detalle")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Gastos: ${FormatUtils.formatMoneyCLP(resumen.totalADistribuir)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Sueldos: ${FormatUtils.formatMoneyCLP(resumen.totalSueldos)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (resumen.totalSueldos > 0) {
                            Text(
                                text = "Porcentaje: ${String.format("%.1f", (resumen.totalADistribuir / resumen.totalSueldos) * 100)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (resumen.aportes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Personas: ${resumen.aportes.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
} 