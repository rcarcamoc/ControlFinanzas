package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ClasificacionMetrics(
    val totalTransacciones: Int,
    val transaccionesClasificadas: Int,
    val transaccionesSinClasificar: Int,
    val precisionPromedio: Double,
    val patronesActivos: Int,
    val categoriasMasUsadas: List<CategoriaMetric>,
    val patronesMasEfectivos: List<PatronMetric>
)

data class CategoriaMetric(
    val nombre: String,
    val cantidad: Int,
    val porcentaje: Double
)

data class PatronMetric(
    val patron: String,
    val categoria: String,
    val precision: Double,
    val frecuencia: Int
)

@Composable
fun ClasificacionMetricsCard(
    metrics: ClasificacionMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Métricas de Clasificación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Estadísticas principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    icon = Icons.Default.List,
                    label = "Total",
                    value = metrics.totalTransacciones.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                MetricItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Clasificadas",
                    value = metrics.transaccionesClasificadas.toString(),
                    color = Color.Green
                )
                
                MetricItem(
                    icon = Icons.Default.Warning,
                    label = "Sin clasificar",
                    value = metrics.transaccionesSinClasificar.toString(),
                    color = Color.Yellow
                )
            }
            
            // Barra de progreso de clasificación
            Column {
                Text(
                    text = "Progreso de clasificación",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LinearProgressIndicator(
                    progress = if (metrics.totalTransacciones > 0) {
                        metrics.transaccionesClasificadas.toFloat() / metrics.totalTransacciones.toFloat()
                    } else 0f,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Green
                )
                
                Text(
                    text = "${(metrics.precisionPromedio * 100).toInt()}% precisión promedio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider()
            
            // Categorías más usadas
            if (metrics.categoriasMasUsadas.isNotEmpty()) {
                Text(
                    text = "Categorías más usadas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                metrics.categoriasMasUsadas.take(3).forEach { categoria ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = categoria.nombre,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text(
                            text = "${categoria.cantidad} (${categoria.porcentaje.toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Patrones más efectivos
            if (metrics.patronesMasEfectivos.isNotEmpty()) {
                Text(
                    text = "Patrones más efectivos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                metrics.patronesMasEfectivos.take(3).forEach { patron ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = patron.patron,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = patron.categoria,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = "${(patron.precision * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                patron.precision >= 0.8 -> Color.Green
                                patron.precision >= 0.6 -> Color.Yellow
                                else -> Color.Red
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 