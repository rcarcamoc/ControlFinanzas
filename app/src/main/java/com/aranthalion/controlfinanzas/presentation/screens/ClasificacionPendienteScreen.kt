package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.clickable
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
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.domain.clasificacion.SugerenciaClasificacion
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasViewModel
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClasificacionPendienteScreen(
    navController: NavHostController,
    viewModel: ClasificacionPendienteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categoriasViewModel: CategoriasViewModel = hiltViewModel()
    val categoriasUiState by categoriasViewModel.uiState.collectAsState()
    
    var transaccionSeleccionada by remember { mutableStateOf<MovimientoEntity?>(null) }
    var showClasificacionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Clasificación Pendiente",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                is ClasificacionPendienteUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ClasificacionPendienteUiState.Success -> {
                    val transacciones = (uiState as ClasificacionPendienteUiState.Success).transacciones
                    
                    if (transacciones.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "¡Excelente!",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Todas las transacciones están clasificadas",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Transacciones Pendientes",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "${transacciones.size} transacciones requieren clasificación",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                            
                            items(transacciones) { transaccion ->
                                TransaccionPendienteItem(
                                    transaccion = transaccion,
                                    onClasificar = {
                                        transaccionSeleccionada = transaccion
                                        showClasificacionDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
                is ClasificacionPendienteUiState.Error -> {
                    Text(
                        text = (uiState as ClasificacionPendienteUiState.Error).mensaje,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    // Diálogo de clasificación
    if (showClasificacionDialog && transaccionSeleccionada != null) {
        val categorias = when (categoriasUiState) {
            is CategoriasUiState.Success -> {
                val domainCategorias = (categoriasUiState as CategoriasUiState.Success).categorias
                domainCategorias.map { domainCategoria ->
                    Categoria(
                        id = domainCategoria.id.toLong(),
                        nombre = domainCategoria.nombre,
                        descripcion = domainCategoria.descripcion,
                        tipo = "GASTO"
                    )
                }
            }
            else -> emptyList()
        }
        
        ClasificacionDialog(
            transaccion = transaccionSeleccionada!!,
            categorias = categorias,
            onDismiss = { 
                showClasificacionDialog = false
                transaccionSeleccionada = null
            },
            onClasificar = { categoriaId ->
                transaccionSeleccionada?.let { transaccion ->
                    viewModel.clasificarTransaccion(transaccion, categoriaId)
                }
                showClasificacionDialog = false
                transaccionSeleccionada = null
            }
        )
    }
}

@Composable
private fun TransaccionPendienteItem(
    transaccion: MovimientoEntity,
    onClasificar: () -> Unit
) {
    val formattedDate = remember(transaccion.fecha) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(transaccion.fecha)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClasificar() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaccion.descripcion,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "• ${transaccion.periodoFacturacion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (transaccion.tipoTarjeta != null) {
                            Text(
                                text = "• ${transaccion.tipoTarjeta}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = FormatUtils.formatMoneyCLP(transaccion.monto),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (transaccion.tipo == "INGRESO") 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        modifier = Modifier.widthIn(min = 80.dp)
                    )
                    
                    IconButton(
                        onClick = onClasificar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Clasificar",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClasificacionDialog(
    transaccion: MovimientoEntity,
    categorias: List<Categoria>,
    onDismiss: () -> Unit,
    onClasificar: (Long) -> Unit
) {
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(null) }
    var expandedCategoria by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Clasificar Transacción",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Información de la transacción
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = transaccion.descripcion,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = FormatUtils.formatMoneyCLP(transaccion.monto),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (transaccion.tipo == "INGRESO") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // Selector de categoría
                ExposedDropdownMenuBox(
                    expanded = expandedCategoria,
                    onExpandedChange = { expandedCategoria = !expandedCategoria }
                ) {
                    OutlinedTextField(
                        value = categoriaSeleccionada?.nombre ?: "Seleccionar categoría",
                        onValueChange = {},
                        label = { Text("Categoría") },
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoria,
                        onDismissRequest = { expandedCategoria = false }
                    ) {
                        categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria.nombre) },
                                onClick = {
                                    categoriaSeleccionada = categoria
                                    expandedCategoria = false
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
                    categoriaSeleccionada?.let { categoria ->
                        onClasificar(categoria.id)
                    }
                },
                enabled = categoriaSeleccionada != null
            ) {
                Text("Clasificar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

sealed class ClasificacionPendienteUiState {
    object Loading : ClasificacionPendienteUiState()
    data class Success(val transacciones: List<MovimientoEntity>) : ClasificacionPendienteUiState()
    data class Error(val mensaje: String) : ClasificacionPendienteUiState()
} 