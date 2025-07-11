package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.UsuarioDao
import com.aranthalion.controlfinanzas.data.local.entity.UsuarioEntity
import com.aranthalion.controlfinanzas.domain.usuario.Usuario
import com.aranthalion.controlfinanzas.domain.usuario.UsuarioRepository
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(
    private val usuarioDao: UsuarioDao
) : UsuarioRepository {

    override suspend fun obtenerTodosLosUsuarios(): List<Usuario> {
        return usuarioDao.obtenerTodosLosUsuarios().map { it.toDomain() }
    }

    override suspend fun obtenerUsuariosActivos(): List<Usuario> {
        return usuarioDao.obtenerUsuariosActivos().map { it.toDomain() }
    }

    override suspend fun obtenerUsuarioPorId(id: Long): Usuario? {
        return usuarioDao.obtenerUsuarioPorId(id)?.toDomain()
    }

    override suspend fun buscarUsuariosPorNombre(nombre: String): List<Usuario> {
        return usuarioDao.buscarUsuariosPorNombre(nombre).map { it.toDomain() }
    }

    override suspend fun insertarUsuario(usuario: Usuario): Long {
        return usuarioDao.insertarUsuario(usuario.toEntity())
    }

    override suspend fun actualizarUsuario(usuario: Usuario) {
        usuarioDao.actualizarUsuario(usuario.toEntity())
    }

    override suspend fun eliminarUsuario(usuario: Usuario) {
        usuarioDao.eliminarUsuario(usuario.toEntity())
    }

    override suspend fun cambiarEstadoUsuario(id: Long, activo: Boolean) {
        usuarioDao.cambiarEstadoUsuario(id, activo)
    }

    override suspend fun obtenerCantidadUsuariosActivos(): Int {
        return usuarioDao.obtenerCantidadUsuariosActivos()
    }

    override suspend fun obtenerUsuarioPorEmail(email: String): Usuario? {
        return usuarioDao.obtenerUsuarioPorEmail(email)?.toDomain()
    }

    private fun UsuarioEntity.toDomain(): Usuario {
        return Usuario(
            id = id,
            nombre = nombre,
            apellido = apellido,
            email = email,
            telefono = telefono,
            activo = activo,
            fechaCreacion = fechaCreacion,
            fechaActualizacion = fechaActualizacion
        )
    }

    private fun Usuario.toEntity(): UsuarioEntity {
        return UsuarioEntity(
            id = id,
            nombre = nombre,
            apellido = apellido,
            email = email,
            telefono = telefono,
            activo = activo,
            fechaCreacion = fechaCreacion,
            fechaActualizacion = fechaActualizacion
        )
    }
} 