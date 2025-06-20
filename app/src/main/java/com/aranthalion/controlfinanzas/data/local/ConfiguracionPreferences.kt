package com.aranthalion.controlfinanzas.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aranthalion.controlfinanzas.presentation.configuracion.TemaApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.configuracionDataStore by preferencesDataStore(name = "configuracion")

class ConfiguracionPreferences(private val context: Context) {
    companion object {
        val TEMA_KEY = stringPreferencesKey("tema_app")
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
} 