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

enum class EstadoImportacion {
    NUEVO,
    DUPLICADO_EXACTO,
    SUGERENCIA_FUSION
}

data class EmailImportItem(
    val movimiento: MovimientoEntity,
    val estado: EstadoImportacion,
    val matchExistente: MovimientoEntity? = null
)

sealed interface EmailSyncUiState {
    object Idle : EmailSyncUiState
    object Loading : EmailSyncUiState
    data class Success(val movimientos: List<EmailImportItem>) : EmailSyncUiState
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

    private val _emailConfigs = MutableStateFlow<List<EmailConfig>>(emptyList())
    val emailConfigs: StateFlow<List<EmailConfig>> = _emailConfigs.asStateFlow()

    private val _testingConnection = MutableStateFlow(false)
    val testingConnection: StateFlow<Boolean> = _testingConnection.asStateFlow()

    private val _connectionResult = MutableStateFlow<Boolean?>(null)
    val connectionResult: StateFlow<Boolean?> = _connectionResult.asStateFlow()

    init {
        loadConfigs()
    }

    fun loadConfigs() {
        _emailConfigs.value = configuracionPreferences.obtenerEmailConfigs()
    }

    fun addEmailConfig(config: EmailConfig) {
        val current = _emailConfigs.value.toMutableList()
        current.add(config)
        saveConfigs(current)
    }

    fun updateEmailConfig(config: EmailConfig) {
        val current = _emailConfigs.value.map { if (it.id == config.id) config else it }
        saveConfigs(current)
    }

    fun deleteEmailConfig(id: String) {
        val current = _emailConfigs.value.filter { it.id != id }
        saveConfigs(current)
    }

    fun toggleEmailConfig(id: String, enabled: Boolean) {
        val current = _emailConfigs.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
        saveConfigs(current)
    }

    private fun saveConfigs(configs: List<EmailConfig>) {
        _emailConfigs.value = configs
        configuracionPreferences.guardarEmailConfigs(configs)
    }

