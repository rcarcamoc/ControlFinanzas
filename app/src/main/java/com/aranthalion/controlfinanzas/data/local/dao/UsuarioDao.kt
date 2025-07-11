package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios ORDER BY nombre ASC, apellido ASC")
    suspend fun obtenerTodosLosUsuarios(): List<UsuarioEntity>

    @Query("SELECT * FROM usuarios WHERE activo = 1 ORDER BY nombre ASC, apellido ASC")
    suspend fun obtenerUsuariosActivos(): List<UsuarioEntity>

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun obtenerUsuarioPorId(id: Long): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE nombre LIKE '%' || :nombre || '%' OR apellido LIKE '%' || :nombre || '%'")
    suspend fun buscarUsuariosPorNombre(nombre: String): List<UsuarioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUsuario(usuario: UsuarioEntity): Long

    @Update
    suspend fun actualizarUsuario(usuario: UsuarioEntity)

    @Delete
    suspend fun eliminarUsuario(usuario: UsuarioEntity)

    @Query("UPDATE usuarios SET activo = :activo WHERE id = :id")
    suspend fun cambiarEstadoUsuario(id: Long, activo: Boolean)

    @Query("SELECT COUNT(*) FROM usuarios WHERE activo = 1")
    suspend fun obtenerCantidadUsuariosActivos(): Int

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun obtenerUsuarioPorEmail(email: String): UsuarioEntity?
} 