package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    suspend fun obtenerCategorias(): List<Categoria>

    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    fun getAllCategorias(): Flow<List<Categoria>>

    @Query("SELECT * FROM categorias WHERE tipo = :tipo ORDER BY nombre ASC")
    fun getCategoriasByTipo(tipo: String): Flow<List<Categoria>>

    @Query("SELECT * FROM categorias WHERE nombre = :nombre LIMIT 1")
    suspend fun obtenerCategoriaPorNombre(nombre: String): Categoria?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun agregarCategoria(categoria: Categoria)

    @Update
    suspend fun actualizarCategoria(categoria: Categoria)

    @Delete
    suspend fun eliminarCategoria(categoria: Categoria)

    @Query("SELECT COUNT(*) FROM categorias WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(:nombre))")
    suspend fun existeCategoria(nombre: String): Int
} 