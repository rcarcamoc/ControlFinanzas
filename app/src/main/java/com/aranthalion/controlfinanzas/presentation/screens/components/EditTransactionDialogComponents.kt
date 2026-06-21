@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasUiState
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun EditarMovimientoDialog(
    movimiento: MovimientoEntity,
    categoriasUiState: CategoriasUiState,
    usuarios: List<com.aranthalion.controlfinanzas.data.local.entity.UsuarioEntity>,
    onDismiss: () -> Unit,
    onConfirm: (MovimientoEntity) -> Unit
) {
    var monto by remember { mutableStateOf(movimiento.monto.toLong().toString()) }
    var descripcion by remember { mutableStateOf(movimiento.descripcion) }
    var categoriaSeleccionada by remember {
        mutableStateOf<Categoria?>(obtenerCategorias(categoriasUiState).find { it.id == movimiento.categoriaId })
    }
    var tipoSeleccionado by remember { mutableStateOf(movimiento.tipo) }
    var expandedPeriodo by remember { mutableStateOf(false) }
    var periodoSeleccionado by remember { mutableStateOf(movimiento.periodoFacturacion) }
    var fechaSeleccionada by remember { mutableStateOf(movimiento.fecha) }
    var showDatePicker by remember { mutableStateOf(false) }
    var tarjeta by remember { mutableStateOf(movimiento.tipoTarjeta ?: "") }
    
    var scope by remember { mutableStateOf(movimiento.scope) }
    var selectedUsuario by remember {
        mutableStateOf<com.aranthalion.controlfinanzas.data.local.entity.UsuarioEntity?>(
            usuarios.find { it.idServidor == movimiento.userId_internal }
        )
    }

    var horaStr by remember {
        val cal = Calendar.getInstance().apply { time = movimiento.fecha }
        mutableStateOf(String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
    }
    
    val periodos = generarPeriodos()
    val categorias = obtenerCategorias(categoriasUiState)
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(200, easing = FastOutLinearInEasing)
        )
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Editar Transacción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Selector de tipo
                    TipoTransaccionSelector(
                        tipoSeleccionado = tipoSeleccionado,
                        onTipoChanged = { tipoSeleccionado = it }
                    )
                    
                    // Campo de monto
                    OutlinedTextField(
                        value = monto,
                        onValueChange = { input ->
                            if (input.isEmpty() || input == "-" || input.matches(Regex("^-?\\d*$"))) {
                                monto = input
                            }
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
                    
                    if (tipoSeleccionado == "GASTO") {
                        Text(
                            text = "💡 Para reversas, ingresa negativo (ej: -50000)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Campo de descripción
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    // Selector de fecha
                    FechaSelector(
                        fechaSeleccionada = fechaSeleccionada,
                        showDatePicker = showDatePicker,
                        onShowDatePickerChanged = { showDatePicker = it },
                        onFechaChanged = { fechaSeleccionada = it }
                    )
                    
                    // Campo de Hora
                    OutlinedTextField(
                        value = horaStr,
                        onValueChange = { input ->
                            if (input.length <= 5) {
                                horaStr = input
                            }
                        },
                        label = { Text("Hora (HH:mm)") },
                        placeholder = { Text("HH:mm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Campo de Tarjeta de Crédito
                    OutlinedTextField(
                        value = tarjeta,
                        onValueChange = { tarjeta = it },
                        label = { Text("Número tarjeta crédito") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Selector de período
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
                    
                    // Selector de categoría
                    if (categorias.isNotEmpty()) {
                        CategoriaSelector(
                            categoriaSeleccionada = categoriaSeleccionada,
                            categorias = categorias,
                            onCategoriaChanged = { categoriaSeleccionada = it }
                        )
                    }

                    // Selector de imputación
                    ScopeSelector(
                        scope = scope,
                        selectedUsuario = selectedUsuario,
                        usuarios = usuarios,
                        onScopeChanged = { newScope, newUsr ->
                            scope = newScope
                            selectedUsuario = newUsr
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val montoDouble = monto.toDoubleOrNull() ?: 0.0
                        val isValidAmount = when (tipoSeleccionado) {
                            "GASTO" -> FormatUtils.isValidAmountForGastos(monto)
                            "INGRESO" -> FormatUtils.isValidAmountForIngresos(monto)
                            "OMITIR" -> true
                            else -> false
                        }
                        val isValidHora = horaStr.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))
                        if (isValidAmount && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank() && isValidHora) {
                            val finalDate = try {
                                val parts = horaStr.split(":")
                                val hr = parts.getOrNull(0)?.toIntOrNull() ?: 0
                                val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                val cal = Calendar.getInstance().apply {
                                    time = fechaSeleccionada
                                    set(Calendar.HOUR_OF_DAY, hr)
                                    set(Calendar.MINUTE, min)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                cal.time
                            } catch (e: Exception) {
                                fechaSeleccionada
                            }

                            val movimientoEditado = movimiento.copy(
                                tipo = tipoSeleccionado,
                                monto = montoDouble,
                                descripcion = descripcion,
                                fecha = finalDate,
                                periodoFacturacion = periodoSeleccionado,
                                categoriaId = categoriaSeleccionada?.id,
                                tipoTarjeta = tarjeta.ifEmpty { null },
                                scope = scope,
                                userId_internal = selectedUsuario?.idServidor
                            )
                            onConfirm(movimientoEditado)
                        }
                    },
                    enabled = {
                        val isValidAmount = when (tipoSeleccionado) {
                            "GASTO" -> FormatUtils.isValidAmountForGastos(monto)
                            "INGRESO" -> FormatUtils.isValidAmountForIngresos(monto)
                            "OMITIR" -> true
                            else -> false
                        }
                        val isValidHora = horaStr.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))
                        isValidAmount && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank() && isValidHora
                    }(),
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
private fun TipoTransaccionSelector(
    tipoSeleccionado: String,
    onTipoChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val tipoLabel = when (tipoSeleccionado) {
        "GASTO" -> "Gasto"
        "INGRESO" -> "Ingreso"
        "OMITIR" -> "Omitir (no afecta cálculos)"
        else -> tipoSeleccionado
    }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = tipoLabel,
            onValueChange = {},
            label = { Text("Tipo de transacción") },
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Gasto") },
                onClick = {
                    onTipoChanged("GASTO")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Ingreso") },
                onClick = {
                    onTipoChanged("INGRESO")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Omitir (no afecta cálculos)") },
                onClick = {
                    onTipoChanged("OMITIR")
                    expanded = false
                }
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
    calendar.add(Calendar.MONTH, 2)
    return (0..12).map { offset ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.MONTH, -offset)
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        "$year-$month"
    }
}

private fun obtenerCategorias(categoriasUiState: CategoriasUiState): List<Categoria> {
    return when (categoriasUiState) {
        is CategoriasUiState.Success -> {
            val domainCategorias = (categoriasUiState as CategoriasUiState.Success).categorias
            domainCategorias.map { domainCategoria ->
                Categoria(
                    id = domainCategoria.id.toLong(),
                    nombre = domainCategoria.nombre,
                    descripcion = domainCategoria.descripcion,
                    tipo = "GASTO"
                )
            }.sortedBy { it.nombre.lowercase() }
        }
        else -> emptyList()
    }
}

@Composable
private fun ScopeSelector(
    scope: String,
    selectedUsuario: com.aranthalion.controlfinanzas.data.local.entity.UsuarioEntity?,
    usuarios: List<com.aranthalion.controlfinanzas.data.local.entity.UsuarioEntity>,
    onScopeChanged: (String, com.aranthalion.controlfinanzas.data.local.entity.UsuarioEntity?) -> Unit
) {
    var expandedScope by remember { mutableStateOf(false) }
    val displayText = when {
        scope == "HOUSEHOLD" -> "🏠 Compartido (Grupo Familiar)"
        selectedUsuario != null -> "👤 Personal - ${selectedUsuario.nombre}"
        else -> "👤 Personal (Sin asignar)"
    }
    
    ExposedDropdownMenuBox(
        expanded = expandedScope,
        onExpandedChange = { expandedScope = !expandedScope }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            label = { Text("Imputación") },
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedScope) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        ExposedDropdownMenu(
            expanded = expandedScope,
            onDismissRequest = { expandedScope = false }
        ) {
            DropdownMenuItem(
                text = { Text("🏠 Compartido (Grupo Familiar)") },
                onClick = {
                    onScopeChanged("HOUSEHOLD", null)
                    expandedScope = false
                }
            )
            DropdownMenuItem(
                text = { Text("👤 Gastos Personales") },
                onClick = {
                    onScopeChanged("PERSONAL", null)
                    expandedScope = false
                }
            )
            usuarios.forEach { usr ->
                DropdownMenuItem(
                    text = { Text("👤 Personal - ${usr.nombre}") },
                    onClick = {
                        onScopeChanged("PERSONAL", usr)
                        expandedScope = false
                    }
                )
            }
        }
    }
}