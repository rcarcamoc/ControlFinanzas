package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background

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
    var tooltipOffset by remember { mutableStateOf(IntOffset.Zero) }
    val density = LocalDensity.current
    
    // Paleta de colores accesible
    val palette = listOf(
        Color(0xFF1976D2), // Azul
        Color(0xFF388E3C), // Verde
        Color(0xFFFBC02D), // Amarillo
        Color(0xFFD32F2F), // Rojo
        Color(0xFF7B1FA2), // Morado
        Color(0xFF0288D1), // Celeste
        Color(0xFFF57C00), // Naranja
        Color(0xFF388E3C)  // Verde oscuro
    )
    
    // Animación para el gráfico
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1500, easing = EaseOutCubic),
        label = "pieAnimation"
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            // Gráfico circular
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
            ) {
                val size = minOf(maxWidth, maxHeight)
                Canvas(modifier = Modifier.size(size)) {
                    var startAngle = -90f
                    data.forEachIndexed { i, slice ->
                        val color = slice.color.takeIf { it != Color.Unspecified } ?: palette[i % palette.size]
                        val sweep = (slice.value / total) * 360f * animatedProgress
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true
                        )
                        startAngle += sweep
                    }
                }
            }
            // Leyenda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
                ) {
                data.take(palette.size).forEachIndexed { i, slice ->
                    val color = slice.color.takeIf { it != Color.Unspecified } ?: palette[i % palette.size]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color = color, shape = MaterialTheme.shapes.small)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                            text = "${slice.label} (${slice.value.toInt()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPieSlice(
    color: Color,
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    isSelected: Boolean
) {
    // Sombra (efecto 3D)
    drawArc(
        color = Color.Black.copy(alpha = 0.1f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = true,
        topLeft = Offset(center.x - radius + 2f, center.y - radius + 2f),
        size = Size(radius * 2, radius * 2)
    )
    
    // Sector principal
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
    
    // Efecto de gradiente (simulado)
    drawArc(
        color = color.copy(alpha = 0.3f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
    
    // Borde si está seleccionado
    if (isSelected) {
        drawArc(
            color = Color.White,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 3f)
        )
    }
} 