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
class VisionImportService @Inject constructor(
    private val config: ConfiguracionPreferences
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "VisionImportService"
    }

    data class ParsedTransaction(
        val date: String,
        val description: String,
        val amount: Double,
        val cardType: String?,
        val suggestedCategoryName: String?
    )

    suspend fun analyzeScreenshot(base64Image: String): Result<List<ParsedTransaction>> = withContext(Dispatchers.IO) {
        if (!config.syncEnabled || config.syncHouseholdId.isBlank()) {
            return@withContext Result.failure(Exception("Sincronización no configurada u hogar ID vacío"))
        }

        try {
            // Reemplazar la ruta de sync con el endpoint de visión
            val syncUrl = config.syncServerUrl
            val visionUrl = if (syncUrl.endsWith("/api/sync")) {
                syncUrl.replace("/api/sync", "/api/import/vision/")
            } else if (syncUrl.endsWith("/api/sync/")) {
                syncUrl.replace("/api/sync/", "/api/import/vision/")
            } else {
                val baseUrl = syncUrl.substringBefore("/api/")
                "$baseUrl/api/import/vision/"
            }

            Log.d(TAG, "📤 Enviando captura a analizar: $visionUrl")

            // Asegurarse de que el string base64 empiece con el prefijo correcto de Data URI si no lo tiene
            val imagePayload = if (base64Image.startsWith("data:image")) {
                base64Image
            } else {
                "data:image/jpeg;base64,$base64Image"
            }

            val payload = mapOf(
                "image" to imagePayload,
                "currentYear" to java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            )

            val jsonBody = gson.toJson(payload)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val email = config.syncEmail
            val password = config.syncPassword
            val credentials = "$email:$password"
            val base64Credentials = Base64.encodeToString(
                credentials.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val request = Request.Builder()
                .url(visionUrl)
                .addHeader("Authorization", "Basic $base64Credentials")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Error en Vision HTTP ${response.code}: $errorBody")
                    return@withContext Result.failure(Exception("Error del servidor: HTTP ${response.code}"))
                }

                val responseBody = response.body?.string() ?: ""
                val data: Map<String, Any> = gson.fromJson(responseBody, object : TypeToken<Map<String, Any>>() {}.type)
                val txsJson = gson.toJson(data["transactions"])
                
                val listType = object : TypeToken<List<ParsedTransaction>>() {}.type
                val transactions: List<ParsedTransaction> = gson.fromJson(txsJson, listType)

                Log.d(TAG, "✅ Captura analizada. Encontradas ${transactions.size} transacciones.")
                return@withContext Result.success(transactions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al analizar captura de pantalla", e)
            return@withContext Result.failure(e)
        }
    }
}
