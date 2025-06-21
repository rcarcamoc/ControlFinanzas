package com.aranthalion.controlfinanzas.domain.clasificacion

data class ClasificacionAutomatica(
    val id: Long = 0,
    val patron: String,
    val categoriaId: Long,
    val nivelConfianza: Double,
    val frecuencia: Int = 1,
    val ultimaActualizacion: Long = System.currentTimeMillis()
) 