package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.usecase.GestionarUsuariosUseCase
import com.aranthalion.controlfinanzas.domain.usuario.Usuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsuariosViewModel @Inject constructor(
    private val gestionarUsuariosUseCase: GestionarUsuariosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UsuariosUiState>(UsuariosUiState.Loading)
    val uiState: StateFlow<UsuariosUiState> = _uiState.asStateFlow()

    private val _usuarios = MutableStateFlow<List<Usuario>>(emptyList())
    val usuarios: StateFlow<List<Usuario>> = _usuarios.asStateFlow()

    private val _totalUsuarios = MutableStateFlow(0)
    val totalUsuarios: StateFlow<Int> = _totalUsuarios.asStateFlow()

    init {
        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        viewModelScope.launch {
            try {
                _uiState.value = UsuariosUiState.Loading
                val usuarios = gestionarUsuariosUseCase.obtenerUsuariosActivos()
                _usuarios.value = usuarios
                _totalUsuarios.value = usuarios.size
                _uiState.value = UsuariosUiState.Success
            } catch (e: Exception) {
                _uiState.value = UsuariosUiState.Error(e.message ?: "Error al cargar usuarios")
            }
        }
    }

    fun agregarUsuario(usuario: Usuario) {
        viewModelScope.launch {
            try {
                _uiState.value = UsuariosUiState.Loading
                
                // Validar usuario
                val errores = gestionarUsuariosUseCase.validarUsuario(usuario)
                if (errores.isNotEmpty()) {
                    _uiState.value = UsuariosUiState.Error(errores.joinToString("\n"))
                    return@launch
                }
                
                gestionarUsuariosUseCase.insertarUsuario(usuario)
                cargarUsuarios()
                
            } catch (e: Exception) {
                _uiState.value = UsuariosUiState.Error(e.message ?: "Error al agregar usuario")
            }
        }
    }

    fun actualizarUsuario(usuario: Usuario) {
        viewModelScope.launch {
            try {
                _uiState.value = UsuariosUiState.Loading
                
                // Validar usuario
                val errores = gestionarUsuariosUseCase.validarUsuario(usuario)
                if (errores.isNotEmpty()) {
                    _uiState.value = UsuariosUiState.Error(errores.joinToString("\n"))
                    return@launch
                }
                
                gestionarUsuariosUseCase.actualizarUsuario(usuario)
                cargarUsuarios()
                
            } catch (e: Exception) {
                _uiState.value = UsuariosUiState.Error(e.message ?: "Error al actualizar usuario")
            }
        }
    }

    fun eliminarUsuario(usuario: Usuario) {
        viewModelScope.launch {
            try {
                _uiState.value = UsuariosUiState.Loading
                gestionarUsuariosUseCase.eliminarUsuario(usuario)
                cargarUsuarios()
            } catch (e: Exception) {
                _uiState.value = UsuariosUiState.Error(e.message ?: "Error al eliminar usuario")
            }
        }
    }

    fun cambiarEstadoUsuario(id: Long, activo: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = UsuariosUiState.Loading
                gestionarUsuariosUseCase.cambiarEstadoUsuario(id, activo)
                cargarUsuarios()
            } catch (e: Exception) {
                _uiState.value = UsuariosUiState.Error(e.message ?: "Error al cambiar estado del usuario")
            }
        }
    }

    fun buscarUsuarios(nombre: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UsuariosUiState.Loading
                val usuarios = if (nombre.isBlank()) {
                    gestionarUsuariosUseCase.obtenerUsuariosActivos()
                } else {
                    gestionarUsuariosUseCase.buscarUsuariosPorNombre(nombre)
                }
                _usuarios.value = usuarios
                _totalUsuarios.value = usuarios.size
                _uiState.value = UsuariosUiState.Success
            } catch (e: Exception) {
                _uiState.value = UsuariosUiState.Error(e.message ?: "Error al buscar usuarios")
            }
        }
    }

    fun recargarUsuarios() {
        cargarUsuarios()
    }
}

sealed class UsuariosUiState {
    object Loading : UsuariosUiState()
    object Success : UsuariosUiState()
    data class Error(val mensaje: String) : UsuariosUiState()
} 