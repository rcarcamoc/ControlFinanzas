package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.scale

@Composable
fun PresupuestoCard(
    presupuesto: com.aranthalion.controlfinanzas.domain.usecase.PresupuestoCategoria,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con información básica
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = presupuesto.categoria.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Presupuesto: ${FormatUtils.formatMoneyCLP(presupuesto.presupuesto)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Gastado: ${FormatUtils.formatMoneyCLP(presupuesto.gastoActual)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorEstado,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Porcentaje destacado
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            color = colorEstado,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${String.format("%.0f", presupuesto.porcentajeGastado)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Gráfico de barras mejorado
            PresupuestoProgressBar(
                porcentaje = presupuesto.porcentajeGastado,
                color = colorEstado,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                var editPressed by remember { mutableStateOf(false) }
                var deletePressed by remember { mutableStateOf(false) }
                val editScale by animateFloatAsState(if (editPressed) 0.92f else 1f, animationSpec = spring())
                val deleteScale by animateFloatAsState(if (deletePressed) 0.92f else 1f, animationSpec = spring())
                IconButton(
                    onClick = {
                        editPressed = true
                        onEditClick()
                        editPressed = false
                    },
                    modifier = Modifier.scale(editScale)
                ) {
                    Icon(
                        imageVector = CustomIcons.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = {
                        deletePressed = true
                        onDeleteClick()
                        deletePressed = false
                    },
                    modifier = Modifier.scale(deleteScale)
                ) {
                    Icon(
                        imageVector = CustomIcons.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun PresupuestoProgressBar(
    porcentaje: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val porcentajeClampado = porcentaje.coerceIn(0.0, 100.0)
    
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((porcentajeClampado / 100.0).toFloat())
                    .background(
                        color = color,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
        
        // Marcadores de referencia
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
                ) {
            Text(
                text = "0%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "80%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "100%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con icono y título
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = colorEstado,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CustomIcons.Category,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Resumen de Presupuestos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            // Estadísticas principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Gastado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = FormatUtils.formatMoneyCLP(resumen.totalGastado),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorEstado
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Presupuestado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = FormatUtils.formatMoneyCLP(resumen.totalPresupuestado),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Barra de progreso mejorada
            Column {
                LinearProgressIndicator(
                    progress = (resumen.porcentajeGastado / 100).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = colorEstado,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                        color = colorEstado,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TextButton(
                        onClick = onVerDetalle,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Ver Detalle")
                    }
                }
            }
            
            // Alertas visuales
            if (resumen.categoriasConAlerta > 0 || resumen.categoriasExcedidas > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (resumen.categoriasConAlerta > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = CustomIcons.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${resumen.categoriasConAlerta} alertas",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    if (resumen.categoriasExcedidas > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = CustomIcons.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${resumen.categoriasExcedidas} excedidas",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
} 