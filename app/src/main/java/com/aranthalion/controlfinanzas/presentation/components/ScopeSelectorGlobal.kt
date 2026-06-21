package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScopeSelectorGlobal(
    modifier: Modifier = Modifier,
    label: String = "Filtro de Imputación"
) {
    val periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
    val scopeSeleccionado by periodoGlobalViewModel.scopeSeleccionado.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    val scopeText = when (scopeSeleccionado) {
        "HOUSEHOLD" -> "🏠 Compartido (Grupo Familiar)"
        "PERSONAL" -> "👤 Gastos Personales"
        else -> "🏠 Compartido (Grupo Familiar)"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = scopeText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("🏠 Compartido (Grupo Familiar)") },
                onClick = {
                    periodoGlobalViewModel.cambiarScope("HOUSEHOLD")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("👤 Gastos Personales") },
                onClick = {
                    periodoGlobalViewModel.cambiarScope("PERSONAL")
                    expanded = false
                }
            )
        }
    }
}
