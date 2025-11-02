package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.presentation.screens.composables.AddEditMovimientoDialog
import com.aranthalion.controlfinanzas.presentation.screens.composables.FiltroDialog
import com.aranthalion.controlfinanzas.presentation.screens.state.rememberTransaccionesScreenState
import com.aranthalion.controlfinanzas.ui.theme.ControlFinanzasTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesScreen(viewModel: TransaccionesViewModel = hiltViewModel()) {
    val state = rememberTransaccionesScreenState()
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transacciones") },
                actions = {
                    IconButton(onClick = { /* TODO: Implement search visibility toggle */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                    IconButton(onClick = { state.mostrarFiltroDialog.value = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { state.mostrarAddDialog.value = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir transacción")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.onEvent(TransaccionesEvent.SearchMovimientos(it))
                },
                label = { Text("Buscar") },
                modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when (val currentState = uiState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Success -> {
                        if (state.mostrarAddDialog.value) {
                            AddEditMovimientoDialog(
                                onDismiss = { state.mostrarAddDialog.value = false },
                                onConfirm = { descripcion, monto, tipo, categoria ->
                                    viewModel.onEvent(TransaccionesEvent.AddMovimiento(descripcion, monto, tipo, categoria))
                                    state.mostrarAddDialog.value = false
                                },
                                categorias = currentState.categorias
                            )
                        }

                        if (state.mostrarFiltroDialog.value) {
                            FiltroDialog(
                                onDismiss = { state.mostrarFiltroDialog.value = false },
                                onConfirm = { tipo, categoria ->
                                    viewModel.onEvent(TransaccionesEvent.FilterMovimientos(tipo, categoria))
                                    state.mostrarFiltroDialog.value = false
                                },
                                categorias = currentState.categorias
                            )
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(currentState.movimientos, key = { it.id }) { movimiento ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.onEvent(TransaccionesEvent.DeleteMovimiento(movimiento.id))
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color = when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                                            else -> Color.Transparent
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(color)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                ) {
                                    MovimientoItem(movimiento = movimiento)
                                }
                            }
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = currentState.message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovimientoItem(movimiento: MovimientoEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = movimiento.descripcion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(movimiento.fecha), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = if (movimiento.tipo == "INGRESO") "+${movimiento.monto}" else "-${movimiento.monto}",
                color = if (movimiento.tipo == "INGRESO") Color.Green else Color.Red,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransaccionesScreenPreview() {
    ControlFinanzasTheme {
        TransaccionesScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun MovimientoItemPreview() {
    ControlFinanzasTheme {
        MovimientoItem(movimiento = MovimientoEntity(
            id = 1,
            tipo = "GASTO",
            monto = 150.0,
            descripcion = "Café y medialunas",
            fecha = Date(),
            periodoFacturacion = "2024-05",
            idUnico = "123"
        ))
    }
}
