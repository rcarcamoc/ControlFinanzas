package com.aranthalion.controlfinanzas.data.remote.ai

import android.util.Log
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

// ──────────────────────────────────────────────
// Modelos de datos para el resumen de IA
// ──────────────────────────────────────────────

data class DatosAnalisisMes(
    val periodo: String,
    val diaActual: Int,
    val diasTotales: Int,
    val presupuestoTotal: Double,
    val gastoActual: Double,
    val ingresos: Double,
    val proyeccionFinMes: Double,
    val categoriaMasAlta: String,
    val cambioCategoriaMasAlta: Double, // % vs mes anterior
    val gastosHormigaTotal: Double,
    val gastosHormigaCantidad: Int,
    val tendenciaVsMesAnterior: Double, // % cambio en gasto total
    val presupuestosExcedidos: List<String>,
    val tasaAhorro: Double
)

data class ResumenIa(
    val texto: String,
    val proveedor: String, // "groq" | "gemini" | "local"
    val timestamp: Long = System.currentTimeMillis()
)

// ──────────────────────────────────────────────
// Rate limiter con backoff exponencial
// ──────────────────────────────────────────────

private class RateLimiter(
    private val maxRequests: Int = 28,      // Groq: 30 req/min, margen de seguridad
    private val windowMs: Long = 60_000L,
    private val maxBackoffMs: Long = 32_000L
) {
    private val requestTimestamps = ArrayDeque<Long>()
    private var backoffMultiplier = 1

    suspend fun ejecutar(block: suspend () -> String): String {
        repeat(5) { intento ->
            limpiarVentana()
            if (requestTimestamps.size < maxRequests) {
                requestTimestamps.addLast(System.currentTimeMillis())
                return try {
                    backoffMultiplier = 1 // reset en éxito
                    block()
                } catch (e: RateLimitException) {
                    val espera = calcularEspera()
                    Log.w("AiRateLimiter", "Rate limit hit, esperando ${espera}ms (intento ${intento + 1})")
                    delay(espera)
                    throw e
                }
            } else {
                // Ventana llena, calcular cuánto esperar
                val masAntiguo = requestTimestamps.firstOrNull() ?: 0L
                val espera = (masAntiguo + windowMs) - System.currentTimeMillis()
                if (espera > 0) {
                    Log.d("AiRateLimiter", "Ventana llena, esperando ${espera}ms")
                    delay(espera + 100)
                }
            }
        }
        throw Exception("Límite de reintentos agotado en RateLimiter")
    }

    private fun limpiarVentana() {
        val ahora = System.currentTimeMillis()
        while (requestTimestamps.isNotEmpty() && ahora - requestTimestamps.first() > windowMs) {
            requestTimestamps.removeFirst()
        }
    }

    private fun calcularEspera(): Long {
        val base = 2.0.pow(backoffMultiplier.toDouble()).toLong() * 1000L
        backoffMultiplier = min(backoffMultiplier + 1, 5)
        return min(base, maxBackoffMs)
    }
}

class RateLimitException(message: String) : Exception(message)

// ──────────────────────────────────────────────
// Caché simple en memoria (1 hora por período)
// ──────────────────────────────────────────────

private data class CacheEntry(val resumen: ResumenIa, val timestamp: Long = System.currentTimeMillis())

private object AiCache {
    private val cache = mutableMapOf<String, CacheEntry>()
    private const val TTL_MS = 60 * 60 * 1000L // 1 hora

    fun get(key: String): ResumenIa? {
        val entry = cache[key] ?: return null
        return if (System.currentTimeMillis() - entry.timestamp < TTL_MS) entry.resumen else null
    }

    fun put(key: String, resumen: ResumenIa) {
        cache[key] = CacheEntry(resumen)
    }
}

// ──────────────────────────────────────────────
// Servicio principal de IA para análisis
// ──────────────────────────────────────────────

