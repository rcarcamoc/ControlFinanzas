package com.aranthalion.controlfinanzas.presentation.screens.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import java.util.Date

/**
 * Estado persistente de la pantalla TransaccionesScreen.
 * Sobrevive a cambios de configuración gracias a rememberSaveable.
 */
data class TransaccionesScreenState(
    val mostrarAddDialog: androidx.compose.runtime.MutableState<Boolean>,
    val mostrarFiltroDialog: androidx.compose.runtime.MutableState<Boolean>,
    val filtroTipoSeleccionado: androidx.compose.runtime.MutableState<String>,
    val filtroCategoriaSeleccionada: androidx.compose.runtime.MutableState<Categoria?>,
    val filtroFechaSeleccionada: androidx.compose.runtime.MutableState<Date?>,
    val busquedaTexto: androidx.compose.runtime.MutableState<String>,
    val movimientoAEditar: androidx.compose.runtime.MutableState<MovimientoEntity?>,
)

@Composable
fun rememberTransaccionesScreenState(): TransaccionesScreenState {
    val mostrarAddDialog = rememberSaveable { mutableStateOf(false) }
    val mostrarFiltroDialog = rememberSaveable { mutableStateOf(false) }
    val filtroTipoSeleccionado = rememberSaveable { mutableStateOf("Todos") }
    val filtroCategoriaSeleccionada = remember { mutableStateOf<Categoria?>(null) }
    val filtroFechaSeleccionada = remember { mutableStateOf<Date?>(null) }
    val busquedaTexto = rememberSaveable { mutableStateOf("") }
    val movimientoAEditar = remember { mutableStateOf<MovimientoEntity?>(null) }
    
    return TransaccionesScreenState(
        mostrarAddDialog = mostrarAddDialog,
        mostrarFiltroDialog = mostrarFiltroDialog,
        filtroTipoSeleccionado = filtroTipoSeleccionado,
        filtroCategoriaSeleccionada = filtroCategoriaSeleccionada,
        filtroFechaSeleccionada = filtroFechaSeleccionada,
        busquedaTexto = busquedaTexto,
        movimientoAEditar = movimientoAEditar
    )
}
