package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.domain.usecase.MetricasRendimiento

@Composable
fun MetricasAvanzadasCard(
    metricas: MetricasRendimiento,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Métricas de Rendimiento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Score Financiero
            ScoreFinancieroCard(metricas.scoreFinanciero)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Métricas detalladas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricaItem(
                    titulo = "Liquidez",
                    valor = "${(metricas.ratioLiquidez * 100).toInt()}%",
                    descripcion = "Ingresos vs Gastos",
                    icon = Icons.Default.Info,
                    color = if (metricas.ratioLiquidez >= 1.2) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                
                MetricaItem(
                    titulo = "Gastos Fijos",
                    valor = "${(metricas.ratioGastosFijos * 100).toInt()}%",
                    descripcion = "Del total ingresos",
                    icon = Icons.Default.Info,
                    color = if (metricas.ratioGastosFijos <= 0.5) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricaItem(
                    titulo = "Ahorro",
                    valor = "${(metricas.ratioAhorro * 100).toInt()}%",
                    descripcion = "Tasa de ahorro",
                    icon = Icons.Default.Info,
                    color = if (metricas.ratioAhorro >= 0.15) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                
                MetricaItem(
                    titulo = "Estabilidad",
                    valor = "${(metricas.indiceEstabilidad * 100).toInt()}%",
                    descripcion = "Índice de estabilidad",
                    icon = Icons.Default.Info,
                    color = if (metricas.indiceEstabilidad >= 0.7) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScoreFinancieroCard(score: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono y texto
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Score Financiero",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = when {
                        score >= 80 -> "Excelente"
                        score >= 60 -> "Bueno"
                        else -> "Necesita mejora"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // Score circular
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.fillMaxSize(),
                    color = when {
                        score >= 80 -> Color(0xFF4CAF50)
                        score >= 60 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    strokeWidth = 6.dp
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun MetricaItem(
    titulo: String,
    valor: String,
    descripcion: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 