@Singleton
class AiAnalisisService @Inject constructor(
    private val configuracion: ConfiguracionPreferences
) {
    private val gson = Gson()
    private val groqRateLimiter = RateLimiter(maxRequests = 28, windowMs = 60_000L)
    private val geminiRateLimiter = RateLimiter(maxRequests = 14, windowMs = 60_000L) // 15 RPM gratis

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Genera un resumen inteligente del mes financiero.
     * Flujo: Groq → Gemini → Plantillas locales
     */
    suspend fun generarResumenMes(datos: DatosAnalisisMes): ResumenIa = withContext(Dispatchers.IO) {
        val cacheKey = "${datos.periodo}_${datos.diaActual}"
        AiCache.get(cacheKey)?.let { return@withContext it }

        if (!configuracion.aiEnabled) {
            return@withContext generarResumenLocal(datos)
        }

        val proveedorPreferido = configuracion.aiProvider

        // Intentar Groq primero (o el preferido)
        if (proveedorPreferido != "gemini") {
            val groqKey = configuracion.groqApiKey
            if (groqKey.isNotBlank()) {
                try {
                    val resumen = groqRateLimiter.ejecutar {
                        llamarGroq(datos, groqKey)
                    }
                    val resultado = ResumenIa(texto = resumen, proveedor = "groq")
                    AiCache.put(cacheKey, resultado)
                    return@withContext resultado
                } catch (e: Exception) {
                    Log.w("AiAnalisis", "Groq falló, intentando Gemini: ${e.message}")
                }
            }
        }

        // Fallback: Gemini
        val geminiKey = configuracion.geminiApiKey
        if (geminiKey.isNotBlank()) {
            try {
                val resumen = geminiRateLimiter.ejecutar {
                    llamarGemini(datos, geminiKey)
                }
                val resultado = ResumenIa(texto = resumen, proveedor = "gemini")
                AiCache.put(cacheKey, resultado)
                return@withContext resultado
            } catch (e: Exception) {
                Log.w("AiAnalisis", "Gemini falló, usando plantillas locales: ${e.message}")
            }
        }

        // Último recurso: plantillas inteligentes locales
        generarResumenLocal(datos).also { AiCache.put(cacheKey, it) }
    }

    // ──────────────────────────────
    // Groq API (Llama 3.3 70B)
    // ──────────────────────────────

    private fun llamarGroq(datos: DatosAnalisisMes, apiKey: String): String {
        val prompt = buildPromptAnalisis(datos)
        val payload = gson.toJson(mapOf(
            "model" to "llama-3.3-70b-versatile",
            "messages" to listOf(
                mapOf("role" to "system", "content" to SYSTEM_PROMPT),
                mapOf("role" to "user", "content" to prompt)
            ),
            "temperature" to 0.3,
            "max_tokens" to 200
        ))

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (response.code == 429) {
            val retryAfter = response.header("retry-after")?.toLongOrNull() ?: 60L
            throw RateLimitException("Groq rate limit: retry after ${retryAfter}s")
        }

        if (!response.isSuccessful) {
            throw Exception("Groq HTTP ${response.code}: ${response.body?.string()}")
        }

        val body = response.body?.string() ?: throw Exception("Groq: respuesta vacía")
        val json = gson.fromJson(body, Map::class.java)
        val choices = json["choices"] as? List<*> ?: throw Exception("Groq: no hay choices")
        val firstChoice = choices.firstOrNull() as? Map<*, *> ?: throw Exception("Groq: choice inválido")
        val message = firstChoice["message"] as? Map<*, *> ?: throw Exception("Groq: no hay message")
        return (message["content"] as? String)?.trim() ?: throw Exception("Groq: content vacío")
    }

    // ──────────────────────────────
    // Gemini API (fallback)
    // ──────────────────────────────

    private suspend fun llamarGemini(datos: DatosAnalisisMes, apiKey: String): String {
        val config = generationConfig { temperature = 0.3f }
        val model = GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = apiKey,
            generationConfig = config
        )
        val prompt = "$SYSTEM_PROMPT\n\n${buildPromptAnalisis(datos)}"
        val response = model.generateContent(prompt)
        return response.text?.trim() ?: throw Exception("Gemini: respuesta vacía")
    }

    // ──────────────────────────────
    // Plantillas locales (siempre disponible)
    // ──────────────────────────────

    private fun generarResumenLocal(datos: DatosAnalisisMes): ResumenIa {
        val sb = StringBuilder()

        // Estado de ritmo
        val pctGastado = if (datos.presupuestoTotal > 0) datos.gastoActual / datos.presupuestoTotal * 100 else 0.0
        val pctPeriodo = if (datos.diasTotales > 0) datos.diaActual.toDouble() / datos.diasTotales * 100 else 0.0
        val ritmo = pctGastado - pctPeriodo

        when {
            ritmo > 20 -> sb.append("⚠️ Llevas el día ${datos.diaActual} de ${datos.diasTotales} y ya consumiste el ${pctGastado.toInt()}% del presupuesto — vas muy acelerado.")
            ritmo > 5  -> sb.append("Llevas el día ${datos.diaActual} y gastaste el ${pctGastado.toInt()}% del presupuesto. El ritmo está un poco alto.")
            ritmo < -10 -> sb.append("✅ Vas muy bien: solo el ${pctGastado.toInt()}% del presupuesto con el ${pctPeriodo.toInt()}% del mes recorrido.")
            else -> sb.append("El ritmo de gasto va acorde al período: ${pctGastado.toInt()}% del presupuesto en el día ${datos.diaActual}.")
        }

        // Categoría más alta con tendencia
        if (datos.categoriaMasAlta.isNotBlank()) {
            val tendenciaTexto = when {
                datos.cambioCategoriaMasAlta > 30  -> " — ↑ subió ${datos.cambioCategoriaMasAlta.toInt()}% vs el mes pasado"
                datos.cambioCategoriaMasAlta > 0   -> " — levemente por encima del mes anterior"
                datos.cambioCategoriaMasAlta < -15 -> " — ↓ bajó ${(-datos.cambioCategoriaMasAlta).toInt()}% vs el mes pasado"
                else                               -> ""
            }
            sb.append(" Tu mayor gasto es ${datos.categoriaMasAlta}$tendenciaTexto.")
        }

        // Gastos hormiga
        if (datos.gastosHormigaTotal > 0) {
            val fmt = { n: Double -> "%,.0f".format(n) }
            sb.append(" Los gastos pequeños suman \$${fmt(datos.gastosHormigaTotal)} en ${datos.gastosHormigaCantidad} compras — invisibles pero significativos.")
        }

        // Presupuestos excedidos
        if (datos.presupuestosExcedidos.isNotEmpty()) {
            val cats = datos.presupuestosExcedidos.take(2).joinToString(" y ")
            sb.append(" 🔴 Presupuesto excedido en: $cats.")
        }

        return ResumenIa(texto = sb.toString().trim(), proveedor = "local")
    }

    // ──────────────────────────────
    // Construcción del prompt
    // ──────────────────────────────

    private fun buildPromptAnalisis(datos: DatosAnalisisMes): String {
        val fmt = { n: Double -> "%,.0f".format(n) }
        val pctGastado = if (datos.presupuestoTotal > 0) datos.gastoActual / datos.presupuestoTotal * 100 else 0.0
        val excedidosTexto = if (datos.presupuestosExcedidos.isEmpty()) "ninguno"
                             else datos.presupuestosExcedidos.joinToString(", ")

        return """
Período: ${datos.periodo} (día ${datos.diaActual} de ${datos.diasTotales})
Presupuesto total: ${'$'}${fmt(datos.presupuestoTotal)}
Gastado: ${'$'}${fmt(datos.gastoActual)} (${pctGastado.toInt()}%)
Proyección fin de mes: ${'$'}${fmt(datos.proyeccionFinMes)}
Ingresos registrados: ${'$'}${fmt(datos.ingresos)}
Tasa de ahorro: ${datos.tasaAhorro.toInt()}%
Categoría con mayor gasto: ${datos.categoriaMasAlta} (cambio vs mes anterior: ${datos.cambioCategoriaMasAlta.toInt()}%)
Gastos hormiga (pequeños recurrentes): ${'$'}${fmt(datos.gastosHormigaTotal)} en ${datos.gastosHormigaCantidad} transacciones
Tendencia general vs mes anterior: ${datos.tendenciaVsMesAnterior.toInt()}%
Presupuestos excedidos: $excedidosTexto
        """.trimIndent()
    }

    companion object {
        private const val SYSTEM_PROMPT = """Eres un asesor financiero personal amigable y directo. 
Analiza los datos del mes y escribe UN párrafo de 3 frases máximo, en español informal y personal.
Sin saludos, sin listas, sin emojis. Solo texto directo y útil.
Enfócate en: el ritmo del gasto vs el período, el mayor riesgo, y una acción concreta."""
    }
}
