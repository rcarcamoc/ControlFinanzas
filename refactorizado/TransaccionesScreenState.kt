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
 */\ndata class TransaccionesScreenState(\n    val mostrarAddDialog: androidx.compose.runtime.MutableState<Boolean>,\n    val mostrarFiltroDialog: androidx.compose.runtime.MutableState<Boolean>,\n    val filtroTipoSeleccionado: androidx.compose.runtime.MutableState<String>,\n    val filtroCategoriaSeleccionada: androidx.compose.runtime.MutableState<Categoria?>,\n    val filtroFechaSeleccionada: androidx.compose.runtime.MutableState<Date?>,\n    val busquedaTexto: androidx.compose.runtime.MutableState<String>,\n    val movimientoAEditar: androidx.compose.runtime.MutableState<MovimientoEntity?>,\n)\n\n@Composable\nfun rememberTransaccionesScreenState(): TransaccionesScreenState {\n    val mostrarAddDialog = rememberSaveable { mutableStateOf(false) }\n    val mostrarFiltroDialog = rememberSaveable { mutableStateOf(false) }\n    val filtroTipoSeleccionado = rememberSaveable { mutableStateOf(\"Todos\") }\n    val filtroCategoriaSeleccionada = remember { mutableStateOf<Categoria?>(null) }\n    val filtroFechaSeleccionada = remember { mutableStateOf<Date?>(null) }\n    val busquedaTexto = rememberSaveable { mutableStateOf(\"\") }\n    val movimientoAEditar = remember { mutableStateOf<MovimientoEntity?>(null) }\n    \n    return TransaccionesScreenState(\n        mostrarAddDialog = mostrarAddDialog,\n        mostrarFiltroDialog = mostrarFiltroDialog,\n        filtroTipoSeleccionado = filtroTipoSeleccionado,\n        filtroCategoriaSeleccionada = filtroCategoriaSeleccionada,\n        filtroFechaSeleccionada = filtroFechaSeleccionada,\n        busquedaTexto = busquedaTexto,\n        movimientoAEditar = movimientoAEditar\n    )\n}