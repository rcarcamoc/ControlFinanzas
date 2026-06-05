package com.aranthalion.controlfinanzas.presentation.screens.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun FirstRunLoadingDialog(
    show: Boolean
) {
    if (show) {
        Log.i("LOG_PRIMER_USO_UI", "[6] Renderizando loading dialog")
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text("Cargando datos...")
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = "Configurando tu aplicación con datos de ejemplo...",
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = { }
        )
    }
}

@Composable
fun FirstRunCategoriasDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onCargarCategorias: () -> Unit,
    onComenzarSinCategorias: () -> Unit
) {
    if (show) {
        Log.i("LOG_PRIMER_USO_UI", "[7] Renderizando diálogo de categorías por defecto")
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text("¿Cargar categorías por defecto?")
            },
            text = {
                Text(
                    "¿Deseas cargar categorías predefinidas como 'Arriendo', 'Supermercado', 'Bencina', etc.? " +
                    "Esto te ayudará a comenzar más rápido, pero siempre podrás crear tus propias categorías después."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onCargarCategorias
                ) {
                    Text("Sí, cargar categorías")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onComenzarSinCategorias
                ) {
                    Text("No, comenzar sin categorías")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstRunPeriodoDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    periodosDisponibles: List<String>,
    periodoInicial: String,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    if (show) {
        Log.i("LOG_PRIMER_USO_UI", "[10] Renderizando diálogo de selección de periodo")
        var periodoSeleccionado by remember { mutableStateOf(periodoInicial) }
        
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("¿Desde qué periodo deseas insertar los presupuestos?") },
            text = {
                Column {
                    Text("Selecciona el periodo inicial. Se insertarán presupuestos desde ese mes hasta el actual (y 2 meses después).")
                    Spacer(modifier = Modifier.height(16.dp))
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = periodoSeleccionado,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Periodo inicial") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            periodosDisponibles.forEach { periodo ->
                                DropdownMenuItem(
                                    text = { Text(periodo) },
                                    onClick = {
                                        Log.i("LOG_PRIMER_USO_UI", "[11] Periodo seleccionado: $periodo")
                                        periodoSeleccionado = periodo
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmar(periodoSeleccionado)
                    }
                ) {
                    Text("Insertar presupuestos")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onCancelar,
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun FirstRunErrorDialog(
    mensajeError: String?,
    onAceptar: () -> Unit
) {
    if (mensajeError != null) {
        Log.i("LOG_PRIMER_USO_UI", "[14] Renderizando diálogo de error")
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text("Error")
            },
            text = {
                Text(mensajeError)
            },
            confirmButton = {
                TextButton(
                    onClick = onAceptar
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}
