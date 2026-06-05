package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.presentation.components.BarChart
import com.aranthalion.controlfinanzas.presentation.components.BarChartData
import com.aranthalion.controlfinanzas.presentation.screens.DashboardAnalisisUiState
import com.aranthalion.controlfinanzas.presentation.screens.GranularidadTendencia
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun CardDistribucionGasto(
    data: DashboardAnalisisUiState.Success,
    expandedCategoryId: Long?,
    onCategoryClick: (Long) -> Unit,
    navController: NavController
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Dónde Gasto (Top 5)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (data.distribucionCategorias.isEmpty()) {
                Text(
                    text = "No hay gastos registrados en este período.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.distribucionCategorias.forEach { categoria ->
                        val isExpanded = expandedCategoryId == categoria.categoriaId

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCategoryClick(categoria.categoriaId) }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = categoria.nombreCategoria,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${categoria.porcentajeDelTotal.toInt()}% del total",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = FormatUtils.formatMoneyCLP(categoria.totalGastado),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { (categoria.porcentajeDelTotal / 100.0).toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // Progressive Disclosure: movimientos expansibles inline
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp, bottom = 4.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val catMovements = data.movimientos.filter { it.categoriaId == categoria.categoriaId }
                                    
                                    if (catMovements.isEmpty()) {
                                        Text(
                                            text = "No se encontraron movimientos registrados en esta categoría.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Transacciones del Período:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    navController.navigate("transacciones?categoriaId=${categoria.categoriaId}")
                                                }
                                            ) {
                                                Text(
                                                    text = "Ver todas",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        
                                        catMovements.take(5).forEach { mov ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = mov.descripcion,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(mov.fecha),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = FormatUtils.formatMoneyCLP(abs(mov.monto)),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }

                                        if (catMovements.size > 5) {
                                            Text(
                                                text = "Y ${catMovements.size - 5} transacciones más...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
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
fun CardEstadoPresupuesto(
    data: DashboardAnalisisUiState.Success,
    navController: NavController
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Estado del Presupuesto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (data.presupuestosConBrecha.isEmpty()) {
                Text(
                    text = "No tienes presupuestos configurados para este período.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.presupuestosConBrecha.forEach { presupuesto ->
                        val pct = presupuesto.porcentajeGastado
                        
                        // Determinación del color del semáforo (barra vs texto legible)
                        val barColor = when {
                            pct <= 80.0 -> Color(0xFF4CAF50) // Verde
                            pct <= 100.0 -> Color(0xFFFFC107) // Amber/Yellow
                            else -> Color(0xFFF44336) // Rojo
                        }
                        val textColor = when {
                            pct <= 80.0 -> Color(0xFF2E7D32) // Verde oscuro
                            pct <= 100.0 -> Color(0xFFB78103) // Amber oscuro (legible)
                            else -> Color(0xFFC62828) // Rojo oscuro
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    navController.navigate("transacciones?categoriaId=${presupuesto.categoriaId}")
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = presupuesto.nombreCategoria,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${FormatUtils.formatMoneyCLP(presupuesto.gastoActual)} / ${FormatUtils.formatMoneyCLP(presupuesto.presupuesto)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                    progress = { (pct / 100.0).coerceAtMost(1.0).toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${pct.toInt()}% gastado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val brecha = presupuesto.brechaPresupuesto
                                Text(
                                    text = if (brecha >= 0) "Disponible: ${FormatUtils.formatMoneyCLP(brecha)}" 
                                           else "Exceso: ${FormatUtils.formatMoneyCLP(abs(brecha))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (brecha >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
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
fun CardTendencias(
    data: DashboardAnalisisUiState.Success,
    onGranularidadChange: (GranularidadTendencia) -> Unit
) {
    val items = if (data.granularidadTendencia == GranularidadTendencia.MENSUAL) data.tendenciaMensual else data.tendenciaSemanal

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tendencia de Gastos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Selector de Granularidad
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (data.granularidadTendencia == GranularidadTendencia.MENSUAL) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onGranularidadChange(GranularidadTendencia.MENSUAL) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Mes",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (data.granularidadTendencia == GranularidadTendencia.MENSUAL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (data.granularidadTendencia == GranularidadTendencia.SEMANAL) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onGranularidadChange(GranularidadTendencia.SEMANAL) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Semana",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (data.granularidadTendencia == GranularidadTendencia.SEMANAL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = "No hay datos de tendencias suficientes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                // Escalar valores a miles de CLP para prevenir overlapping en los labels del chart
                val scaledData = items.map { item ->
                    BarChartData(
                        label = item.etiqueta,
                        value = (item.gastos / 1000f).toFloat(),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "(Valores expresados en miles de CLP)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BarChart(
                    data = scaledData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}
