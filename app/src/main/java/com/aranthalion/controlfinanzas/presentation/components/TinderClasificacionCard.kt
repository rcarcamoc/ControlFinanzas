package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
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
import com.aranthalion.controlfinanzas.presentation.screens.SugerenciaCategoria
import java.text.SimpleDateFormat
import java.util.*

data class TransaccionTinder(
    val transaccion: ExcelTransaction,
    val categoriaSugerida: Categoria,
    val nivelConfianza: Double,
    val patron: String,
    val tipoCoincidencia: String = "PATRON"
)

@Composable
fun TinderClasificacionCard(
    transaccionTinder: TransaccionTinder,
    sugerenciasCategorias: List<SugerenciaCategoria>,
    categoriaSeleccionada: SugerenciaCategoria?,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onSeleccionarCategoria: (Long) -> Unit,
    onMostrarSelectorManual: () -> Unit,
    onConfirmarClasificacion: () -> Unit,
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
                text = "Clasificar transacción",
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
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Sugerencias de categorías
            Text(
                text = "Sugerencias de categoría:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chips de sugerencias
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(sugerenciasCategorias) { sugerencia ->
                    SugerenciaChip(
                        sugerencia = sugerencia,
                        isSelected = categoriaSeleccionada?.categoriaId == sugerencia.categoriaId,
                        onClick = { onSeleccionarCategoria(sugerencia.categoriaId) }
                    )
                }
                
                // Chip para selección manual
                item {
                    Card(
                        onClick = onMostrarSelectorManual,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Otra categoría",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Otra...",
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Información de confianza si hay categoría seleccionada
            categoriaSeleccionada?.let { categoria ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Categoría seleccionada:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = categoria.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        if (categoria.nivelConfianza > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Barra de confianza
                            LinearProgressIndicator(
                                progress = categoria.nivelConfianza.toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                                color = when {
                                    categoria.nivelConfianza >= 0.8f -> Color.Green
                                    categoria.nivelConfianza >= 0.6f -> Color.Yellow
                                    else -> Color.Red
                                }
                            )
                            
                            Text(
                                text = "Confianza: ${(categoria.nivelConfianza * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        
                        if (categoria.tipoCoincidencia != "MANUAL") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Patrón: ${categoria.patron}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
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
                
                if (categoriaSeleccionada != null) {
                    Button(
                        onClick = onConfirmarClasificacion,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirmar"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar")
                    }
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
        }
    }
}

@Composable
fun SugerenciaChip(
    sugerencia: SugerenciaCategoria,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        animationSpec = tween(durationMillis = 200),
        label = "elevation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    Card(
        onClick = onClick,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                sugerencia.nivelConfianza >= 0.8f -> Color.Green.copy(alpha = 0.1f)
                sugerencia.nivelConfianza >= 0.6f -> Color.Yellow.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = sugerencia.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            
            if (sugerencia.nivelConfianza > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(sugerencia.nivelConfianza * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SelectorCategoriaManual(
    categorias: List<Categoria>,
    onSeleccionar: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCategorias = categorias.filter { 
        it.nombre.contains(searchQuery, ignoreCase = true) 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Seleccionar categoría")
        },
        text = {
            Column {
                // Campo de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar categoría") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lista de categorías filtradas
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCategorias) { categoria ->
                        Card(
                            onClick = { 
                                onSeleccionar(categoria.id)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = categoria.nombre,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
} 