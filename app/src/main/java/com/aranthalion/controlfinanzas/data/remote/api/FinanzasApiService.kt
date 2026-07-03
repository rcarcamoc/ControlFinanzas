package com.aranthalion.controlfinanzas.data.remote.api

import com.aranthalion.controlfinanzas.data.remote.api.dto.*
import retrofit2.http.*

interface FinanzasApiService {

    // === REFRESH (caché completo o incremental) ===
    @GET("api/mobile/refresh")
    suspend fun refresh(
        @Query("householdId") householdId: String,
        @Query("since") since: Long? = null,
        @Query("billingPeriod") billingPeriod: String? = null,
        @Query("overwrite") overwrite: Boolean? = null
    ): RefreshResponse

    // === TRANSACTIONS ===
    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("householdId") householdId: String
    ): List<TransactionDto>

    @POST("api/transactions")
    suspend fun createTransaction(@Body transaction: CreateTransactionDto): okhttp3.ResponseBody

    @PATCH("api/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body transaction: UpdateTransactionDto
    ): okhttp3.ResponseBody

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Any

    @DELETE("api/transactions")
    suspend fun deleteTransactionsByPeriod(
        @Query("billingPeriod") period: String
    ): Any

    // === CATEGORIES ===
    @GET("api/categories")
    suspend fun getCategories(@Query("householdId") householdId: String): List<CategoryDto>

    @POST("api/categories")
    suspend fun createCategory(@Body category: CreateCategoryDto): CategoryDto

    // === BUDGETS ===
    @GET("api/budgets")
    suspend fun getBudgets(
        @Query("householdId") householdId: String
    ): List<BudgetDto>

    @POST("api/budgets")
    suspend fun createBudget(@Body budget: CreateBudgetDto): BudgetDto

    // === SALARIES ===
    @GET("api/salaries")
    suspend fun getSalaries(@Query("householdId") householdId: String): List<SalaryDto>

    @POST("api/salaries")
    suspend fun createSalary(@Body salary: CreateSalaryDto): SalaryDto

    @DELETE("api/salaries")
    suspend fun deleteSalary(@Query("id") id: String): Any

    // === DEBTS ===
    @GET("api/debts")
    suspend fun getDebts(@Query("householdId") householdId: String): List<DebtDto>

    @POST("api/debts")
    suspend fun createDebt(@Body debt: CreateDebtDto): DebtDto

    @DELETE("api/debts")
    suspend fun deleteDebt(@Query("id") id: String): Any

    // === CLASSIFY ===
    @POST("api/classify")
    suspend fun classifyTransactions(@Body body: Map<String, Int>): Any

    // === DASHBOARD ===
    @GET("api/mobile/dashboard")
    suspend fun getDashboardData(
        @Query("householdId") householdId: String,
        @Query("period") period: String
    ): DashboardResponse
}
