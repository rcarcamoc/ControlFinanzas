package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.ui.theme.ControlFinanzasTheme
import com.aranthalion.controlfinanzas.presentation.configuracion.TemaApp
import com.aranthalion.controlfinanzas.domain.usecase.EstadoPresupuesto

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    description: String? = null,
    modifier: Modifier = Modifier,
    isMonetary: Boolean = true,
    trend: String? = null, // Para mostrar tendencias como "+20.1%"
    presupuestoInfo: PresupuestoInfo? = null // Nueva información de presupuesto
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con título e icono
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Valor principal
            Text(
                text = if (isMonetary && value.toDoubleOrNull() != null) {
                    FormatUtils.formatMoneyCLP(value.toDouble())
                } else {
                    value
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Información de presupuesto si está disponible
            presupuestoInfo?.let { info ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Barra de progreso del presupuesto
                    LinearProgressIndicator(
                        progress = (info.porcentajeGastado.toFloat() / 100f).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                        color = when (info.estado) {
                            EstadoPresupuesto.NORMAL -> MaterialTheme.colorScheme.primary
                            EstadoPresupuesto.ADVERTENCIA -> MaterialTheme.colorScheme.tertiary
                            EstadoPresupuesto.CRITICO -> MaterialTheme.colorScheme.error
                            EstadoPresupuesto.EXCEDIDO -> MaterialTheme.colorScheme.error
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    // Información del presupuesto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${info.porcentajeGastado.toInt()}% usado",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (info.estado) {
                                EstadoPresupuesto.NORMAL -> MaterialTheme.colorScheme.primary
                                EstadoPresupuesto.ADVERTENCIA -> MaterialTheme.colorScheme.tertiary
                                EstadoPresupuesto.CRITICO -> MaterialTheme.colorScheme.error
                                EstadoPresupuesto.EXCEDIDO -> MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = "Restante: ${FormatUtils.formatMoneyCLP(info.presupuestoRestante)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Descripción y tendencia
            if (description != null || trend != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    trend?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (it.startsWith("+")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Data class para información de presupuesto
data class PresupuestoInfo(
    val porcentajeGastado: Double,
    val presupuestoRestante: Double,
    val estado: EstadoPresupuesto
)

// Previews para diferentes estados del StatCard
@Preview(
    name = "StatCard - Gasto Total",
    showBackground = true,
    backgroundColor = 0xFFFEF9F6
)
@Composable
fun StatCardGastoPreview() {
    ControlFinanzasTheme {
        StatCard(
            title = "Gasto Total (Este Mes)",
            value = "$1,850.75",
            icon = CustomIcons.KeyboardArrowDown,
            description = "Este mes",
            trend = "+20.1%",
            isMonetary = false
        )
    }
}

@Preview(
    name = "StatCard - Cumplimiento Presupuesto",
    showBackground = true,
    backgroundColor = 0xFFFEF9F6
)
@Composable
fun StatCardPresupuestoCumplimientoPreview() {
    ControlFinanzasTheme {
        StatCard(
            title = "Cumplimiento Presupuesto",
            value = "75%",
            icon = CustomIcons.Star,
            description = "Basado en presupuestos actuales",
            isMonetary = false,
            presupuestoInfo = PresupuestoInfo(
                porcentajeGastado = 75.0,
                presupuestoRestante = 675.25,
                estado = EstadoPresupuesto.NORMAL
            )
        )
    }
}

@Preview(
    name = "StatCard - Presupuesto Crítico",
    showBackground = true,
    backgroundColor = 0xFFFEF9F6
)
@Composable
fun StatCardPresupuestoCriticoPreview() {
    ControlFinanzasTheme {
        StatCard(
            title = "Cumplimiento Presupuesto",
            value = "95%",
            icon = Icons.Default.Warning,
            description = "¡Presupuesto casi agotado!",
            isMonetary = false,
            presupuestoInfo = PresupuestoInfo(
                porcentajeGastado = 95.0,
                presupuestoRestante = 50.0,
                estado = EstadoPresupuesto.CRITICO
            )
        )
    }
}

@Preview(
    name = "StatCard - Presupuesto Excedido",
    showBackground = true,
    backgroundColor = 0xFFFEF9F6
)
@Composable
fun StatCardPresupuestoExcedidoPreview() {
    ControlFinanzasTheme {
        StatCard(
            title = "Cumplimiento Presupuesto",
            value = "110%",
            icon = Icons.Default.Warning,
            description = "Presupuesto excedido",
            isMonetary = false,
            presupuestoInfo = PresupuestoInfo(
                porcentajeGastado = 110.0,
                presupuestoRestante = -100.0,
                estado = EstadoPresupuesto.EXCEDIDO
            )
        )
    }
}

@Preview(
    name = "StatCard - Sin Descripción",
    showBackground = true,
    backgroundColor = 0xFFFEF9F6
)
@Composable
fun StatCardSinDescripcionPreview() {
    ControlFinanzasTheme {
        StatCard(
            title = "Categorías Activas",
            value = "12",
            icon = Icons.Default.Person,
            isMonetary = false
        )
    }
}

@Preview(
    name = "StatCard - Grid de 4 con Presupuesto",
    showBackground = true,
    backgroundColor = 0xFFFEF9F6
)
@Composable
fun StatCardGridConPresupuestoPreview() {
    ControlFinanzasTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Gasto Total",
                    value = "$1,850.75",
                    icon = Icons.Default.KeyboardArrowDown,
                    description = "Este mes",
                    trend = "+20.1%",
                    modifier = Modifier.weight(1f),
                    isMonetary = false
                )
                StatCard(
                    title = "Cumplimiento Presupuesto",
                    value = "75%",
                    icon = Icons.Default.Star,
                    description = "Basado en presupuestos",
                    modifier = Modifier.weight(1f),
                    isMonetary = false,
                    presupuestoInfo = PresupuestoInfo(
                        porcentajeGastado = 75.0,
                        presupuestoRestante = 675.25,
                        estado = EstadoPresupuesto.NORMAL
                    )
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Proyección",
                    value = "$2,100.00",
                    icon = Icons.Default.Add,
                    description = "Fin de mes",
                    modifier = Modifier.weight(1f),
                    isMonetary = false
                )
                StatCard(
                    title = "Categorías",
                    value = "12",
                    icon = Icons.Default.Person,
                    description = "Activas",
                    modifier = Modifier.weight(1f),
                    isMonetary = false
                )
            }
        }
    }
}

// Previews para diferentes temas
@Preview(
    name = "StatCard - Tema Azul",
    showBackground = true,
    backgroundColor = 0xFFF3F8FD
)
@Composable
fun StatCardTemaAzulPreview() {
    ControlFinanzasTheme(temaApp = TemaApp.AZUL) {
        StatCard(
            title = "Gasto Total (Este Mes)",
            value = "$1,850.75",
            icon = Icons.Default.KeyboardArrowDown,
            description = "Este mes",
            trend = "+20.1%",
            isMonetary = false
        )
    }
}

@Preview(
    name = "StatCard - Tema Verde",
    showBackground = true,
    backgroundColor = 0xFFF3FDF6
)
@Composable
fun StatCardTemaVerdePreview() {
    ControlFinanzasTheme(temaApp = TemaApp.VERDE) {
        StatCard(
            title = "Gasto Total (Este Mes)",
            value = "$1,850.75",
            icon = Icons.Default.KeyboardArrowDown,
            description = "Este mes",
            trend = "+20.1%",
            isMonetary = false
        )
    }
} 