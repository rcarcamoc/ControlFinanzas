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

@Composable
fun CuentaPorCobrarItem(
    cuenta: CuentaPorCobrar,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCobrar: () -> Unit,
    onCancelar: () -> Unit
) {
    var showActionDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (cuenta.estado) {
                EstadoCuenta.PENDIENTE -> MaterialTheme.colorScheme.surface
                EstadoCuenta.COBRADO -> MaterialTheme.colorScheme.primaryContainer
                EstadoCuenta.CANCELADO -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cuenta.motivo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (cuenta.usuarioNombre?.isNotBlank() == true) {
                        Text(
                            text = "A: ${cuenta.usuarioNombre}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = FormatUtils.formatMoneyCLP(cuenta.monto),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (cuenta.periodoCobro?.isNotBlank() == true) {
                        Text(
                            text = "Período: ${cuenta.periodoCobro}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (cuenta.notas?.isNotBlank() == true) {
                        Text(
                            text = cuenta.notas,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    // Estado
                    Text(
                        text = when (cuenta.estado) {
                            EstadoCuenta.PENDIENTE -> "Pendiente"
                            EstadoCuenta.COBRADO -> "Cobrado"
                            EstadoCuenta.CANCELADO -> "Cancelado"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (cuenta.estado) {
                            EstadoCuenta.PENDIENTE -> MaterialTheme.colorScheme.primary
                            EstadoCuenta.COBRADO -> MaterialTheme.colorScheme.tertiary
                            EstadoCuenta.CANCELADO -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                Column {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { showActionDialog = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                }
            }
        }
    }

    // Diálogo de acciones
    AnimatedVisibility(
        visible = showActionDialog,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(200, easing = FastOutLinearInEasing)
        )
    ) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text("Acciones") },
            text = { Text("Selecciona una acción para esta cuenta") },
            confirmButton = {
                Column {
                    if (cuenta.estado == EstadoCuenta.PENDIENTE) {
                        TextButton(
                            onClick = {
                                onCobrar()
                                showActionDialog = false
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Marcar como Cobrado")
                        }
                        TextButton(
                            onClick = {
                                onCancelar()
                                showActionDialog = false
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancelar")
                        }
                    }
                    TextButton(
                        onClick = {
                            showActionDialog = false
                            showDeleteDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showActionDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo de confirmación de eliminación
    AnimatedVisibility(
        visible = showDeleteDialog,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(200, easing = FastOutLinearInEasing)
        )
    ) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Cuenta") },
            text = { Text("¿Estás seguro de que quieres eliminar esta cuenta?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarCuentaDialog(
    cuenta: CuentaPorCobrar?,
    usuarios: List<Usuario>,
    onDismiss: () -> Unit,
    onConfirm: (CuentaPorCobrar) -> Unit
) {
    var motivo by remember { mutableStateOf(cuenta?.motivo ?: "") }
    var monto by remember { mutableStateOf(cuenta?.monto?.toString() ?: "") }
    var usuarioSeleccionado by remember { mutableStateOf(cuenta?.usuarioId ?: 0L) }
    var periodoCobro by remember { mutableStateOf(cuenta?.periodoCobro ?: "") }
    var notas by remember { mutableStateOf(cuenta?.notas ?: "") }
    var showUsuarioSelector by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cuenta == null) "Agregar Cuenta" else "Editar Cuenta") },
        text = {
            Column {
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto *") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Selector de usuario
                val usuarioActual = usuarios.find { it.id == usuarioSeleccionado }
                OutlinedTextField(
                    value = usuarioActual?.nombreCompleto ?: "Seleccionar usuario",
                    onValueChange = { },
                    label = { Text("Usuario *") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showUsuarioSelector = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = periodoCobro,
                    onValueChange = { periodoCobro = it },
                    label = { Text("Período (YYYY-MM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val montoDouble = monto.toDoubleOrNull() ?: 0.0
                    val nuevaCuenta = CuentaPorCobrar(
                        id = cuenta?.id ?: 0,
                        motivo = motivo.trim(),
                        monto = montoDouble,
                        usuarioId = usuarioSeleccionado,
                        periodoCobro = periodoCobro.trim().takeIf { it.isNotBlank() },
                        notas = notas.trim().takeIf { it.isNotBlank() },
                        estado = cuenta?.estado ?: EstadoCuenta.PENDIENTE
                    )
                    onConfirm(nuevaCuenta)
                },
                enabled = motivo.isNotBlank() && monto.toDoubleOrNull() ?: 0.0 > 0 && usuarioSeleccionado > 0
            ) {
                Text(if (cuenta == null) "Agregar" else "Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    // Selector de usuario
    AnimatedVisibility(
        visible = showUsuarioSelector,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(200, easing = FastOutLinearInEasing)
        )
    ) {
        AlertDialog(
            onDismissRequest = { showUsuarioSelector = false },
            title = { Text("Seleccionar Usuario") },
            text = {
                LazyColumn {
                    items(usuarios) { usuario ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = usuarioSeleccionado == usuario.id,
                                onClick = { usuarioSeleccionado = usuario.id }
                            )
                            Text(usuario.nombreCompleto)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUsuarioSelector = false }) {
                    Text("Seleccionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsuarioSelector = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
} 