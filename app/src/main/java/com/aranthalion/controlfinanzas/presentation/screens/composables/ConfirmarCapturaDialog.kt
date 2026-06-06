package com.aranthalion.controlfinanzas.presentation.screens.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.remote.ai.VisionImportService
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmarCapturaDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<MovimientoEntity>) -> Unit,
    transaccionesExtraidas: List<VisionImportService.ParsedTransaction>,
    categorias: List<Categoria>,
    periodoFacturacionActual: String
) {
    // Estado interno para las transacciones editables
    val transaccionesState = remember {
        mutableStateListOf<EditableTransaction>().apply {
            addAll(transaccionesExtraidas.map { tx ->
                val suggestedCat = categorias.find { 
                    it.nombre.equals(tx.suggestedCategoryName, ignoreCase = true) 
                }
                EditableTransaction(
                    date = tx.date,
                    description = tx.description,
                    amount = tx.amount.toInt().toString(),
                    category = suggestedCat,
                    cardType = tx.cardType,
                    selected = true
                )
            })
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Cabecera
                Text(
                    text = "Confirmar Transacciones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Revisa y edita los gastos extraídos de la captura antes de ingresarlos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Lista de transacciones
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(transaccionesState) { index, item ->
                        TransactionEditItem(
                            item = item,
                            categorias = categorias,
                            onItemChanged = { updated ->
                                transaccionesState[index] = updated
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val selectedCount = transaccionesState.count { it.selected }
                    Button(
                        onClick = {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val entities = transaccionesState.filter { it.selected }.map { tx ->
                                val dateObj = try {
                                    sdf.parse(tx.date) ?: Date()
                                } catch (e: Exception) {
                                    Date()
                                }
                                MovimientoEntity(
                                    descripcion = tx.description,
                                    monto = tx.amount.toDoubleOrNull() ?: 0.0,
                                    tipo = "GASTO",
                                    categoriaId = tx.category?.id,
                                    fecha = dateObj,
                                    periodoFacturacion = periodoFacturacionActual,
                                    tipoTarjeta = tx.cardType,
                                    idUnico = UUID.randomUUID().toString()
                                )
                            }
                            onConfirm(entities)
                        },
                        enabled = selectedCount > 0 && transaccionesState.filter { it.selected }.all { 
                            it.description.isNotBlank() && (it.amount.toDoubleOrNull() ?: 0.0) > 0.0 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Importar ($selectedCount)")
                    }
                }
            }
        }
    }
}

data class EditableTransaction(
    val date: String,
    val description: String,
    val amount: String,
    val category: Categoria?,
    val cardType: String?,
    val selected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditItem(
    item: EditableTransaction,
    categorias: List<Categoria>,
    onItemChanged: (EditableTransaction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.selected) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            1.dp,
            if (item.selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox de selección
            Checkbox(
                checked = item.selected,
                onCheckedChange = { isChecked ->
                    onItemChanged(item.copy(selected = isChecked))
                },
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Fecha y Monto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = item.date,
                        onValueChange = { onItemChanged(item.copy(date = it)) },
                        label = { Text("Fecha (AAAA-MM-DD)") },
                        singleLine = true,
                        enabled = item.selected,
                        modifier = Modifier.weight(1.1f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    OutlinedTextField(
                        value = item.amount,
                        onValueChange = { input ->
                            val cleaned = input.replace("[^\\d]".toRegex(), "")
                            onItemChanged(item.copy(amount = cleaned))
                        },
                        label = { Text("Monto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = item.selected,
                        modifier = Modifier.weight(0.9f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Descripción
                OutlinedTextField(
                    value = item.description,
                    onValueChange = { onItemChanged(item.copy(description = it)) },
                    label = { Text("Descripción") },
                    singleLine = true,
                    enabled = item.selected,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Selector de Categoría
                var expandedCategoria by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedCategoria && item.selected,
                    onExpandedChange = { 
                        if (item.selected) expandedCategoria = !expandedCategoria 
                    }
                ) {
                    OutlinedTextField(
                        value = item.category?.nombre ?: "Sin categoría",
                        onValueChange = {},
                        label = { Text("Categoría") },
                        readOnly = true,
                        enabled = item.selected,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = { 
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria && item.selected) 
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoria && item.selected,
                        onDismissRequest = { expandedCategoria = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin categoría") },
                            onClick = {
                                onItemChanged(item.copy(category = null))
                                expandedCategoria = false
                            }
                        )
                        categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria.nombre) },
                                onClick = {
                                    onItemChanged(item.copy(category = categoria))
                                    expandedCategoria = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
