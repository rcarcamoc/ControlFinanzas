package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.domain.usecase.GestionarMovimientosUseCase
import com.aranthalion.controlfinanzas.domain.categoria.GestionarCategoriasUseCase
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import android.util.Log

@HiltViewModel
class MovimientosViewModel @Inject constructor(
    private val gestionarMovimientosUseCase: GestionarMovimientosUseCase,
    private val gestionarCategoriasUseCase: GestionarCategoriasUseCase,
    private val clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
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
                
                // Usar consultas optimizadas del HITO 1
                val movimientos = if (periodo != "Todos") {
                    // Usar consulta optimizada por período
                    gestionarMovimientosUseCase.obtenerMovimientosPorPeriodoOptimizado(periodo)
                } else {
                    // Usar consulta optimizada con límite
                    gestionarMovimientosUseCase.obtenerMovimientosOptimizado()
                }
                
                // Usar cache para categorías (HITO 1.3)
                val categorias = gestionarMovimientosUseCase.obtenerCategoriasOptimizado()
                
                println("🔍 DEBUG: Movimientos obtenidos: ${movimientos.size}")
                movimientos.take(5).forEach { movimiento ->
                    println("  - ${movimiento.descripcion}: ${movimiento.fecha} (período: ${movimiento.periodoFacturacion})")
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
                gestionarMovimientosUseCase.agregarMovimiento(movimiento, "INSERT", "MovimientosViewModel")
                
                // Aprender del patrón si se asignó una categoría manualmente
                if (movimiento.categoriaId != null) {
                    Log.d("MovimientosViewModel", "🧠 Aprendiendo patrón de clasificación manual: '${movimiento.descripcion}' -> Categoría ID: ${movimiento.categoriaId}")
                    clasificacionUseCase.aprenderPatron(movimiento.descripcion, movimiento.categoriaId)
                }
                
                cargarMovimientos()
            } catch (e: Exception) {
                _uiState.value = MovimientosUiState.Error(e.message ?: "Error al agregar movimiento")
            }
        }
    }

    fun actualizarMovimiento(movimiento: MovimientoEntity) {
        viewModelScope.launch {
            try {
                // Obtener el movimiento anterior para comparar
                val movimientos = gestionarMovimientosUseCase.obtenerMovimientos()
                val movimientoAnterior = movimientos.find { it.id == movimiento.id }
                
                gestionarMovimientosUseCase.actualizarMovimiento(movimiento, "UPDATE", "MovimientosViewModel")
                
                // Aprender del patrón si se asignó una categoría manualmente (nueva o cambiada)
                if (movimiento.categoriaId != null && 
                    (movimientoAnterior?.categoriaId == null || movimientoAnterior.categoriaId != movimiento.categoriaId)) {
                    Log.d("MovimientosViewModel", "🧠 Aprendiendo patrón de clasificación manual actualizada: '${movimiento.descripcion}' -> Categoría ID: ${movimiento.categoriaId}")
                    clasificacionUseCase.aprenderPatron(movimiento.descripcion, movimiento.categoriaId)
                }
                
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
                // Para gastos, sumamos todos los valores (positivos y negativos)
                // Los negativos representan reversas y reducen el gasto total
                gastos += movimiento.monto
            }
        }

        _totales.value = Totales(
            ingresos = ingresos,
            gastos = abs(gastos), // Mostramos el valor absoluto para el display
            balance = ingresos - abs(gastos)
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

    suspend fun obtenerMovimientos(): List<MovimientoEntity> {
        return withContext(Dispatchers.IO) {
            gestionarMovimientosUseCase.obtenerMovimientos()
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