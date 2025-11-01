package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.presentation.screens.state.TransaccionesScreenState
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasUiState
import com.aranthalion.controlfinanzas.domain.categoria.Categoria as DomainCategoria
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun FiltroTransaccionesDialog(
    mostrar: Boolean,
    onDismiss: () -> Unit,
    screenState: TransaccionesScreenState,
    categoriasUiState: CategoriasUiState
) {
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
                    text = "Filtrar Transacciones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Filtro por tipo
                    FiltroTipoTransaccion(
                        tipoSeleccionado = screenState.filtroTipoSeleccionado.value,
                        onTipoChanged = { screenState.filtroTipoSeleccionado.value = it }
                    )
                    
                    Divider()
                    
                    // Filtro por categoría
                    FiltroCategoria(
                        categoriaSeleccionada = screenState.filtroCategoriaSeleccionada.value,
                        onCategoriaChanged = { screenState.filtroCategoriaSeleccionada.value = it },
                        categoriasUiState = categoriasUiState
                    )
                    
                    Divider()
                    
                    // Filtro por fecha
                    FiltroFecha(
                        fechaSeleccionada = screenState.filtroFechaSeleccionada.value
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        screenState.filtroTipoSeleccionado.value = "Todos"
                        screenState.filtroCategoriaSeleccionada.value = null
                        screenState.filtroFechaSeleccionada.value = null
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Limpiar")
                }
            }
        )
    }
}

@Composable
private fun FiltroTipoTransaccion(
    tipoSeleccionado: String,
    onTipoChanged: (String) -> Unit
) {
    val tipos = listOf("Todos", "Ingresos", "Gastos", "Omitir")
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tipo de transacción",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        tipos.forEach { tipo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTipoChanged(tipo) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = tipoSeleccionado == tipo,
                    onClick = { onTipoChanged(tipo) }
                )
                Text(
                    text = tipo,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FiltroCategoria(
    categoriaSeleccionada: DomainCategoria?,
    onCategoriaChanged: (DomainCategoria?) -> Unit,
    categoriasUiState: CategoriasUiState
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Categoría",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        when (categoriasUiState) {
            is CategoriasUiState.Success -> {
                val categorias = (categoriasUiState as CategoriasUiState.Success).categorias
                
                // Opción: Sin categoría
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoriaChanged(null) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = categoriaSeleccionada == null,
                        onClick = { onCategoriaChanged(null) }
                    )
                    Text(
                        text = "Todas",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                categorias.forEach { categoria ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoriaChanged(categoria) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = categoriaSeleccionada == categoria,
                            onClick = { onCategoriaChanged(categoria) }
                        )
                        Text(
                            text = categoria.nombre,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            else -> {
                Text(
                    text = "Categorías no disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FiltroFecha(fechaSeleccionada: java.util.Date?) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Fecha",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = fechaSeleccionada?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                } ?: "",
                onValueChange = { },
                label = { Text("Desde") },
                enabled = false,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            
            OutlinedTextField(
                value = fechaSeleccionada?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                } ?: "",
                onValueChange = { },
                label = { Text("Hasta") },
                enabled = false,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}
