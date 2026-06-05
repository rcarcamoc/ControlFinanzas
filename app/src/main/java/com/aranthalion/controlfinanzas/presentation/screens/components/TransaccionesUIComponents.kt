package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.presentation.screens.MovimientosUiState
import com.aranthalion.controlfinanzas.presentation.screens.state.TransaccionesScreenState
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasUiState
import com.aranthalion.controlfinanzas.domain.categoria.Categoria as DomainCategoria
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import java.text.SimpleDateFormat
import java.util.Locale
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.domain.clasificacion.ResultadoClasificacion
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesTopAppBar(
    onFilterPressed: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Transacciones",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = {
            IconButton(onClick = onFilterPressed) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Filtrar transacciones",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ClasificacionProgressBar(uiState: MovimientosUiState) {
    if (uiState !is MovimientosUiState.Success) return
    
    val movimientos = uiState.movimientos
    val clasificadas = movimientos.count { it.categoriaId != null }
    val porcentaje = if (movimientos.isNotEmpty()) {
        clasificadas.toFloat() / movimientos.size
    } else {
        0f
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        LinearProgressIndicator(
            progress = porcentaje,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "${(porcentaje * 100).toInt()}% transacciones clasificadas",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun TransaccionesSubtitle() {
    Text(
        text = "Gestiona tus ingresos y gastos",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
fun SearchTransactionField(
    searchText: String,
    onSearchChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchChanged,
        label = { Text("Buscar transacciones...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { onSearchChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar búsqueda",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun LoadingTransactionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cargando transacciones...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ErrorTransactionCard(errorMessage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Error al cargar transacciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
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

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

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
