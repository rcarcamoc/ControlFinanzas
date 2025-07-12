package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.presentation.screens.EstadisticasTinder

@Composable
fun TinderEstadisticasCard(
    estadisticas: EstadisticasTinder,
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
                text = "Progreso de Clasificación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Aceptadas
                EstadisticaItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Aceptadas",
                    value = estadisticas.aceptadas.toString(),
                    color = Color.Green,
                    modifier = Modifier.weight(1f)
                )
                
                // Rechazadas
                EstadisticaItem(
                    icon = Icons.Default.Close,
                    label = "Rechazadas",
                    value = estadisticas.rechazadas.toString(),
                    color = Color.Red,
                    modifier = Modifier.weight(1f)
                )
                
                // Pendientes
                EstadisticaItem(
                    icon = Icons.Default.Warning,
                    label = "Pendientes",
                    value = estadisticas.pendientes.toString(),
                    color = Color.Yellow,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Barra de progreso
            LinearProgressIndicator(
                progress = if (estadisticas.totalProcesadas > 0) {
                    estadisticas.totalProcesadas.toFloat() / 
                    (estadisticas.totalProcesadas + estadisticas.pendientes)
                } else 0f,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "${estadisticas.totalProcesadas} de ${estadisticas.totalProcesadas + estadisticas.pendientes} procesadas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EstadisticaItem(
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