package com.aranthalion.controlfinanzas.data.remote.ai

import android.util.Base64
import android.util.Log
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfImportService @Inject constructor(
    private val config: ConfiguracionPreferences
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "PdfImportService"
    }

    data class ParsedTransaction(
        val date: String,
        val description: String,
        val amount: Double,
        val cardType: String?,
        val suggestedCategoryName: String?
    )

    data class BillingPeriodResponse(
        val start: String?,
        val end: String?
    )

    data class PdfImportResult(
        val success: Boolean,
        val billingPeriod: BillingPeriodResponse?,
        val cardNumber: String?,
        val transactions: List<ParsedTransaction>
    )

    suspend fun importPdf(base64Pdf: String, password: String): Result<PdfImportResult> = withContext(Dispatchers.IO) {
        if (!config.syncEnabled || config.syncHouseholdId.isBlank()) {
            return@withContext Result.failure(Exception("Sincronización no configurada u hogar ID vacío"))
        }

        try {
            val syncUrl = config.syncServerUrl
            val pdfUrl = if (syncUrl.endsWith("/api/sync")) {
                syncUrl.replace("/api/sync", "/api/import/pdf/")
            } else if (syncUrl.endsWith("/api/sync/")) {
                syncUrl.replace("/api/sync/", "/api/import/pdf/")
            } else {
                val baseUrl = syncUrl.substringBefore("/api/")
                "$baseUrl/api/import/pdf/"
            }

            Log.d(TAG, "📤 Enviando PDF a analizar: $pdfUrl")

            val payload = mapOf(
                "file" to base64Pdf,
                "password" to password
            )

            val jsonBody = gson.toJson(payload)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val email = config.syncEmail
            val passwordCredentials = config.syncPassword
            val credentials = "$email:$passwordCredentials"
            val base64Credentials = Base64.encodeToString(
                credentials.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val request = Request.Builder()
                .url(pdfUrl)
                .addHeader("Authorization", "Basic $base64Credentials")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Error en PDF HTTP ${response.code}: $errorBody")
                    return@withContext Result.failure(Exception("Error del servidor: HTTP ${response.code}"))
                }

                val responseBody = response.body?.string() ?: ""
                val result: PdfImportResult = gson.fromJson(responseBody, PdfImportResult::class.java)

                Log.d(TAG, "✅ PDF analizado. Encontradas ${result.transactions?.size ?: 0} transacciones.")
                return@withContext Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al analizar PDF", e)
            return@withContext Result.failure(e)
        }
    }
}
