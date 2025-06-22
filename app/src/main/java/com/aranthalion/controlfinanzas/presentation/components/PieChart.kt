package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.*

data class PieChartData(
    val label: String,
    val value: Float,
    val color: Color,
    val id: String = ""
)

@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    title: String? = null,
    onSliceClick: ((PieChartData) -> Unit)? = null
) {
    if (data.isEmpty()) return
    
    val total = data.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0) return
    
    var selectedSlice by remember { mutableStateOf<PieChartData?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
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
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gráfico de torta
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(180.dp)
                        .clickable(enabled = onSliceClick != null) {
                            // Lógica de click en el gráfico
                        }
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = min(size.width, size.height) / 2 - 10f
                    var startAngle = 0f
                    
                    data.forEach { slice ->
                        val sweepAngle = (slice.value / total) * 360f
                        val isSelected = selectedSlice?.id == slice.id
                        
                        // Dibujar sector
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                        
                        // Borde si está seleccionado
                        if (isSelected) {
                            drawArc(
                                color = Color.Black,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = 3f)
                            )
                        }
                        
                        startAngle += sweepAngle
                    }
                }
            }
            
            // Leyenda
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.forEach { slice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            selectedSlice = if (selectedSlice?.id == slice.id) null else slice
                            onSliceClick?.invoke(slice)
                        }
                    ) {
                        Canvas(
                            modifier = Modifier.size(16.dp)
                        ) {
                            drawRect(
                                color = slice.color,
                                size = Size(16.dp.toPx(), 16.dp.toPx())
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = slice.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "%.1f%%".format((slice.value / total) * 100),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
} 