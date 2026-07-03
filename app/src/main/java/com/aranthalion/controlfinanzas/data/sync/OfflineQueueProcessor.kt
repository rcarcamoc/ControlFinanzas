package com.aranthalion.controlfinanzas.data.sync

import com.aranthalion.controlfinanzas.data.local.dao.OfflineOperationDao
import com.aranthalion.controlfinanzas.data.local.entity.OfflineOperationEntity
import com.aranthalion.controlfinanzas.data.remote.api.FinanzasApiService
import com.aranthalion.controlfinanzas.data.remote.api.dto.*
import com.aranthalion.controlfinanzas.data.remote.connectivity.ConnectivityMonitor
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineQueueProcessor @Inject constructor(
    private val api: FinanzasApiService,
    private val offlineDao: OfflineOperationDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val gson: Gson
) {
    suspend fun processQueue(): Result<Int> {
        if (!connectivityMonitor.isOnline.value) {
            return Result.failure(Exception("No internet connection"))
        }
        
        val operations = offlineDao.getPendingOperations()
        var processed = 0
        
        for (op in operations) {
            try {
                when (op.entityType) {
                    "TRANSACTION" -> processTransactionOp(op)
                    "BUDGET" -> processBudgetOp(op)
                    "SALARY" -> processSalaryOp(op)
                    "DEBT" -> processDebtOp(op)
                    "PATTERN" -> processPatternOp(op)
                    "CATEGORY" -> processCategoryOp(op)
                }
                offlineDao.delete(op)
                processed++
            } catch (e: Exception) {
                if (op.retryCount >= 3) {
                    offlineDao.delete(op) // Drop operation after 3 failed retries to avoid blocking the queue
                } else {
                    offlineDao.markRetry(op.id, e.message ?: "Unknown error")
                }
            }
        }
        return Result.success(processed)
    }
    
    private suspend fun processTransactionOp(op: OfflineOperationEntity) {
        when (op.operationType) {
            "CREATE" -> {
                val dto = gson.fromJson(op.payload, CreateTransactionDto::class.java)
                api.createTransaction(dto)
            }
            "UPDATE" -> {
                val dto = gson.fromJson(op.payload, UpdateTransactionDto::class.java)
                api.updateTransaction(op.entityId, dto)
            }
            "DELETE" -> {
                api.deleteTransaction(op.entityId)
            }
        }
    }

    private suspend fun processCategoryOp(op: OfflineOperationEntity) {
        when (op.operationType) {
            "CREATE" -> {
                val dto = gson.fromJson(op.payload, CreateCategoryDto::class.java)
                api.createCategory(dto)
            }
        }
    }

    private suspend fun processBudgetOp(op: OfflineOperationEntity) {
        when (op.operationType) {
            "CREATE" -> {
                val dto = gson.fromJson(op.payload, CreateBudgetDto::class.java)
                api.createBudget(dto)
            }
        }
    }

    private suspend fun processSalaryOp(op: OfflineOperationEntity) {
        when (op.operationType) {
            "CREATE" -> {
                val dto = gson.fromJson(op.payload, CreateSalaryDto::class.java)
                api.createSalary(dto)
            }
            "DELETE" -> {
                api.deleteSalary(op.entityId)
            }
        }
    }

    private suspend fun processDebtOp(op: OfflineOperationEntity) {
        when (op.operationType) {
            "CREATE" -> {
                val dto = gson.fromJson(op.payload, CreateDebtDto::class.java)
                api.createDebt(dto)
            }
            "DELETE" -> {
                api.deleteDebt(op.entityId)
            }
        }
    }

    private suspend fun processPatternOp(op: OfflineOperationEntity) {
        // AI or manually classification pattern creation online
    }
}
