package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.data.local.entity.SueldoEntity
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.domain.usecase.AporteProporcional
import com.aranthalion.controlfinanzas.domain.usecase.ResumenAporteProporcional
import com.aranthalion.controlfinanzas.presentation.components.HistorialAportesCharts

@Composable
fun SueldoItem(
    sueldo: SueldoEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sueldo.nombrePersona,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Período: ${sueldo.periodo}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Sueldo: ${FormatUtils.formatMoneyCLP(sueldo.sueldo)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showDeleteDialog,
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
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Sueldo") },
            text = { Text("¿Estás seguro de que quieres eliminar el sueldo de ${sueldo.nombrePersona} para el período ${sueldo.periodo}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AporteItem(aporte: AporteProporcional) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = aporte.nombrePersona,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Sueldo: ${FormatUtils.formatMoneyCLP(aporte.sueldo)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Porcentaje: ${String.format("%.1f", aporte.porcentajeAporte)}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Aporte:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = FormatUtils.formatMoneyCLP(aporte.montoAporte),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarSueldoDialog(
    sueldo: SueldoEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit,
    personasDisponibles: List<String>,
    periodosDisponibles: List<String>,
    periodoActual: String
) {
    var nombrePersona by remember { mutableStateOf(sueldo?.nombrePersona ?: "") }
    var sueldoValue by remember { mutableStateOf(sueldo?.sueldo?.toLong()?.toString() ?: "") }
    var showPersonaInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (sueldo == null) "Agregar Sueldo" else "Editar Sueldo") },
        text = {
            Column {
                // Selector de persona
                Text("Persona:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (personasDisponibles.isNotEmpty() && !showPersonaInput) {
                    personasDisponibles.forEach { persona ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = nombrePersona == persona,
                                onClick = { nombrePersona = persona }
                            )
                            Text(persona)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = showPersonaInput,
                            onClick = { showPersonaInput = true }
                        )
                        Text("Nueva persona")
                    }
                } else {
                    showPersonaInput = true
                }
                
                if (showPersonaInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nombrePersona,
                        onValueChange = { nombrePersona = it },
                        label = { Text("Nombre de la persona") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Período (usando período global)
                Text("Período:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = periodoActual,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Campo de sueldo
                OutlinedTextField(
                    value = sueldoValue,
                    onValueChange = {
                        val cleaned = it.replace("[^\\d]".toRegex(), "")
                        sueldoValue = cleaned
                    },
                    label = { Text("Sueldo") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sueldoDouble = sueldoValue.toDoubleOrNull() ?: 0.0
                    if (nombrePersona.isNotBlank() && periodoActual.isNotBlank() && sueldoDouble > 0) {
                        onConfirm(nombrePersona, periodoActual, sueldoDouble)
                    }
                },
                enabled = nombrePersona.isNotBlank() && periodoActual.isNotBlank() && sueldoValue.toDoubleOrNull() ?: 0.0 > 0
            ) {
                Text(if (sueldo == null) "Guardar" else "Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun HistorialAportesDialog(
    onDismiss: () -> Unit,
    onPeriodoSelected: (String) -> Unit
) {
    var historial by remember { mutableStateOf<List<ResumenAporteProporcional>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Cargar historial cuando se abre el diálogo
    LaunchedEffect(Unit) {
        isLoading = false
        historial = emptyList() // Aquí se cargarían los datos reales
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial de Aportes") },
        text = { 
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (historial.isEmpty()) {
                    Text("No hay datos históricos disponibles. Agrega sueldos en diferentes períodos para ver el historial.")
                } else {
                    HistorialAportesCharts(
                        historial = historial,
                        onPeriodoSelected = onPeriodoSelected
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        modifier = Modifier.fillMaxWidth(0.95f)
    )
}
