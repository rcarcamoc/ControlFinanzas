package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ClasificacionStats(
    val totalTransacciones: Int,
    val clasificadas: Int,
    val sinClasificar: Int,
    val porcentajeClasificadas: Float
)

@Composable
fun ClasificacionStatsCard(
    stats: ClasificacionStats,
    onClasificarClick: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Estado de Clasificación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Clasificadas
                EstadisticaClasificacionItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Clasificadas",
                    value = stats.clasificadas.toString(),
                    color = Color.Green,
                    modifier = Modifier.weight(1f)
                )
                
                // Sin clasificar
                EstadisticaClasificacionItem(
                    icon = Icons.Default.Warning,
                    label = "Sin clasificar",
                    value = stats.sinClasificar.toString(),
                    color = Color.Yellow,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Barra de progreso
            LinearProgressIndicator(
                progress = stats.porcentajeClasificadas,
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    stats.porcentajeClasificadas >= 0.8f -> Color.Green
                    stats.porcentajeClasificadas >= 0.6f -> Color.Yellow
                    else -> Color.Red
                }
            )
            
            Text(
                text = "${(stats.porcentajeClasificadas * 100).toInt()}% clasificadas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Botón para clasificar si hay transacciones sin clasificar
            // Eliminado según requerimiento del usuario
        }
    }
}

@Composable
private fun EstadisticaClasificacionItem(
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
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
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