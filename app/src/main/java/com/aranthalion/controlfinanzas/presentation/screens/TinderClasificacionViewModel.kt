package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.TinderClasificacionService
import com.aranthalion.controlfinanzas.data.util.ExcelTransaction
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.domain.clasificacion.SugerenciaClasificacion
import com.aranthalion.controlfinanzas.presentation.components.TransaccionTinder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TinderClasificacionUiState(
    val transaccionesPendientes: List<TransaccionTinder> = emptyList(),
    val transaccionActual: TransaccionTinder? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val estadisticas: EstadisticasTinder = EstadisticasTinder(),
    val mostrarTinder: Boolean = false
)

data class EstadisticasTinder(
    val totalProcesadas: Int = 0,
    val aceptadas: Int = 0,
    val rechazadas: Int = 0,
    val pendientes: Int = 0
)

@HiltViewModel
class TinderClasificacionViewModel @Inject constructor(
    private val clasificacionUseCase: GestionarClasificacionAutomaticaUseCase,
    private val categoriaRepository: CategoriaRepository,
    private val movimientoRepository: MovimientoRepository,
    private val tinderService: TinderClasificacionService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TinderClasificacionUiState())
    val uiState: StateFlow<TinderClasificacionUiState> = _uiState.asStateFlow()

    /**
     * Procesa una lista de transacciones y encuentra las que tienen sugerencias válidas
     * IMPORTANTE: Todas las transacciones van al Tinder ya que no se asignan categorías automáticamente
     */
    fun procesarTransaccionesParaTinder(transacciones: List<ExcelTransaction>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val categorias = categoriaRepository.obtenerCategorias().map { com.aranthalion.controlfinanzas.data.repository.CategoriaMapper.toEntity(it) }
                val transaccionesTinder = mutableListOf<TransaccionTinder>()
                
                transacciones.forEach { transaccion ->
                    // IMPORTANTE: Procesar TODAS las transacciones ya que ninguna tiene categoría asignada automáticamente
                    val sugerencia = clasificacionUseCase.sugerirCategoria(transaccion.descripcion)
                    if (sugerencia != null && sugerencia.nivelConfianza >= 0.3) {
                        val categoria = categorias.find { it.id == sugerencia.categoriaId }
                        if (categoria != null) {
                            transaccionesTinder.add(
                                TransaccionTinder(
                                    transaccion = transaccion,
                                    categoriaSugerida = categoria,
                                    nivelConfianza = sugerencia.nivelConfianza,
                                    patron = sugerencia.patron
                                )
                            )
                        }
                    } else {
                        // Si no hay sugerencia válida, crear una transacción sin categoría sugerida
                        transaccionesTinder.add(
                            TransaccionTinder(
                                transaccion = transaccion,
                                categoriaSugerida = categorias.firstOrNull() ?: categorias.first(), // Categoría por defecto
                                nivelConfianza = 0.0,
                                patron = "Sin patrón"
                            )
                        )
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    transaccionesPendientes = transaccionesTinder,
                    transaccionActual = transaccionesTinder.firstOrNull(),
                    isLoading = false,
                    mostrarTinder = transaccionesTinder.isNotEmpty(),
                    estadisticas = _uiState.value.estadisticas.copy(
                        pendientes = transaccionesTinder.size
                    )
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al procesar transacciones: ${e.message}"
                )
            }
        }
    }

    /**
     * Acepta la clasificación sugerida para la transacción actual
     */
    fun aceptarClasificacion() {
        viewModelScope.launch {
            val transaccionActual = _uiState.value.transaccionActual ?: return@launch
            
            try {
                // Registrar aceptación en el servicio
                tinderService.registrarAceptacion(transaccionActual)
                
                // Actualizar estadísticas
                val estadisticas = _uiState.value.estadisticas.copy(
                    totalProcesadas = _uiState.value.estadisticas.totalProcesadas + 1,
                    aceptadas = _uiState.value.estadisticas.aceptadas + 1,
                    pendientes = _uiState.value.estadisticas.pendientes - 1
                )
                
                // Pasar a la siguiente transacción
                pasarSiguienteTransaccion(estadisticas)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al aceptar clasificación: ${e.message}"
                )
            }
        }
    }

    /**
     * Rechaza la clasificación sugerida para la transacción actual
     */
    fun rechazarClasificacion() {
        viewModelScope.launch {
            val transaccionActual = _uiState.value.transaccionActual ?: return@launch
            
            try {
                // Registrar rechazo en el servicio
                tinderService.registrarRechazo(transaccionActual)
                
                // Actualizar estadísticas
                val estadisticas = _uiState.value.estadisticas.copy(
                    totalProcesadas = _uiState.value.estadisticas.totalProcesadas + 1,
                    rechazadas = _uiState.value.estadisticas.rechazadas + 1,
                    pendientes = _uiState.value.estadisticas.pendientes - 1
                )
                
                // Pasar a la siguiente transacción
                pasarSiguienteTransaccion(estadisticas)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al rechazar clasificación: ${e.message}"
                )
            }
        }
    }

    /**
     * Pasa a la siguiente transacción en la cola
     */
    private fun pasarSiguienteTransaccion(estadisticas: EstadisticasTinder) {
        val transaccionesRestantes = _uiState.value.transaccionesPendientes.drop(1)
        
        _uiState.value = _uiState.value.copy(
            transaccionesPendientes = transaccionesRestantes,
            transaccionActual = transaccionesRestantes.firstOrNull(),
            estadisticas = estadisticas,
            mostrarTinder = transaccionesRestantes.isNotEmpty()
        )
    }

    /**
     * Cierra el Tinder y guarda las transacciones procesadas
     */
    fun finalizarTinder() {
        viewModelScope.launch {
            try {
                // Aquí podrías guardar las transacciones procesadas en la base de datos
                // Por ahora solo cerramos el Tinder
                
                _uiState.value = _uiState.value.copy(
                    mostrarTinder = false,
                    transaccionActual = null,
                    transaccionesPendientes = emptyList()
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al finalizar: ${e.message}"
                )
            }
        }
    }

    /**
     * Limpia el error actual
     */
    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reinicia el estado del Tinder
     */
    fun reiniciarTinder() {
        _uiState.value = TinderClasificacionUiState()
    }
} 