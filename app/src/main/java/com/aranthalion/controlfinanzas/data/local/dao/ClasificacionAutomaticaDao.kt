package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.local.entity.ClasificacionAutomaticaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClasificacionAutomaticaDao {
    
    @Query("SELECT * FROM clasificacion_automatica ORDER BY nivelConfianza DESC, frecuencia DESC")
    suspend fun obtenerTodosLosPatrones(): List<ClasificacionAutomaticaEntity>
    
    @Query("SELECT * FROM clasificacion_automatica WHERE patron = :patron LIMIT 1")
    suspend fun obtenerPatronPorDescripcion(patron: String): ClasificacionAutomaticaEntity?
    
    @Query("SELECT * FROM clasificacion_automatica WHERE patron LIKE '%' || :descripcion || '%' OR :descripcion LIKE '%' || patron || '%' ORDER BY nivelConfianza DESC, frecuencia DESC LIMIT 1")
    suspend fun buscarMejorCoincidencia(descripcion: String): ClasificacionAutomaticaEntity?
    
    // Cambiar a IGNORE para evitar duplicados
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarPatron(patron: ClasificacionAutomaticaEntity)
    
    @Update
    suspend fun actualizarPatron(patron: ClasificacionAutomaticaEntity)
    
    @Query("UPDATE clasificacion_automatica SET frecuencia = frecuencia + 1, nivelConfianza = :nuevaConfianza, ultimaActualizacion = :timestamp WHERE patron = :patron AND categoriaId = :categoriaId")
    suspend fun actualizarFrecuenciaYConfianza(patron: String, categoriaId: Long, nuevaConfianza: Double, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM clasificacion_automatica")
    suspend fun eliminarTodosLosPatrones()
    
    @Query("SELECT COUNT(*) FROM clasificacion_automatica")
    suspend fun obtenerCantidadPatrones(): Int
    
    // Nuevo método para verificar si existe un patrón
    @Query("SELECT COUNT(*) FROM clasificacion_automatica WHERE patron = :patron AND categoriaId = :categoriaId")
    suspend fun existePatron(patron: String, categoriaId: Long): Int
} 