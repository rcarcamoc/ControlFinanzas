package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.UsuarioEntity
import com.aranthalion.controlfinanzas.data.local.dao.UsuarioDao
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.categoria.GestionarCategoriasUseCase
import com.aranthalion.controlfinanzas.domain.usecase.GestionarMovimientosUseCase
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.first
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.remote.ai.VisionImportService
import javax.inject.Inject

sealed interface TransaccionesEvent {
    data class DeleteMovimiento(val id: Long) : TransaccionesEvent
    data class AddMovimiento(val descripcion: String, val monto: Double, val tipo: String, val categoria: Categoria?, val fecha: Date, val periodo: String, val scope: String, val userId_internal: String?) : TransaccionesEvent
    data class FilterMovimientos(val tipo: String?, val categoria: Categoria?) : TransaccionesEvent
    data class SearchMovimientos(val query: String) : TransaccionesEvent
    data class UpdateMovimientoCategoria(val id: Long, val categoriaId: Long?, val scope: String? = null) : TransaccionesEvent
    data class UpdateMovimientosCategoria(val ids: List<Long>, val categoriaId: Long?, val scope: String? = null) : TransaccionesEvent
    data class EditMovimiento(val movimiento: MovimientoEntity) : TransaccionesEvent
    data class FilterByPeriodo(val periodo: String) : TransaccionesEvent
    data class FilterByScope(val scope: String) : TransaccionesEvent
    data class DeletePeriodo(val periodo: String) : TransaccionesEvent
}

sealed interface UiState {
    object Loading : UiState
    data class Success(
        val movimientos: List<MovimientoEntity>,
        val categorias: List<Categoria>,
        val usuarios: List<UsuarioEntity>
    ) : UiState
    data class Error(val message: String) : UiState
}

