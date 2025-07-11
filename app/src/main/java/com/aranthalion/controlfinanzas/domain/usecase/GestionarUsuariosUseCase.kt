package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.domain.usuario.Usuario
import com.aranthalion.controlfinanzas.domain.usuario.UsuarioRepository
import javax.inject.Inject

class GestionarUsuariosUseCase @Inject constructor(
    private val usuarioRepository: UsuarioRepository
) {
    suspend fun obtenerTodosLosUsuarios(): List<Usuario> {
        return usuarioRepository.obtenerTodosLosUsuarios()
    }

    suspend fun obtenerUsuariosActivos(): List<Usuario> {
        return usuarioRepository.obtenerUsuariosActivos()
    }

    suspend fun obtenerUsuarioPorId(id: Long): Usuario? {
        return usuarioRepository.obtenerUsuarioPorId(id)
    }

    suspend fun buscarUsuariosPorNombre(nombre: String): List<Usuario> {
        return usuarioRepository.buscarUsuariosPorNombre(nombre)
    }

    suspend fun insertarUsuario(usuario: Usuario): Long {
        return usuarioRepository.insertarUsuario(usuario)
    }

    suspend fun actualizarUsuario(usuario: Usuario) {
        usuarioRepository.actualizarUsuario(usuario)
    }

    suspend fun eliminarUsuario(usuario: Usuario) {
        usuarioRepository.eliminarUsuario(usuario)
    }

    suspend fun cambiarEstadoUsuario(id: Long, activo: Boolean) {
        usuarioRepository.cambiarEstadoUsuario(id, activo)
    }

    suspend fun obtenerCantidadUsuariosActivos(): Int {
        return usuarioRepository.obtenerCantidadUsuariosActivos()
    }

    suspend fun obtenerUsuarioPorEmail(email: String): Usuario? {
        return usuarioRepository.obtenerUsuarioPorEmail(email)
    }

    suspend fun validarUsuario(usuario: Usuario): List<String> {
        val errores = mutableListOf<String>()
        
        if (usuario.nombre.isBlank()) {
            errores.add("El nombre es obligatorio")
        }
        
        if (usuario.apellido.isBlank()) {
            errores.add("El apellido es obligatorio")
        }
        
        if (usuario.email?.isNotBlank() == true) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(usuario.email).matches()) {
                errores.add("El formato del email no es válido")
            }
        }
        
        return errores
    }
} 