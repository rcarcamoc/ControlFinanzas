package com.aranthalion.controlfinanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auditoria")
data class AuditoriaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tabla: String, // "movimientos", "presupuestos", "categorias", "clasificacion"
    val operacion: String, // "INSERT", "UPDATE", "DELETE", "DELETE_PERIODO"
    val entidadId: Long?, // ID de la entidad afectada
    val detalles: String, // Descripción detallada de la operación
    val daoResponsable: String, // DAO que ejecutó la operación
    val usuarioResponsable: String = "SYSTEM" // Para futuras implementaciones
) 