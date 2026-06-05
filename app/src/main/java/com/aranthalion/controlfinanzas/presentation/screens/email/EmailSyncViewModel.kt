package com.aranthalion.controlfinanzas.presentation.screens.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.remote.email.EmailConfig
import com.aranthalion.controlfinanzas.data.remote.email.EmailFetcherService
import com.aranthalion.controlfinanzas.domain.usecase.GestionarMovimientosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EmailSyncUiState {
    object Idle : EmailSyncUiState
    object Loading : EmailSyncUiState
    data class Success(val movimientos: List<MovimientoEntity>) : EmailSyncUiState
    data class Error(val message: String) : EmailSyncUiState
}

@HiltViewModel
class EmailSyncViewModel @Inject constructor(
    private val emailFetcherService: EmailFetcherService,
    private val configuracionPreferences: ConfiguracionPreferences,
    private val gestionarMovimientosUseCase: GestionarMovimientosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<EmailSyncUiState>(EmailSyncUiState.Idle)
    val uiState: StateFlow<EmailSyncUiState> = _uiState.asStateFlow()

    private val _emailConfig = MutableStateFlow(configuracionPreferences.obtenerEmailConfig())
    val emailConfig: StateFlow<EmailConfig> = _emailConfig.asStateFlow()

    private val _testingConnection = MutableStateFlow(false)
    val testingConnection: StateFlow<Boolean> = _testingConnection.asStateFlow()

    private val _connectionResult = MutableStateFlow<Boolean?>(null)
    val connectionResult: StateFlow<Boolean?> = _connectionResult.asStateFlow()

    fun updateConfig(config: EmailConfig) {
        _emailConfig.value = config
        configuracionPreferences.guardarEmailConfig(config)
    }

    fun testConnection() {
        viewModelScope.launch {
            _testingConnection.value = true
            _connectionResult.value = null
            val success = emailFetcherService.testConnection(_emailConfig.value)
            _connectionResult.value = success
            _testingConnection.value = false
        }
    }

    fun fetchEmails() {
        viewModelScope.launch {
            _uiState.value = EmailSyncUiState.Loading
            try {
                val results = emailFetcherService.fetchTransactionsFromEmail()
                _uiState.value = EmailSyncUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = EmailSyncUiState.Error(e.message ?: "Error al descargar transacciones de correos.")
            }
        }
    }

    fun importarMovimientos(movimientos: List<MovimientoEntity>) {
        viewModelScope.launch {
            _uiState.value = EmailSyncUiState.Loading
            try {
                val existentes = gestionarMovimientosUseCase.obtenerIdUnicos()
                var guardados = 0
                for (mov in movimientos) {
                    if (mov.idUnico !in existentes) {
                        gestionarMovimientosUseCase.agregarMovimiento(mov)
                        guardados++
                    }
                }
                _uiState.value = EmailSyncUiState.Idle
                // Refrescar la descarga
                fetchEmails()
            } catch (e: Exception) {
                _uiState.value = EmailSyncUiState.Error(e.message ?: "Error al guardar transacciones en la base de datos.")
            }
        }
    }
}
