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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.presentation.components.StatCard
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAnalisisScreen(
    viewModel: DashboardAnalisisViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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

            // Análisis por categorías
            if (uiState.movimientosPorCategoria.isNotEmpty()) {
                item {
                    Text(
                        text = "Análisis por Categorías",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(uiState.movimientosPorCategoria.take(10)) { movimiento ->
                    MovimientoPorCategoriaItem(movimiento = movimiento)
                }
            }

            // Tendencia mensual
            if (uiState.tendenciaMensual.isNotEmpty()) {
                item {
                    Text(
                        text = "Tendencia Mensual",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(uiState.tendenciaMensual) { tendencia ->
                    TendenciaMensualItem(tendencia = tendencia)
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
                    icon = Icons.Default.ArrowBack,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                StatCard(
                    title = "Gastos",
                    value = NumberFormat.getCurrencyInstance().format(resumen.gastos),
                    icon = Icons.Default.ArrowBack,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                StatCard(
                    title = "Balance",
                    value = NumberFormat.getCurrencyInstance().format(resumen.balance),
                    icon = Icons.Default.ArrowBack,
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
fun MovimientoPorCategoriaItem(movimiento: com.aranthalion.controlfinanzas.domain.usecase.MovimientoPorCategoria) {
    Card(
        modifier = Modifier.fillMaxWidth()
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

@Composable
fun TendenciaMensualItem(tendencia: com.aranthalion.controlfinanzas.domain.usecase.TendenciaMensual) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = tendencia.mes,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Ingresos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = NumberFormat.getCurrencyInstance().format(tendencia.ingresos),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column {
                    Text(
                        text = "Gastos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = NumberFormat.getCurrencyInstance().format(tendencia.gastos),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Column {
                    Text(
                        text = "Balance",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = NumberFormat.getCurrencyInstance().format(tendencia.balance),
                        fontWeight = FontWeight.Medium,
                        color = if (tendencia.balance >= 0) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
} 