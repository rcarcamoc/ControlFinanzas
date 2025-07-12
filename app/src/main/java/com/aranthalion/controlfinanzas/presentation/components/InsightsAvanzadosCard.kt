package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.domain.usecase.*

@Composable
fun InsightsAvanzadosCard(
    insights: List<InsightComportamiento>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Insights Avanzados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            if (insights.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No se detectaron insights relevantes en este período.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    insights.forEach { insight ->
                        InsightItem(insight = insight)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightItem(insight: InsightComportamiento) {
    val (backgroundColor, textColor, icon) = when (insight.severidad) {
        SeveridadInsight.ALTA -> Triple(
            Color(0xFFFFEBEE), // Rojo claro
            Color(0xFFC62828), // Rojo oscuro
            Icons.Default.Warning
        )
        SeveridadInsight.MEDIA -> Triple(
            Color(0xFFFFF3E0), // Amarillo claro
            Color(0xFFE65100), // Amarillo oscuro
            Icons.Default.Info
        )
        SeveridadInsight.BAJA -> Triple(
            Color(0xFFE3F2FD), // Azul claro
            Color(0xFF1565C0), // Azul oscuro
            Icons.Default.Info
        )
        SeveridadInsight.POSITIVA -> Triple(
            Color(0xFFE8F5E8), // Verde claro
            Color(0xFF2E7D32), // Verde oscuro
            Icons.Default.CheckCircle
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = insight.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Text(
                text = insight.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${insight.valor.toInt()} ${insight.unidad}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )

                insight.categoriaNombre?.let { categoria ->
                    AssistChip(
                        onClick = { },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = textColor.copy(alpha = 0.1f)
                        ),
                        label = {
                            Text(
                                text = categoria,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                        }
                    )
                }
            }

            Text(
                text = insight.accionRecomendada,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AgrupacionesCard(
    agrupaciones: List<AgrupacionTransacciones>,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Agrupaciones Inteligentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (agrupaciones.isEmpty()) {
                Text(
                    text = "No se encontraron agrupaciones relevantes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    agrupaciones.forEach { agrupacion ->
                        AgrupacionItem(agrupacion = agrupacion)
                    }
                }
            }
        }
    }
}

@Composable
private fun AgrupacionItem(agrupacion: AgrupacionTransacciones) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = agrupacion.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                AssistChip(
                    onClick = { },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    label = {
                        Text(
                            text = agrupacion.tipo.name.replace("_", " "),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AgrupacionStat(
                    label = "Total",
                    value = "${agrupacion.total.toInt()}",
                    icon = Icons.Default.Info
                )
                AgrupacionStat(
                    label = "Cantidad",
                    value = "${agrupacion.cantidad}",
                    icon = Icons.Default.List
                )
                AgrupacionStat(
                    label = "Promedio",
                    value = "${agrupacion.promedio.toInt()}",
                    icon = Icons.Default.Info
                )
            }

            agrupacion.patron?.let { patron ->
                Text(
                    text = "Patrón: $patron",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            agrupacion.categoriaNombre?.let { categoria ->
                AssistChip(
                    onClick = { },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    label = {
                        Text(
                            text = categoria,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AgrupacionStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RecomendacionesCard(
    recomendaciones: List<RecomendacionPersonalizada>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recomendaciones Personalizadas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (recomendaciones.isEmpty()) {
                Text(
                    text = "No hay recomendaciones específicas para este período.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    recomendaciones.forEach { recomendacion ->
                        RecomendacionItem(recomendacion = recomendacion)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecomendacionItem(recomendacion: RecomendacionPersonalizada) {
    val (backgroundColor, textColor) = when (recomendacion.prioridad) {
        PrioridadRecomendacion.ALTA -> Pair(
            Color(0xFFFFEBEE), // Rojo claro
            Color(0xFFC62828)  // Rojo oscuro
        )
        PrioridadRecomendacion.MEDIA -> Pair(
            Color(0xFFFFF3E0), // Amarillo claro
            Color(0xFFE65100)  // Amarillo oscuro
        )
        PrioridadRecomendacion.BAJA -> Pair(
            Color(0xFFE8F5E8), // Verde claro
            Color(0xFF2E7D32)  // Verde oscuro
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recomendacion.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                AssistChip(
                    onClick = { },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = textColor.copy(alpha = 0.1f)
                    ),
                    label = {
                        Text(
                            text = recomendacion.prioridad.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor
                        )
                    }
                )
            }

            Text(
                text = recomendacion.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Impacto estimado: ${recomendacion.impactoEstimado.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )

                Text(
                    text = recomendacion.dificultad.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }

            Text(
                text = recomendacion.accionConcreta,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
    }
} 