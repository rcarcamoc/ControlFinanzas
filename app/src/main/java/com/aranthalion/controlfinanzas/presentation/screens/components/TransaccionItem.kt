package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.domain.clasificacion.ResultadoClasificacion
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionItem(
    movimiento: MovimientoEntity,
    categorias: List<Categoria>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateCategory: (Long?) -> Unit,
    clasificacionUseCase: GestionarClasificacionAutomaticaUseCase,
    modifier: Modifier = Modifier
) {
    val categoria = categorias.find { it.id == movimiento.categoriaId }
    val formattedDate = remember(movimiento.fecha) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(movimiento.fecha)
    }
    
    var deleteConfirmCountdown by remember { mutableStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Buscar sugerencia automática si no tiene categoría
    var sugerenciaCategoriaId by remember { mutableStateOf<Long?>(null) }
    var nivelConfianzaSugerido by remember { mutableStateOf(0.0) }
    var cargandoSugerencia by remember { mutableStateOf(false) }

    LaunchedEffect(movimiento.categoriaId, movimiento.descripcion) {
        if (movimiento.categoriaId == null) {
            cargandoSugerencia = true
            try {
                val resultado = clasificacionUseCase.obtenerSugerenciaMejorada(movimiento.descripcion)
                when (resultado) {
                    is ResultadoClasificacion.AltaConfianza -> {
                        sugerenciaCategoriaId = resultado.categoriaId
                        nivelConfianzaSugerido = resultado.confianza
                    }
                    is ResultadoClasificacion.BajaConfianza -> {
                        val mejor = resultado.sugerencias.maxByOrNull { it.nivelConfianza }
                        if (mejor != null && clasificacionUseCase.esConfianzaSuficiente(mejor.nivelConfianza)) {
                            sugerenciaCategoriaId = mejor.categoriaId
                            nivelConfianzaSugerido = mejor.nivelConfianza
                        } else {
                            sugerenciaCategoriaId = null
                        }
                    }
                    else -> {
                        sugerenciaCategoriaId = null
                    }
                }
            } catch (e: Exception) {
                sugerenciaCategoriaId = null
            } finally {
                cargandoSugerencia = false
            }
        } else {
            sugerenciaCategoriaId = null
        }
    }

    val categoriaSugerida = sugerenciaCategoriaId?.let { id -> categorias.find { it.id == id } }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (movimiento.categoriaId == null) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Fila superior: Categoría y badge de clasificación
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clickable { menuExpanded = true }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = categoria?.nombre ?: "Sin categoría",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (movimiento.categoriaId == null) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Cambiar categoría",
                                tint = if (movimiento.categoriaId == null) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            if (movimiento.categoriaId == null) {
                                SuggestionBadge(text = "Pendiente", color = MaterialTheme.colorScheme.error)
                            } else {
                                SuggestionBadge(
                                    text = "Guardada",
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sin categoría") },
                                onClick = {
                                    onUpdateCategory(null)
                                    menuExpanded = false
                                }
                            )
                            categorias.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.nombre) },
                                    onClick = {
                                        onUpdateCategory(cat.id)
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Descripción
                    Text(
                        text = movimiento.descripcion.ifEmpty { "Sin descripción" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Fecha
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Fila derecha: Monto y acciones
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (movimiento.tipo == "INGRESO") {
                            "+${FormatUtils.formatMoneyCLP(movimiento.monto)}"
                        } else {
                            "-${FormatUtils.formatMoneyCLP(movimiento.monto)}"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (movimiento.tipo == "INGRESO") 
                            Color(0xFF2E7D32) 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                if (deleteConfirmCountdown == 0) {
                                    deleteConfirmCountdown = 2
                                    scope.launch {
                                        repeat(2) {
                                            delay(500)
                                            deleteConfirmCountdown--
                                        }
                                    }
                                } else if (deleteConfirmCountdown == 1) {
                                    onDelete()
                                    deleteConfirmCountdown = 0
                                }
                            },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (deleteConfirmCountdown > 0) 
                                    MaterialTheme.colorScheme.errorContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                modifier = Modifier.size(16.dp),
                                tint = if (deleteConfirmCountdown > 0) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Advertencia de eliminación confirmación
            AnimatedVisibility(
                visible = deleteConfirmCountdown > 0,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = "Toca de nuevo para confirmar eliminación",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Caja de sugerencia rápida por ML local
            AnimatedVisibility(
                visible = movimiento.categoriaId == null && categoriaSugerida != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                categoriaSugerida?.let { sugCat ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "¿Es '${sugCat.nombre}'?",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Confianza local: ${(nivelConfianzaSugerido * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onUpdateCategory(sugCat.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Confirmar",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sí", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionBadge(text: String, color: Color) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
