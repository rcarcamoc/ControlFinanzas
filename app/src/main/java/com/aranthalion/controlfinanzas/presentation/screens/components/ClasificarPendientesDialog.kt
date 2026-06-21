package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.domain.clasificacion.ResultadoClasificacion
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClasificarPendientesDialog(
    movimientosSinCategoria: List<MovimientoEntity>,
    categorias: List<Categoria>,
    onUpdateCategory: (List<Long>, Long?, String) -> Unit,
    clasificacionUseCase: GestionarClasificacionAutomaticaUseCase,
    onDismiss: () -> Unit
) {
    // Tomar un snapshot estático al abrir el diálogo para definir la sesión de clasificación
    val sessionMovimientos = remember { movimientosSinCategoria }
    var currentIndex by remember { mutableStateOf(0) }
    val idsClasificados = remember { mutableStateListOf<Long>() }
    var modoPorCategorias by remember { mutableStateOf(false) }
    val sugerenciasMap = remember { mutableStateMapOf<Long, Long?>() }

    LaunchedEffect(sessionMovimientos) {
        sessionMovimientos.forEach { mov ->
            if (sugerenciasMap[mov.id] == null) {
                try {
                    val resultado = clasificacionUseCase.obtenerSugerenciaMejorada(mov.descripcion)
                    val catId = when (resultado) {
                        is ResultadoClasificacion.AltaConfianza -> resultado.categoriaId
                        is ResultadoClasificacion.BajaConfianza -> {
                            val mejor = resultado.sugerencias.maxByOrNull { it.nivelConfianza }
                            if (mejor != null && clasificacionUseCase.esConfianzaSuficiente(mejor.nivelConfianza)) {
                                mejor.categoriaId
                            } else {
                                null
                            }
                        }
                        else -> null
                    }
                    sugerenciasMap[mov.id] = catId
                } catch (e: Exception) {
                    sugerenciasMap[mov.id] = null
                }
            }
        }
    }

    LaunchedEffect(currentIndex, idsClasificados.size) {
        while (currentIndex < sessionMovimientos.size && idsClasificados.contains(sessionMovimientos[currentIndex].id)) {
            currentIndex++
        }
    }

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

                // Selector de modo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !modoPorCategorias,
                        onClick = { modoPorCategorias = false },
                        label = { Text("Asistente Rápido") }
                    )
                    FilterChip(
                        selected = modoPorCategorias,
                        onClick = { modoPorCategorias = true },
                        label = { Text("Por Categorías") }
                    )
                }

                if (!modoPorCategorias) {
                    if (currentIndex < sessionMovimientos.size) {
                    val movimiento = sessionMovimientos[currentIndex]
                    var scopeSeleccionado by remember(movimiento.id) { mutableStateOf(movimiento.scope) }
                    
                    val similarMovimientos = remember(movimiento.id) {
                        sessionMovimientos.subList(currentIndex + 1, sessionMovimientos.size)
                            .filter { areSimilar(movimiento.descripcion, it.descripcion) }
                    }
                    
                    val seleccionadosSimilares = remember(movimiento.id) {
                        mutableStateListOf<Long>().apply {
                            addAll(similarMovimientos.map { it.id })
                        }
                    }

                    val clasificarActualYSimilares = { categoriaId: Long? ->
                        val idsAClasificar = listOf(movimiento.id) + seleccionadosSimilares.toList()
                        onUpdateCategory(idsAClasificar, categoriaId, scopeSeleccionado)
                        idsClasificados.addAll(idsAClasificar)
                        currentIndex++
                    }
                    
                    // Barra de progreso de la cola
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = (currentIndex.toFloat() / sessionMovimientos.size),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Transacción ${currentIndex + 1} de ${sessionMovimientos.size} pendientes",
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
                                Column {
                                    Text(
                                        text = SimpleDateFormat("EEEE, dd/MM/yyyy HH:mm", Locale.getDefault())
                                            .format(movimiento.fecha)
                                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    movimiento.tipoTarjeta?.let { tarjeta ->
                                        if (tarjeta.isNotEmpty()) {
                                            Text(
                                                text = "Tarjeta: $tarjeta",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
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

                    // Selector de ámbito (Grupo Familiar vs Personal)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Imputación / Ámbito:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = scopeSeleccionado == "HOUSEHOLD",
                                onClick = { scopeSeleccionado = "HOUSEHOLD" },
                                label = { Text("🏠 Grupo Familiar") }
                            )
                            FilterChip(
                                selected = scopeSeleccionado == "PERSONAL",
                                onClick = { scopeSeleccionado = "PERSONAL" },
                                label = { Text("👤 Gastos Personales") }
                            )
                        }
                    }

                    // Transacciones Similares Encontradas
                    if (similarMovimientos.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "¿Clasificar también similares? (${seleccionadosSimilares.size} seleccionadas)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .maxHeight(120.dp)
                                ) {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        listItems(similarMovimientos) { sim ->
                                            val isChecked = seleccionadosSimilares.contains(sim.id)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        if (checked) {
                                                            seleccionadosSimilares.add(sim.id)
                                                        } else {
                                                            seleccionadosSimilares.remove(sim.id)
                                                        }
                                                    },
                                                    modifier = Modifier.scale(0.85f)
                                                )
                                                Text(
                                                    text = sim.descripcion.ifEmpty { "Sin descripción" },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
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
                                        clasificarActualYSimilares(sugCat.id)
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
                            gridItems(categoriasOrdenadas) { cat ->
                                Button(
                                    onClick = {
                                        clasificarActualYSimilares(cat.id)
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
                            onClick = {
                                clasificarActualYSimilares(-1L)
                            }
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
                } else {
                    // MODO POR CATEGORÍAS
                    var categoriaSeleccionadaId by remember { mutableStateOf<Long?>(null) }
                    var filtroBusqueda by remember { mutableStateOf("") }
                    val seleccionadosLote = remember { mutableStateListOf<Long>() }
                    var scopeSeleccionadoLote by remember { mutableStateOf("HOUSEHOLD") }

                    val activeCategory = categorias.find { it.id == categoriaSeleccionadaId }

                    if (categoriaSeleccionadaId == null) {
                        Text(
                            text = "Selecciona una categoría para clasificar en lote:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Box(modifier = Modifier.weight(1f).maxHeight(320.dp)) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 130.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                gridItems(categorias) { cat ->
                                    val count = sessionMovimientos.count { mov ->
                                        !idsClasificados.contains(mov.id) && sugerenciasMap[mov.id] == cat.id
                                    }
                                    Button(
                                        onClick = {
                                            categoriaSeleccionadaId = cat.id
                                            seleccionadosLote.clear()
                                            seleccionadosLote.addAll(
                                                sessionMovimientos.filter { mov ->
                                                    !idsClasificados.contains(mov.id) && sugerenciasMap[mov.id] == cat.id
                                                }.map { it.id }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (count > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (count > 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = cat.nombre,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (count > 0) {
                                                Text(
                                                    text = "$count sugerencias",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Categoría: ${activeCategory?.nombre ?: ""}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = { categoriaSeleccionadaId = null }) {
                                    Text("Volver")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Ámbito lote:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                FilterChip(
                                    selected = scopeSeleccionadoLote == "HOUSEHOLD",
                                    onClick = { scopeSeleccionadoLote = "HOUSEHOLD" },
                                    label = { Text("🏠 Familiar") }
                                )
                                FilterChip(
                                    selected = scopeSeleccionadoLote == "PERSONAL",
                                    onClick = { scopeSeleccionadoLote = "PERSONAL" },
                                    label = { Text("👤 Personal") }
                                )
                            }

                            OutlinedTextField(
                                value = filtroBusqueda,
                                onValueChange = { filtroBusqueda = it },
                                placeholder = { Text("Buscar descripción...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            val filtradosSugeridos = remember(sessionMovimientos, idsClasificados.size, categoriaSeleccionadaId, filtroBusqueda) {
                                sessionMovimientos.filter { mov ->
                                    !idsClasificados.contains(mov.id) &&
                                    sugerenciasMap[mov.id] == categoriaSeleccionadaId &&
                                    (filtroBusqueda.isEmpty() || mov.descripcion.contains(filtroBusqueda, ignoreCase = true))
                                }
                            }

                            val filtradosOtros = remember(sessionMovimientos, idsClasificados.size, categoriaSeleccionadaId, filtroBusqueda) {
                                sessionMovimientos.filter { mov ->
                                    !idsClasificados.contains(mov.id) &&
                                    sugerenciasMap[mov.id] != categoriaSeleccionadaId &&
                                    (filtroBusqueda.isEmpty() || mov.descripcion.contains(filtroBusqueda, ignoreCase = true))
                                }
                            }

                            Box(modifier = Modifier.weight(1f).maxHeight(200.dp)) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (filtradosSugeridos.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Sugeridos para esta categoría:",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        listItems(filtradosSugeridos) { sim ->
                                            val isChecked = seleccionadosLote.contains(sim.id)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        if (checked) seleccionadosLote.add(sim.id) else seleccionadosLote.remove(sim.id)
                                                    },
                                                    modifier = Modifier.scale(0.85f)
                                                )
                                                Text(
                                                    text = sim.descripcion,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = FormatUtils.formatMoneyCLP(sim.monto),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    if (filtradosOtros.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Otros movimientos sin clasificar:",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        listItems(filtradosOtros) { sim ->
                                            val isChecked = seleccionadosLote.contains(sim.id)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        if (checked) seleccionadosLote.add(sim.id) else seleccionadosLote.remove(sim.id)
                                                    },
                                                    modifier = Modifier.scale(0.85f)
                                                )
                                                Text(
                                                    text = sim.descripcion,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = FormatUtils.formatMoneyCLP(sim.monto),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (seleccionadosLote.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        onUpdateCategory(seleccionadosLote.toList(), categoriaSeleccionadaId, scopeSeleccionadoLote)
                                        idsClasificados.addAll(seleccionadosLote)
                                        seleccionadosLote.clear()
                                        categoriaSeleccionadaId = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Confirmar Lote (${seleccionadosLote.size})")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.maxHeight(maxHeight: androidx.compose.ui.unit.Dp): Modifier = layout { measurable, constraints ->
    val targetMaxHeight = maxHeight.roundToPx()
    val minHeight = constraints.minHeight.coerceAtMost(targetMaxHeight)
    val height = constraints.maxHeight.coerceAtMost(targetMaxHeight).coerceAtLeast(minHeight)
    val placeable = measurable.measure(
        constraints.copy(
            minHeight = minHeight,
            maxHeight = height
        )
    )
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}

private fun cleanDescription(desc: String): List<String> {
    return desc.lowercase()
        .replace(Regex("[^a-z0-9\\s]"), "")
        .split(Regex("\\s+"))
        .filter { it.length > 2 }
}

private fun getSimilarity(desc1: String, desc2: String): Double {
    val words1 = cleanDescription(desc1)
    val words2 = cleanDescription(desc2)
    if (words1.isEmpty() || words2.isEmpty()) return 0.0
    val intersection = words1.filter { words2.contains(it) }
    val union = (words1 + words2).distinct()
    return intersection.size.toDouble() / union.size.toDouble()
}

private fun areSimilar(desc1: String, desc2: String): Boolean {
    val d1 = desc1.lowercase().trim()
    val d2 = desc2.lowercase().trim()
    if (d1 == d2) return true
    
    val clean1 = cleanDescription(d1)
    val clean2 = cleanDescription(d2)
    if (clean1.isEmpty() || clean2.isEmpty()) return false
    
    if (clean1.size >= 2 && clean2.size >= 2) {
        if (clean1[0] == clean2[0] && clean1[1] == clean2[1]) return true
    }
    
    return getSimilarity(desc1, desc2) >= 0.4
}
