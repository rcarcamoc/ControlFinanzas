package com.aranthalion.controlfinanzas.presentation.movimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.movimiento.GestionarMovimientosManualesUseCase
import com.aranthalion.controlfinanzas.domain.movimiento.MovimientoManual
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class MovimientosManualesViewModel @Inject constructor(
    private val gestionarMovimientosManualesUseCase: GestionarMovimientosManualesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MovimientosManualesUiState>(MovimientosManualesUiState.Loading)
    val uiState: StateFlow<MovimientosManualesUiState> = _uiState.asStateFlow()

    private val _filtroTipo = MutableStateFlow<TipoMovimiento?>(null)
    val filtroTipo: StateFlow<TipoMovimiento?> = _filtroTipo.asStateFlow()

    init {
        cargarMovimientos()
    }

    fun cargarMovimientos() {
        viewModelScope.launch {
            try {
                _uiState.value = MovimientosManualesUiState.Loading
                val movimientos = gestionarMovimientosManualesUseCase.getAllMovimientos()
                movimientos.collect { listaMovimientos ->
                    _uiState.value = MovimientosManualesUiState.Success(listaMovimientos)
                }
            } catch (e: Exception) {
                _uiState.value = MovimientosManualesUiState.Error(e.message ?: "Error al cargar movimientos")
            }
        }
    }

    fun agregarMovimiento(movimiento: MovimientoManual) {
        viewModelScope.launch {
            try {
                gestionarMovimientosManualesUseCase.insertMovimiento(movimiento)
                cargarMovimientos()
            } catch (e: Exception) {
                _uiState.value = MovimientosManualesUiState.Error(e.message ?: "Error al agregar movimiento")
            }
        }
    }

    fun actualizarMovimiento(movimiento: MovimientoManual) {
        viewModelScope.launch {
            try {
                gestionarMovimientosManualesUseCase.updateMovimiento(movimiento)
                cargarMovimientos()
            } catch (e: Exception) {
                _uiState.value = MovimientosManualesUiState.Error(e.message ?: "Error al actualizar movimiento")
            }
        }
    }

    fun eliminarMovimiento(movimiento: MovimientoManual) {
        viewModelScope.launch {
            try {
                gestionarMovimientosManualesUseCase.deleteMovimiento(movimiento)
                cargarMovimientos()
            } catch (e: Exception) {
                _uiState.value = MovimientosManualesUiState.Error(e.message ?: "Error al eliminar movimiento")
            }
        }
    }

    fun filtrarPorTipo(tipo: TipoMovimiento?) {
        _filtroTipo.value = tipo
        viewModelScope.launch {
            try {
                _uiState.value = MovimientosManualesUiState.Loading
                val movimientos = if (tipo != null) {
                    gestionarMovimientosManualesUseCase.getMovimientosByTipo(tipo)
                } else {
                    gestionarMovimientosManualesUseCase.getAllMovimientos()
                }
                movimientos.collect { listaMovimientos ->
                    _uiState.value = MovimientosManualesUiState.Success(listaMovimientos)
                }
            } catch (e: Exception) {
                _uiState.value = MovimientosManualesUiState.Error(e.message ?: "Error al filtrar movimientos")
            }
        }
    }
}

sealed class MovimientosManualesUiState {
    object Loading : MovimientosManualesUiState()
    data class Success(val movimientos: List<MovimientoManual>) : MovimientosManualesUiState()
    data class Error(val mensaje: String) : MovimientosManualesUiState()
} 