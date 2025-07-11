package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.usecase.GestionarCuentasPorCobrarUseCase
import com.aranthalion.controlfinanzas.domain.cuenta.CuentaPorCobrar
import com.aranthalion.controlfinanzas.domain.cuenta.EstadoCuenta
import com.aranthalion.controlfinanzas.domain.usuario.Usuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CuentasPorCobrarViewModel @Inject constructor(
    private val gestionarCuentasPorCobrarUseCase: GestionarCuentasPorCobrarUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CuentasPorCobrarUiState>(CuentasPorCobrarUiState.Loading)
    val uiState: StateFlow<CuentasPorCobrarUiState> = _uiState.asStateFlow()

    private val _cuentas = MutableStateFlow<List<CuentaPorCobrar>>(emptyList())
    val cuentas: StateFlow<List<CuentaPorCobrar>> = _cuentas.asStateFlow()

    private val _usuarios = MutableStateFlow<List<Usuario>>(emptyList())
    val usuarios: StateFlow<List<Usuario>> = _usuarios.asStateFlow()

    private val _totalPendiente = MutableStateFlow(0.0)
    val totalPendiente: StateFlow<Double> = _totalPendiente.asStateFlow()

    private val _cantidadPendientes = MutableStateFlow(0)
    val cantidadPendientes: StateFlow<Int> = _cantidadPendientes.asStateFlow()

    init {
        cargarDatos()
    }

    private fun cargarDatos() {
        viewModelScope.launch {
            try {
                _uiState.value = CuentasPorCobrarUiState.Loading
                
                // Cargar cuentas pendientes
                val cuentas = gestionarCuentasPorCobrarUseCase.obtenerCuentasPendientes()
                _cuentas.value = cuentas
                
                // Cargar usuarios disponibles
                val usuarios = gestionarCuentasPorCobrarUseCase.obtenerUsuariosDisponibles()
                _usuarios.value = usuarios
                
                // Calcular totales
                val totalPendiente = gestionarCuentasPorCobrarUseCase.obtenerTotalPendiente() ?: 0.0
                _totalPendiente.value = totalPendiente
                
                val cantidadPendientes = gestionarCuentasPorCobrarUseCase.obtenerCantidadCuentasPendientes()
                _cantidadPendientes.value = cantidadPendientes
                
                _uiState.value = CuentasPorCobrarUiState.Success
            } catch (e: Exception) {
                _uiState.value = CuentasPorCobrarUiState.Error(e.message ?: "Error al cargar datos")
            }
        }
    }

    fun agregarCuenta(cuenta: CuentaPorCobrar) {
        viewModelScope.launch {
            try {
                _uiState.value = CuentasPorCobrarUiState.Loading
                
                // Validar cuenta
                val errores = gestionarCuentasPorCobrarUseCase.validarCuenta(cuenta)
                if (errores.isNotEmpty()) {
                    _uiState.value = CuentasPorCobrarUiState.Error(errores.joinToString("\n"))
                    return@launch
                }
                
                gestionarCuentasPorCobrarUseCase.insertarCuenta(cuenta)
                cargarDatos()
                
            } catch (e: Exception) {
                _uiState.value = CuentasPorCobrarUiState.Error(e.message ?: "Error al agregar cuenta")
            }
        }
    }

    fun actualizarCuenta(cuenta: CuentaPorCobrar) {
        viewModelScope.launch {
            try {
                _uiState.value = CuentasPorCobrarUiState.Loading
                
                // Validar cuenta
                val errores = gestionarCuentasPorCobrarUseCase.validarCuenta(cuenta)
                if (errores.isNotEmpty()) {
                    _uiState.value = CuentasPorCobrarUiState.Error(errores.joinToString("\n"))
                    return@launch
                }
                
                gestionarCuentasPorCobrarUseCase.actualizarCuenta(cuenta)
                cargarDatos()
                
            } catch (e: Exception) {
                _uiState.value = CuentasPorCobrarUiState.Error(e.message ?: "Error al actualizar cuenta")
            }
        }
    }

    fun eliminarCuenta(cuenta: CuentaPorCobrar) {
        viewModelScope.launch {
            try {
                _uiState.value = CuentasPorCobrarUiState.Loading
                gestionarCuentasPorCobrarUseCase.eliminarCuenta(cuenta)
                cargarDatos()
            } catch (e: Exception) {
                _uiState.value = CuentasPorCobrarUiState.Error(e.message ?: "Error al eliminar cuenta")
            }
        }
    }

    fun cambiarEstadoCuenta(id: Long, estado: EstadoCuenta) {
        viewModelScope.launch {
            try {
                _uiState.value = CuentasPorCobrarUiState.Loading
                gestionarCuentasPorCobrarUseCase.cambiarEstadoCuenta(id, estado)
                cargarDatos()
            } catch (e: Exception) {
                _uiState.value = CuentasPorCobrarUiState.Error(e.message ?: "Error al cambiar estado de la cuenta")
            }
        }
    }

    fun buscarCuentas(motivo: String) {
        viewModelScope.launch {
            try {
                _uiState.value = CuentasPorCobrarUiState.Loading
                val cuentas = if (motivo.isBlank()) {
                    gestionarCuentasPorCobrarUseCase.obtenerCuentasPendientes()
                } else {
                    gestionarCuentasPorCobrarUseCase.buscarCuentasPorMotivo(motivo)
                }
                _cuentas.value = cuentas
                _uiState.value = CuentasPorCobrarUiState.Success
            } catch (e: Exception) {
                _uiState.value = CuentasPorCobrarUiState.Error(e.message ?: "Error al buscar cuentas")
            }
        }
    }

    fun obtenerCuentasPorUsuario(usuarioId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = CuentasPorCobrarUiState.Loading
                val cuentas = gestionarCuentasPorCobrarUseCase.obtenerCuentasPorUsuario(usuarioId)
                _cuentas.value = cuentas
                _uiState.value = CuentasPorCobrarUiState.Success
            } catch (e: Exception) {
                _uiState.value = CuentasPorCobrarUiState.Error(e.message ?: "Error al cargar cuentas del usuario")
            }
        }
    }

    fun recargarDatos() {
        cargarDatos()
    }
}

sealed class CuentasPorCobrarUiState {
    object Loading : CuentasPorCobrarUiState()
    object Success : CuentasPorCobrarUiState()
    data class Error(val mensaje: String) : CuentasPorCobrarUiState()
} 