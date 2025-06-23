package com.aranthalion.controlfinanzas.domain.clasificacion

import java.util.*

interface ClasificacionAutomaticaRepository {
    suspend fun guardarPatron(patron: String, categoriaId: Long)
    suspend fun obtenerSugerencia(descripcion: String): SugerenciaClasificacion?
    suspend fun obtenerTodosLosPatrones(): List<ClasificacionAutomatica>
    suspend fun actualizarConfianza(patron: String, categoriaId: Long, nuevaConfianza: Double)
    suspend fun cargarDatosHistoricos()
    
    suspend fun agregarRegla(regla: ReglaClasificacion)
    suspend fun obtenerReglas(): List<ReglaClasificacion>
    suspend fun actualizarRegla(regla: ReglaClasificacion)
    suspend fun eliminarRegla(reglaId: Long)
    
    suspend fun obtenerPatronesAprendidos(): List<PatronAprendido>
    suspend fun obtenerPatronPorDescripcion(descripcion: String): PatronAprendido?
    suspend fun agregarPatron(patron: PatronAprendido)
    suspend fun actualizarPatron(patron: PatronAprendido)
    
    suspend fun obtenerTotalClasificaciones(): Int
    suspend fun obtenerClasificacionesAutomaticas(): Int
    suspend fun obtenerPrecisionPromedio(): Double
    suspend fun obtenerCategoriasMasUsadas(): List<CategoriaUso>
    suspend fun obtenerPatronesMasEfectivos(): List<PatronEfectivo>
    
    suspend fun registrarClasificacion(descripcion: String, categoriaId: Long, esCorrecta: Boolean)
}

data class SugerenciaClasificacion(
    val categoriaId: Long,
    val nivelConfianza: Double,
    val patron: String
)

data class ReglaClasificacion(
    val id: Long,
    val patron: String,
    val categoriaId: Long,
    val categoriaNombre: String,
    val tipo: TipoRegla,
    val prioridad: Int,
    val activa: Boolean = true
)

enum class TipoRegla {
    EXACTA,
    CONTIENE,
    INICIA_CON,
    TERMINA_CON,
    REGEX,
    SIMILITUD
}

data class PatronAprendido(
    val descripcion: String,
    val categoriaId: Long,
    val frecuencia: Int,
    val ultimaActualizacion: Date,
    val confianza: Double
)

data class CategoriaUso(
    val categoriaId: Long,
    val categoriaNombre: String,
    val cantidadUsos: Int,
    val porcentajeDelTotal: Double
)

data class PatronEfectivo(
    val patron: String,
    val categoriaNombre: String,
    val aciertos: Int,
    val totalAplicaciones: Int,
    val precision: Double
)