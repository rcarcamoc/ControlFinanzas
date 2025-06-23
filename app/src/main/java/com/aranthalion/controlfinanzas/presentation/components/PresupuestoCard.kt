package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.domain.usecase.PresupuestoCategoria
import com.aranthalion.controlfinanzas.domain.usecase.EstadoPresupuesto

@Composable
fun PresupuestoCard(
    presupuesto: PresupuestoCategoria,
    onEditPresupuesto: (Long, Double?) -> Unit
) {
    val colorEstado = when (presupuesto.estado) {
        EstadoPresupuesto.NORMAL -> Color(0xFF4CAF50) // Verde
        EstadoPresupuesto.ADVERTENCIA -> Color(0xFFFF9800) // Naranja
        EstadoPresupuesto.CRITICO -> Color(0xFFFF5722) // Rojo
        EstadoPresupuesto.EXCEDIDO -> Color(0xFFD32F2F) // Rojo oscuro
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = presupuesto.categoria.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${FormatUtils.formatMoneyCLP(presupuesto.gastoActual)} / ${FormatUtils.formatMoneyCLP(presupuesto.presupuesto)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Text(
                    text = "${String.format("%.1f", presupuesto.porcentajeGastado)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorEstado
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Barra de progreso
            LinearProgressIndicator(
                progress = (presupuesto.porcentajeGastado / 100).toFloat().coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = colorEstado,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Indicadores de estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (presupuesto.estado) {
                        EstadoPresupuesto.NORMAL -> "Normal"
                        EstadoPresupuesto.ADVERTENCIA -> "⚠️ Advertencia"
                        EstadoPresupuesto.CRITICO -> "🚨 Crítico"
                        EstadoPresupuesto.EXCEDIDO -> "💥 Excedido"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorEstado
                )
                
                TextButton(
                    onClick = { onEditPresupuesto(presupuesto.categoria.id, presupuesto.presupuesto) }
                ) {
                    Text("Editar")
                }
            }
        }
    }
}

@Composable
fun ResumenPresupuestosCard(
    resumen: com.aranthalion.controlfinanzas.domain.usecase.ResumenPresupuestos,
    onVerDetalle: () -> Unit
) {
    val colorEstado = when {
        resumen.porcentajeGastado <= 80 -> Color(0xFF4CAF50) // Verde
        resumen.porcentajeGastado <= 90 -> Color(0xFFFF9800) // Naranja
        resumen.porcentajeGastado <= 100 -> Color(0xFFFF5722) // Rojo
        else -> Color(0xFFD32F2F) // Rojo oscuro
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Resumen de Presupuestos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Gastado",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = FormatUtils.formatMoneyCLP(resumen.totalGastado),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Presupuestado",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = FormatUtils.formatMoneyCLP(resumen.totalPresupuestado),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Barra de progreso general
            LinearProgressIndicator(
                progress = (resumen.porcentajeGastado / 100).toFloat().coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = colorEstado
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format("%.1f", resumen.porcentajeGastado)}% del presupuesto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorEstado
                )
                
                TextButton(onClick = onVerDetalle) {
                    Text("Ver Detalle")
                }
            }
            
            if (resumen.categoriasConAlerta > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ ${resumen.categoriasConAlerta} categorías con alerta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            if (resumen.categoriasExcedidas > 0) {
                Text(
                    text = "💥 ${resumen.categoriasExcedidas} categorías excedidas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
} 