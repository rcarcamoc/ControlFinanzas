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
    
    // Nuevos métodos para el sistema mejorado
    suspend fun obtenerSugerenciaMejorada(descripcion: String): ResultadoClasificacion
    suspend fun actualizarCacheClasificaciones()
    suspend fun obtenerEstadisticasClasificacion(): EstadisticasClasificacion
}

data class SugerenciaClasificacion(
    val categoriaId: Long,
    val nivelConfianza: Double,
    val patron: String
)

// Nuevo sistema de resultados de clasificación
sealed class ResultadoClasificacion {
    data class AltaConfianza(
        val categoriaId: Long,
        val confianza: Double,
        val patron: String,
        val tipoCoincidencia: TipoCoincidencia
    ) : ResultadoClasificacion()
    
    data class BajaConfianza(
        val sugerencias: List<SugerenciaClasificacion>,
        val confianzaMaxima: Double
    ) : ResultadoClasificacion()
    
    data class SinCoincidencias(
        val descripcion: String,
        val razon: String
    ) : ResultadoClasificacion()
}

enum class TipoCoincidencia {
    EXACTA,           // 100% confianza
    PARCIAL,          // 90% confianza
    FUZZY_ALTA,       // 80% confianza
    FUZZY_MEDIA,      // 60% confianza
    HISTORICO,        // Basado en historial
    PATRON            // Basado en patrones aprendidos
}

data class EstadisticasClasificacion(
    val totalTransacciones: Int = 0,
    val clasificacionesExactas: Int = 0,
    val clasificacionesParciales: Int = 0,
    val clasificacionesFuzzy: Int = 0,
    val sinClasificar: Int = 0,
    val precisionPromedio: Double = 0.0,
    val comerciosMasFrecuentes: List<ComercioFrecuente> = emptyList(),
    val categoriasMasUsadas: List<CategoriaUso> = emptyList()
)

data class ComercioFrecuente(
    val nombreNormalizado: String,
    val nombreOriginal: String,
    val categoriaId: Long,
    val frecuencia: Int,
    val confianzaPromedio: Double
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
    val nombre: String,
    val frecuencia: Int,
    val porcentaje: Double
)

data class PatronEfectivo(
    val patron: String,
    val categoriaId: Long,
    val precision: Double,
    val frecuencia: Int
)