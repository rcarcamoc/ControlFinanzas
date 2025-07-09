package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity
import com.aranthalion.controlfinanzas.domain.usecase.GestionarPresupuestosUseCase
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import com.aranthalion.controlfinanzas.domain.usecase.PresupuestoCategoria
import com.aranthalion.controlfinanzas.domain.usecase.ResumenPresupuestos
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PresupuestosViewModel @Inject constructor(
    private val gestionarPresupuestosUseCase: GestionarPresupuestosUseCase,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PresupuestosUiState>(PresupuestosUiState.Loading)
    val uiState: StateFlow<PresupuestosUiState> = _uiState.asStateFlow()

    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias.asStateFlow()

    private val _presupuestosPorCategoria = MutableStateFlow<Map<Long, PresupuestoCategoriaEntity>>(emptyMap())
    val presupuestosPorCategoria: StateFlow<Map<Long, PresupuestoCategoriaEntity>> = _presupuestosPorCategoria.asStateFlow()

    private val _resumen = MutableStateFlow<ResumenPresupuestos?>(null)
    val resumen: StateFlow<ResumenPresupuestos?> = _resumen.asStateFlow()

    private val _presupuestosCompletos = MutableStateFlow<List<PresupuestoCategoria>>(emptyList())
    val presupuestosCompletos: StateFlow<List<PresupuestoCategoria>> = _presupuestosCompletos.asStateFlow()

    fun cargarPresupuestos(periodo: String) {
        viewModelScope.launch {
            try {
                println("🔍 PRESUPUESTO: Cargando presupuestos para periodo: $periodo")
                val categorias = categoriaRepository.obtenerCategorias()
                val categoriasUnicas = categorias.distinctBy { it.nombre.trim().lowercase() }
                val presupuestos = gestionarPresupuestosUseCase.obtenerPresupuestosPorPeriodo(periodo)
                val presupuestosMap = presupuestos.associateBy { it.categoriaId }
                val resumen = gestionarPresupuestosUseCase.obtenerResumenPresupuestos(periodo)
                val presupuestosCompletos = gestionarPresupuestosUseCase.obtenerEstadoPresupuestos(periodo)
                println("🔍 PRESUPUESTO: Resumen obtenido: totalPresupuestado=${resumen.totalPresupuestado}, totalGastado=${resumen.totalGastado}, porcentaje=${resumen.porcentajeGastado}")
                _categorias.value = categoriasUnicas
                _presupuestosPorCategoria.value = presupuestosMap
                _resumen.value = resumen
                _presupuestosCompletos.value = presupuestosCompletos
                _uiState.value = PresupuestosUiState.Success
            } catch (e: Exception) {
                println("❌ PRESUPUESTO: ${e.message}")
                _uiState.value = PresupuestosUiState.Error(e.message ?: "Error al cargar presupuestos")
            }
        }
    }

    fun guardarPresupuesto(categoriaId: Long, monto: Double, periodo: String) {
        viewModelScope.launch {
            try {
                // Guardar para el mes actual
                val entityActual = PresupuestoCategoriaEntity(
                    categoriaId = categoriaId,
                    monto = monto,
                    periodo = periodo
                )
                gestionarPresupuestosUseCase.guardarPresupuesto(entityActual)

                // Replicar para los próximos 2 meses si no existe presupuesto
                val formato = java.text.SimpleDateFormat("yyyy-MM")
                val fechaBase = formato.parse(periodo)
                val calendar = java.util.Calendar.getInstance()
                calendar.time = fechaBase
                for (i in 1..2) {
                    calendar.add(java.util.Calendar.MONTH, 1)
                    val periodoFuturo = formato.format(calendar.time)
                    val existe = gestionarPresupuestosUseCase.obtenerPresupuestosPorPeriodo(periodoFuturo)
                        .any { it.categoriaId == categoriaId }
                    if (!existe) {
                        val entityFuturo = PresupuestoCategoriaEntity(
                            categoriaId = categoriaId,
                            monto = monto,
                            periodo = periodoFuturo
                        )
                        gestionarPresupuestosUseCase.guardarPresupuesto(entityFuturo)
                    }
                }
                cargarPresupuestos(periodo)
            } catch (e: Exception) {
                _uiState.value = PresupuestosUiState.Error(e.message ?: "Error al guardar presupuesto")
            }
        }
    }

    // Lazy copy: si se navega a un mes futuro sin presupuesto, replicar el último valor conocido
    fun lazyCopyPresupuestoSiNoExiste(categoriaId: Long, periodo: String) {
        viewModelScope.launch {
            val existe = gestionarPresupuestosUseCase.obtenerPresupuestosPorPeriodo(periodo)
                .any { it.categoriaId == categoriaId }
            if (!existe) {
                // Buscar el último presupuesto anterior
                val formato = java.text.SimpleDateFormat("yyyy-MM")
                val calendar = java.util.Calendar.getInstance()
                calendar.time = formato.parse(periodo) ?: return@launch
                for (i in 1..24) { // Buscar hasta 2 años atrás
                    calendar.add(java.util.Calendar.MONTH, -1)
                    val periodoAnterior = formato.format(calendar.time)
                    val presupuestos = gestionarPresupuestosUseCase.obtenerPresupuestosPorPeriodo(periodoAnterior)
                    val anterior = presupuestos.find { it.categoriaId == categoriaId }
                    if (anterior != null) {
                        val entity = PresupuestoCategoriaEntity(
                            categoriaId = categoriaId,
                            monto = anterior.monto,
                            periodo = periodo
                        )
                        gestionarPresupuestosUseCase.guardarPresupuesto(entity)
                        break
                    }
                }
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

    fun actualizarPresupuestoCategoria(categoriaId: Long, presupuesto: Double?, periodo: String) {
        viewModelScope.launch {
            try {
                gestionarPresupuestosUseCase.actualizarPresupuestoCategoria(categoriaId, presupuesto)
                // Recargar presupuestos
                cargarPresupuestos(periodo)
            } catch (e: Exception) {
                _uiState.value = PresupuestosUiState.Error(e.message ?: "Error al actualizar presupuesto")
            }
        }
    }

    fun obtenerCategoriasConAlerta(periodo: String) {
        viewModelScope.launch {
            try {
                val alertas = gestionarPresupuestosUseCase.obtenerCategoriasConAlerta(periodo)
                // Aquí podrías manejar las alertas como notificaciones
            } catch (e: Exception) {
                // Manejo de error
            }
        }
    }
}

sealed class PresupuestosUiState {
    object Loading : PresupuestosUiState()
    object Success : PresupuestosUiState()
    data class Error(val mensaje: String) : PresupuestosUiState()
} 