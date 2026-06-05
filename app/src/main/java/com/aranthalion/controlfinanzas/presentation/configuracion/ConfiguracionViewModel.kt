package com.aranthalion.controlfinanzas.presentation.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.remote.sync.SyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TemaApp {
    NARANJA, AZUL, VERDE
}

@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val prefs: ConfiguracionPreferences,
    private val syncService: SyncService
) : ViewModel() {
    private val _temaSeleccionado = MutableStateFlow(TemaApp.NARANJA)
    val temaSeleccionado: StateFlow<TemaApp> = _temaSeleccionado.asStateFlow()

    private val _aiEnabled = MutableStateFlow(prefs.aiEnabled)
    val aiEnabled: StateFlow<Boolean> = _aiEnabled.asStateFlow()

    private val _groqApiKey = MutableStateFlow(prefs.groqApiKey)
    val groqApiKey: StateFlow<String> = _groqApiKey.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.geminiApiKey)
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _aiProvider = MutableStateFlow(prefs.aiProvider)
    val aiProvider: StateFlow<String> = _aiProvider.asStateFlow()

    // Sync state flows
    private val _syncEnabled = MutableStateFlow(prefs.syncEnabled)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    private val _syncServerUrl = MutableStateFlow(prefs.syncServerUrl)
    val syncServerUrl: StateFlow<String> = _syncServerUrl.asStateFlow()

    private val _syncHouseholdId = MutableStateFlow(prefs.syncHouseholdId)
    val syncHouseholdId: StateFlow<String> = _syncHouseholdId.asStateFlow()

    private val _syncEmail = MutableStateFlow(prefs.syncEmail)
    val syncEmail: StateFlow<String> = _syncEmail.asStateFlow()

    private val _syncPassword = MutableStateFlow(prefs.syncPassword)
    val syncPassword: StateFlow<String> = _syncPassword.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(prefs.lastSyncTimestamp)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    init {
        prefs.obtenerTema().onEach { tema ->
            _temaSeleccionado.value = tema
        }.launchIn(viewModelScope)
    }

    fun cambiarTema(tema: TemaApp) {
        viewModelScope.launch {
            prefs.guardarTema(tema)
            _temaSeleccionado.value = tema
        }
    }

    fun guardarAiConfig(enabled: Boolean, groqKey: String, geminiKey: String, provider: String) {
        prefs.aiEnabled = enabled
        prefs.groqApiKey = groqKey.trim()
        prefs.geminiApiKey = geminiKey.trim()
        prefs.aiProvider = provider

        _aiEnabled.value = enabled
        _groqApiKey.value = groqKey.trim()
        _geminiApiKey.value = geminiKey.trim()
        _aiProvider.value = provider
    }

    fun guardarSyncConfig(enabled: Boolean, url: String, householdId: String, email: String, password: String) {
        prefs.syncEnabled = enabled
        prefs.syncServerUrl = url.trim()
        prefs.syncHouseholdId = householdId.trim()
        prefs.syncEmail = email.trim()
        prefs.syncPassword = password.trim()

        _syncEnabled.value = enabled
        _syncServerUrl.value = url.trim()
        _syncHouseholdId.value = householdId.trim()
        _syncEmail.value = email.trim()
        _syncPassword.value = password.trim()
    }

    fun cargarConfiguracion() {
        _syncEnabled.value = prefs.syncEnabled
        _syncServerUrl.value = prefs.syncServerUrl
        _syncHouseholdId.value = prefs.syncHouseholdId
        _syncEmail.value = prefs.syncEmail
        _syncPassword.value = prefs.syncPassword
        _lastSyncTimestamp.value = prefs.lastSyncTimestamp
        _aiEnabled.value = prefs.aiEnabled
        _groqApiKey.value = prefs.groqApiKey
        _geminiApiKey.value = prefs.geminiApiKey
        _aiProvider.value = prefs.aiProvider

        // Si acabamos de vincular por deep link (tenemos overwrite action pendiente), sincronizamos de inmediato
        if (prefs.syncEnabled && prefs.syncEmail.isNotBlank() && prefs.syncHouseholdId.isNotBlank() && prefs.syncPassword.isNotBlank() && prefs.syncOverwriteAction.isNotBlank()) {
            ejecutarSincronizacion()
        }
    }

    fun desvincular() {
        prefs.syncEnabled = false
        prefs.syncHouseholdId = ""
        prefs.syncEmail = ""
        prefs.syncPassword = ""
        prefs.lastSyncTimestamp = 0L
        prefs.syncOverwriteAction = ""

        _syncEnabled.value = false
        _syncHouseholdId.value = ""
        _syncEmail.value = ""
        _syncPassword.value = ""
        _lastSyncTimestamp.value = 0L
        _syncStatus.value = "Desvinculado con éxito."
    }

    fun ejecutarSincronizacion() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Sincronizando..."
            val result = syncService.sincronizar()
            _isSyncing.value = false
            result.onSuccess { msg ->
                _syncStatus.value = msg
                _lastSyncTimestamp.value = prefs.lastSyncTimestamp
            }.onFailure { err ->
                _syncStatus.value = "Error: ${err.message ?: "Falló la conexión"}"
            }
        }
    }

    fun obtenerColoresTema(tema: TemaApp): Map<String, Int> {
        return when (tema) {
            TemaApp.NARANJA -> mapOf(
                "primary" to android.R.color.holo_orange_dark,
                "primaryVariant" to android.R.color.holo_orange_light,
                "secondary" to android.R.color.holo_orange_light,
                "background" to android.R.color.white,
                "surface" to android.R.color.white
            )
            TemaApp.AZUL -> mapOf(
                "primary" to android.R.color.holo_blue_dark,
                "primaryVariant" to android.R.color.holo_blue_light,
                "secondary" to android.R.color.holo_blue_light,
                "background" to android.R.color.white,
                "surface" to android.R.color.white
            )
            TemaApp.VERDE -> mapOf(
                "primary" to android.R.color.holo_green_dark,
                "primaryVariant" to android.R.color.holo_green_light,
                "secondary" to android.R.color.holo_green_light,
                "background" to android.R.color.white,
                "surface" to android.R.color.white
            )
        }
    }
} 