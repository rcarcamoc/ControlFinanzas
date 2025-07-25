package com.aranthalion.controlfinanzas.presentation.categoria

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.usecase.GestionarPresupuestosUseCase
import com.aranthalion.controlfinanzas.presentation.components.CustomIcons
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    viewModel: CategoriasViewModel = hiltViewModel(),
    gestionarPresupuestosUseCase: GestionarPresupuestosUseCase = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var categoriaToEdit by remember { mutableStateOf<Categoria?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(CustomIcons.Add, contentDescription = "Agregar categoría")
                    }
                }
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
            onConfirm = { nombre, descripcion, presupuesto, activarPresupuesto ->
                viewModel.agregarCategoria(nombre, descripcion)
                if (activarPresupuesto && presupuesto > 0) {
                    scope.launch {
                        // Guardar presupuesto para el mes actual
                        val calendar = Calendar.getInstance()
                        val periodo = String.format("%04d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
                        gestionarPresupuestosUseCase.guardarPresupuesto(
                            com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity(
                                categoriaId = 0L, // Se debe actualizar con el id real tras guardar la categoría
                                monto = presupuesto,
                                periodo = periodo
                            )
                        )
                    }
                }
                showAddDialog = false
            }
        )
    }

    categoriaToEdit?.let { categoria ->
        CategoriaDialog(
            categoria = categoria,
            onDismiss = { categoriaToEdit = null },
            onConfirm = { nombre, descripcion, presupuesto, activarPresupuesto ->
                // Actualizar categoría y presupuesto
                categoriaToEdit = null
                if (activarPresupuesto && presupuesto > 0) {
                    scope.launch {
                        val calendar = Calendar.getInstance()
                        val periodo = String.format("%04d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
                        gestionarPresupuestosUseCase.guardarPresupuesto(
                            com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity(
                                categoriaId = categoria.id.toLong(),
                                monto = presupuesto,
                                periodo = periodo
                            )
                        )
                    }
                }
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
                    Icon(CustomIcons.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(CustomIcons.Delete, contentDescription = "Eliminar")
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
    onConfirm: (String, String, Double, Boolean) -> Unit
) {
    var nombre by remember { mutableStateOf(categoria?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(categoria?.descripcion ?: "") }
    var presupuesto by remember { mutableStateOf(0.0) }
    var activarPresupuesto by remember { mutableStateOf(false) }

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
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = activarPresupuesto, onCheckedChange = { activarPresupuesto = it })
                    Text("Asignar presupuesto mensual", modifier = Modifier.padding(start = 8.dp))
                }
                if (activarPresupuesto) {
                    OutlinedTextField(
                        value = if (presupuesto == 0.0) "" else presupuesto.toString(),
                        onValueChange = { presupuesto = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("Presupuesto mensual") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nombre.isNotBlank()) {
                        onConfirm(nombre, descripcion, presupuesto, activarPresupuesto)
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