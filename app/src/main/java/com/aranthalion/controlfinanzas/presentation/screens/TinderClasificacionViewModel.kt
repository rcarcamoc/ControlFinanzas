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
import com.aranthalion.controlfinanzas.domain.clasificacion.ResultadoClasificacion
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
    
    init {
        cargarTransaccionesPendientes()
    }
    
    fun cargarTransaccionesPendientes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Obtener transacciones sin clasificar del servicio
                val transacciones = tinderService.obtenerTransaccionesPendientes()
                val categoriasDominio = categoriaRepository.obtenerCategorias()
                
                // Convertir categorías del dominio a entidades
                val categorias = categoriasDominio.map { categoriaDominio ->
                    Categoria(
                        id = categoriaDominio.id,
                        nombre = categoriaDominio.nombre,
                        descripcion = categoriaDominio.nombre,
                        tipo = "Gasto"
                    )
                }
                
                val transaccionesTinder = mutableListOf<TransaccionTinder>()
                
                transacciones.forEach { transaccion ->
                    // USAR EL NUEVO SISTEMA MEJORADO
                    val resultadoClasificacion = clasificacionUseCase.obtenerSugerenciaMejorada(transaccion.descripcion)
                    
                    when (resultadoClasificacion) {
                        is ResultadoClasificacion.AltaConfianza -> {
                            // Alta confianza - mostrar sugerencia automática
                            val categoria = categorias.find { it.id == resultadoClasificacion.categoriaId }
                            if (categoria != null) {
                                transaccionesTinder.add(
                                    TransaccionTinder(
                                        transaccion = transaccion,
                                        categoriaSugerida = categoria,
                                        nivelConfianza = resultadoClasificacion.confianza,
                                        patron = resultadoClasificacion.patron,
                                        tipoCoincidencia = resultadoClasificacion.tipoCoincidencia.name
                                    )
                                )
                            }
                        }
                        is ResultadoClasificacion.BajaConfianza -> {
                            // Baja confianza - mostrar múltiples opciones
                            val mejorSugerencia = resultadoClasificacion.sugerencias.firstOrNull()
                            if (mejorSugerencia != null) {
                                val categoria = categorias.find { it.id == mejorSugerencia.categoriaId }
                                if (categoria != null) {
                                    transaccionesTinder.add(
                                        TransaccionTinder(
                                            transaccion = transaccion,
                                            categoriaSugerida = categoria,
                                            nivelConfianza = mejorSugerencia.nivelConfianza,
                                            patron = mejorSugerencia.patron,
                                            tipoCoincidencia = "BAJA_CONFIANZA"
                                        )
                                    )
                                }
                            }
                        }
                        is ResultadoClasificacion.SinCoincidencias -> {
                            // Sin coincidencias - usar categoría por defecto
                            transaccionesTinder.add(
                                TransaccionTinder(
                                    transaccion = transaccion,
                                    categoriaSugerida = categorias.firstOrNull() ?: categorias.first(),
                                    nivelConfianza = 0.0,
                                    patron = "Sin patrón",
                                    tipoCoincidencia = "SIN_COINCIDENCIAS"
                                )
                            )
                        }
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    transaccionesPendientes = transaccionesTinder,
                    transaccionActual = transaccionesTinder.firstOrNull(),
                    isLoading = false,
                    mostrarTinder = transaccionesTinder.isNotEmpty()
                )
                
                actualizarEstadisticas()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar transacciones: ${e.message}"
                )
            }
        }
    }
    
    fun aceptarTransaccion() {
        viewModelScope.launch {
            val transaccionActual = _uiState.value.transaccionActual ?: return@launch
            
            try {
                // Aprender el patrón con la categoría aceptada
                clasificacionUseCase.aprenderPatron(
                    transaccionActual.transaccion.descripcion,
                    transaccionActual.categoriaSugerida.id
                )
                
                // Guardar la transacción con la categoría
                val movimiento = transaccionActual.transaccion.toMovimientoEntity(
                    categoriaId = transaccionActual.categoriaSugerida.id
                )
                movimientoRepository.agregarMovimiento(movimiento)
                
                // Pasar a la siguiente transacción
                pasarSiguienteTransaccion()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al aceptar transacción: ${e.message}"
                )
            }
        }
    }
    
    fun rechazarTransaccion() {
        viewModelScope.launch {
            val transaccionActual = _uiState.value.transaccionActual ?: return@launch
            
            try {
                // Guardar la transacción sin categoría (para clasificación manual posterior)
                val movimiento = transaccionActual.transaccion.toMovimientoEntity()
                movimientoRepository.agregarMovimiento(movimiento)
                
                // Pasar a la siguiente transacción
                pasarSiguienteTransaccion()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al rechazar transacción: ${e.message}"
                )
            }
        }
    }
    
    fun cambiarCategoria(categoriaId: Long) {
        viewModelScope.launch {
            val transaccionActual = _uiState.value.transaccionActual ?: return@launch
            
            try {
                // Aprender el patrón con la nueva categoría
                clasificacionUseCase.aprenderPatron(
                    transaccionActual.transaccion.descripcion,
                    categoriaId
                )
                
                // Guardar la transacción con la nueva categoría
                val movimiento = transaccionActual.transaccion.toMovimientoEntity(
                    categoriaId = categoriaId
                )
                movimientoRepository.agregarMovimiento(movimiento)
                
                // Pasar a la siguiente transacción
                pasarSiguienteTransaccion()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al cambiar categoría: ${e.message}"
                )
            }
        }
    }
    
    private fun pasarSiguienteTransaccion() {
        val transaccionesPendientes = _uiState.value.transaccionesPendientes.toMutableList()
        val transaccionActual = transaccionesPendientes.removeFirstOrNull()
        
        _uiState.value = _uiState.value.copy(
            transaccionesPendientes = transaccionesPendientes,
            transaccionActual = transaccionesPendientes.firstOrNull(),
            mostrarTinder = transaccionesPendientes.isNotEmpty()
        )
        
        actualizarEstadisticas()
    }
    
    private fun actualizarEstadisticas() {
        val total = _uiState.value.transaccionesPendientes.size + 
                   _uiState.value.estadisticas.aceptadas + 
                   _uiState.value.estadisticas.rechazadas
        
        _uiState.value = _uiState.value.copy(
            estadisticas = _uiState.value.estadisticas.copy(
                totalProcesadas = total,
                pendientes = _uiState.value.transaccionesPendientes.size
            )
        )
    }
    
    fun recargarTransacciones() {
        cargarTransaccionesPendientes()
    }
    
    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
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