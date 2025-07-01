package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BudgetProgressBar(
    currentValue: Float,
    maxValue: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    showPercentage: Boolean = true,
    showValues: Boolean = true
) {
    if (maxValue <= 0) return
    
    val progress = (currentValue / maxValue).coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()
    
    // Animación del progreso
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "progressAnimation"
    )
    
    // Colores según el porcentaje
    val progressColor = when {
        percentage > 100 -> MaterialTheme.colorScheme.error
        percentage > 80 -> MaterialTheme.colorScheme.tertiary
        percentage > 60 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Label y valores
        if (label != null || showValues) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (showValues) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "%.0f".format(currentValue),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "/",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%.0f".format(maxValue),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Barra de progreso
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Barra de fondo
            LinearProgressIndicator(
                progress = 1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = trackColor,
                trackColor = trackColor
            )
            
            // Barra de progreso animada
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
                trackColor = Color.Transparent
            )
        }
        
        // Porcentaje
        if (showPercentage) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            percentage > 100 -> MaterialTheme.colorScheme.errorContainer
                            percentage > 80 -> MaterialTheme.colorScheme.tertiaryContainer
                            percentage > 60 -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "${percentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            percentage > 100 -> MaterialTheme.colorScheme.onErrorContainer
                            percentage > 80 -> MaterialTheme.colorScheme.onTertiaryContainer
                            percentage > 60 -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    height: androidx.compose.ui.unit.Dp = 8.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "simpleProgressAnimation"
    )
    
    LinearProgressIndicator(
        progress = animatedProgress,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        color = color,
        trackColor = trackColor
    )
} 