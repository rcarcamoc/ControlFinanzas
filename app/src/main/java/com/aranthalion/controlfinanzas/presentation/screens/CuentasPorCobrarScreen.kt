package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aranthalion.controlfinanzas.domain.cuenta.CuentaPorCobrar
import com.aranthalion.controlfinanzas.domain.cuenta.EstadoCuenta
import com.aranthalion.controlfinanzas.domain.usuario.Usuario
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import java.text.SimpleDateFormat
import java.util.*
import com.aranthalion.controlfinanzas.presentation.screens.components.CuentaPorCobrarItem
import com.aranthalion.controlfinanzas.presentation.screens.components.AgregarCuentaDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentasPorCobrarScreen(
    navController: NavHostController,
    viewModel: CuentasPorCobrarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()
    val usuarios by viewModel.usuarios.collectAsState()
    val totalPendiente by viewModel.totalPendiente.collectAsState()
    val cantidadPendientes by viewModel.cantidadPendientes.collectAsState()
    
    var showAddCuentaDialog by remember { mutableStateOf(false) }
    var cuentaToEdit by remember { mutableStateOf<CuentaPorCobrar?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        viewModel.buscarCuentas(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuentas por Cobrar") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddCuentaDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar cuenta")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is CuentasPorCobrarUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is CuentasPorCobrarUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Barra de búsqueda
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Buscar por motivo") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )

                        // Resumen
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Pendiente",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = FormatUtils.formatMoneyCLP(totalPendiente),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$cantidadPendientes cuentas pendientes",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Lista de cuentas
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (cuentas.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (searchQuery.isBlank()) 
                                                    "No hay cuentas pendientes" 
                                                else 
                                                    "No se encontraron cuentas",
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            if (searchQuery.isBlank()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(onClick = { showAddCuentaDialog = true }) {
                                                    Text("Agregar primera cuenta")
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(cuentas) { cuenta ->
                                    CuentaPorCobrarItem(
                                        cuenta = cuenta,
                                        onEdit = { cuentaToEdit = cuenta },
                                        onDelete = { viewModel.eliminarCuenta(cuenta) },
                                        onCobrar = { viewModel.cambiarEstadoCuenta(cuenta.id, EstadoCuenta.COBRADO) },
                                        onCancelar = { viewModel.cambiarEstadoCuenta(cuenta.id, EstadoCuenta.CANCELADO) }
                                    )
                                }
                            }
                        }
                    }
                }
                is CuentasPorCobrarUiState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (uiState as CuentasPorCobrarUiState.Error).mensaje,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Diálogo para agregar/editar cuenta
        AnimatedVisibility(
            visible = showAddCuentaDialog || cuentaToEdit != null,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200, easing = FastOutLinearInEasing)
            )
        ) {
            AgregarCuentaDialog(
                cuenta = cuentaToEdit,
                usuarios = usuarios,
                onDismiss = { 
                    showAddCuentaDialog = false
                    cuentaToEdit = null
                },
                onConfirm = { cuenta ->
                    if (cuentaToEdit != null) {
                        viewModel.actualizarCuenta(cuenta)
                        cuentaToEdit = null
                    } else {
                        viewModel.agregarCuenta(cuenta)
                    }
                    showAddCuentaDialog = false
                }
            )
        }
    }
} 