package com.aranthalion.controlfinanzas.data.remote.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.clasificacion.ResultadoClasificacion
import com.aranthalion.controlfinanzas.domain.clasificacion.SugerenciaClasificacion
import com.aranthalion.controlfinanzas.domain.clasificacion.TipoCoincidencia
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
class GeminiClasificadorService @Inject constructor(
    private val configuracionPreferences: ConfiguracionPreferences
) {
    private val gson = Gson()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Clasifica una transacción usando IA.
     * Intenta usar el proveedor preferido (Groq Llama 3 por defecto) y cae a Gemini como fallback.
     */
    suspend fun obtenerClasificacionIA(
        descripcion: String,
        categorias: List<Categoria>
    ): ResultadoClasificacion = withContext(Dispatchers.IO) {
        if (!configuracionPreferences.aiEnabled) {
            return@withContext ResultadoClasificacion.SinCoincidencias(
                descripcion = descripcion,
                razon = "La Inteligencia Artificial está desactivada en la configuración."
            )
        }

        val proveedorPreferido = configuracionPreferences.aiProvider
        val categoriasJson = categorias.map { mapOf("id" to it.id, "nombre" to it.nombre) }
        val categoriesListText = gson.toJson(categoriasJson)

        // 1. Intentar con Groq si está habilitado y es el preferido (o el fallback primario)
        if (proveedorPreferido != "gemini") {
            val groqKey = configuracionPreferences.groqApiKey
            if (groqKey.isNotBlank()) {
                try {
                    val responseText = llamarGroq(descripcion, categoriesListText, groqKey)
                    val sugerencias = parsearRespuestaMultiple(responseText, descripcion)
                    if (sugerencias.isNotEmpty()) {
                        return@withContext crearResultadoClasificacion(sugerencias, descripcion)
                    }
                } catch (e: Exception) {
                    Log.w("GeminiClasificador", "Clasificación con Groq falló, intentando fallback Gemini: ${e.message}")
                }
            }
        }

        // 2. Fallback: Intentar con Gemini
        val geminiKey = configuracionPreferences.geminiApiKey
        if (geminiKey.isNotBlank()) {
            try {
                val prompt = buildPrompt(descripcion, categoriesListText)
                val config = generationConfig {
                    responseMimeType = "application/json"
                    temperature = 0.1f
                }
                val model = GenerativeModel(
                    modelName = "gemini-2.0-flash",
                    apiKey = geminiKey,
                    generationConfig = config
                )
                val response = model.generateContent(prompt)
                val responseText = response.text ?: ""
                val sugerencias = parsearRespuestaMultiple(responseText, descripcion)
                if (sugerencias.isNotEmpty()) {
                    return@withContext crearResultadoClasificacion(sugerencias, descripcion)
                }
            } catch (e: Exception) {
                Log.e("GeminiClasificador", "Clasificación con fallback Gemini falló: ${e.message}")
            }
        }

        // 3. Si ambos fallan
        ResultadoClasificacion.SinCoincidencias(
            descripcion = descripcion,
            razon = "No hay proveedores de IA configurados o todos fallaron."
        )
    }

    private fun llamarGroq(descripcion: String, categoriesListText: String, apiKey: String): String {
        val prompt = buildPrompt(descripcion, categoriesListText)
        val payload = gson.toJson(mapOf(
            "model" to "llama-3.3-70b-versatile",
            "messages" to listOf(
                mapOf("role" to "system", "content" to "Eres un asistente experto en clasificación financiera con comprensión semántica profunda. Solo respondes con un array JSON puro, sin formato markdown ni texto explicativo."),
                mapOf("role" to "user", "content" to prompt)
            ),
            "temperature" to 0.1,
            "max_tokens" to 500
        ))

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
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
    }

    private fun crearResultadoClasificacion(
        sugerencias: List<SugerenciaClasificacion>,
        descripcion: String
    ): ResultadoClasificacion {
        val mejor = sugerencias.first()
        return if (mejor.nivelConfianza >= 0.7) {
            ResultadoClasificacion.AltaConfianza(
                categoriaId = mejor.categoriaId,
                confianza = mejor.nivelConfianza,
                patron = descripcion,
                tipoCoincidencia = TipoCoincidencia.PATRON,
                sugerenciasAlternativas = sugerencias.drop(1)
            )
        } else {
            ResultadoClasificacion.BajaConfianza(
                sugerencias = sugerencias,
                confianzaMaxima = mejor.nivelConfianza
            )
        }
    }

    private fun buildPrompt(descripcion: String, categoriesListText: String): String {
        return """
            Analiza la descripción de una transacción bancaria y sugiere las 3 categorías MÁS PROBABLES de la lista disponible, ordenadas por relevancia.
            
            Descripción de la transacción: "$descripcion"
            
            Categorías disponibles (formato JSON):
            $categoriesListText
            
            Instrucciones de clasificación semántica:
            1. Analiza QUÉ TIPO de negocio, comercio o gasto representa la descripción.
            2. Identifica marcas comerciales conocidas y prioriza su categoría real:
               - Supermercados / Almacén: "Santa Isabel", "Jumbo", "Lider", "Unimarc", "Oxxo", "Tottus", "Mayorista 10", "Alvi"
               - Hogar y Construcción: "Sodimac", "Easy", "Homecenter", "Construmart"
               - Farmacias / Salud: "Cruz Verde", "Ahumada", "Salcobrand", "Dr. Simi"
               - Combustible / Transporte: "Copec", "Shell", "Petrobras", "Upa", "Pronto"
               - Retail / Tiendas: "Falabella", "Ripley", "Paris", "Hites", "La Polar"
            3. FILTRA Y DESESTIMA ubicaciones secundarias, nombres de calles o sucursales:
               - Si la transacción dice "santa isabel san diego santiago", identifica "Santa Isabel" como la marca principal (Supermercado). "san diego" (calle) y "santiago" (comuna) son solo la dirección de la sucursal. NO lo clasifiques como "gastos comunes" ni "arriendo de departamento".
               - Si la transacción dice "copec providencia", clasifícalo como Combustible/Transporte, no como gastos de la comuna.
            4. Considera similitudes conceptuales:
               - Una panadería, pastelería, carnicería, verdulería son tipos de "almacén" o "supermercado".
               - Un restaurante, café, bar son "alimentación" o "restaurantes".
               - Servicios digitales como streaming son "entretenimiento" o "suscripciones".
            5. NO te limites a buscar coincidencias textuales exactas. Piensa en la NATURALEZA del gasto.
            6. Si no existe una categoría perfecta, sugiere la más cercana conceptualmente.
            7. Para cada sugerencia, asigna una confianza realista (0.0 a 1.0).
            
            Retorna un array JSON con exactamente 3 objetos, cada uno con:
            - "categoriaId": (número) ID de la categoría sugerida
            - "confianza": (número flotante entre 0.0 y 1.0) confianza en la predicción
            - "justificacion": (texto corto) explicación semántica de por qué elegiste esta categoría
            
            Ordena de mayor a menor confianza. No repitas categorías. Solo retorna el array JSON puro, sin bloques de código markdown ```json ... ``` ni explicaciones externas.
        """.trimIndent()
    }

    /**
     * Parsea la respuesta de la IA que viene como array de sugerencias
     */
    private fun parsearRespuestaMultiple(
        responseText: String,
        descripcion: String
    ): List<SugerenciaClasificacion> {
        val cleanJson = responseText.trim()
            .replace("^```json".toRegex(), "")
            .replace("^```".toRegex(), "")
            .replace("```$".toRegex(), "")
            .trim()

        return try {
            val listType = object : TypeToken<List<GeminiSugerencia>>() {}.type
            val sugerencias: List<GeminiSugerencia> = gson.fromJson(cleanJson, listType)
            
            sugerencias
                .filter { it.categoriaId != null && it.confianza > 0.0 }
                .map { sugerencia ->
                    SugerenciaClasificacion(
                        categoriaId = sugerencia.categoriaId!!,
                        nivelConfianza = sugerencia.confianza.coerceIn(0.0, 1.0),
                        patron = descripcion
                    )
                }
                .sortedByDescending { it.nivelConfianza }
        } catch (e: Exception) {
            Log.w("GeminiClasificador", "Error parseando array, intentando formato simple", e)
            try {
                val single = gson.fromJson(cleanJson, GeminiSugerencia::class.java)
                if (single?.categoriaId != null) {
                    listOf(
                        SugerenciaClasificacion(
                            categoriaId = single.categoriaId!!,
                            nivelConfianza = single.confianza.coerceIn(0.0, 1.0),
                            patron = descripcion
                        )
                    )
                } else emptyList()
            } catch (e2: Exception) {
                Log.e("GeminiClasificador", "Error total al parsear respuesta", e2)
                emptyList()
            }
        }
    }

    private data class GeminiSugerencia(
        val categoriaId: Long?,
        val confianza: Double,
        val justificacion: String = ""
    )
}
