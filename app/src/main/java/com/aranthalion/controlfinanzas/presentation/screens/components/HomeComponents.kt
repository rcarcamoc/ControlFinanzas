package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.domain.usecase.ResumenPresupuestos
import com.aranthalion.controlfinanzas.presentation.components.BarChart
import com.aranthalion.controlfinanzas.presentation.components.PieChart
import com.aranthalion.controlfinanzas.presentation.components.PieChartData
import com.aranthalion.controlfinanzas.presentation.components.procesarDatosParaGrafico
import com.aranthalion.controlfinanzas.presentation.components.CustomIcons
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.presentation.components.PresupuestoInfo
import com.aranthalion.controlfinanzas.domain.usecase.EstadoPresupuesto
import androidx.compose.ui.graphics.Color

@Composable
fun GastoMensualCard(gastos: List<MovimientoEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tendencia de Gasto Mensual",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = CustomIcons.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            val chartData = procesarDatosParaGrafico(gastos, MaterialTheme.colorScheme.primary)
            
            BarChart(
                data = chartData,
                title = "Tendencia de Gasto Mensual",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
fun EstadoPresupuestoCard(
    resumenPresupuestos: ResumenPresupuestos?,
    navController: NavHostController
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estado del Presupuesto",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = CustomIcons.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            resumenPresupuestos?.let { resumen ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (resumen.porcentajeGastado.toFloat() / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            resumen.porcentajeGastado <= 80 -> MaterialTheme.colorScheme.primary
                            resumen.porcentajeGastado <= 90 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Gastado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatMoneyCLP(resumen.totalGastado),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Presupuestado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatMoneyCLP(resumen.totalPresupuestado),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            navController.navigate("presupuestos")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver Detalles del Presupuesto")
                    }
                }
            } ?: run {
                Text(
                    text = "No hay presupuestos configurados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GastoPorCategoriaCard(
    categorias: List<Categoria>,
    gastos: List<MovimientoEntity>,
    navController: NavHostController
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gasto por Categoría",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = CustomIcons.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            val gastosPorCategoria = gastos.groupBy { it.categoriaId }
                .mapValues { (_, movimientos) -> 
                    movimientos.sumOf { FormatUtils.normalizeAmount(it.monto) }
                }
            
            val pieChartData = categorias.mapIndexedNotNull { index, categoria ->
                val gasto = gastosPorCategoria[categoria.id] ?: 0.0
                val color = when (index % 5) {
                    0 -> com.aranthalion.controlfinanzas.ui.theme.Chart1
                    1 -> com.aranthalion.controlfinanzas.ui.theme.Chart2
                    2 -> com.aranthalion.controlfinanzas.ui.theme.Chart3
                    3 -> com.aranthalion.controlfinanzas.ui.theme.Chart4
                    else -> com.aranthalion.controlfinanzas.ui.theme.Chart5
                }
                if (gasto > 0) PieChartData(categoria.nombre, gasto.toFloat(), color, categoria.id.toString()) else null
            }.take(5)
            
            if (pieChartData.isNotEmpty()) {
                PieChart(
                    data = pieChartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                Text(
                    text = "No hay gastos categorizados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            
            Button(
                onClick = {
                    navController.navigate("dashboardAnalisis")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver Análisis Detallado")
            }
        }
    }
}

@Composable
fun TransaccionesSinClasificarCard(
    movimientosSinCategoria: Int,
    navController: NavHostController
) {
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
                Text(
                    text = "$movimientosSinCategoria transacciones sin clasificar",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Button(onClick = {
                    navController.navigate("transacciones")
                }) {
                    Text("Clasificar")
                }
            }
        }
    }
}

@Composable
fun UltimaActualizacionCard(currentTime: String) {
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
                imageVector = CustomIcons.Info,
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
