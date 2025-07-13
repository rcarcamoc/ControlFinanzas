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
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.presentation.components.TinderClasificacionCard
import com.aranthalion.controlfinanzas.presentation.components.TransaccionTinder

@Composable
fun TinderClasificacionScreen(
    viewModel: TinderClasificacionViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Mostrar error si existe
    uiState.error?.let { error ->
        LaunchedEffect(error) {
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
        Dialog(
            onDismissRequest = { onDismiss() },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            TinderClasificacionDialog(
                uiState = uiState,
                onAceptar = { viewModel.aceptarTransaccion() },
                onRechazar = { viewModel.rechazarTransaccion() },
                onDismiss = { onDismiss() },
                onClose = onDismiss
            )
        }
    }
}

@Composable
fun TinderClasificacionDialog(
    uiState: TinderClasificacionUiState,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onDismiss: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                uiState.transaccionActual?.let { transaccionTinder ->
                    TinderClasificacionCard(
                        transaccionTinder = transaccionTinder,
                        onAceptar = onAceptar,
                        onRechazar = onRechazar,
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
        // Contenido del Tinder centrado
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            TinderClasificacionCard(
                transaccionTinder = uiState.transaccionActual!!,
                onAceptar = onAceptar,
                onRechazar = onRechazar,
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
} 