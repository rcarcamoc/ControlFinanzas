package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.categoria.GestionarCategoriasUseCase
import com.aranthalion.controlfinanzas.domain.usecase.GestionarMovimientosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

sealed interface TransaccionesEvent {
    data class DeleteMovimiento(val id: Long) : TransaccionesEvent
    data class AddMovimiento(val descripcion: String, val monto: Double, val tipo: String, val categoria: Categoria?) : TransaccionesEvent
    data class FilterMovimientos(val tipo: String?, val categoria: Categoria?) : TransaccionesEvent
    data class SearchMovimientos(val query: String) : TransaccionesEvent
}

sealed interface UiState {
    object Loading : UiState
    data class Success(val movimientos: List<MovimientoEntity>, val categorias: List<Categoria>) : UiState
    data class Error(val message: String) : UiState
}

@HiltViewModel
class TransaccionesViewModel @Inject constructor(
    private val gestionarMovimientosUseCase: GestionarMovimientosUseCase,
    private val gestionarCategoriasUseCase: GestionarCategoriasUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentTipoFilter: String? = null
    private var currentCategoriaFilter: Categoria? = null
    private var currentSearchQuery: String = ""

    init {
        fetchData()
    }

    fun onEvent(event: TransaccionesEvent) {
        when (event) {
            is TransaccionesEvent.DeleteMovimiento -> {
                // Handle delete event
            }
            is TransaccionesEvent.AddMovimiento -> {
                addMovimiento(event.descripcion, event.monto, event.tipo, event.categoria)
            }
            is TransaccionesEvent.FilterMovimientos -> {
                currentTipoFilter = event.tipo
                currentCategoriaFilter = event.categoria
                fetchData()
            }
            is TransaccionesEvent.SearchMovimientos -> {
                currentSearchQuery = event.query
                fetchData()
            }
        }
    }

    private fun fetchData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val movimientos = gestionarMovimientosUseCase.obtenerMovimientos().filter { movimiento ->
                    val tipoMatch = currentTipoFilter?.let { it == movimiento.tipo } ?: true
                    val categoriaMatch = currentCategoriaFilter?.let { it.id == movimiento.categoriaId } ?: true
                    val searchMatch = if (currentSearchQuery.isBlank()) {
                        true
                    } else {
                        movimiento.descripcion.contains(currentSearchQuery, ignoreCase = true)
                    }
                    tipoMatch && categoriaMatch && searchMatch
                }
                gestionarCategoriasUseCase.getAllCategorias().collect { categorias ->
                    _uiState.value = UiState.Success(movimientos, categorias)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error al cargar datos")
            }
        }
    }

    private fun addMovimiento(descripcion: String, monto: Double, tipo: String, categoria: Categoria?) {
        viewModelScope.launch {
            val movimiento = MovimientoEntity(
                descripcion = descripcion,
                monto = monto,
                tipo = tipo,
                categoriaId = categoria?.id,
                fecha = Date(),
                periodoFacturacion = "2024-05", //TODO: Get current period
                idUnico = UUID.randomUUID().toString()
            )
            gestionarMovimientosUseCase.agregarMovimiento(movimiento)
            fetchData()
        }
    }
}
