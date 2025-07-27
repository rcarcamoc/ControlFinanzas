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
import com.aranthalion.controlfinanzas.domain.usuario.Usuario
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosScreen(
    navController: NavHostController,
    viewModel: UsuariosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val usuarios by viewModel.usuarios.collectAsState()
    val totalUsuarios by viewModel.totalUsuarios.collectAsState()
    
    var showAddUsuarioDialog by remember { mutableStateOf(false) }
    var usuarioToEdit by remember { mutableStateOf<Usuario?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        viewModel.buscarUsuarios(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Usuarios") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddUsuarioDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar usuario")
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
                is UsuariosUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UsuariosUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Barra de búsqueda
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Buscar usuarios") },
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Total Usuarios",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$totalUsuarios usuarios",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Lista de usuarios
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (usuarios.isEmpty()) {
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
                                                    "No hay usuarios registrados" 
                                                else 
                                                    "No se encontraron usuarios",
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            if (searchQuery.isBlank()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(onClick = { showAddUsuarioDialog = true }) {
                                                    Text("Agregar primer usuario")
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(usuarios) { usuario ->
                                    UsuarioItem(
                                        usuario = usuario,
                                        onEdit = { usuarioToEdit = usuario },
                                        onDelete = { viewModel.eliminarUsuario(usuario) }
                                    )
                                }
                            }
                        }
                    }
                }
                is UsuariosUiState.Error -> {
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
                                text = (uiState as UsuariosUiState.Error).mensaje,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Diálogo para agregar/editar usuario
        AnimatedVisibility(
            visible = showAddUsuarioDialog || usuarioToEdit != null,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200, easing = FastOutLinearInEasing)
            )
        ) {
            AgregarUsuarioDialog(
                usuario = usuarioToEdit,
                onDismiss = { 
                    showAddUsuarioDialog = false
                    usuarioToEdit = null
                },
                onConfirm = { usuario ->
                    if (usuarioToEdit != null) {
                        viewModel.actualizarUsuario(usuario)
                        usuarioToEdit = null
                    } else {
                        viewModel.agregarUsuario(usuario)
                    }
                    showAddUsuarioDialog = false
                }
            )
        }
    }
}

@Composable
fun UsuarioItem(
    usuario: Usuario,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (usuario.activo) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usuario.nombreCompleto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (usuario.email?.isNotBlank() == true) {
                    Text(
                        text = usuario.email,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (usuario.telefono?.isNotBlank() == true) {
                    Text(
                        text = usuario.telefono,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (!usuario.activo) {
                    Text(
                        text = "Inactivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }

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
            title = { Text("Eliminar Usuario") },
            text = { Text("¿Estás seguro de que quieres eliminar a ${usuario.nombreCompleto}?") },
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
fun AgregarUsuarioDialog(
    usuario: Usuario?,
    onDismiss: () -> Unit,
    onConfirm: (Usuario) -> Unit
) {
    var nombre by remember { mutableStateOf(usuario?.nombre ?: "") }
    var apellido by remember { mutableStateOf(usuario?.apellido ?: "") }
    var email by remember { mutableStateOf(usuario?.email ?: "") }
    var telefono by remember { mutableStateOf(usuario?.telefono ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (usuario == null) "Agregar Usuario" else "Editar Usuario") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apellido,
                    onValueChange = { apellido = it },
                    label = { Text("Apellido *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (opcional)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono (opcional)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nuevoUsuario = Usuario(
                        id = usuario?.id ?: 0,
                        nombre = nombre.trim(),
                        apellido = apellido.trim(),
                        email = email.trim().takeIf { it.isNotBlank() },
                        telefono = telefono.trim().takeIf { it.isNotBlank() },
                        activo = usuario?.activo ?: true
                    )
                    onConfirm(nuevoUsuario)
                },
                enabled = nombre.isNotBlank() && apellido.isNotBlank()
            ) {
                Text(if (usuario == null) "Agregar" else "Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
} 