package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.presentation.components.TinderClasificacionCard
import com.aranthalion.controlfinanzas.presentation.components.SelectorCategoriaManual
import com.aranthalion.controlfinanzas.presentation.components.TransaccionTinder
import android.util.Log

@Composable
fun TinderClasificacionScreen(
    viewModel: TinderClasificacionViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    Log.i("LOG_TINDER_CLASIFICACION", "[8] Renderizando pantalla TinderClasificacionScreen")
    val uiState by viewModel.uiState.collectAsState()
    
    // Mostrar error si existe
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            Log.i("LOG_TINDER_CLASIFICACION", "[9] Error detectado en TinderClasificacionScreen: $error")
            // Aquí podrías mostrar un Snackbar o Toast
        }
    }
    
    // Dialog principal del Tinder
    AnimatedVisibility(
        visible = uiState.mostrarTinder,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { it }
        ),
        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { it }
        )
    ) {
        Log.i("LOG_TINDER_CLASIFICACION", "[10] Mostrando Dialog principal de TinderClasificacionScreen")
        Dialog(
            onDismissRequest = { 
                Log.i("LOG_TINDER_CLASIFICACION", "[11] onDismissRequest ejecutado en Dialog de TinderClasificacionScreen")
                onDismiss() 
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val dialogWidth = (screenWidth * 0.98f).coerceAtMost(450.dp)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .width(dialogWidth)
        ) {
            TinderClasificacionDialog(
                uiState = uiState,
                onAceptar = { 
                    Log.i("LOG_TINDER_CLASIFICACION", "[12] Botón Aceptar presionado")
                    viewModel.aceptarTransaccion() 
                },
                onRechazar = { 
                    Log.i("LOG_TINDER_CLASIFICACION", "[13] Botón Rechazar presionado")
                    viewModel.rechazarTransaccion() 
                },
                onSeleccionarCategoria = { 
                    Log.i("LOG_TINDER_CLASIFICACION", "[14] Categoría seleccionada: $it")
                    viewModel.seleccionarCategoria(it) 
                },
                onMostrarSelectorManual = { 
                    Log.i("LOG_TINDER_CLASIFICACION", "[15] Mostrar selector manual de categoría")
                    viewModel.mostrarSelectorManual() 
                },
                onConfirmarClasificacion = { 
                    Log.i("LOG_TINDER_CLASIFICACION", "[16] Confirmar clasificación manual")
                    viewModel.confirmarClasificacion() 
                },
                onSeleccionarCategoriaManual = { 
                    Log.i("LOG_TINDER_CLASIFICACION", "[17] Categoría manual seleccionada: $it")
                    viewModel.seleccionarCategoriaManual(it) 
                },
                onDismiss = { 
                    Log.i("LOG_TINDER_CLASIFICACION", "[18] onDismiss ejecutado en TinderClasificacionDialog")
                    onDismiss() 
                },
                onClose = { 
                    Log.i("LOG_TINDER_CLASIFICACION", "[19] onClose ejecutado en TinderClasificacionDialog")
                    onDismiss() 
                }
            )
            }
        }
    }
    
    // Dialog para selector manual de categorías
    if (uiState.mostrarSelectorManual) {
        Log.i("LOG_TINDER_CLASIFICACION", "[20] Mostrando SelectorCategoriaManual")
        SelectorCategoriaManual(
            categorias = uiState.categoriasDisponibles,
            onSeleccionar = { categoriaId ->
                Log.i("LOG_TINDER_CLASIFICACION", "[21] Categoría seleccionada manualmente: $categoriaId")
                viewModel.rechazarYSeleccionarManual(categoriaId)
            },
            onDismiss = {
                Log.i("LOG_TINDER_CLASIFICACION", "[22] onDismiss ejecutado en SelectorCategoriaManual")
                viewModel.ocultarSelectorManual()
            }
        )
    }
    
    // Snackbar para feedback
    if (uiState.mostrarFeedback) {
        LaunchedEffect(uiState.mensajeFeedback) {
            Log.i("LOG_TINDER_CLASIFICACION", "[23] Mostrando feedback: ${uiState.mensajeFeedback}")
            // Aquí podrías mostrar un Snackbar con el mensaje
        }
    }
}

@Composable
fun TinderClasificacionDialog(
    uiState: TinderClasificacionUiState,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onSeleccionarCategoria: (Long) -> Unit,
    onMostrarSelectorManual: () -> Unit,
    onConfirmarClasificacion: () -> Unit,
    onSeleccionarCategoriaManual: (Long) -> Unit,
    onDismiss: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con estadísticas y botón de cerrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Estadísticas
                Column {
                    Text(
                        text = "Clasificación Automática",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Procesadas: ${uiState.estadisticas.totalProcesadas} | " +
                                "Aceptadas: ${uiState.estadisticas.aceptadas} | " +
                                "Rechazadas: ${uiState.estadisticas.rechazadas}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Botón cerrar
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Barra de progreso
            LinearProgressIndicator(
                progress = if (uiState.estadisticas.totalProcesadas > 0) {
                    (uiState.estadisticas.totalProcesadas.toFloat() / 
                     (uiState.estadisticas.totalProcesadas + uiState.estadisticas.pendientes))
                } else 0f,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "${uiState.estadisticas.pendientes} transacciones pendientes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Contenido principal
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Preparando clasificación automática...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Analizando transacciones y generando sugerencias",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                uiState.transaccionActual?.let { transaccionTinder ->
                    TinderClasificacionCard(
                        transaccionTinder = transaccionTinder,
                        sugerenciasCategorias = uiState.sugerenciasCategorias,
                        categoriaSeleccionada = uiState.categoriaSeleccionada,
                        onAceptar = onAceptar,
                        onRechazar = onRechazar,
                        onSeleccionarCategoria = onSeleccionarCategoria,
                        onMostrarSelectorManual = onMostrarSelectorManual,
                        onConfirmarClasificacion = onConfirmarClasificacion,
                        onDismiss = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                } ?: run {
                    // No hay más transacciones para procesar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "¡Excelente trabajo!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Has procesado todas las transacciones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Finalizar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TinderClasificacionOverlay(
    uiState: TinderClasificacionUiState,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onDismiss: () -> Unit
) {
    // Overlay de fondo oscuro
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        // Contenido del overlay
        TinderClasificacionDialog(
            uiState = uiState,
                onAceptar = onAceptar,
                onRechazar = onRechazar,
            onSeleccionarCategoria = { /* Implementar */ },
            onMostrarSelectorManual = { /* Implementar */ },
            onConfirmarClasificacion = { /* Implementar */ },
            onSeleccionarCategoriaManual = { /* Implementar */ },
                onDismiss = onDismiss,
            onClose = onDismiss
            )
    }
} 