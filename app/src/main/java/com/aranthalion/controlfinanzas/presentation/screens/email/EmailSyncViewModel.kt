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

    private fun esDuplicado(nuevo: MovimientoEntity, existentes: List<MovimientoEntity>): Boolean {
        // 1. Coincidencia exacta de idUnico
        if (existentes.any { it.idUnico == nuevo.idUnico }) return true

        // 2. Coincidencia por monto, tipo y fecha (mismo día ignorando hora)
        val nuevoCal = java.util.Calendar.getInstance().apply { time = nuevo.fecha }
        val nuevoYear = nuevoCal.get(java.util.Calendar.YEAR)
        val nuevoMonth = nuevoCal.get(java.util.Calendar.MONTH)
        val nuevoDay = nuevoCal.get(java.util.Calendar.DAY_OF_MONTH)

        val descNueva = nuevo.descripcion.lowercase().trim()
        val comercioNuevo = descNueva
            .replace("importado correo:", "")
            .replace("importado excel:", "")
            .trim()

        for (existente in existentes) {
            if (existente.monto == nuevo.monto && existente.tipo == nuevo.tipo) {
                val extCal = java.util.Calendar.getInstance().apply { time = existente.fecha }
                val extYear = extCal.get(java.util.Calendar.YEAR)
                val extMonth = extCal.get(java.util.Calendar.MONTH)
                val extDay = extCal.get(java.util.Calendar.DAY_OF_MONTH)

                if (nuevoYear == extYear && nuevoMonth == extMonth && nuevoDay == extDay) {
                    val descExistente = existente.descripcion.lowercase().trim()
                    val comercioExistente = descExistente
                        .replace("importado correo:", "")
                        .replace("importado excel:", "")
                        .trim()

                    // Si coinciden en comercio o si uno contiene al otro
                    if (comercioExistente.isEmpty() || comercioNuevo.isEmpty() ||
                        comercioExistente.contains(comercioNuevo) || 
                        comercioNuevo.contains(comercioExistente) ||
                        comercioExistente.take(5) == comercioNuevo.take(5)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun importarMovimientos(movimientos: List<MovimientoEntity>) {
        viewModelScope.launch {
            _uiState.value = EmailSyncUiState.Loading
            try {
                val existentes = gestionarMovimientosUseCase.obtenerMovimientos()
                var guardados = 0
                for (mov in movimientos) {
                    if (!esDuplicado(mov, existentes)) {
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
