package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import com.aranthalion.controlfinanzas.domain.usecase.GestionarPresupuestosUseCase
import com.aranthalion.controlfinanzas.domain.usecase.PresupuestoCategoria
import com.aranthalion.controlfinanzas.domain.usecase.ResumenPresupuestos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PresupuestosViewModel @Inject constructor(
    private val gestionarPresupuestosUseCase: GestionarPresupuestosUseCase,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PresupuestosUiState>(PresupuestosUiState.Loading)
    val uiState: StateFlow<PresupuestosUiState> = _uiState.asStateFlow()

    private val _periodoSeleccionado = MutableStateFlow(obtenerPeriodoActual())
    val periodoSeleccionado: StateFlow<String> = _periodoSeleccionado.asStateFlow()

    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias.asStateFlow()

    private val _presupuestosPorCategoria = MutableStateFlow<Map<Long, PresupuestoCategoriaEntity>>(emptyMap())
    val presupuestosPorCategoria: StateFlow<Map<Long, PresupuestoCategoriaEntity>> = _presupuestosPorCategoria.asStateFlow()

    private val _resumen = MutableStateFlow<ResumenPresupuestos?>(null)
    val resumen: StateFlow<ResumenPresupuestos?> = _resumen.asStateFlow()

    private val _periodosDisponibles = MutableStateFlow<List<String>>(generarPeriodosDisponibles())
    val periodosDisponibles: List<String> get() = _periodosDisponibles.value

    init {
        cargarPresupuestos(_periodoSeleccionado.value)
    }

    fun cargarPresupuestos(periodo: String) {
        viewModelScope.launch {
            try {
                _uiState.value = PresupuestosUiState.Loading
                _periodoSeleccionado.value = periodo
                
                val categorias = categoriaRepository.obtenerCategorias()
                val categoriasUnicas = categorias.distinctBy { it.nombre.trim().lowercase() }
                val presupuestos = gestionarPresupuestosUseCase.obtenerPresupuestosPorPeriodo(periodo)
                val resumen = gestionarPresupuestosUseCase.obtenerResumenPresupuestos(periodo)
                
                _categorias.value = categoriasUnicas
                _presupuestosPorCategoria.value = presupuestos.associateBy { it.categoriaId }
                _resumen.value = resumen
                _uiState.value = PresupuestosUiState.Success
                
            } catch (e: Exception) {
                _uiState.value = PresupuestosUiState.Error(e.message ?: "Error al cargar presupuestos")
            }
        }
    }

    fun guardarPresupuesto(categoriaId: Long, monto: Double, periodo: String) {
        viewModelScope.launch {
            try {
                val entity = PresupuestoCategoriaEntity(
                    categoriaId = categoriaId,
                    monto = monto,
                    periodo = periodo
                )
                gestionarPresupuestosUseCase.guardarPresupuesto(entity)
                cargarPresupuestos(periodo)
            } catch (e: Exception) {
                _uiState.value = PresupuestosUiState.Error(e.message ?: "Error al guardar presupuesto")
            }
        }
    }

    fun eliminarPresupuesto(categoriaId: Long, periodo: String) {
        viewModelScope.launch {
            try {
                val entity = _presupuestosPorCategoria.value[categoriaId]
                if (entity != null) {
                    gestionarPresupuestosUseCase.eliminarPresupuesto(entity)
                }
                cargarPresupuestos(periodo)
            } catch (e: Exception) {
                _uiState.value = PresupuestosUiState.Error(e.message ?: "Error al eliminar presupuesto")
            }
        }
    }

    fun actualizarPresupuestoCategoria(categoriaId: Long, presupuesto: Double?) {
        viewModelScope.launch {
            try {
                gestionarPresupuestosUseCase.actualizarPresupuestoCategoria(categoriaId, presupuesto)
                
                // Recargar presupuestos
                cargarPresupuestos(_periodoSeleccionado.value)
                
            } catch (e: Exception) {
                _uiState.value = PresupuestosUiState.Error(e.message ?: "Error al actualizar presupuesto")
            }
        }
    }

    fun obtenerCategoriasConAlerta() {
        viewModelScope.launch {
            try {
                val alertas = gestionarPresupuestosUseCase.obtenerCategoriasConAlerta(_periodoSeleccionado.value)
                // Aquí podrías manejar las alertas como notificaciones
            } catch (e: Exception) {
                // Manejar error silenciosamente
            }
        }
    }

    private fun obtenerPeriodoActual(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    private fun generarPeriodosDisponibles(): List<String> {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val periodos = mutableListOf<String>()
        // Últimos 11 meses + mes actual + 2 futuros
        for (i in -11..2) {
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, 1)
            cal.add(Calendar.MONTH, i)
            val periodo = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            periodos.add(periodo)
        }
        return periodos.sortedDescending()
    }
}

sealed class PresupuestosUiState {
    object Loading : PresupuestosUiState()
    object Success : PresupuestosUiState()
    data class Error(val mensaje: String) : PresupuestosUiState()
} 