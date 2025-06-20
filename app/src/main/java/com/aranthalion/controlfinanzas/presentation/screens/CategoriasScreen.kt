package com.aranthalion.controlfinanzas.presentation.screens

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasViewModel
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    navController: NavHostController,
    viewModel: CategoriasViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var categoriaToEdit by remember { mutableStateOf<Categoria?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Categorías",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar categoría")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
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
                is CategoriasUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is CategoriasUiState.Success -> {
                    val categorias = (uiState as CategoriasUiState.Success).categorias
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categorias) { categoria ->
                            CategoriaItem(
                                categoria = categoria,
                                onEdit = { categoriaToEdit = categoria },
                                onDelete = { viewModel.eliminarCategoria(categoria) }
                            )
                        }
                    }
                }
                is CategoriasUiState.Error -> {
                    Text(
                        text = (uiState as CategoriasUiState.Error).mensaje,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        CategoriaDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { nombre, descripcion ->
                viewModel.agregarCategoria(nombre, descripcion)
                showAddDialog = false
            }
        )
    }

    categoriaToEdit?.let { categoria ->
        CategoriaDialog(
            categoria = categoria,
            onDismiss = { categoriaToEdit = null },
            onConfirm = { nombre, descripcion ->
                viewModel.agregarCategoria(nombre, descripcion)
                categoriaToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaItem(
    categoria: Categoria,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = categoria.nombre,
                    style = MaterialTheme.typography.titleMedium
                )
                if (categoria.descripcion.isNotEmpty()) {
                    Text(
                        text = categoria.descripcion,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaDialog(
    categoria: Categoria? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nombre by remember { mutableStateOf(categoria?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(categoria?.descripcion ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (categoria == null) "Nueva Categoría" else "Editar Categoría") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nombre.isNotBlank()) {
                        onConfirm(nombre, descripcion)
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
} 