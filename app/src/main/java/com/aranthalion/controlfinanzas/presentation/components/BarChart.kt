package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

data class BarChartData(
    val label: String,
    val value: Float,
    val color: Color,
    val budgetValue: Float? = null // Valor del presupuesto (opcional)
)

@Composable
fun BarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    title: String? = null,
    showValues: Boolean = true,
    showBudget: Boolean = false // Nuevo parámetro para mostrar presupuesto
) {
    if (data.isEmpty()) return
    
    val maxValue = if (showBudget && data.any { it.budgetValue != null }) {
        data.maxOfOrNull { maxOf(it.value, it.budgetValue ?: 0f) } ?: 0f
    } else {
        data.maxOfOrNull { it.value } ?: 0f
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
    ) {
        // Título
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
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
                    .padding(bottom = 40.dp, top = 20.dp)
            ) {
                val barWidth = (size.width - (data.size - 1) * 8f) / data.size
                val maxBarHeight = size.height - 40f
                
                data.forEachIndexed { index, barData ->
                    val x = index * (barWidth + 8f)
                    
                    // Si hay presupuesto, dibujar barra de fondo (presupuesto)
                    if (showBudget && barData.budgetValue != null) {
                        val budgetHeight = if (maxValue > 0) {
                            (barData.budgetValue / maxValue) * maxBarHeight
                        } else 0f
                        
                        val budgetY = size.height - budgetHeight - 20f
                        
                        // Barra de fondo (presupuesto) - color gris claro
                        drawRect(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            topLeft = Offset(x, budgetY),
                            size = Size(barWidth, budgetHeight)
                        )
                    }
                    
                    // Barra principal (gasto)
                    val barHeight = if (maxValue > 0) {
                        (barData.value / maxValue) * maxBarHeight
                    } else 0f
                    
                    val y = size.height - barHeight - 20f
                    
                    // Dibujar barra de gasto
                    drawRect(
                        color = barData.color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                    )
                }
            }
            
            // Mostrar valores y porcentajes encima de las barras
            if (showValues) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp, top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    data.forEach { barData ->
                        val percentage = if (showBudget && barData.budgetValue != null && barData.budgetValue > 0) {
                            (barData.value / barData.budgetValue * 100).toInt()
                        } else null
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (percentage != null) {
                                Text(
                                    text = "${percentage}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        percentage > 100 -> MaterialTheme.colorScheme.error
                                        percentage > 80 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            } else {
                                Text(
                                    text = "%.0f".format(barData.value),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
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
                    .align(Alignment.BottomCenter)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { barData ->
                    Text(
                        text = barData.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                }
            }
        }
    }
} 