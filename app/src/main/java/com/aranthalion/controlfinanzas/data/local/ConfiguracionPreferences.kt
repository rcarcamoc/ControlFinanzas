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
    }

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
} 