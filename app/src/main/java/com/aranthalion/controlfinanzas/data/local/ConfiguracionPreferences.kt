package com.aranthalion.controlfinanzas.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aranthalion.controlfinanzas.presentation.configuracion.TemaApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.content.SharedPreferences
import javax.inject.Inject
import com.google.gson.Gson

val Context.configuracionDataStore by preferencesDataStore(name = "configuracion")

class ConfiguracionPreferences @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("configuracion", Context.MODE_PRIVATE)
    
    companion object {
        val TEMA_KEY = stringPreferencesKey("tema_app")
        private const val KEY_PERIODO_GLOBAL = "periodo_global"
        private const val KEY_DATOS_CARGADOS = "datos_cargados"
        private const val KEY_CLASIFICACION_CARGADA = "clasificacion_cargada"
        private const val KEY_IS_FIRST_RUN = "is_first_run"
        private const val KEY_HISTORICAL_DATA_LOADED = "historical_data_loaded"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GROQ_API_KEY = "groq_api_key"
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_AI_PROVIDER = "ai_provider" // "groq" | "gemini" | "local"
        private const val KEY_SYNC_SERVER_URL = "sync_server_url"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_SYNC_HOUSEHOLD_ID = "sync_household_id"
        private const val KEY_SYNC_EMAIL = "sync_email"
        private const val KEY_SYNC_PASSWORD = "sync_password"
        private const val KEY_SYNC_OVERWRITE_ACTION = "sync_overwrite_action"
        private const val KEY_SYNC_HOUSEHOLD_NAME = "sync_household_name"
    }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GROQ_API_KEY, value).apply()

    var aiEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AI_ENABLED, value).apply()

    var aiProvider: String
        get() = prefs.getString(KEY_AI_PROVIDER, "groq") ?: "groq"
        set(value) = prefs.edit().putString(KEY_AI_PROVIDER, value).apply()

    var syncServerUrl: String
        get() = prefs.getString(KEY_SYNC_SERVER_URL, "http://129.151.113.195/finanzas/api/sync") ?: "http://129.151.113.195/finanzas/api/sync"
        set(value) = prefs.edit().putString(KEY_SYNC_SERVER_URL, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, value).apply()

    var syncEnabled: Boolean
        get() = prefs.getBoolean(KEY_SYNC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SYNC_ENABLED, value).apply()

    var syncHouseholdId: String
        get() = prefs.getString(KEY_SYNC_HOUSEHOLD_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SYNC_HOUSEHOLD_ID, value).apply()

    var syncHouseholdName: String
        get() = prefs.getString(KEY_SYNC_HOUSEHOLD_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SYNC_HOUSEHOLD_NAME, value).apply()

    var syncEmail: String
        get() = prefs.getString(KEY_SYNC_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SYNC_EMAIL, value).apply()

    var syncPassword: String
        get() = prefs.getString(KEY_SYNC_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SYNC_PASSWORD, value).apply()

    var syncOverwriteAction: String
        get() = prefs.getString(KEY_SYNC_OVERWRITE_ACTION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SYNC_OVERWRITE_ACTION, value).apply()

    fun obtenerTema(): Flow<TemaApp> = context.configuracionDataStore.data.map { preferences: Preferences ->
        when (preferences[TEMA_KEY]) {
            "AZUL" -> TemaApp.AZUL
            "VERDE" -> TemaApp.VERDE
            else -> TemaApp.NARANJA
        }
    }

    suspend fun guardarTema(tema: TemaApp) {
        context.configuracionDataStore.edit { preferences ->
            preferences[TEMA_KEY] = tema.name
        }
    }

    var periodoGlobal: String
        get() = prefs.getString(KEY_PERIODO_GLOBAL, "2025-01") ?: "2025-01"
        set(value) = prefs.edit().putString(KEY_PERIODO_GLOBAL, value).apply()

    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_RUN, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_FIRST_RUN, value).apply()

    var historicalDataLoaded: Boolean
        get() = prefs.getBoolean(KEY_HISTORICAL_DATA_LOADED, false)
        set(value) = prefs.edit().putBoolean(KEY_HISTORICAL_DATA_LOADED, value).apply()

    fun markFirstRunComplete() {
        isFirstRun = false
    }

    fun markHistoricalDataLoaded() {
        historicalDataLoaded = true
    }

    fun markHistoricalDataNotLoaded() {
        historicalDataLoaded = false
    }

    fun guardarPeriodoGlobal(periodo: String) {
        prefs.edit().putString(KEY_PERIODO_GLOBAL, periodo).apply()
    }
    
    fun obtenerPeriodoGlobal(): String? {
        return prefs.getString(KEY_PERIODO_GLOBAL, null)
    }

    fun guardarPeriodoDatesMap(configs: Map<String, com.aranthalion.controlfinanzas.data.util.BillingPeriodConfig>) {
        val json = Gson().toJson(configs)
        prefs.edit().putString("periodo_dates_map", json).apply()
    }

    fun obtenerPeriodoDatesMap(): Map<String, com.aranthalion.controlfinanzas.data.util.BillingPeriodConfig> {
        val json = prefs.getString("periodo_dates_map", null)
        if (json.isNullOrBlank()) {
            return emptyMap()
        }
        val type = object : com.google.gson.reflect.TypeToken<Map<String, com.aranthalion.controlfinanzas.data.util.BillingPeriodConfig>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    fun guardarDatosCargados(cargados: Boolean) {
        prefs.edit().putBoolean(KEY_DATOS_CARGADOS, cargados).apply()
    }
    
    fun obtenerDatosCargados(): Boolean {
        return prefs.getBoolean(KEY_DATOS_CARGADOS, false)
    }
    
    fun guardarClasificacionCargada(cargada: Boolean) {
        prefs.edit().putBoolean(KEY_CLASIFICACION_CARGADA, cargada).apply()
    }
    
    fun obtenerClasificacionCargada(): Boolean {
        return prefs.getBoolean(KEY_CLASIFICACION_CARGADA, false)
    }

    fun guardarEmailConfig(config: com.aranthalion.controlfinanzas.data.remote.email.EmailConfig) {
        prefs.edit().apply {
            putString("email_host", config.host)
            putInt("email_port", config.port)
            putString("email_username", config.username)
            putString("email_password", config.password)
            putString("email_protocol", config.protocol)
            putBoolean("email_use_ssl", config.useSSL)
        }.apply()
    }

    fun obtenerEmailConfig(): com.aranthalion.controlfinanzas.data.remote.email.EmailConfig {
        return com.aranthalion.controlfinanzas.data.remote.email.EmailConfig(
            host = prefs.getString("email_host", "mail.recc.001webhospedaje.com") ?: "mail.recc.001webhospedaje.com",
            port = prefs.getInt("email_port", 993),
            username = prefs.getString("email_username", "recibemail@recc.001webhospedaje.com") ?: "recibemail@recc.001webhospedaje.com",
            password = prefs.getString("email_password", "Gatochuchu") ?: "Gatochuchu",
            protocol = prefs.getString("email_protocol", "imaps") ?: "imaps",
            useSSL = prefs.getBoolean("email_use_ssl", true)
        )
    }

    fun guardarEmailConfigs(configs: List<com.aranthalion.controlfinanzas.data.remote.email.EmailConfig>) {
        val json = Gson().toJson(configs)
        prefs.edit().putString("email_configs_list_json", json).apply()
    }

    fun obtenerEmailConfigs(): List<com.aranthalion.controlfinanzas.data.remote.email.EmailConfig> {
        val json = prefs.getString("email_configs_list_json", null)
        if (json.isNullOrBlank()) {
            val oldSingle = obtenerEmailConfig()
            val defaultList = listOf(oldSingle)
            guardarEmailConfigs(defaultList)
            return defaultList
        }
        val type = object : com.google.gson.reflect.TypeToken<List<com.aranthalion.controlfinanzas.data.remote.email.EmailConfig>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    var emailSyncAutoEnabled: Boolean
        get() = prefs.getBoolean("email_sync_auto_enabled", false)
        set(value) = prefs.edit().putBoolean("email_sync_auto_enabled", value).apply()

    var emailSyncIntervalMinutes: Int
        get() = prefs.getInt("email_sync_interval_minutes", 30)
        set(value) = prefs.edit().putInt("email_sync_interval_minutes", value).apply()

    fun guardarScopeGlobal(scope: String) {
        prefs.edit().putString("scope_global", scope).apply()
    }
    
    fun obtenerScopeGlobal(): String {
        return prefs.getString("scope_global", "HOUSEHOLD") ?: "HOUSEHOLD"
    }
} 