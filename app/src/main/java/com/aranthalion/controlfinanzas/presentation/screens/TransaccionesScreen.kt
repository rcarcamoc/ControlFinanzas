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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesScreen(
    categoriaId: Long? = null,
    viewModel: TransaccionesViewModel = hiltViewModel()
) {
    val state = rememberTransaccionesScreenState()

    LaunchedEffect(categoriaId) {
        if (categoriaId != null) {
            viewModel.filtrarPorCategoriaId(categoriaId)
        }
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
                            currentState.movimientos.filter { it.categoriaId == null }
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
            }
        }
    }
}

@Composable
fun ClasificarPendientesDialog(
    movimientosSinCategoria: List<MovimientoEntity>,
    categorias: List<Categoria>,
    onUpdateCategory: (Long, Long?) -> Unit,
    clasificacionUseCase: GestionarClasificacionAutomaticaUseCase,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header del Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Asistente de Clasificación",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                if (currentIndex < movimientosSinCategoria.size) {
                    val movimiento = movimientosSinCategoria[currentIndex]
                    
                    // Barra de progreso de la cola
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = (currentIndex.toFloat() / movimientosSinCategoria.size),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Transacción ${currentIndex + 1} de ${movimientosSinCategoria.size} pendientes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Detalle del movimiento actual
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = movimiento.descripcion.ifEmpty { "Sin descripción" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(movimiento.fecha),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (movimiento.tipo == "INGRESO") {
                                        "+${FormatUtils.formatMoneyCLP(movimiento.monto)}"
                                    } else {
                                        "-${FormatUtils.formatMoneyCLP(movimiento.monto)}"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (movimiento.tipo == "INGRESO") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Buscar sugerencia automática para el item actual
                    var sugerenciaCategoriaId by remember(movimiento.id) { mutableStateOf<Long?>(null) }
                    var confianza by remember(movimiento.id) { mutableStateOf(0.0) }
                    var cargandoSugerencia by remember(movimiento.id) { mutableStateOf(true) }
                    var resultadoClasificacion by remember(movimiento.id) { mutableStateOf<ResultadoClasificacion?>(null) }

                    LaunchedEffect(movimiento.id) {
                        try {
                            val resultado = clasificacionUseCase.obtenerSugerenciaMejorada(movimiento.descripcion)
                            resultadoClasificacion = resultado
                            when (resultado) {
                                is ResultadoClasificacion.AltaConfianza -> {
                                    sugerenciaCategoriaId = resultado.categoriaId
                                    confianza = resultado.confianza
                                }
                                is ResultadoClasificacion.BajaConfianza -> {
                                    val mejor = resultado.sugerencias.maxByOrNull { it.nivelConfianza }
                                    if (mejor != null && clasificacionUseCase.esConfianzaSuficiente(mejor.nivelConfianza)) {
                                        sugerenciaCategoriaId = mejor.categoriaId
                                        confianza = mejor.nivelConfianza
                                    }
                                }
                                else -> {}
                            }
                        } catch (e: Exception) {
                            // Ignorar error de sugerencia
                        } finally {
                            cargandoSugerencia = false
                        }
                    }

                    val sugCat = sugerenciaCategoriaId?.let { id -> categorias.find { it.id == id } }

                    // Ordenar las categorías para selección manual por porcentaje de probabilidad
                    val categoriasOrdenadas = remember(categorias, resultadoClasificacion) {
                        val resultado = resultadoClasificacion
                        if (resultado == null) {
                            categorias
                        } else {
                            categorias.sortedByDescending { categoria ->
                                when (resultado) {
                                    is ResultadoClasificacion.AltaConfianza -> {
                                        if (categoria.id == resultado.categoriaId) {
                                            resultado.confianza
                                        } else {
                                            val alt = resultado.sugerenciasAlternativas.find { it.categoriaId == categoria.id }
                                            alt?.nivelConfianza ?: 0.0
                                        }
                                    }
                                    is ResultadoClasificacion.BajaConfianza -> {
                                        val sugerencia = resultado.sugerencias.find { it.categoriaId == categoria.id }
                                        sugerencia?.nivelConfianza ?: 0.0
                                    }
                                    is ResultadoClasificacion.SinCoincidencias -> 0.0
                                }
                            }
                        }
                    }

                    // Sección de Sugerencia Rápida
                    if (cargandoSugerencia) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (sugCat != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "¿Sugerir '${sugCat.nombre}'?",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Confianza: ${(confianza * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        onUpdateCategory(movimiento.id, sugCat.id)
                                        currentIndex++
                                    }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Aceptar sugerencia")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Aceptar")
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No hay sugerencias automáticas seguras para esta transacción.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Divider()

                    // Grilla de categorías para selección manual directa
                    Text(
                        text = "Seleccionar Categoría:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Box(modifier = Modifier.weight(1f, fill = false).maxHeight(240.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categoriasOrdenadas) { cat ->
                                Button(
                                    onClick = {
                                        onUpdateCategory(movimiento.id, cat.id)
                                        currentIndex++
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = cat.nombre,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Acciones inferiores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { currentIndex++ }
                        ) {
                            Text("Omitir")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente")
                        }
                    }
                } else {
                    // Estado completado
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "¡Excelente trabajo!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Has revisado todas las transacciones pendientes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("Finalizar")
                        }
                    }
                }
            }
        }
    }
}

// Extensión Modifier para max height en Compose
fun Modifier.maxHeight(maxHeight: androidx.compose.ui.unit.Dp): Modifier = layout { measurable, constraints ->
    val height = constraints.maxHeight.coerceAtMost(maxHeight.roundToPx())
    val placeable = measurable.measure(constraints.copy(maxHeight = height))
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}
