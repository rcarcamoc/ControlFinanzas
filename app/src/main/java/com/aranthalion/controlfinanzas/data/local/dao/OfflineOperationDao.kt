package com.aranthalion.controlfinanzas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.aranthalion.controlfinanzas.data.local.entity.OfflineOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineOperationDao {
    @Query("SELECT * FROM offline_operations ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<OfflineOperationEntity>
    
    @Query("SELECT COUNT(*) FROM offline_operations")
    fun getPendingCount(): Flow<Int>
    
    @Insert
    suspend fun insert(operation: OfflineOperationEntity)
    
    @Delete
    suspend fun delete(operation: OfflineOperationEntity)
    
    @Query("DELETE FROM offline_operations WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("UPDATE offline_operations SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun markRetry(id: Long, error: String)
}
