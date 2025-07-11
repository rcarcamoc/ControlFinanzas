package com.aranthalion.controlfinanzas.domain.usuario

data class Usuario(
    val id: Long = 0,
    val nombre: String,
    val apellido: String,
    val email: String? = null,
    val telefono: String? = null,
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
) {
    val nombreCompleto: String
        get() = "$nombre $apellido".trim()
} 