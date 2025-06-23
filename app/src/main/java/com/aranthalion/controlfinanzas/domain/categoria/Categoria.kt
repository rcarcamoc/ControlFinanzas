package com.aranthalion.controlfinanzas.domain.categoria

data class Categoria(
    val id: Long = 0,
    val nombre: String,
    val descripcion: String = "",
    val tipo: String = "Gasto", // "Gasto" o "Ingreso"
    val presupuestoMensual: Double? = null // Presupuesto mensual opcional
) 