package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.max

data class BarChartData(
    val label: String,
    val value: Float,
    val color: Color,
    val budgetValue: Float? = null
)

@Composable
fun BarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    title: String? = null,
    showValues: Boolean = true,
    showBudget: Boolean = false
) {
    if (data.isEmpty()) return
    
    val maxValue = if (showBudget && data.any { it.budgetValue != null }) {
        data.maxOfOrNull { maxOf(it.value, it.budgetValue ?: 0f) } ?: 0f
    } else {
        data.maxOfOrNull { it.value } ?: 0f
    }
    
    // Animación para las barras
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "barAnimation"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(20.dp)
        ) {
            // Título
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Leyenda
            if (showBudget) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gasto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Presupuesto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Gráfico
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 50.dp, top = 20.dp)
                ) {
                    val barWidth = (size.width - (data.size - 1) * 12f) / data.size
                    val maxBarHeight = size.height - 40f
                    
                    data.forEachIndexed { index, barData ->
                        val x = index * (barWidth + 12f)
                        
                        // Barra de presupuesto (fondo)
                        if (showBudget && barData.budgetValue != null) {
                            val budgetHeight = if (maxValue > 0) {
                                (barData.budgetValue / maxValue) * maxBarHeight
                            } else 0f
                            
                            val budgetY = size.height - budgetHeight - 20f
                            
                            drawRect(
                                color = Color.Gray.copy(alpha = 0.3f),
                                topLeft = Offset(x, budgetY),
                                size = Size(barWidth, budgetHeight)
                            )
                        }
                        
                        // Barra principal (gasto)
                        val barHeight = if (maxValue > 0) {
                            (barData.value / maxValue) * maxBarHeight * animatedProgress
                        } else 0f
                        
                        val y = size.height - barHeight - 20f
                        
                        drawRect(
                            color = barData.color,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight)
                        )
                        
                        // Borde
                        drawRect(
                            color = barData.color.copy(alpha = 0.6f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                        )
                    }
                }
            }
            
            // Valores encima de las barras
            if (showValues) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 50.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    data.forEachIndexed { index, barData ->
                        val percentage = if (showBudget && barData.budgetValue != null && barData.budgetValue > 0) {
                            (barData.value / barData.budgetValue * 100).toInt()
                        } else null
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (percentage != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            percentage > 100 -> MaterialTheme.colorScheme.errorContainer
                                            percentage > 80 -> MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        }
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Text(
                                        text = "${percentage}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            percentage > 100 -> MaterialTheme.colorScheme.onErrorContainer
                                            percentage > 80 -> MaterialTheme.colorScheme.onTertiaryContainer
                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Text(
                                    text = "%.0f".format(barData.value),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            
            // Etiquetas del eje X
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { barData ->
                    Text(
                        text = barData.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
} 