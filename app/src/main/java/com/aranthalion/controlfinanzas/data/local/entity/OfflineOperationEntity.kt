package com.aranthalion.controlfinanzas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_operations")
data class OfflineOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,     // "TRANSACTION", "BUDGET", "SALARY", etc.
    val operationType: String,  // "CREATE", "UPDATE", "DELETE"
    val entityId: String,       // Local unique ID or Server ID
    val payload: String,        // JSON serialized object payload
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)
