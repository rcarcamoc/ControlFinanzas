package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.util.ExcelTransaction
import java.text.SimpleDateFormat
import java.util.*

data class TransaccionTinder(
    val transaccion: ExcelTransaction,
    val categoriaSugerida: Categoria,
    val nivelConfianza: Double,
    val patron: String
)

@Composable
fun TinderClasificacionCard(
    transaccionTinder: TransaccionTinder,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    val rotation by animateFloatAsState(
        targetValue = (offsetX * 0.1f),
        animationSpec = tween(durationMillis = 100),
        label = "rotation"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (kotlin.math.abs(offsetX) > 200f) 0.5f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (kotlin.math.abs(offsetX) > 100f) 0.9f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    LaunchedEffect(offsetX) {
        when {
            offsetX > 200f -> {
                onAceptar()
                offsetX = 0f
                offsetY = 0f
            }
            offsetX < -200f -> {
                onRechazar()
                offsetX = 0f
                offsetY = 0f
            }
        }
    }
    
    Card(
        modifier = modifier
            .offset(
                x = with(LocalDensity.current) { offsetX.toDp() },
                y = with(LocalDensity.current) { offsetY.toDp() }
            )
            .rotate(rotation)
            .graphicsLayer(
                alpha = alpha,
                scaleX = scale,
                scaleY = scale
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(offsetX) < 200f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                ) { _, dragAmount ->
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header con iconos de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de rechazo (izquierda)
                if (offsetX < 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Rechazar",
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Indicador de aceptación (derecha)
                if (offsetX > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Green.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Aceptar",
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Información de la transacción
            Text(
                text = "¿Clasificar esta transacción?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Detalles de la transacción
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = transaccionTinder.transaccion.descripcion,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Monto:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${String.format("%,.0f", transaccionTinder.transaccion.monto)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (transaccionTinder.transaccion.monto > 0) Color.Red else Color.Green
                        )
                    }
                    
                    transaccionTinder.transaccion.fecha?.let { fecha ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Fecha:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sugerencia de categoría
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Categoría Sugerida:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = transaccionTinder.categoriaSugerida.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Patrón: ${transaccionTinder.patron}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Barra de confianza
                    LinearProgressIndicator(
                        progress = transaccionTinder.nivelConfianza.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            transaccionTinder.nivelConfianza >= 0.8f -> Color.Green
                            transaccionTinder.nivelConfianza >= 0.6f -> Color.Yellow
                            else -> Color.Red
                        }
                    )
                    
                    Text(
                        text = "Confianza: ${(transaccionTinder.nivelConfianza * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onRechazar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Rechazar"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rechazar")
                }
                
                Button(
                    onClick = onAceptar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Aceptar"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aceptar")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Más tarde")
            }
        }
    }
} 