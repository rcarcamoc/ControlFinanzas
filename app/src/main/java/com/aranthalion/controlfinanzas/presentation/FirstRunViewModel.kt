package com.aranthalion.controlfinanzas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.data.repository.SueldoRepository
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class FirstRunViewModel @Inject constructor(
    private val configuracionPreferences: ConfiguracionPreferences,
    private val categoriaRepository: CategoriaRepository,
    private val movimientoRepository: MovimientoRepository,
    private val presupuestoCategoriaRepository: PresupuestoCategoriaRepository,
    private val sueldoRepository: SueldoRepository,
    private val clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FirstRunUiState>(FirstRunUiState.Initial)
    val uiState: StateFlow<FirstRunUiState> = _uiState.asStateFlow()

    fun cargarDatosHistoricos() {
        viewModelScope.launch {
            try {
                _uiState.value = FirstRunUiState.Loading
                
                // Cargar categorías por defecto
                categoriaRepository.insertDefaultCategorias()
                
                // Cargar datos históricos de movimientos
                movimientoRepository.cargarDatosHistoricos()
                
                // Cargar sistema de clasificación automática
                clasificacionUseCase.cargarDatosHistoricos()
                
                // Marcar que se han cargado los datos históricos
                configuracionPreferences.markHistoricalDataLoaded()
                
                // Marcar que ya no es la primera ejecución
                configuracionPreferences.markFirstRunComplete()
                
                _uiState.value = FirstRunUiState.Success
                
            } catch (e: Exception) {
                _uiState.value = FirstRunUiState.Error(
                    "Error al cargar datos históricos: ${e.message}"
                )
            }
        }
    }

    fun comenzarDesdeCero() {
        viewModelScope.launch {
            try {
                _uiState.value = FirstRunUiState.Loading
                
                // Limpiar todos los datos históricos para una instalación completamente limpia
                movimientoRepository.limpiarTodosLosDatos()
                
                // Marcar que NO se han cargado datos históricos
                configuracionPreferences.markHistoricalDataNotLoaded()
                
                // Marcar que ya no es la primera ejecución
                configuracionPreferences.markFirstRunComplete()
                
                _uiState.value = FirstRunUiState.Success
                
            } catch (e: Exception) {
                _uiState.value = FirstRunUiState.Error(
                    "Error al configurar la aplicación: ${e.message}"
                )
            }
        }
    }

    fun cargarSoloCategorias() {
        viewModelScope.launch {
            try {
                _uiState.value = FirstRunUiState.Loading
                
                // Solo cargar categorías básicas por defecto
                categoriaRepository.insertDefaultCategorias()
                
                // Marcar que ya no es la primera ejecución
                configuracionPreferences.markFirstRunComplete()
                
                _uiState.value = FirstRunUiState.Success
                
            } catch (e: Exception) {
                _uiState.value = FirstRunUiState.Error(
                    "Error al cargar categorías: ${e.message}"
                )
            }
        }
    }

    fun cargarSoloCategoriasYPresupuestos(periodoInicial: String, periodosDisponibles: List<String>) {
        viewModelScope.launch {
            try {
                _uiState.value = FirstRunUiState.Loading
                
                // Limpiar datos existentes para asegurar inserción limpia
                Log.i("LOG_PRIMER_USO", "[0] Limpiando datos existentes")
                
                // Insertar categorías por defecto
                categoriaRepository.insertDefaultCategorias()
                Log.i("LOG_PRIMER_USO", "[1] Categorías por defecto insertadas")
                
                // Esperar a que Room asigne los IDs
                kotlinx.coroutines.delay(1000)
                
                val categorias = categoriaRepository.obtenerCategorias().filter { it.tipo.equals("Gasto", ignoreCase = true) }
                Log.i("LOG_PRIMER_USO", "[2] Categorías de gasto obtenidas: ${categorias.size}")
                
                // Verificar que las categorías tengan IDs válidos
                categorias.forEach { categoria ->
                    Log.i("LOG_PRIMER_USO", "[2.1] Categoria: id=${categoria.id}, nombre=${categoria.nombre}, presupuestoMensual=${categoria.presupuestoMensual}")
                    if (categoria.id == 0L) {
                        Log.e("LOG_PRIMER_USO", "[ERROR] Categoría sin ID válido: ${categoria.nombre}")
                    }
                }
                
                val idxInicial = periodosDisponibles.indexOf(periodoInicial)
                val periodosAInsertar = if (idxInicial >= 0) periodosDisponibles.subList(idxInicial, periodosDisponibles.size) else listOf(periodoInicial)
                Log.i("LOG_PRIMER_USO", "[3] Periodos a insertar: $periodosAInsertar")
                
                var presupuestosInsertados = 0
                for (categoria in categorias) {
                    val monto = categoria.presupuestoMensual ?: 0.0
                    if (monto > 0) { // Solo insertar presupuestos con monto > 0
                        for (periodo in periodosAInsertar) {
                            Log.i("LOG_PRIMER_USO", "[4] Insertando presupuesto: categoriaId=${categoria.id}, nombre=${categoria.nombre}, monto=$monto, periodo=$periodo")
                            try {
                                presupuestoCategoriaRepository.insertarPresupuesto(
                                    com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity(
                                        categoriaId = categoria.id,
                                        monto = monto,
                                        periodo = periodo
                                    )
                                )
                                presupuestosInsertados++
                                Log.i("LOG_PRIMER_USO", "[4.1] Presupuesto insertado exitosamente")
                            } catch (e: Exception) {
                                Log.e("LOG_PRIMER_USO", "[ERROR] Error insertando presupuesto: ${e.message}")
                            }
                        }
                    } else {
                        Log.i("LOG_PRIMER_USO", "[4.2] Omitiendo presupuesto para ${categoria.nombre} (monto = 0)")
                    }
                }
                
                Log.i("LOG_PRIMER_USO", "[5] Inserción de presupuestos finalizada. Total insertados: $presupuestosInsertados")
                
                // Verificar presupuestos insertados
                val presupuestosVerificados = presupuestoCategoriaRepository.obtenerPresupuestosPorPeriodo(periodoInicial)
                Log.i("LOG_PRIMER_USO", "[6] Presupuestos verificados para periodo $periodoInicial: ${presupuestosVerificados.size}")
                presupuestosVerificados.forEach { presupuesto ->
                    Log.i("LOG_PRIMER_USO", "[6.1] Presupuesto: categoriaId=${presupuesto.categoriaId}, monto=${presupuesto.monto}, periodo=${presupuesto.periodo}")
                }
                
                configuracionPreferences.markFirstRunComplete()
                _uiState.value = FirstRunUiState.Success
            } catch (e: Exception) {
                Log.e("LOG_PRIMER_USO", "[ERROR] ${e.message}")
                _uiState.value = FirstRunUiState.Error(
                    "Error al cargar categorías y presupuestos: ${e.message}"
                )
            }
        }
    }

    fun resetError() {
        _uiState.value = FirstRunUiState.Initial
    }
}

sealed class FirstRunUiState {
    object Initial : FirstRunUiState()
    object Loading : FirstRunUiState()
    object Success : FirstRunUiState()
    data class Error(val mensaje: String) : FirstRunUiState()
} 