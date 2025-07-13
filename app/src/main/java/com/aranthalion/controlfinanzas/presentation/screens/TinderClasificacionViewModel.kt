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
                
                procesarTransaccionesParaTinder(transacciones, categorias)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar transacciones: ${e.message}"
                )
            }
        }
    }
    
    fun cargarTransaccionesEspecificas(transacciones: List<ExcelTransaction>) {
        println("🔍 TINDER_DEBUG: Iniciando carga de transacciones específicas: ${transacciones.size}")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val categoriasDominio = categoriaRepository.obtenerCategorias()
                println("🔍 TINDER_DEBUG: Categorías del dominio obtenidas: ${categoriasDominio.size}")
                
                // Convertir categorías del dominio a entidades
                val categorias = categoriasDominio.map { categoriaDominio ->
                    Categoria(
                        id = categoriaDominio.id,
                        nombre = categoriaDominio.nombre,
                        descripcion = categoriaDominio.nombre,
                        tipo = "Gasto"
                    )
                }
                
                println("🔍 TINDER_DEBUG: Categorías convertidas: ${categorias.size}")
                procesarTransaccionesParaTinder(transacciones, categorias)
                
            } catch (e: Exception) {
                println("❌ TINDER_DEBUG: Error al cargar transacciones específicas: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar transacciones específicas: ${e.message}"
                )
            }
        }
    }
    
    private fun procesarTransaccionesParaTinder(transacciones: List<ExcelTransaction>, categorias: List<Categoria>) {
        viewModelScope.launch {
            println("🔍 TINDER_DEBUG: Procesando ${transacciones.size} transacciones")
            val transaccionesTinder = mutableListOf<TransaccionTinder>()
            
            // Procesar transacciones
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
                
                println("🔍 TINDER_DEBUG: Transacciones procesadas: ${transaccionesTinder.size}")
                println("🔍 TINDER_DEBUG: Primera transacción: ${transaccionesTinder.firstOrNull()?.transaccion?.descripcion}")
                println("🔍 TINDER_DEBUG: Categorías disponibles: ${categorias.size}")
                println("🔍 TINDER_DEBUG: Mostrar Tinder: ${transaccionesTinder.isNotEmpty()}")
                
                _uiState.value = _uiState.value.copy(
                    transaccionesPendientes = transaccionesTinder,
                    transaccionActual = transaccionesTinder.firstOrNull(),
                    categoriasDisponibles = categorias,
                    isLoading = false,
                    mostrarTinder = transaccionesTinder.isNotEmpty()
                )
            
            // Generar sugerencias para la primera transacción
            if (transaccionesTinder.isNotEmpty()) {
                generarSugerenciasParaTransaccion(transaccionesTinder.first())
            }
            
            actualizarEstadisticas()
        }
    }
    
    private fun generarSugerenciasParaTransaccion(transaccionTinder: TransaccionTinder) {
        viewModelScope.launch {
            try {
                val resultadoClasificacion = clasificacionUseCase.obtenerSugerenciaMejorada(transaccionTinder.transaccion.descripcion)
                val categorias = _uiState.value.categoriasDisponibles
                
                val sugerencias = when (resultadoClasificacion) {
                    is ResultadoClasificacion.AltaConfianza -> {
                        val categoria = categorias.find { it.id == resultadoClasificacion.categoriaId }
                        if (categoria != null) {
                            listOf(
                                SugerenciaCategoria(
                                    categoriaId = categoria.id,
                                    nombre = categoria.nombre,
                                    nivelConfianza = resultadoClasificacion.confianza,
                                    patron = resultadoClasificacion.patron,
                                    tipoCoincidencia = resultadoClasificacion.tipoCoincidencia.name,
                                    esSeleccionada = true // Auto-seleccionar alta confianza
                                )
                            )
                        } else emptyList()
                    }
                    is ResultadoClasificacion.BajaConfianza -> {
                        resultadoClasificacion.sugerencias.mapNotNull { sugerencia ->
                            val categoria = categorias.find { it.id == sugerencia.categoriaId }
                            categoria?.let {
                                SugerenciaCategoria(
                                    categoriaId = it.id,
                                    nombre = it.nombre,
                                    nivelConfianza = sugerencia.nivelConfianza,
                                    patron = sugerencia.patron,
                                    tipoCoincidencia = "BAJA_CONFIANZA"
                                )
                            }
                        }.sortedByDescending { it.nivelConfianza }
                    }
                    is ResultadoClasificacion.SinCoincidencias -> {
                        // Mostrar categorías más usadas como sugerencias
                        categorias.take(5).map { categoria ->
                            SugerenciaCategoria(
                                categoriaId = categoria.id,
                                nombre = categoria.nombre,
                                nivelConfianza = 0.0,
                                patron = "Sin patrón",
                                tipoCoincidencia = "SIN_COINCIDENCIAS"
                            )
                        }
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    sugerenciasCategorias = sugerencias,
                    categoriaSeleccionada = sugerencias.firstOrNull { it.esSeleccionada }
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al generar sugerencias: ${e.message}"
                )
            }
        }
    }
    
    fun seleccionarCategoria(categoriaId: Long) {
        val sugerencias = _uiState.value.sugerenciasCategorias.map { sugerencia ->
            sugerencia.copy(esSeleccionada = sugerencia.categoriaId == categoriaId)
        }
        
        val categoriaSeleccionada = sugerencias.find { it.esSeleccionada }
        
        _uiState.value = _uiState.value.copy(
            sugerenciasCategorias = sugerencias,
            categoriaSeleccionada = categoriaSeleccionada,
            mostrarSelectorManual = false
        )
    }
    
    fun mostrarSelectorManual() {
        _uiState.value = _uiState.value.copy(mostrarSelectorManual = true)
    }
    
    fun seleccionarCategoriaManual(categoriaId: Long) {
        val categorias = _uiState.value.categoriasDisponibles
        val categoria = categorias.find { it.id == categoriaId }
        
        if (categoria != null) {
            val categoriaSeleccionada = SugerenciaCategoria(
                categoriaId = categoria.id,
                nombre = categoria.nombre,
                nivelConfianza = 0.0,
                patron = "Selección manual",
                tipoCoincidencia = "MANUAL",
                esSeleccionada = true
            )
            
            _uiState.value = _uiState.value.copy(
                categoriaSeleccionada = categoriaSeleccionada,
                mostrarSelectorManual = false
            )
        }
    }
    
    fun confirmarClasificacion() {
        viewModelScope.launch {
            val transaccionActual = _uiState.value.transaccionActual ?: return@launch
            val categoriaSeleccionada = _uiState.value.categoriaSeleccionada ?: return@launch
            
            try {
                // Aprender el patrón con la categoría seleccionada
                clasificacionUseCase.aprenderPatron(
                    transaccionActual.transaccion.descripcion,
                    categoriaSeleccionada.categoriaId
                )
                
                // Buscar el movimiento existente y actualizarlo
                val movimientos = movimientoRepository.obtenerMovimientos()
                val movimientoExistente = movimientos.find { it.idUnico == transaccionActual.transaccion.codigoReferencia }
                
                if (movimientoExistente != null) {
                    // Actualizar el movimiento existente
                    val movimientoActualizado = movimientoExistente.copy(
                        categoriaId = categoriaSeleccionada.categoriaId,
                        fechaActualizacion = System.currentTimeMillis(),
                        metodoActualizacion = "TINDER_CLASIFICACION",
                        daoResponsable = "TinderClasificacionViewModel"
                    )
                    movimientoRepository.actualizarMovimiento(movimientoActualizado)
                } else {
                    // Si no existe, crear uno nuevo (caso raro)
                    val movimiento = transaccionActual.transaccion.toMovimientoEntity(
                        categoriaId = categoriaSeleccionada.categoriaId
                    )
                    movimientoRepository.agregarMovimiento(movimiento)
                }
                
                // Mostrar feedback
                mostrarFeedback("✅ Clasificado como: ${categoriaSeleccionada.nombre}")
                
                // Pasar a la siguiente transacción
                pasarSiguienteTransaccion()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al confirmar clasificación: ${e.message}"
                )
            }
        }
    }

    fun aceptarTransaccion() {
        viewModelScope.launch {
            val transaccionActual = _uiState.value.transaccionActual ?: return@launch
            val categoriaSeleccionada = _uiState.value.categoriaSeleccionada
            
            try {
                // Buscar el movimiento existente
                val movimientos = movimientoRepository.obtenerMovimientos()
                val movimientoExistente = movimientos.find { it.idUnico == transaccionActual.transaccion.codigoReferencia }
                
                val categoriaId = if (categoriaSeleccionada != null) {
                    categoriaSeleccionada.categoriaId
                } else {
                    transaccionActual.categoriaSugerida.id
                }
                
                val nombreCategoria = if (categoriaSeleccionada != null) {
                    categoriaSeleccionada.nombre
                } else {
                    transaccionActual.categoriaSugerida.nombre
                }
                
                // Aprender el patrón
                clasificacionUseCase.aprenderPatron(
                    transaccionActual.transaccion.descripcion,
                    categoriaId
                )
                
                if (movimientoExistente != null) {
                    // Actualizar el movimiento existente
                    val movimientoActualizado = movimientoExistente.copy(
                        categoriaId = categoriaId,
                        fechaActualizacion = System.currentTimeMillis(),
                        metodoActualizacion = "TINDER_CLASIFICACION",
                        daoResponsable = "TinderClasificacionViewModel"
                    )
                    movimientoRepository.actualizarMovimiento(movimientoActualizado)
                } else {
                    // Si no existe, crear uno nuevo (caso raro)
                    val movimiento = transaccionActual.transaccion.toMovimientoEntity(
                        categoriaId = categoriaId
                    )
                    movimientoRepository.agregarMovimiento(movimiento)
                }
                
                mostrarFeedback("✅ Aceptado: $nombreCategoria")
                
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
                // Mostrar selector manual para que el usuario elija la categoría correcta
                _uiState.value = _uiState.value.copy(mostrarSelectorManual = true)
                
            } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(
                    error = "Error al rechazar transacción: ${e.message}"
        )
            }
        }
    }

    fun rechazarYSeleccionarManual(categoriaId: Long) {
        viewModelScope.launch {
            val transaccionActual = _uiState.value.transaccionActual ?: return@launch
            
            try {
                // Aprender el patrón con la categoría seleccionada manualmente
                clasificacionUseCase.aprenderPatron(
                    transaccionActual.transaccion.descripcion,
                    categoriaId
                )
                
                // Buscar el movimiento existente y actualizarlo
                val movimientos = movimientoRepository.obtenerMovimientos()
                val movimientoExistente = movimientos.find { it.idUnico == transaccionActual.transaccion.codigoReferencia }
                
                if (movimientoExistente != null) {
                    // Actualizar el movimiento existente
                    val movimientoActualizado = movimientoExistente.copy(
                        categoriaId = categoriaId,
                        fechaActualizacion = System.currentTimeMillis(),
                        metodoActualizacion = "TINDER_CLASIFICACION",
                        daoResponsable = "TinderClasificacionViewModel"
                    )
                    movimientoRepository.actualizarMovimiento(movimientoActualizado)
                } else {
                    // Si no existe, crear uno nuevo (caso raro)
                    val movimiento = transaccionActual.transaccion.toMovimientoEntity(
                        categoriaId = categoriaId
                    )
                    movimientoRepository.agregarMovimiento(movimiento)
                }
                
                mostrarFeedback("✅ Clasificado manualmente y aprendido")
                
                // Pasar a la siguiente transacción
                pasarSiguienteTransaccion()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al procesar selección manual: ${e.message}"
                )
            }
        }
    }
    
    private fun mostrarFeedback(mensaje: String) {
        _uiState.value = _uiState.value.copy(
            mostrarFeedback = true,
            mensajeFeedback = mensaje
        )
        
        // Ocultar feedback después de 2 segundos
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(mostrarFeedback = false)
        }
    }
    
    private fun pasarSiguienteTransaccion() {
        val transaccionesPendientes = _uiState.value.transaccionesPendientes.toMutableList()
        val transaccionActual = transaccionesPendientes.removeFirstOrNull()
        
        _uiState.value = _uiState.value.copy(
            transaccionesPendientes = transaccionesPendientes,
            transaccionActual = transaccionesPendientes.firstOrNull(),
            sugerenciasCategorias = emptyList(),
            categoriaSeleccionada = null,
            mostrarTinder = transaccionesPendientes.isNotEmpty()
        )
        
        // Generar sugerencias para la siguiente transacción
        if (transaccionesPendientes.isNotEmpty()) {
            generarSugerenciasParaTransaccion(transaccionesPendientes.first())
        }
        
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

    fun ocultarSelectorManual() {
        _uiState.value = _uiState.value.copy(mostrarSelectorManual = false)
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