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
import kotlin.math.abs
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.*

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
    if (data.isEmpty()) {
        // Mostrar mensaje cuando no hay datos
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
                    .heightIn(min = 220.dp)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                Text(
                    text = "No hay datos para mostrar",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }
    
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

    // Estado para tooltip
    var tooltipIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    
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
                .heightIn(min = 220.dp, max = 380.dp)
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
                
                // Tooltips interactivos
                if (tooltipIndex != null && tooltipIndex!! in data.indices) {
                    val barData = data[tooltipIndex!!]
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = tooltipOffset.x.toInt(),
                                    y = tooltipOffset.y.toInt() - 60 // arriba de la barra
                                )
                            }
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(barData.label, style = MaterialTheme.typography.labelMedium)
                            Text(
                                FormatUtils.formatMoneyCLP(barData.value.toDouble()),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Overlay para detectar clicks
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 50.dp, top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    data.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    tooltipIndex = if (tooltipIndex == index) null else index
                                    // Calcular posición aproximada del tooltip
                                    tooltipOffset = Offset(
                                        x = (index + 0.5f) * (with(density) { 40.dp.toPx() }),
                                        y = with(density) { 40.dp.toPx() }
                                    )
                                }
                        ) {}
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
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    ) {
                                        Text(
                                            text = "$percentage%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = FormatUtils.formatMoneyCLP(barData.value.toDouble()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
}

/**
 * Función para procesar movimientos y crear datos para el gráfico de barras
 * Agrupa por período de facturación y muestra los últimos 6 meses
 */
fun procesarDatosParaGrafico(movimientos: List<MovimientoEntity>, primaryColor: Color): List<BarChartData> {
    if (movimientos.isEmpty()) return emptyList()
    
    // Filtrar solo gastos (tipo GASTO)
    val gastos = movimientos.filter { it.tipo == "GASTO" }
    
    if (gastos.isEmpty()) return emptyList()
    
    // Agrupar por período de facturación y sumar gastos
    val gastosPorPeriodo = gastos.groupBy { it.periodoFacturacion }
        .mapValues { (_, movimientos) -> 
            // Para gastos, sumamos todos los valores (positivos y negativos)
            // Los negativos representan reversas y reducen el gasto total
            val gastoTotal = movimientos.sumOf { it.monto }
            abs(gastoTotal)
        }
        .toList()
        .sortedBy { it.first } // Ordenar por período
    
    // Tomar los últimos 6 meses (o menos si no hay suficientes)
    val ultimosMeses = gastosPorPeriodo.takeLast(6)
    
    // Formatear etiquetas (mostrar solo mes y año)
    val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val displayFormat = SimpleDateFormat("MMM yyyy", Locale("es", "CL"))
    
    return ultimosMeses.map { (periodo, total) ->
        val fecha = dateFormat.parse(periodo) ?: Date()
        val label = displayFormat.format(fecha)
        
        BarChartData(
            label = label,
            value = total.toFloat(),
            color = primaryColor
        )
    }
} 