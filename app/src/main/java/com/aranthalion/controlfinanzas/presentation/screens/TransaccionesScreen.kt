package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.presentation.screens.composables.AddEditMovimientoDialog
import com.aranthalion.controlfinanzas.presentation.screens.composables.FiltroDialog
import com.aranthalion.controlfinanzas.presentation.screens.components.TransaccionItem
import com.aranthalion.controlfinanzas.presentation.screens.components.EditarMovimientoDialog
import com.aranthalion.controlfinanzas.presentation.screens.components.ClasificarPendientesDialog
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasUiState
import com.aranthalion.controlfinanzas.presentation.screens.state.rememberTransaccionesScreenState
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.domain.clasificacion.ResultadoClasificacion
import androidx.compose.ui.layout.layout
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorGlobal
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.aranthalion.controlfinanzas.presentation.screens.composables.ConfirmarCapturaDialog
import com.aranthalion.controlfinanzas.data.remote.ai.VisionImportService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesScreen(
    categoriaId: Long? = null,
    viewModel: TransaccionesViewModel = hiltViewModel(),
    periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
) {
    val state = rememberTransaccionesScreenState()
    val context = LocalContext.current
    val visionLoading by viewModel.visionLoading.collectAsState()
    var parsedTransactions by remember { mutableStateOf<List<VisionImportService.ParsedTransaction>?>(null) }
    var mostrarConfirmacionDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = uriToBase64(context, it)
            if (base64 != null) {
                viewModel.importarCaptura(
                    base64Image = base64,
                    onParsed = { txs ->
                        parsedTransactions = txs
                        mostrarConfirmacionDialog = true
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                Toast.makeText(context, "Error al leer la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val periodoSeleccionado by periodoGlobalViewModel.periodoSeleccionado.collectAsState()

    LaunchedEffect(categoriaId) {
        if (categoriaId != null) {
            viewModel.filtrarPorCategoriaId(categoriaId)
        }
    }

    LaunchedEffect(periodoSeleccionado) {
        viewModel.onEvent(TransaccionesEvent.FilterByPeriodo(periodoSeleccionado))
    }

    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var mostrarAsistenteClasificacion by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { state.mostrarAddDialog.value = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir transacción")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Selector de período global
            PeriodoSelectorGlobal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Buscador estilizado con botón de filtro al lado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.onEvent(TransaccionesEvent.SearchMovimientos(it))
                    },
                    placeholder = { Text("Buscar transacciones...") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { state.mostrarFiltroDialog.value = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtrar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Escanear captura",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val currentState = uiState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Success -> {
                        if (state.mostrarAddDialog.value) {
                            AddEditMovimientoDialog(
                                onDismiss = { state.mostrarAddDialog.value = false },
                                onConfirm = { descripcion, monto, tipo, categoria, fecha, periodo ->
                                    viewModel.onEvent(
                                        TransaccionesEvent.AddMovimiento(
                                            descripcion = descripcion,
                                            monto = monto,
                                            tipo = tipo,
                                            categoria = categoria,
                                            fecha = fecha,
                                            periodo = periodo
                                        )
                                    )
                                    state.mostrarAddDialog.value = false
                                },
                                categorias = currentState.categorias
                            )
                        }

                        if (state.mostrarFiltroDialog.value) {
                            FiltroDialog(
                                onDismiss = { state.mostrarFiltroDialog.value = false },
                                onConfirm = { tipo, categoria ->
                                    viewModel.onEvent(TransaccionesEvent.FilterMovimientos(tipo, categoria))
                                    state.mostrarFiltroDialog.value = false
                                },
                                categorias = currentState.categorias
                            )
                        }

                        if (mostrarConfirmacionDialog && parsedTransactions != null) {
                            ConfirmarCapturaDialog(
                                onDismiss = {
                                    mostrarConfirmacionDialog = false
                                    parsedTransactions = null
                                },
                                onConfirm = { entities ->
                                    entities.forEach { entity ->
                                        viewModel.onEvent(
                                            TransaccionesEvent.AddMovimiento(
                                                descripcion = entity.descripcion,
                                                monto = entity.monto,
                                                tipo = entity.tipo,
                                                categoria = currentState.categorias.find { it.id == entity.categoriaId },
                                                fecha = entity.fecha,
                                                periodo = entity.periodoFacturacion ?: periodoSeleccionado
                                            )
                                        )
                                    }
                                    mostrarConfirmacionDialog = false
                                    parsedTransactions = null
                                    Toast.makeText(context, "Gastos importados con éxito", Toast.LENGTH_SHORT).show()
                                },
                                transaccionesExtraidas = parsedTransactions!!,
                                categorias = currentState.categorias,
                                periodoFacturacionActual = periodoSeleccionado
                            )
                        }

                        state.movimientoAEditar.value?.let { movimiento ->
                            EditarMovimientoDialog(
                                movimiento = movimiento,
                                categoriasUiState = CategoriasUiState.Success(currentState.categorias),
                                onDismiss = { state.movimientoAEditar.value = null },
                                onConfirm = { movimientoEditado ->
                                    viewModel.onEvent(TransaccionesEvent.EditMovimiento(movimientoEditado))
                                    state.movimientoAEditar.value = null
                                }
                            )
                        }

                        val movimientosSinCategoria = remember(currentState.movimientos) {
                            currentState.movimientos.filter { it.categoriaId == null && it.tipo != "OMITIR" }
                        }

                        if (mostrarAsistenteClasificacion) {
                            ClasificarPendientesDialog(
                                movimientosSinCategoria = movimientosSinCategoria,
                                categorias = currentState.categorias,
                                onUpdateCategory = { movimientoId, catId ->
                                    viewModel.onEvent(TransaccionesEvent.UpdateMovimientoCategoria(movimientoId, catId))
                                },
                                clasificacionUseCase = viewModel.clasificacionUseCase,
                                onDismiss = { mostrarAsistenteClasificacion = false }
                            )
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Tarjeta de progreso de clasificación
                            val total = currentState.movimientos.size
                            val clasificadas = currentState.movimientos.count { it.categoriaId != null }
                            val porcentaje = if (total > 0) clasificadas.toFloat() / total else 0f
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Progreso de Clasificación",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$clasificadas de $total",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = porcentaje,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${(porcentaje * 100).toInt()}% de transacciones categorizadas",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    if (movimientosSinCategoria.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { mostrarAsistenteClasificacion = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Clasificar Pendientes", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp)
                            ) {
                                items(currentState.movimientos, key = { it.id }) { movimiento ->
                                    TransaccionItem(
                                        movimiento = movimiento,
                                        categorias = currentState.categorias,
                                        onEdit = {
                                            state.movimientoAEditar.value = movimiento
                                        },
                                        onDelete = {
                                            viewModel.onEvent(TransaccionesEvent.DeleteMovimiento(movimiento.id))
                                        },
                                        onUpdateCategory = { catId ->
                                            viewModel.onEvent(TransaccionesEvent.UpdateMovimientoCategoria(movimiento.id, catId))
                                        },
                                        clasificacionUseCase = viewModel.clasificacionUseCase
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = currentState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (visionLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Analizando captura con IA...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
        }
    }
}
}

private fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } else null
    } catch (e: Exception) {
        null
    }
}

