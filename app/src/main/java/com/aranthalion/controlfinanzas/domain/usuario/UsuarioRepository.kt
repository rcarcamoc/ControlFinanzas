package com.aranthalion.controlfinanzas.domain.usuario

interface UsuarioRepository {
    suspend fun obtenerTodosLosUsuarios(): List<Usuario>
    suspend fun obtenerUsuariosActivos(): List<Usuario>
    suspend fun obtenerUsuarioPorId(id: Long): Usuario?
    suspend fun buscarUsuariosPorNombre(nombre: String): List<Usuario>
    suspend fun insertarUsuario(usuario: Usuario): Long
    suspend fun actualizarUsuario(usuario: Usuario)
    suspend fun eliminarUsuario(usuario: Usuario)
    suspend fun cambiarEstadoUsuario(id: Long, activo: Boolean)
    suspend fun obtenerCantidadUsuariosActivos(): Int
    suspend fun obtenerUsuarioPorEmail(email: String): Usuario?
} 