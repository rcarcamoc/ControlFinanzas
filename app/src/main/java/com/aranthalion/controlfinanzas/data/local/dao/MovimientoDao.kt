package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.*
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface MovimientoDao {
    @Query("SELECT * FROM movimientos ORDER BY fecha DESC LIMIT 1000")
    suspend fun obtenerMovimientos(): List<MovimientoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun agregarMovimiento(movimiento: MovimientoEntity)

    @Update
    suspend fun actualizarMovimiento(movimiento: MovimientoEntity)

    // Métodos de auditoría
    @Query("UPDATE movimientos SET fechaActualizacion = :timestamp, metodoActualizacion = :metodo, daoResponsable = :dao WHERE id = :id")
    suspend fun actualizarAuditoria(id: Long, timestamp: Long, metodo: String, dao: String)
    
    @Query("SELECT * FROM movimientos ORDER BY fechaActualizacion DESC LIMIT 50")
    suspend fun obtenerMovimientosRecientes(): List<MovimientoEntity>
    
    @Query("SELECT * FROM movimientos WHERE metodoActualizacion = :metodo ORDER BY fechaActualizacion DESC")
    suspend fun obtenerMovimientosPorMetodo(metodo: String): List<MovimientoEntity>

    @Delete
    suspend fun eliminarMovimiento(movimiento: MovimientoEntity)

    @Query("SELECT * FROM movimientos WHERE tipo = :tipo ORDER BY fecha DESC LIMIT 500")
    fun getMovimientosByTipo(tipo: String): Flow<List<MovimientoEntity>>

    @Query("SELECT * FROM movimientos WHERE fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY fecha DESC LIMIT 1000")
    suspend fun obtenerMovimientosPorPeriodo(fechaInicio: Date, fechaFin: Date): List<MovimientoEntity>

    @Query("DELETE FROM movimientos")
    suspend fun deleteAllMovimientos()

    @Query("SELECT idUnico FROM movimientos")
    suspend fun obtenerIdUnicos(): List<String>

    @Query("SELECT idUnico FROM movimientos WHERE periodoFacturacion = :periodo")
    suspend fun obtenerIdUnicosPorPeriodo(periodo: String?): List<String>

    @Query("SELECT idUnico, categoriaId FROM movimientos WHERE periodoFacturacion = :periodo")
    suspend fun obtenerCategoriasPorIdUnico(periodo: String?): List<IdUnicoCategoria>

    @Query("DELETE FROM movimientos WHERE periodoFacturacion = :periodo")
    suspend fun eliminarMovimientosPorPeriodo(periodo: String?)

    // Nuevo método para obtener movimientos que ya tienen categoría asignada
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NOT NULL ORDER BY fecha DESC")
    suspend fun obtenerMovimientosConCategoria(): List<MovimientoEntity>
    
    // Consultas optimizadas usando campos normalizados (HITO 1.1)
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NULL ORDER BY fecha DESC LIMIT :limit")
    suspend fun obtenerMovimientosSinCategoria(limit: Int = 50): List<MovimientoEntity>
    
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NULL AND descripcionNormalizada LIKE '%' || :patron || '%' ORDER BY fecha DESC LIMIT :limit")
    suspend fun obtenerMovimientosSinCategoriaPorPatron(patron: String, limit: Int = 50): List<MovimientoEntity>
    
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NULL AND montoCategoria = :categoriaMonto ORDER BY fecha DESC LIMIT :limit")
    suspend fun obtenerMovimientosSinCategoriaPorMonto(categoriaMonto: String, limit: Int = 50): List<MovimientoEntity>
    
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NULL AND fechaMes = :mes ORDER BY fecha DESC LIMIT :limit")
    suspend fun obtenerMovimientosSinCategoriaPorMes(mes: String, limit: Int = 50): List<MovimientoEntity>
    
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NOT NULL AND descripcionNormalizada = :descripcionNormalizada ORDER BY fecha DESC LIMIT :limit")
    suspend fun obtenerMovimientosSimilares(descripcionNormalizada: String, limit: Int = 10): List<MovimientoEntity>
    
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NOT NULL AND montoCategoria = :categoriaMonto AND descripcionNormalizada LIKE '%' || :patron || '%' ORDER BY fecha DESC LIMIT :limit")
    suspend fun obtenerMovimientosSimilaresPorMonto(patron: String, categoriaMonto: String, limit: Int = 10): List<MovimientoEntity>
    
    // Método para actualizar campos normalizados
    @Query("UPDATE movimientos SET descripcionNormalizada = :descripcionNormalizada, montoCategoria = :montoCategoria, fechaMes = :fechaMes, fechaDiaSemana = :fechaDiaSemana, fechaDia = :fechaDia, fechaAnio = :fechaAnio WHERE id = :id")
    suspend fun actualizarCamposNormalizados(
        id: Long,
        descripcionNormalizada: String,
        montoCategoria: String,
        fechaMes: String,
        fechaDiaSemana: String,
        fechaDia: Int,
        fechaAnio: Int
    )
    
    // Método para actualizar campos normalizados por período
    @Query("UPDATE movimientos SET descripcionNormalizada = :descripcionNormalizada, montoCategoria = :montoCategoria, fechaMes = :fechaMes, fechaDiaSemana = :fechaDiaSemana, fechaDia = :fechaDia, fechaAnio = :fechaAnio WHERE periodoFacturacion = :periodo")
    suspend fun actualizarCamposNormalizadosPorPeriodo(
        periodo: String,
        descripcionNormalizada: String,
        montoCategoria: String,
        fechaMes: String,
        fechaDiaSemana: String,
        fechaDia: Int,
        fechaAnio: Int
    )
    
    // Consultas optimizadas para clasificación automática (HITO 1.2)
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NULL AND descripcionNormalizada != '' ORDER BY fecha DESC LIMIT :limit")
    suspend fun obtenerMovimientosSinCategoriaOptimizado(limit: Int = 50): List<MovimientoEntity>
    
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NOT NULL AND descripcionNormalizada = :descripcionNormalizada ORDER BY fecha DESC LIMIT :limit")
    suspend fun obtenerMovimientosSimilaresExactos(descripcionNormalizada: String, limit: Int = 10): List<MovimientoEntity>
    
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NOT NULL AND montoCategoria = :categoriaMonto ORDER BY fecha DESC LIMIT :limit")
    suspend fun obtenerMovimientosPorCategoriaMonto(categoriaMonto: String, limit: Int = 20): List<MovimientoEntity>
    
    @Query("SELECT * FROM movimientos WHERE categoriaId IS NOT NULL AND fechaMes = :mes ORDER BY fecha DESC LIMIT :limit")
    suspend fun obtenerMovimientosPorMes(mes: String, limit: Int = 20): List<MovimientoEntity>
    
    @Query("SELECT COUNT(*) FROM movimientos WHERE categoriaId IS NULL")
    suspend fun contarMovimientosSinCategoria(): Int
    
    @Query("SELECT COUNT(*) FROM movimientos WHERE categoriaId IS NOT NULL")
    suspend fun contarMovimientosConCategoria(): Int
    
    // Consulta optimizada por período usando campos normalizados
    @Query("SELECT * FROM movimientos WHERE fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY fecha DESC LIMIT 1000")
    suspend fun obtenerMovimientosPorPeriodoOptimizado(fechaInicio: Date, fechaFin: Date): List<MovimientoEntity>

    data class IdUnicoCategoria(
        val idUnico: String,
        val categoriaId: Long?
    )
} 