package com.aranthalion.controlfinanzas.data.remote.api.dto

data class RefreshResponse(
    val serverTimestamp: Long,
    val transactions: List<TransactionDto>,
    val categories: List<CategoryDto>,
    val budgets: List<BudgetDto>,
    val salaries: List<SalaryDto>,
    val patterns: List<PatternDto>,
    val debts: List<DebtDto>,
    val users: List<UserDto>,
    val deletedIds: List<String>
)

data class TransactionDto(
    val idUnico: String,
    val amount: Double,
    val date: Long,
    val type: String, // "INGRESO" o "GASTO"
    val description: String,
    val categoryName: String,
    val cardType: String,
    val billingPeriod: String,
    val ignored: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val scope: String,
    val userId_internal: String,
    val userName: String
)

data class CategoryDto(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
    val updatedAt: Long
)

data class BudgetDto(
    val categoryName: String,
    val amount: Double,
    val period: String, // YYYY-MM
    val updatedAt: Long,
    val scope: String // "PERSONAL" o "HOUSEHOLD"
)

data class SalaryDto(
    val nombrePersona: String,
    val periodo: String, // YYYY-MM
    val sueldo: Double,
    val updatedAt: Long
)

data class PatternDto(
    val pattern: String,
    val categoryName: String,
    val confidence: Double,
    val frequency: Int,
    val updatedAt: Long
)

data class DebtDto(
    val debtorName: String,
    val creditorName: String,
    val amount: Double,
    val reason: String,
    val status: String,
    val billingPeriod: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String
)

// Request DTOs
data class CreateTransactionDto(
    val amount: Double,
    val currency: String,
    val date: String, // ISO String or date format (server expects date representation)
    val type: String, // "INCOME" o "EXPENSE"
    val description: String,
    val accountId: String,
    val categoryId: String?,
    val categoryName: String?,
    val householdId: String,
    val billingPeriod: String,
    val externalId: String? = null
)

data class UpdateTransactionDto(
    val status: String? = null,
    val ignored: Boolean? = null,
    val scope: String? = null,
    val userId_internal: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null
)

data class CreateCategoryDto(
    val name: String,
    val color: String,
    val householdId: String?
)

data class UpdateCategoryDto(
    val name: String,
    val color: String
)

data class CreateBudgetDto(
    val categoryId: String?,
    val categoryName: String? = null,
    val limit: Double,
    val month: Int,
    val year: Int,
    val householdId: String?,
    val period: String = "MONTHLY"
)

data class CreateSalaryDto(
    val id: String? = null,
    val householdId: String,
    val period: String,
    val amount: Double,
    val targetUserId: String?,
    val dummyUserName: String?
)

data class CreateDebtDto(
    val id: String? = null,
    val householdId: String,
    val debtorId: String?,
    val debtorName: String,
    val creditorId: String?,
    val creditorName: String,
    val amount: Double,
    val reason: String,
    val status: String = "PENDIENTE",
    val billingPeriod: String?,
    val dueDate: String?, // ISO String or date representation
    val notes: String?
)

data class UpdateDebtDto(
    val status: String
)

data class CreatePatternDto(
    val pattern: String,
    val categoryName: String,
    val householdId: String
)

data class DashboardResponse(
    val period: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val categories: List<DashboardCategoryDto>,
    val budgets: List<DashboardBudgetDto>,
    val trends: List<DashboardTrendDto>
)

data class DashboardCategoryDto(
    val categoryId: String,
    val name: String,
    val color: String,
    val icon: String,
    val amount: Double
)

data class DashboardBudgetDto(
    val categoryName: String,
    val limit: Double,
    val amount: Double
)

data class DashboardTrendDto(
    val period: String,
    val income: Double,
    val expense: Double
)
