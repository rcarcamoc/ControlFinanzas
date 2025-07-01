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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Gráfico de torta
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    var tapPosition by remember { mutableStateOf<Offset?>(null) }
                    Canvas(
                        modifier = Modifier
                            .size(200.dp)
                            .pointerInput(data, selectedSlice) {
                                detectTapGestures { offset ->
                                    tapPosition = offset
                                }
                            }
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = min(size.width, size.height) / 2 - 15f
                        var startAngle = -90f // Empezar desde arriba
                        var tappedSlice: PieChartData? = null
                        data.forEach { slice ->
                            val sweepAngle = (slice.value / total) * 360f * animatedProgress
                            val isSelected = selectedSlice?.id == slice.id
                            val offset = if (isSelected) 8f else 0f
                            // Dibujar sector con efecto 3D
                            drawPieSlice(
                                color = slice.color,
                                center = center,
                                radius = radius + offset,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                isSelected = isSelected
                            )
                            // Detección de tap
                            tapPosition?.let { tap ->
                                val dx = tap.x - center.x
                                val dy = tap.y - center.y
                                val angle = (atan2(dy, dx) * 180f / Math.PI).toFloat() + 180f
                                val normAngle = (angle + 360f) % 360f
                                val sliceStart = (startAngle + 360f) % 360f
                                val sliceEnd = (startAngle + sweepAngle + 360f) % 360f
                                if (normAngle in sliceStart..sliceEnd) {
                                    tappedSlice = slice
                                }
                            }
                            startAngle += sweepAngle
                        }
                        if (tappedSlice != null) {
                            selectedSlice = if (selectedSlice?.id == tappedSlice?.id) null else tappedSlice
                            tapPosition = null
                        }
                    }
                    // Tooltip flotante centrado
                    if (selectedSlice != null) {
                        val slice = selectedSlice!!
                        val percentage = (slice.value / total) * 100
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(slice.label, style = MaterialTheme.typography.labelMedium)
                                Text("${slice.value.toInt()} (${"%.1f".format(percentage)}%)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                // Leyenda mejorada y accesible
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    data.forEach { slice ->
                        val percentage = (slice.value / total) * 100
                        val isSelected = selectedSlice?.id == slice.id
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSlice = if (selectedSlice?.id == slice.id) null else slice
                                    onSliceClick?.invoke(slice)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 4.dp else 1.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Indicador de color
                                Card(
                                    modifier = Modifier.size(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = slice.color
                                    ),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Información
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slice.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) 
                                            MaterialTheme.colorScheme.onPrimaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "%.1f%%".format(percentage),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) 
                                                MaterialTheme.colorScheme.onPrimaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = slice.value.toInt().toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) 
                                                MaterialTheme.colorScheme.onPrimaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant
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