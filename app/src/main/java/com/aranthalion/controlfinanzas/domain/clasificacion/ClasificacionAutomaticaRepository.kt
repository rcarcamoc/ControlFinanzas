package com.aranthalion.controlfinanzas.domain.clasificacion

interface ClasificacionAutomaticaRepository {
    suspend fun guardarPatron(patron: String, categoriaId: Long)
    suspend fun obtenerSugerencia(descripcion: String): SugerenciaClasificacion?
    suspend fun obtenerTodosLosPatrones(): List<ClasificacionAutomatica>
    suspend fun actualizarConfianza(patron: String, categoriaId: Long, nuevaConfianza: Double)
    suspend fun cargarDatosHistoricos()
}

data class SugerenciaClasificacion(
    val categoriaId: Long,
    val nivelConfianza: Double,
    val patron: String
) 