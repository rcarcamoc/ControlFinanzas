package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun PeriodoSelectorDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    title: String = "Seleccionar Período",
    initialStartDate: LocalDate = LocalDate.now().minusMonths(1),
    initialEndDate: LocalDate = LocalDate.now()
) {
    if (isVisible) {
        var startDate by remember { mutableStateOf(initialStartDate) }
        var endDate by remember { mutableStateOf(initialEndDate) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Fecha de inicio:")
                    DatePicker(
                        selectedDate = startDate,
                        onDateSelected = { startDate = it }
                    )
                    
                    Text("Fecha de fin:")
                    DatePicker(
                        selectedDate = endDate,
                        onDateSelected = { endDate = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(startDate, endDate)
                        onDismiss()
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
                ),
                showModeToggle = false
            )
        }
    }
} 