    fun testConnectionForConfig(config: EmailConfig, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _testingConnection.value = true
            val success = emailFetcherService.testConnection(config)
            onResult(success)
            _testingConnection.value = false
        }
    }

    fun fetchEmails() {
        viewModelScope.launch {
            _uiState.value = EmailSyncUiState.Loading
            try {
                val results = emailFetcherService.fetchTransactionsFromEmail()
                val existentes = gestionarMovimientosUseCase.obtenerMovimientos()
                
                val items = results.map { nuevo ->
                    // 1. Coincidencia exacta de idUnico o mismos datos exactos
                    if (existentes.any { it.idUnico == nuevo.idUnico }) {
                        return@map EmailImportItem(nuevo, EstadoImportacion.DUPLICADO_EXACTO)
                    }
                    
                    val match = buscarCoincidencia(nuevo, existentes)
                    if (match != null) {
                        val extCal = java.util.Calendar.getInstance().apply { time = match.fecha }
                        val extHoraCero = extCal.get(java.util.Calendar.HOUR_OF_DAY) == 0 && extCal.get(java.util.Calendar.MINUTE) == 0
                        val nuevoCal = java.util.Calendar.getInstance().apply { time = nuevo.fecha }
                        val nuevoHoraNoCero = nuevoCal.get(java.util.Calendar.HOUR_OF_DAY) != 0 || nuevoCal.get(java.util.Calendar.MINUTE) != 0
                        
                        val faltaTarjeta = match.tipoTarjeta.isNullOrBlank() && !nuevo.tipoTarjeta.isNullOrBlank()
                        val faltaHora = extHoraCero && nuevoHoraNoCero
                        
                        if (faltaTarjeta || faltaHora) {
                            EmailImportItem(nuevo, EstadoImportacion.SUGERENCIA_FUSION, match)
                        } else {
                            EmailImportItem(nuevo, EstadoImportacion.DUPLICADO_EXACTO)
                        }
                    } else {
                        // Fallback checking duplicate
                        if (esDuplicado(nuevo, existentes)) {
                            EmailImportItem(nuevo, EstadoImportacion.DUPLICADO_EXACTO)
                        } else {
                            EmailImportItem(nuevo, EstadoImportacion.NUEVO)
                        }
                    }
                }
                
                _uiState.value = EmailSyncUiState.Success(items)
            } catch (e: Exception) {
                _uiState.value = EmailSyncUiState.Error(e.message ?: "Error al descargar transacciones de correos.")
            }
        }
    }

    private fun buscarCoincidencia(nuevo: MovimientoEntity, existentes: List<MovimientoEntity>): MovimientoEntity? {
        val descNueva = nuevo.descripcion.lowercase().trim()
        val comercioNuevo = descNueva
            .replace("importado correo:", "")
            .replace("importado excel:", "")
            .trim()

        for (existente in existentes) {
            if (existente.monto == nuevo.monto && existente.tipo == nuevo.tipo) {
                // Comprobamos si la diferencia absoluta en fechas es menor o igual a 30 días
                val diffMillis = Math.abs(nuevo.fecha.time - existente.fecha.time)
                val diffDays = diffMillis / (1000L * 60 * 60 * 24)
                if (diffDays <= 30) {
                    val descExistente = existente.descripcion.lowercase().trim()
                    val comercioExistente = descExistente
                        .replace("importado correo:", "")
                        .replace("importado excel:", "")
                        .trim()

                    // Si coinciden en comercio o si uno contiene al otro
                    if (comercioExistente.isNotEmpty() && comercioNuevo.isNotEmpty() && (
                        comercioExistente.contains(comercioNuevo) || 
                        comercioNuevo.contains(comercioExistente) ||
                        comercioExistente.take(5) == comercioNuevo.take(5)
                    )) {
                        return existente
                    }
                }
            }
        }
        return null
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

    fun importarMovimientos(items: List<EmailImportItem>) {
        viewModelScope.launch {
            _uiState.value = EmailSyncUiState.Loading
            try {
                var guardados = 0
                var actualizados = 0
                for (item in items) {
                    when (item.estado) {
                        EstadoImportacion.NUEVO -> {
                            gestionarMovimientosUseCase.agregarMovimiento(item.movimiento)
                            guardados++
                        }
                        EstadoImportacion.SUGERENCIA_FUSION -> {
                            val match = item.matchExistente
                            if (match != null) {
                                val tarjetaActualizada = if (match.tipoTarjeta.isNullOrBlank()) item.movimiento.tipoTarjeta else match.tipoTarjeta
                                
                                val extCal = java.util.Calendar.getInstance().apply { time = match.fecha }
                                val extHoraCero = extCal.get(java.util.Calendar.HOUR_OF_DAY) == 0 && extCal.get(java.util.Calendar.MINUTE) == 0
                                val fechaActualizada = if (extHoraCero) {
                                    val nuevoCal = java.util.Calendar.getInstance().apply { time = item.movimiento.fecha }
                                    java.util.Calendar.getInstance().apply {
                                        time = match.fecha
                                        set(java.util.Calendar.HOUR_OF_DAY, nuevoCal.get(java.util.Calendar.HOUR_OF_DAY))
                                        set(java.util.Calendar.MINUTE, nuevoCal.get(java.util.Calendar.MINUTE))
                                        set(java.util.Calendar.SECOND, nuevoCal.get(java.util.Calendar.SECOND))
                                    }.time
                                } else {
                                    match.fecha
                                }
                                
                                val movimientoActualizado = match.copy(
                                    tipoTarjeta = tarjetaActualizada,
                                    fecha = fechaActualizada,
                                    fechaActualizacion = System.currentTimeMillis(),
                                    metodoActualizacion = "MERGE_EMAIL"
                                )
                                gestionarMovimientosUseCase.actualizarMovimiento(movimientoActualizado)
                                actualizados++
                            }
                        }
                        EstadoImportacion.DUPLICADO_EXACTO -> {
                            // Ignorar duplicados
                        }
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
