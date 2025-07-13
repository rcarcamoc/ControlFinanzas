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
    
    // Animación para las barras
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "barAnimation"
    )

    // Estado para tooltip
    var tooltipIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipOffset by remember { mutableStateOf(Offset.Zero) }

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
            // Gráfico de barras
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
            ) {
                val barWidth = maxOf(24.dp, maxWidth / (data.size * 2))
                val space = 12.dp
                val density = LocalDensity.current
                val maxHeightPx = with(density) { maxHeight.toPx() }
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(space)
                ) {
                    data.forEachIndexed { i, bar ->
                        val color = bar.color.takeIf { it != Color.Unspecified } ?: palette[i % palette.size]
                        val barHeightPx = if (maxValue > 0) (bar.value / maxValue) * (maxHeightPx - 32) else 0f
                        val barHeightDp = with(density) { (barHeightPx * animatedProgress).toDp() }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (showValues) {
                                Text(
                                    text = if (bar.value % 1f == 0f) bar.value.toInt().toString() else String.format("%.1f", bar.value),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = color,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .height(barHeightDp)
                                    .width(barWidth)
                                    .background(color = color, shape = MaterialTheme.shapes.medium)
                            )
                            Text(
                                text = bar.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            // Leyenda si hay más de una categoría de color
            if (data.map { it.label }.distinct().size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    data.take(palette.size).forEachIndexed { i, bar ->
                        val color = bar.color.takeIf { it != Color.Unspecified } ?: palette[i % palette.size]
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
                                text = bar.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
    
    // Filtrar solo gastos (tipo GASTO) y excluir transacciones omitidas
    val gastos = movimientos.filter { 
        it.tipo == "GASTO" && 
        it.tipo != "OMITIR" // Excluir transacciones omitidas
    }
    
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