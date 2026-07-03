package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.presentation.configuracion.ConfiguracionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingPeriodConfigCard(
    viewModel: ConfiguracionViewModel
) {
    val periodosConfig by viewModel.periodoDatesMap.collectAsState()
    var mostrarAddEditDialog by remember { mutableStateOf(false) }
    var periodoAEditar by remember { mutableStateOf<String?>(null) }
    var fechaInicioAEditar by remember { mutableStateOf("") }
    var fechaFinAEditar by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Fechas de Períodos de Facturación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "Define las fechas de inicio y fin para cada período de facturación para clasificar automáticamente las transacciones.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (periodosConfig.isEmpty()) {
                Text(
                    text = "No hay fechas personalizadas definidas. Se usará el mes natural (del 1 al 30/31).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    periodosConfig.forEach { (periodo, config) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Período: $periodo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${config.startDateStr} al ${config.endDateStr}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        periodoAEditar = periodo
                                        fechaInicioAEditar = config.startDateStr
                                        fechaFinAEditar = config.endDateStr
                                        mostrarAddEditDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = { viewModel.eliminarPeriodoDate(periodo) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    periodoAEditar = ""
                    fechaInicioAEditar = ""
                    fechaFinAEditar = ""
                    mostrarAddEditDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Definir Rango de Período")
            }
        }
    }

    if (mostrarAddEditDialog) {
        var inputPeriodo by remember { mutableStateOf(periodoAEditar ?: "") }
        var inputFechaInicio by remember { mutableStateOf(fechaInicioAEditar) }
        var inputFechaFin by remember { mutableStateOf(fechaFinAEditar) }

        var showInicioDatePicker by remember { mutableStateOf(false) }
        var showFinDatePicker by remember { mutableStateOf(false) }

        val context = androidx.compose.ui.platform.LocalContext.current

        AlertDialog(
            onDismissRequest = { mostrarAddEditDialog = false },
            title = {
                Text(
                    text = if (periodoAEditar.isNullOrEmpty()) "Definir Rango de Período" else "Editar Rango de Período",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (periodoAEditar.isNullOrEmpty()) {
                        OutlinedTextField(
                            value = inputPeriodo,
                            onValueChange = { inputPeriodo = it },
                            label = { Text("Período (ej: 2025-06)") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("YYYY-MM") }
                        )
                    } else {
                        Text(
                            text = "Período: $inputPeriodo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Selector de Fecha Inicio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputFechaInicio,
                            onValueChange = { inputFechaInicio = it },
                            label = { Text("Fecha Inicio (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("yyyy-MM-dd") }
                        )
                        IconButton(onClick = { showInicioDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha inicio")
                        }
                    }

                    // Selector de Fecha Fin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputFechaFin,
                            onValueChange = { inputFechaFin = it },
                            label = { Text("Fecha Fin (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("yyyy-MM-dd") }
                        )
                        IconButton(onClick = { showFinDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha fin")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val regexPeriodo = Regex("^\\d{4}-\\d{2}$")
                        val regexFecha = Regex("^\\d{4}-\\d{2}-\\d{2}$")
                        if (!regexPeriodo.matches(inputPeriodo)) {
                            android.widget.Toast.makeText(context, "Formato de período inválido (debe ser YYYY-MM)", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (!regexFecha.matches(inputFechaInicio) || !regexFecha.matches(inputFechaFin)) {
                            android.widget.Toast.makeText(context, "Formato de fecha inválido (debe ser YYYY-MM-DD)", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        viewModel.guardarPeriodoDate(inputPeriodo, inputFechaInicio, inputFechaFin)
                        mostrarAddEditDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAddEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        
        if (showInicioDatePicker) {
            val initialDate = try { sdf.parse(inputFechaInicio) } catch(e: Exception) { java.util.Date() }
            val cal = java.util.Calendar.getInstance().apply { time = initialDate }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = cal.timeInMillis)
            DatePickerDialog(
                onDismissRequest = { showInicioDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            inputFechaInicio = sdf.format(java.util.Date(it))
                        }
                        showInicioDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showInicioDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showFinDatePicker) {
            val initialDate = try { sdf.parse(inputFechaFin) } catch(e: Exception) { java.util.Date() }
            val cal = java.util.Calendar.getInstance().apply { time = initialDate }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = cal.timeInMillis)
            DatePickerDialog(
                onDismissRequest = { showFinDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            inputFechaFin = sdf.format(java.util.Date(it))
                        }
                        showFinDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showFinDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
