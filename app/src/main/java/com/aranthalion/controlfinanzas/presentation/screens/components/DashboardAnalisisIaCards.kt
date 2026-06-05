package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.presentation.screens.AlertaAnalisis
import com.aranthalion.controlfinanzas.presentation.screens.DashboardAnalisisUiState
import com.aranthalion.controlfinanzas.presentation.screens.TipoAlerta
import kotlin.math.abs

@Composable
fun AlertaBanner(alerta: AlertaAnalisis) {
    val containerColor = when (alerta.tipo) {
        TipoAlerta.DANGER -> MaterialTheme.colorScheme.errorContainer
        TipoAlerta.WARNING -> Color(0xFFFFF9C4) // Amarillo suave
        TipoAlerta.INFO -> MaterialTheme.colorScheme.secondaryContainer
        TipoAlerta.SUCCESS -> Color(0xFFC8E6C9) // Verde suave
    }
    val contentColor = when (alerta.tipo) {
        TipoAlerta.DANGER -> MaterialTheme.colorScheme.onErrorContainer
        TipoAlerta.WARNING -> Color(0xFF5D4037)
        TipoAlerta.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
        TipoAlerta.SUCCESS -> Color(0xFF1B5E20)
    }
    val icon = when (alerta.tipo) {
        TipoAlerta.DANGER -> Icons.Default.Error
        TipoAlerta.WARNING -> Icons.Default.Warning
        TipoAlerta.INFO -> Icons.Default.Info
        TipoAlerta.SUCCESS -> Icons.Default.CheckCircle
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = alerta.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = alerta.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun CardResumenIa(data: DashboardAnalisisUiState.Success) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Borde superior degradado para resaltar el look premium
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Brush.horizontalGradient(listOf(primaryColor, secondaryColor)))
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resumen del Mes (IA)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!data.cargandoResumenIa && data.resumenIa != null) {
                    val badgeColor = when (data.resumenIa.proveedor) {
                        "groq" -> Color(0xFFE0F7FA)
                        "gemini" -> Color(0xFFE8F5E9)
                        else -> Color(0xFFF5F5F5)
                    }
                    val badgeText = when (data.resumenIa.proveedor) {
                        "groq" -> "Groq (Llama 3)"
                        "gemini" -> "Gemini"
                        else -> "Local"
                    }
                    val badgeContentColor = when (data.resumenIa.proveedor) {
                        "groq" -> Color(0xFF006064)
                        "gemini" -> Color(0xFF1B5E20)
                        else -> Color(0xFF616161)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeContentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (data.cargandoResumenIa) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generando resumen financiero...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (data.errorResumenIa != null) {
                Text(
                    text = "No se pudo generar el resumen inteligente. Se usarán los datos locales.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (data.resumenIa != null) {
                Text(
                    text = data.resumenIa.texto,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CardRitmoGasto(data: DashboardAnalisisUiState.Success) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ritmo y Proyección de Gasto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (data.presupuestoTotal == 0.0) {
                Text(
                    text = "Se requiere configurar presupuestos para estimar el ritmo de gasto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Presupuesto Mensual:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FormatUtils.formatMoneyCLP(data.presupuestoTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Proyección Fin de Mes:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val proyeccionColor = if (data.proyeccionFinMes > data.presupuestoTotal) MaterialTheme.colorScheme.error 
                                              else Color(0xFF4CAF50)
                        Text(
                            text = FormatUtils.formatMoneyCLP(data.proyeccionFinMes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = proyeccionColor
                        )
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Barra 1: Progreso del tiempo (días del mes)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Días Transcurridos:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Día ${data.diaActual} de ${data.diasTotales} (${data.porcentajePeriodoTranscurrido.toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (data.porcentajePeriodoTranscurrido / 100.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // Barra 2: Progreso del presupuesto consumido
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Presupuesto Gastado:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${FormatUtils.formatMoneyCLP(data.gastoActual)} (${data.porcentajePresupuestoGastado.toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val pctColor = if (data.diferenciaRitmo > 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        LinearProgressIndicator(
                            progress = { (data.porcentajePresupuestoGastado / 100.0).coerceAtMost(1.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = pctColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                // Mensaje contextual del Ritmo
                val mensajeRitmo = when {
                    data.diferenciaRitmo > 15 -> "⚠️ Vas un ${data.diferenciaRitmo.toInt()}% más rápido de lo presupuestado. A este ritmo, agotarás tus fondos antes de fin de mes."
                    data.diferenciaRitmo > 5 -> "Vas un poco rápido en tus consumos. Se recomienda moderar tus compras no esenciales."
                    data.diferenciaRitmo < -10 -> "✅ ¡Excelente ritmo! Vas gastando un ${abs(data.diferenciaRitmo).toInt()}% más lento que el avance del mes."
                    else -> "El avance de tus gastos va perfectamente en línea con el período del mes."
                }
                val mensajeColor = if (data.diferenciaRitmo > 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

                Text(
                    text = mensajeRitmo,
                    style = MaterialTheme.typography.bodySmall,
                    color = mensajeColor,
                    fontWeight = if (data.diferenciaRitmo > 10) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun CardGastosHormiga(data: DashboardAnalisisUiState.Success) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gastos Hormiga 🐜",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total: ${FormatUtils.formatMoneyCLP(data.gastosHormigaTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Compras pequeñas recurrentes que terminan impactando significativamente tu presupuesto mensual.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                data.gastosHormiga.take(4).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.descripcion,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.cantidadCompras} compras • Promedio: ${FormatUtils.formatMoneyCLP(item.promedioCompra)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = FormatUtils.formatMoneyCLP(item.totalAcumulado),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
