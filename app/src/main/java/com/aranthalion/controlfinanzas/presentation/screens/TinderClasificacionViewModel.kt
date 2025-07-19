package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.TinderClasificacionService
import com.aranthalion.controlfinanzas.data.repository.TinderPreloadService
import com.aranthalion.controlfinanzas.data.util.ExcelTransaction
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.domain.clasificacion.SugerenciaClasificacion
import com.aranthalion.controlfinanzas.domain.clasificacion.ResultadoClasificacion
import com.aranthalion.controlfinanzas.presentation.components.TransaccionTinder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

data class SugerenciaCategoria(
    val categoriaId: Long,
    val nombre: String,
    val nivelConfianza: Double,
    val patron: String,
    val tipoCoincidencia: String,
    val esSeleccionada: Boolean = false
)

data class TinderClasificacionUiState(
    val transaccionesPendientes: List<TransaccionTinder> = emptyList(),
    val transaccionActual: TransaccionTinder? = null,
    val sugerenciasCategorias: List<SugerenciaCategoria> = emptyList(),
    val categoriaSeleccionada: SugerenciaCategoria? = null,
    val mostrarSelectorManual: Boolean = false,
    val categoriasDisponibles: List<Categoria> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val estadisticas: EstadisticasTinder = EstadisticasTinder(),
    val mostrarTinder: Boolean = false,
    val mostrarFeedback: Boolean = false,
    val mensajeFeedback: String = ""
)

data class EstadisticasTinder(
    val totalProcesadas: Int = 0,
    val aceptadas: Int = 0,
    val rechazadas: Int = 0,
    val pendientes: Int = 0
)

@HiltViewModel
class TinderClasificacionViewModel @Inject constructor(
    // Eliminar la inyección de tinderLogic
) : ViewModel() {
    private val tinderLogic = TinderClasificacionLogic()
    private val _uiState = MutableStateFlow(TinderClasificacionUiState())
    val uiState: StateFlow<TinderClasificacionUiState> = _uiState.asStateFlow()

    // El ViewModel solo orquesta, no ejecuta lógica automática ni precarga
    // Métodos públicos para la UI:
    fun cargarTransaccionesEspecificas(transacciones: List<ExcelTransaction>) = tinderLogic.cargarTransaccionesEspecificas(transacciones, _uiState)
    fun aceptarTransaccion() = tinderLogic.aceptarTransaccion(_uiState)
    fun rechazarTransaccion() = tinderLogic.rechazarTransaccion(_uiState)
    fun seleccionarCategoria(id: Long) = tinderLogic.seleccionarCategoria(id, _uiState)
    fun mostrarSelectorManual() = tinderLogic.mostrarSelectorManual(_uiState)
    fun confirmarClasificacion() = tinderLogic.confirmarClasificacion(_uiState)
    fun seleccionarCategoriaManual(id: Long) = tinderLogic.seleccionarCategoriaManual(id, _uiState)
    fun rechazarYSeleccionarManual(categoriaId: Long) = tinderLogic.seleccionarCategoriaManual(categoriaId, _uiState)
    fun ocultarSelectorManual() = tinderLogic.mostrarSelectorManual(_uiState)
}

// Extensión para convertir ExcelTransaction a MovimientoEntity
private fun ExcelTransaction.toMovimientoEntity(categoriaId: Long? = null) = 
    com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity(
        id = 0,
        idUnico = codigoReferencia ?: "",
        descripcion = descripcion,
        monto = monto,
        fecha = fecha ?: java.util.Date(),
        tipo = if (monto > 0) "GASTO" else "INGRESO",
        categoriaId = categoriaId,
        periodoFacturacion = periodoFacturacion ?: "",
        fechaActualizacion = System.currentTimeMillis(),
        metodoActualizacion = "TINDER_CLASIFICACION",
        daoResponsable = "TinderClasificacionViewModel"
    ) 