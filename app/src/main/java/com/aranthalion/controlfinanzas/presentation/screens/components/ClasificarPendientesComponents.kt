package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun QueueProgressBar(
    currentIndex: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = (currentIndex.toFloat() / totalCount),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Transacción ${currentIndex + 1} de $totalCount pendientes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PendingMovimientoDetailCard(
    movimiento: MovimientoEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScopeSelector(
    scopeSeleccionado: String,
    onScopeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                onClick = { onScopeChange("HOUSEHOLD") },
                label = { Text("🏠 Grupo Familiar") }
            )
            FilterChip(
                selected = scopeSeleccionado == "PERSONAL",
                onClick = { onScopeChange("PERSONAL") },
                label = { Text("👤 Gastos Personales") }
            )
        }
    }
}

@Composable
fun SimilarMovimientosCard(
    similarMovimientos: List<MovimientoEntity>,
    seleccionadosSimilares: List<Long>,
    onToggleSimilar: (Long, Boolean) -> Unit,
    onShowDetail: (MovimientoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listItems(similarMovimientos) { sim ->
                        val isChecked = seleccionadosSimilares.contains(sim.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShowDetail(sim) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    onToggleSimilar(sim.id, checked)
                                },
                                modifier = Modifier.scale(0.85f)
                            )
                            Text(
                                text = sim.descripcion.ifEmpty { "Sin descripción" },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = FormatUtils.formatMoneyCLP(sim.monto),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sim.tipo == "INGRESO") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiSuggestionCard(
    cargandoSugerencia: Boolean,
    sugCat: Categoria?,
    confianza: Double,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (cargandoSugerencia) {
        Box(
            modifier = modifier.fillMaxWidth().height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    } else if (sugCat != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
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
                Button(onClick = onAccept) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
}
