package com.aranthalion.controlfinanzas.presentation.screens.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.domain.categoria.Categoria

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltroDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?, Categoria?) -> Unit,
    categorias: List<Categoria>
) {
    var tipo by remember { mutableStateOf<String?>(null) }
    var expandedTipo by remember { mutableStateOf(false) }
    var selectedCategoria by remember { mutableStateOf<Categoria?>(null) }
    var expandedCategoria by remember { mutableStateOf(false) }
    val sortedCategorias = remember(categorias) { categorias.sortedBy { it.nombre.lowercase() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Filtrar Movimientos") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    TextField(
                        value = tipo ?: "Todos",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos") },
                            onClick = {
                                tipo = null
                                expandedTipo = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("GASTO") },
                            onClick = {
                                tipo = "GASTO"
                                expandedTipo = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("INGRESO") },
                            onClick = {
                                tipo = "INGRESO"
                                expandedTipo = false
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedCategoria,
                    onExpandedChange = { expandedCategoria = !expandedCategoria }
                ) {
                    TextField(
                        value = selectedCategoria?.nombre ?: "Todas",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoria,
                        onDismissRequest = { expandedCategoria = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas") },
                            onClick = {
                                selectedCategoria = null
                                expandedCategoria = false
                            }
                        )
                        sortedCategorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria.nombre) },
                                onClick = {
                                    selectedCategoria = categoria
                                    expandedCategoria = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(tipo, selectedCategoria)
            }) {
                Text("Filtrar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
