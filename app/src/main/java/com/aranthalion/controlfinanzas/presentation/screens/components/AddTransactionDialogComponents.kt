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
import com.aranthalion.controlfinanzas.data.util.ExcelProcessor
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import androidx.compose.foundation.text.KeyboardOptions
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarTransaccionDialog(
    mostrar: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, Long?) -> Unit,
    categoriasUiState: CategoriasUiState,
    periodoGlobal: String
) {
    if (!mostrar) return
    
    var monto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(null) }
    var tipoSeleccionado by remember { mutableStateOf("GASTO") }
    var expandedPeriodo by remember { mutableStateOf(false) }
    var periodoSeleccionado by remember { mutableStateOf(periodoGlobal) }
    
    val periodos = generarPeriodos()
    val categorias = obtenerCategorias(categoriasUiState)
    
    AnimatedVisibility(
        visible = mostrar,
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
                    text = "Nueva Transacción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
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
                        if (isValidAmount && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()) {
                            onConfirm(
                                tipoSeleccionado,
                                montoDouble,
                                descripcion,
                                periodoSeleccionado,
                                categoriaSeleccionada?.id
                            )
                        }
                    },
                    enabled = {
                        val isValidAmount = when (tipoSeleccionado) {
                            "GASTO" -> FormatUtils.isValidAmountForGastos(monto)
                            "INGRESO" -> FormatUtils.isValidAmountForIngresos(monto)
                            "OMITIR" -> true
                            else -> false
                        }
                        isValidAmount && descripcion.isNotBlank() && periodoSeleccionado.isNotBlank()
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
private fun TipoTransaccionSelector(
    tipoSeleccionado: String,
    onTipoChanged: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tipo de transacción",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
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
        
        // Opción Omitir
        TipoTransaccionButton(
            text = "Omitir (no afecta cálculos)",
            isSelected = tipoSeleccionado == "OMITIR",
            onClick = { onTipoChanged("OMITIR") },
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            onContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.fillMaxWidth()
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
            .clickable(onClick = onClick)
            .padding(12.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) containerColor else MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) onContainerColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
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
            }
        }
        else -> emptyList()
    }
}