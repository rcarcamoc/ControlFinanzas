package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aranthalion.controlfinanzas.presentation.FirstRunViewModel
import com.aranthalion.controlfinanzas.presentation.FirstRunUiState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import java.text.SimpleDateFormat
import java.util.Calendar
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstRunScreen(
    navController: NavHostController,
    viewModel: FirstRunViewModel = hiltViewModel()
) {
    Log.i("LOG_PRIMER_USO_UI", "[1] Renderizando FirstRunScreen")
    val uiState by viewModel.uiState.collectAsState()
    var showLoadingDialog by remember { mutableStateOf(false) }
    var showCategoriasDialog by remember { mutableStateOf(false) }
    var showPeriodoDialog by remember { mutableStateOf(false) }
    var periodoSeleccionado by remember { mutableStateOf("") }
    val periodosDisponibles = remember {
        val formato = SimpleDateFormat("yyyy-MM")
        val calendar = Calendar.getInstance()
        val lista = mutableListOf<String>()
        // 12 meses antes
        for (i in 12 downTo 1) {
            calendar.add(Calendar.MONTH, -1)
            lista.add(0, formato.format(calendar.time))
        }
        // Actual
        calendar.time = java.util.Date()
        lista.add(formato.format(calendar.time))
        // 2 meses después
        for (i in 1..2) {
            calendar.add(Calendar.MONTH, 1)
            lista.add(formato.format(calendar.time))
        }
        lista
    }
    if (periodoSeleccionado.isEmpty() && periodosDisponibles.isNotEmpty()) {
        periodoSeleccionado = periodosDisponibles[12] // El actual
    }

    LaunchedEffect(uiState) {
        Log.i("LOG_PRIMER_USO_UI", "[2] LaunchedEffect: uiState=$uiState")
        when (uiState) {
            is FirstRunUiState.Success -> {
                Log.i("LOG_PRIMER_USO_UI", "[3] Navegando a home tras Success")
                // Navegar a la pantalla principal
                navController.navigate("home") {
                    popUpTo("first_run") { inclusive = true }
                }
            }
            is FirstRunUiState.Loading -> {
                Log.i("LOG_PRIMER_USO_UI", "[4] Mostrando loading dialog")
                showLoadingDialog = true
            }
            is FirstRunUiState.Error -> {
                Log.i("LOG_PRIMER_USO_UI", "[5] Error: ${(uiState as FirstRunUiState.Error).mensaje}")
                showLoadingDialog = false
            }
            else -> {
                showLoadingDialog = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo y título
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                    initialOffsetY = { -100 },
                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.size(120.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Bienvenida a Finanzas personales",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Tu asistente personal para el control de finanzas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Opciones de configuración inicial
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 500)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(1000, delayMillis = 500, easing = FastOutSlowInEasing)
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = "Configuración inicial",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "Elige cómo quieres comenzar con tu control de finanzas:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Opción 1: Cargar datos históricos
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Cargar datos de ejemplo",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "Incluye categorías predefinidas, patrones de clasificación automática y datos históricos de ejemplo para que puedas explorar todas las funcionalidades de la app.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { viewModel.cargarDatosHistoricos() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cargar datos de ejemplo")
                                }
                            }
                        }
                        
                        // Opción 2: Instalación limpia
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Comenzar desde cero",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "Inicia con una instalación limpia. Podrás crear tus propias categorías y comenzar a registrar tus transacciones desde el primer momento.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                OutlinedButton(
                                    onClick = { showCategoriasDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Comenzar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de carga
    if (showLoadingDialog) {
        Log.i("LOG_PRIMER_USO_UI", "[6] Renderizando loading dialog")
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text("Cargando datos...")
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = "Configurando tu aplicación con datos de ejemplo...",
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = { }
        )
    }

    // Diálogo para preguntar si cargar categorías
    if (showCategoriasDialog) {
        Log.i("LOG_PRIMER_USO_UI", "[7] Renderizando diálogo de categorías por defecto")
        AlertDialog(
            onDismissRequest = { showCategoriasDialog = false },
            title = {
                Text("¿Cargar categorías por defecto?")
            },
            text = {
                Text(
                    "¿Deseas cargar categorías predefinidas como 'Arriendo', 'Supermercado', 'Bencina', etc.? " +
                    "Esto te ayudará a comenzar más rápido, pero siempre podrás crear tus propias categorías después."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Log.i("LOG_PRIMER_USO_UI", "[8] Botón 'Sí, cargar categorías' presionado")
                        showCategoriasDialog = false
                        showPeriodoDialog = true
                    }
                ) {
                    Text("Sí, cargar categorías")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        Log.i("LOG_PRIMER_USO_UI", "[9] Botón 'No, comenzar sin categorías' presionado")
                        showCategoriasDialog = false
                        viewModel.comenzarDesdeCero()
                    }
                ) {
                    Text("No, comenzar sin categorías")
                }
            }
        )
    }
    // Diálogo de selección de periodo para presupuestos
    if (showPeriodoDialog) {
        Log.i("LOG_PRIMER_USO_UI", "[10] Renderizando diálogo de selección de periodo")
        AlertDialog(
            onDismissRequest = { showPeriodoDialog = false },
            title = { Text("¿Desde qué periodo deseas insertar los presupuestos?") },
            text = {
                Column {
                    Text("Selecciona el periodo inicial. Se insertarán presupuestos desde ese mes hasta el actual (y 2 meses después).")
                    Spacer(modifier = Modifier.height(16.dp))
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = periodoSeleccionado,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Periodo inicial") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            periodosDisponibles.forEach { periodo ->
                                DropdownMenuItem(
                                    text = { Text(periodo) },
                                    onClick = {
                                        Log.i("LOG_PRIMER_USO_UI", "[11] Periodo seleccionado: $periodo")
                                        periodoSeleccionado = periodo
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Log.i("LOG_PRIMER_USO_UI", "[12] Botón 'Insertar presupuestos' presionado. Periodo seleccionado: $periodoSeleccionado")
                        showPeriodoDialog = false
                        viewModel.cargarSoloCategoriasYPresupuestos(periodoSeleccionado, periodosDisponibles)
                    }
                ) {
                    Text("Insertar presupuestos")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        Log.i("LOG_PRIMER_USO_UI", "[13] Botón 'Cancelar' en diálogo de periodo presionado")
                        showPeriodoDialog = false 
                    },
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Mostrar error si ocurre
    if (uiState is FirstRunUiState.Error) {
        Log.i("LOG_PRIMER_USO_UI", "[14] Renderizando diálogo de error")
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text("Error")
            },
            text = {
                Text((uiState as FirstRunUiState.Error).mensaje)
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        Log.i("LOG_PRIMER_USO_UI", "[15] Botón 'Aceptar' en diálogo de error presionado")
                        viewModel.resetError() 
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
} 