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
import com.aranthalion.controlfinanzas.presentation.screens.MovimientosViewModel
import com.aranthalion.controlfinanzas.presentation.screens.state.TransaccionesScreenState
import com.aranthalion.controlfinanzas.presentation.screens.MovimientosUiState
import com.aranthalion.controlfinanzas.presentation.categoria.CategoriasUiState
import com.aranthalion.controlfinanzas.domain.categoria.Categoria as DomainCategoria
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.aranthalion.controlfinanzas.data.util.ExcelProcessor

@Composable
fun TransaccionesActionBar(
    uiState: MovimientosUiState,
    viewModel: MovimientosViewModel,
    navController: NavHostController,
    screenState: TransaccionesScreenState,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botón: Nueva transacción
        Button(
            onClick = { screenState.mostrarAddDialog.value = true },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar transacción",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Nueva")
        }
        
        // Botón: Clasificar con ML
        Button(
            onClick = {
                procesarTransaccionesSinClasificar(
                    uiState = uiState,
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                    scope = scope
                )
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Clasificar"
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clasificar")
        }
        
        // Botón: Eliminar duplicados
        Button(
            onClick = {
                eliminarTransaccionesDuplicadas(
                    uiState = uiState,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    scope = scope
                )
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar duplicados"
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Duplicados")
        }
    }
}

private fun procesarTransaccionesSinClasificar(
    uiState: MovimientosUiState,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    if (uiState !is MovimientosUiState.Success) {
        scope.launch {
            snackbarHostState.showSnackbar(
                "No se pudo obtener el estado de las transacciones"
            )
        }
        return
    }
    
    val movimientos = uiState.movimientos
    val transaccionesSinCategoria = movimientos.filter { it.categoriaId == null }
    
    if (transaccionesSinCategoria.isNotEmpty()) {
        scope.launch {
            snackbarHostState.showSnackbar(
                "Procesando ${transaccionesSinCategoria.size} transacciones sin clasificar"
            )
        }
        navController.navigate("tinder_clasificacion")
    } else {
        scope.launch {
            snackbarHostState.showSnackbar(
                "No hay transacciones pendientes de clasificación"
            )
        }
    }
}

private fun eliminarTransaccionesDuplicadas(
    uiState: MovimientosUiState,
    viewModel: MovimientosViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    if (uiState !is MovimientosUiState.Success) return
    
    val movimientos = uiState.movimientos
    val unicos = mutableSetOf<String>()
    val duplicados = mutableListOf<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>()
    
    for (mov in movimientos) {
        val idUnico = ExcelProcessor.generarIdUnico(mov.fecha, mov.monto, mov.descripcion)
        if (idUnico in unicos) {
            duplicados.add(mov)
        } else {
            unicos.add(idUnico)
        }
    }
    
    for (dup in duplicados) {
        viewModel.eliminarMovimiento(dup)
    }
    
    scope.launch {
        snackbarHostState.showSnackbar(
            "Se eliminaron ${duplicados.size} transacciones duplicadas"
        )
    }
}