@HiltViewModel
class TransaccionesViewModel @Inject constructor(
    private val gestionarMovimientosUseCase: GestionarMovimientosUseCase,
    private val gestionarCategoriasUseCase: GestionarCategoriasUseCase,
    val clasificacionUseCase: GestionarClasificacionAutomaticaUseCase,
    private val configuracionPreferences: ConfiguracionPreferences,
    private val visionImportService: VisionImportService,
    private val usuarioDao: UsuarioDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _visionLoading = MutableStateFlow(false)
    val visionLoading: StateFlow<Boolean> = _visionLoading.asStateFlow()

    val customPeriodConfigs: Map<String, com.aranthalion.controlfinanzas.data.util.BillingPeriodConfig>
        get() = configuracionPreferences.obtenerPeriodoDatesMap()

    // Movimientos sin categoría de TODOS los scopes para el asistente de clasificación
    private val _movimientosSinCategoriaTodosScopes = MutableStateFlow<List<MovimientoEntity>>(emptyList())
    val movimientosSinCategoriaTodosScopes: StateFlow<List<MovimientoEntity>> = _movimientosSinCategoriaTodosScopes.asStateFlow()

    fun importarCaptura(
        base64Image: String,
        onParsed: (List<VisionImportService.ParsedTransaction>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _visionLoading.value = true
            val result = visionImportService.analyzeScreenshot(base64Image)
            _visionLoading.value = false
            result.onSuccess {
                onParsed(it)
            }.onFailure {
                onError(it.message ?: "Error al procesar la captura")
            }
        }
    }

    private var currentTipoFilter: String? = null
    private var currentCategoriaFilter: Categoria? = null
    private var currentSearchQuery: String = ""
    private var currentPeriodoFilter: String? = null
    private var currentScopeFilter: String = "HOUSEHOLD"

    init {
        currentPeriodoFilter = configuracionPreferences.obtenerPeriodoGlobal()
        currentScopeFilter = configuracionPreferences.obtenerScopeGlobal()
        fetchData()
    }

    fun onEvent(event: TransaccionesEvent) {
        when (event) {
            is TransaccionesEvent.DeleteMovimiento -> {
                viewModelScope.launch {
                    // Buscar por ID directo sin filtro de scope
                    val movimiento = gestionarMovimientosUseCase.obtenerMovimientoPorId(event.id)
                    if (movimiento != null) {
                        gestionarMovimientosUseCase.eliminarMovimiento(movimiento)
                        fetchData()
                    }
                }
            }
            is TransaccionesEvent.AddMovimiento -> {
                addMovimiento(event.descripcion, event.monto, event.tipo, event.categoria, event.fecha, event.periodo, event.scope, event.userId_internal)
            }
            is TransaccionesEvent.FilterMovimientos -> {
                currentTipoFilter = event.tipo
                currentCategoriaFilter = event.categoria
                fetchData()
            }
            is TransaccionesEvent.SearchMovimientos -> {
                currentSearchQuery = event.query
                fetchData()
            }
            is TransaccionesEvent.UpdateMovimientoCategoria -> {
                viewModelScope.launch {
                    // Buscar por ID directo sin filtro de scope — corrige el bug donde
                    // transacciones con scope PERSONAL no se encontraban desde vista HOUSEHOLD
                    val movimientoAnterior = gestionarMovimientosUseCase.obtenerMovimientoPorId(event.id)
                    if (movimientoAnterior != null) {
                        val targetScope = event.scope ?: movimientoAnterior.scope
                        val resolvedUserIdInternal = if (targetScope == "PERSONAL") {
                            movimientoAnterior.userId_internal ?: usuarioDao.obtenerTodosLosUsuarios()
                                .find { it.email?.equals(configuracionPreferences.syncEmail, ignoreCase = true) == true }?.idServidor
                        } else {
                            null
                        }
                        val nuevoMovimiento = if (event.categoriaId == -1L) {
                            movimientoAnterior.copy(
                                tipo = "OMITIR",
                                categoriaId = null,
                                scope = targetScope,
                                userId_internal = resolvedUserIdInternal
                            )
                        } else {
                            movimientoAnterior.copy(
                                categoriaId = event.categoriaId,
                                scope = targetScope,
                                userId_internal = resolvedUserIdInternal
                            )
                        }
                        gestionarMovimientosUseCase.actualizarMovimiento(nuevoMovimiento)
                        
                        if (event.categoriaId != null && event.categoriaId != -1L) {
                            clasificacionUseCase.aprenderPatron(nuevoMovimiento.descripcion, event.categoriaId)
                        }
                        fetchData()
                    }
                }
            }
            is TransaccionesEvent.UpdateMovimientosCategoria -> {
                viewModelScope.launch {
                    event.ids.forEach { id ->
                        val movimientoAnterior = gestionarMovimientosUseCase.obtenerMovimientoPorId(id)
                        if (movimientoAnterior != null) {
                            val targetScope = event.scope ?: movimientoAnterior.scope
                            val resolvedUserIdInternal = if (targetScope == "PERSONAL") {
                                movimientoAnterior.userId_internal ?: usuarioDao.obtenerTodosLosUsuarios()
                                    .find { it.email?.equals(configuracionPreferences.syncEmail, ignoreCase = true) == true }?.idServidor
                            } else {
                                null
                            }
                            val nuevoMovimiento = if (event.categoriaId == -1L) {
                                movimientoAnterior.copy(
                                    tipo = "OMITIR",
                                    categoriaId = null,
                                    scope = targetScope,
                                    userId_internal = resolvedUserIdInternal
                                )
                            } else {
                                movimientoAnterior.copy(
                                    categoriaId = event.categoriaId,
                                    scope = targetScope,
                                    userId_internal = resolvedUserIdInternal
                                )
                            }
                            gestionarMovimientosUseCase.actualizarMovimiento(nuevoMovimiento)
                            
                            if (event.categoriaId != null && event.categoriaId != -1L) {
                                clasificacionUseCase.aprenderPatron(nuevoMovimiento.descripcion, event.categoriaId)
                            }
                        }
                    }
                    fetchData()
                }
            }
            is TransaccionesEvent.EditMovimiento -> {
                viewModelScope.launch {
                    gestionarMovimientosUseCase.actualizarMovimiento(event.movimiento)
                    if (event.movimiento.categoriaId != null) {
                        clasificacionUseCase.aprenderPatron(event.movimiento.descripcion, event.movimiento.categoriaId)
                    }
                    fetchData()
                }
            }
            is TransaccionesEvent.FilterByPeriodo -> {
                currentPeriodoFilter = event.periodo
                fetchData()
            }
            is TransaccionesEvent.FilterByScope -> {
                currentScopeFilter = event.scope
                configuracionPreferences.guardarScopeGlobal(event.scope)
                fetchData()
            }
            is TransaccionesEvent.DeletePeriodo -> {
                viewModelScope.launch {
                    gestionarMovimientosUseCase.eliminarMovimientosPorPeriodo(event.periodo)
                    fetchData()
                }
            }
        }
    }

    private fun fetchData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // obtenerMovimientos() ya filtra por scope global en el Repository,
                // NO aplicar doble filtro de scope aquí
                val movimientos = gestionarMovimientosUseCase.obtenerMovimientos().filter { movimiento ->
                    val tipoMatch = currentTipoFilter?.let { it == movimiento.tipo } ?: true
                    val categoriaMatch = currentCategoriaFilter?.let { it.id == movimiento.categoriaId } ?: true
                    val periodoMatch = currentPeriodoFilter?.let { 
                        it == "Todos" || movimiento.periodoFacturacion == it 
                    } ?: true
                    val searchMatch = if (currentSearchQuery.isBlank()) {
                        true
                    } else {
                        movimiento.descripcion.contains(currentSearchQuery, ignoreCase = true)
                    }
                    tipoMatch && categoriaMatch && periodoMatch && searchMatch
                }
                
                // Ordenar: primero los sin clasificar (sin categoría y no OMITIR), luego el resto, ambos grupos por fecha descendente
                val movimientosOrdenados = movimientos.sortedWith(
                    compareBy<MovimientoEntity> { movimiento ->
                        // 0 = sin clasificar (primero), 1 = clasificados (después)
                        if (movimiento.categoriaId == null && movimiento.tipo != "OMITIR") 0 else 1
                    }.thenByDescending { it.fecha }
                )
                val usuarios = usuarioDao.obtenerTodosLosUsuarios()
                
                // Cargar movimientos sin categoría de TODOS los scopes para el asistente
                _movimientosSinCategoriaTodosScopes.value = 
                    gestionarMovimientosUseCase.obtenerMovimientosSinCategoriaTodosScopes()
                
                gestionarCategoriasUseCase.getAllCategorias().collect { categorias ->
                    _uiState.value = UiState.Success(movimientosOrdenados, categorias, usuarios)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error al cargar datos")
            }
        }
    }

    private fun addMovimiento(descripcion: String, monto: Double, tipo: String, categoria: Categoria?, fecha: Date, periodo: String, scope: String, userId_internal: String?) {
        viewModelScope.launch {
            val resolvedUserIdInternal = if (scope == "PERSONAL") {
                userId_internal ?: usuarioDao.obtenerTodosLosUsuarios()
                    .find { it.email?.equals(configuracionPreferences.syncEmail, ignoreCase = true) == true }?.idServidor
            } else {
                userId_internal
            }
            val movimiento = MovimientoEntity(
                descripcion = descripcion,
                monto = monto,
                tipo = tipo,
                categoriaId = categoria?.id,
                fecha = fecha,
                periodoFacturacion = periodo,
                idUnico = com.aranthalion.controlfinanzas.data.util.ExcelProcessor.generarIdUnico(fecha, monto, descripcion),
                scope = scope,
                userId_internal = resolvedUserIdInternal
            )
            gestionarMovimientosUseCase.agregarMovimiento(movimiento)
            
            if (categoria?.id != null) {
                clasificacionUseCase.aprenderPatron(descripcion, categoria.id)
            }
            
            fetchData()
        }
    }
    
    fun filtrarPorCategoriaId(categoriaId: Long) {
        viewModelScope.launch {
            try {
                val categorias = gestionarCategoriasUseCase.getAllCategorias().first()
                val categoria = categorias.find { it.id == categoriaId }
                if (categoria != null) {
                    currentCategoriaFilter = categoria
                    fetchData()
                }
            } catch (e: Exception) {
                // Silently ignore or log
            }
        }
    }

    private fun obtenerPeriodoActual(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }
}
