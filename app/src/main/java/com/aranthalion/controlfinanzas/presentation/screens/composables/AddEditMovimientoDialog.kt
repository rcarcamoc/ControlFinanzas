@file:OptIn(ExperimentalMaterial3Api::class)

package com.aranthalion.controlfinanzas.presentation.screens.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddEditMovimientoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Categoria?, Date, String) -> Unit,
    categorias: List<Categoria>
) {
    var descripcion by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("GASTO") }
    var selectedCategoria by remember { mutableStateOf<Categoria?>(null) }
    var fechaSeleccionada by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val periodos = remember { generarPeriodos() }
    var expandedPeriodo by remember { mutableStateOf(false) }
    var periodoSeleccionado by remember {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        mutableStateOf(String.format("%04d-%02d", year, month))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Añadir Transacción",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Selector de tipo (Gasto / Ingreso)
                TipoTransaccionSelector(
                    tipoSeleccionado = tipo,
                    onTipoChanged = { tipo = it }
                )

                // Campo de Monto
                OutlinedTextField(
                    value = monto,
                    onValueChange = { input ->
                        val cleaned = input.replace("[^\\d]".toRegex(), "")
                        monto = cleaned
                    },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Campo de Descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Selector de Fecha
                FechaSelector(
                    fechaSeleccionada = fechaSeleccionada,
                    showDatePicker = showDatePicker,
                    onShowDatePickerChanged = { showDatePicker = it },
                    onFechaChanged = { fechaSeleccionada = it }
                )

                // Selector de Período
                PeriodoSelector(
                    periodoSeleccionado = periodoSeleccionado,
                    periodos = periodos,
                    expandedPeriodo = expandedPeriodo,
                    onExpandedChanged = { expandedPeriodo = it },
                    onPeriodoChanged = {
                        periodoSeleccionado = it
                        expandedPeriodo = false
                    }
                )

                // Selector de Categoría
                CategoriaSelector(
                    categoriaSeleccionada = selectedCategoria,
                    categorias = categorias,
                    onCategoriaChanged = { selectedCategoria = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val montoDouble = monto.toDoubleOrNull() ?: 0.0
                    if (descripcion.isNotBlank() && montoDouble > 0) {
                        onConfirm(descripcion, montoDouble, tipo, selectedCategoria, fechaSeleccionada, periodoSeleccionado)
                    }
                },
                enabled = descripcion.isNotBlank() && (monto.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun TipoTransaccionSelector(
    tipoSeleccionado: String,
    onTipoChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TipoTransaccionButton(
            text = "Gasto",
            isSelected = tipoSeleccionado == "GASTO",
            onClick = { onTipoChanged("GASTO") },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            onContainerColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        
        TipoTransaccionButton(
            text = "Ingreso",
            isSelected = tipoSeleccionado == "INGRESO",
            onClick = { onTipoChanged("INGRESO") },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TipoTransaccionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    onContainerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) containerColor else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) onContainerColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FechaSelector(
    fechaSeleccionada: Date,
    showDatePicker: Boolean,
    onShowDatePickerChanged: (Boolean) -> Unit,
    onFechaChanged: (Date) -> Unit
) {
    val formattedDate = remember(fechaSeleccionada) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fechaSeleccionada)
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = formattedDate,
            onValueChange = {},
            label = { Text("Fecha") },
            readOnly = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { onShowDatePickerChanged(true) }
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Seleccionar fecha",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        calendar.time = fechaSeleccionada
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = calendar.timeInMillis
        )
        
        DatePickerDialog(
            onDismissRequest = { onShowDatePickerChanged(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onFechaChanged(Date(it))
                        }
                        onShowDatePickerChanged(false)
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onShowDatePickerChanged(false) }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
}

@Composable
private fun PeriodoSelector(
    periodoSeleccionado: String,
    periodos: List<String>,
    expandedPeriodo: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    onPeriodoChanged: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expandedPeriodo,
        onExpandedChange = onExpandedChanged
    ) {
        OutlinedTextField(
            value = periodoSeleccionado,
            onValueChange = {},
            label = { Text("Período de Facturación") },
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriodo) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        ExposedDropdownMenu(
            expanded = expandedPeriodo,
            onDismissRequest = { onExpandedChanged(false) }
        ) {
            periodos.forEach { periodo ->
                DropdownMenuItem(
                    text = { Text(periodo) },
                    onClick = {
                        onPeriodoChanged(periodo)
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoriaSelector(
    categoriaSeleccionada: Categoria?,
    categorias: List<Categoria>,
    onCategoriaChanged: (Categoria?) -> Unit
) {
    var expandedCategoria by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expandedCategoria,
        onExpandedChange = { expandedCategoria = !expandedCategoria }
    ) {
        OutlinedTextField(
            value = categoriaSeleccionada?.nombre ?: "Sin categoría",
            onValueChange = {},
            label = { Text("Categoría (opcional)") },
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        ExposedDropdownMenu(
            expanded = expandedCategoria,
            onDismissRequest = { expandedCategoria = false }
        ) {
            DropdownMenuItem(
                text = { Text("Sin categoría") },
                onClick = {
                    onCategoriaChanged(null)
                    expandedCategoria = false
                }
            )
            categorias.forEach { categoria ->
                DropdownMenuItem(
                    text = { Text(categoria.nombre) },
                    onClick = {
                        onCategoriaChanged(categoria)
                        expandedCategoria = false
                    }
                )
            }
        }
    }
}

private fun generarPeriodos(): List<String> {
    val calendar = Calendar.getInstance()
    // Ofrecer períodos desde hace 12 meses hasta dentro de 2 meses
    calendar.add(Calendar.MONTH, 2)
    return (0..14).map { offset ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.MONTH, -offset)
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        "$year-$month"
    }
}
