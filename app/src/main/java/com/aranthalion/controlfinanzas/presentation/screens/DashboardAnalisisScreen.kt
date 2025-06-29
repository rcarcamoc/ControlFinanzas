package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.presentation.components.BarChart
import com.aranthalion.controlfinanzas.presentation.components.PieChart
import androidx.compose.ui.graphics.Color
import com.aranthalion.controlfinanzas.presentation.components.BarChartData
import com.aranthalion.controlfinanzas.presentation.components.PieChartData
import android.util.Log
import androidx.compose.runtime.LaunchedEffect

@Composable
fun DashboardAnalisisScreen(
    viewModel: DashboardAnalisisViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.value
    
    val barColors = listOf(
        Color(0xFF2196F3), Color(0xFF42A5F5), Color(0xFF64B5F6), Color(0xFF90CAF9), Color(0xFF1976D2)
    )
    val pieColors = listOf(
        Color(0xFF4CAF50), Color(0xFF81C784), Color(0xFFA5D6A7), Color(0xFFC8E6C9), Color(0xFF388E3C)
    )
    
    // Logs defensivos
    LaunchedEffect(uiState) {
        Log.d("DashboardAnalisisScreen", "Renderizando análisis: $uiState")
        if (uiState.evolucionHistorica.isEmpty()) {
            Log.w("DashboardAnalisisScreen", "evolucionHistorica está vacía")
        }
        if (uiState.distribucionCategorias.isEmpty()) {
            Log.w("DashboardAnalisisScreen", "distribucionCategorias está vacía")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Análisis Financiero Detallado",
            style = MaterialTheme.typography.headlineMedium
        )
        // KPIs jerárquicos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Placeholder para KPIs
            Text("Saldo actual: $${uiState.saldoActual}")
            Text("Tasa de ahorro: ${uiState.tasaAhorro}%")
        }
        // Gráfico de evolución histórica
        Text("Evolución histórica de ingresos y egresos")
        if (uiState.evolucionHistorica.isNotEmpty() && uiState.evolucionHistorica.all { it.first.isNotBlank() }) {
            BarChart(data = uiState.evolucionHistorica.mapIndexed { i, (label, value) ->
                BarChartData(label, value.toFloat(), barColors[i % barColors.size])
            })
        } else {
            Text("No hay datos suficientes para mostrar el gráfico de evolución histórica", color = MaterialTheme.colorScheme.error)
        }
        // Gráfico de composición por categoría
        Text("Distribución de gastos por categoría")
        if (uiState.distribucionCategorias.isNotEmpty() && uiState.distribucionCategorias.all { it.first.isNotBlank() }) {
            PieChart(data = uiState.distribucionCategorias.mapIndexed { i, (label, value) ->
                PieChartData(label, value.toFloat(), pieColors[i % pieColors.size])
            })
        } else {
            Text("No hay datos suficientes para mostrar el gráfico de categorías", color = MaterialTheme.colorScheme.error)
        }
        // Filtros y drill-down (placeholder)
        Button(onClick = { /* TODO: abrir filtros */ }) {
            Text("Filtrar por fecha/categoría")
        }
    }
} 