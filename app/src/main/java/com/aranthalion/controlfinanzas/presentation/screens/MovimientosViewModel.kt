package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.domain.usecase.GestionarMovimientosUseCase
import com.aranthalion.controlfinanzas.domain.categoria.GestionarCategoriasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class MovimientosViewModel @Inject constructor(
    private val gestionarMovimientosUseCase: GestionarMovimientosUseCase,
    private val gestionarCategoriasUseCase: GestionarCategoriasUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MovimientosUiState>(MovimientosUiState.Loading)
    val uiState: StateFlow<MovimientosUiState> = _uiState.asStateFlow()

    private val _totales = MutableStateFlow(Totales(0.0, 0.0, 0.0))
    val totales: StateFlow<Totales> = _totales.asStateFlow()

    init {
        // No cargar movimientos automáticamente, esperar a que se establezca el período
    }

    fun cargarMovimientos() {
        viewModelScope.launch {
            try {
                val movimientos = gestionarMovimientosUseCase.obtenerMovimientos()
                val categorias = gestionarMovimientosUseCase.obtenerCategorias()
                
                // Ordenar movimientos: primero los sin categoría, luego por fecha descendente
                val movimientosOrdenados = movimientos.sortedWith(
                    compareBy<MovimientoEntity> { it.categoriaId == null }.reversed()
                    .thenByDescending { it.fecha }
                )
                
                _uiState.value = MovimientosUiState.Success(movimientosOrdenados, categorias)
                calcularTotales(movimientosOrdenados)
            } catch (e: Exception) {
                _uiState.value = MovimientosUiState.Error(e.message ?: "Error al cargar movimientos")
            }
        }
    }

    fun cargarMovimientosPorPeriodo(periodo: String) {
        viewModelScope.launch {
            try {
                println("🔍 DEBUG: Cargando movimientos para período: $periodo")
                val movimientos = gestionarMovimientosUseCase.obtenerMovimientos()
                val categorias = gestionarMovimientosUseCase.obtenerCategorias()
                
                println("🔍 DEBUG: Movimientos obtenidos: ${movimientos.size}")
                movimientos.take(5).forEach { movimiento ->
                    println("  - ${movimiento.descripcion}: ${movimiento.fecha} (tipo: ${movimiento.tipo})")
                }
                
                // Ordenar movimientos: primero los sin categoría, luego por fecha descendente
                val movimientosOrdenados = movimientos.sortedWith(
                    compareBy<MovimientoEntity> { it.categoriaId == null }.reversed()
                    .thenByDescending { it.fecha }
                )
                
                _uiState.value = MovimientosUiState.Success(movimientosOrdenados, categorias)
                calcularTotales(movimientosOrdenados)
                println("🔍 DEBUG: Estado actualizado con ${movimientosOrdenados.size} movimientos")
            } catch (e: Exception) {
                println("❌ ERROR: ${e.message}")
                _uiState.value = MovimientosUiState.Error(e.message ?: "Error al cargar movimientos")
            }
        }
    }

    fun agregarMovimiento(movimiento: MovimientoEntity) {
        viewModelScope.launch {
            try {
                gestionarMovimientosUseCase.agregarMovimiento(movimiento)
                cargarMovimientos()
            } catch (e: Exception) {
                _uiState.value = MovimientosUiState.Error(e.message ?: "Error al agregar movimiento")
            }
        }
    }

    fun actualizarMovimiento(movimiento: MovimientoEntity) {
        viewModelScope.launch {
            try {
                gestionarMovimientosUseCase.actualizarMovimiento(movimiento)
                cargarMovimientos()
            } catch (e: Exception) {
                _uiState.value = MovimientosUiState.Error(e.message ?: "Error al actualizar movimiento")
            }
        }
    }

    fun eliminarMovimiento(movimiento: MovimientoEntity) {
        viewModelScope.launch {
            try {
                gestionarMovimientosUseCase.eliminarMovimiento(movimiento)
                cargarMovimientos()
            } catch (e: Exception) {
                _uiState.value = MovimientosUiState.Error(e.message ?: "Error al eliminar movimiento")
            }
        }
    }

    private fun calcularTotales(movimientos: List<MovimientoEntity>) {
        var ingresos = 0.0
        var gastos = 0.0

        movimientos.forEach { movimiento ->
            if (movimiento.tipo == "INGRESO") {
                ingresos += movimiento.monto
            } else {
                gastos += movimiento.monto
            }
        }

        _totales.value = Totales(
            ingresos = ingresos,
            gastos = gastos,
            balance = ingresos - gastos
        )
    }

    suspend fun obtenerIdUnicosExistentes(): Set<String> {
        return withContext(Dispatchers.IO) {
            gestionarMovimientosUseCase.obtenerIdUnicos()
        }
    }

    suspend fun obtenerIdUnicosExistentesPorPeriodo(periodo: String?): Set<String> {
        return withContext(Dispatchers.IO) {
            gestionarMovimientosUseCase.obtenerIdUnicosPorPeriodo(periodo)
        }
    }

    suspend fun obtenerCategoriasPorIdUnico(periodo: String?): Map<String, Long?> {
        return withContext(Dispatchers.IO) {
            gestionarMovimientosUseCase.obtenerCategoriasPorIdUnico(periodo)
        }
    }

    suspend fun eliminarMovimientosPorPeriodo(periodo: String?) {
        withContext(Dispatchers.IO) {
            gestionarMovimientosUseCase.eliminarMovimientosPorPeriodo(periodo)
        }
    }
}

data class Totales(
    val ingresos: Double,
    val gastos: Double,
    val balance: Double
)

sealed class MovimientosUiState {
    object Loading : MovimientosUiState()
    data class Success(
        val movimientos: List<MovimientoEntity>,
        val categorias: List<Categoria>
    ) : MovimientosUiState()
    data class Error(val mensaje: String) : MovimientosUiState()
} 