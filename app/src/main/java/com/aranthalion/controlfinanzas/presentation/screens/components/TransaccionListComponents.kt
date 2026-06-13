package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TransaccionesListCard(
    movimientosFiltrados: List<MovimientoEntity>,
    categorias: List<Categoria>,
    periodoGlobal: String,
    onEditMovimiento: (MovimientoEntity) -> Unit,
    onDeleteMovimiento: (MovimientoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transacciones (${movimientosFiltrados.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (movimientosFiltrados.isNotEmpty()) {
                    Text(
                        text = "Período: $periodoGlobal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (movimientosFiltrados.isEmpty()) {
                EmptyTransactionsState()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    movimientosFiltrados.forEach { movimiento ->
                        TransaccionListItem(
                            movimiento = movimiento,
                            categorias = categorias,
                            onEdit = onEditMovimiento,
                            onDelete = onDeleteMovimiento
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTransactionsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "No hay transacciones",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Agrega tu primera transacción para comenzar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TransaccionListItem(
    movimiento: MovimientoEntity,
    categorias: List<Categoria>,
    onEdit: (MovimientoEntity) -> Unit,
    onDelete: (MovimientoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoria = categorias.find { it.id == movimiento.categoriaId }
    val formattedDate = remember(movimiento.fecha) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(movimiento.fecha)
    }
    var deleteConfirmCountdown by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (movimiento.categoriaId == null) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(4.dp))
            
            // Información principal
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Categoría y estado
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = categoria?.nombre ?: "Sin categoría",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (movimiento.categoriaId == null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (movimiento.categoriaId == null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Sin clasificar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(
                                    horizontal = 4.dp,
                                    vertical = 1.dp
                                )
                            )
                        }
                    }
                }
                
                // Descripción
                if (movimiento.descripcion.isNotEmpty()) {
                    Text(
                        text = movimiento.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Fecha
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Tarjeta
                if (!movimiento.tipoTarjeta.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tarjeta: ${movimiento.tipoTarjeta}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Período de facturación
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Período: ${movimiento.periodoFacturacion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Información del monto y tipo
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = FormatUtils.formatMoneyCLP(movimiento.monto),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (movimiento.tipo) {
                        "INGRESO" -> MaterialTheme.colorScheme.primary
                        "GASTO" -> MaterialTheme.colorScheme.error
                        "OMITIR" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (movimiento.tipo) {
                        "INGRESO" -> MaterialTheme.colorScheme.primaryContainer
                        "GASTO" -> MaterialTheme.colorScheme.errorContainer
                        "OMITIR" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = movimiento.tipo,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (movimiento.tipo) {
                            "INGRESO" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "GASTO" -> MaterialTheme.colorScheme.onErrorContainer
                            "OMITIR" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            
            // Botones de acción
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onEdit(movimiento) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar transacción",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
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
                            onDelete(movimiento)
                            deleteConfirmCountdown = 0
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar transacción",
                        tint = if (deleteConfirmCountdown > 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        if (deleteConfirmCountdown > 0) {
            Text(
                text = "Toca de nuevo para confirmar eliminación",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
    }
}
