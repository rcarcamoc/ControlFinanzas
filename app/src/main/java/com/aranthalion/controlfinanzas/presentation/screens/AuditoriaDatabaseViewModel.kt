package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.entity.AuditoriaEntity
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.repository.AuditoriaService
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuditoriaDatabaseUiState {
    object Loading : AuditoriaDatabaseUiState()
    data class Success(val data: AuditoriaDatabaseData) : AuditoriaDatabaseUiState()
    data class Error(val mensaje: String) : AuditoriaDatabaseUiState()
}

data class AuditoriaDatabaseData(
    val auditoriaReciente: List<AuditoriaEntity>,
    val movimientosRecientes: List<MovimientoEntity>,
    val tablaSeleccionada: String
)

@HiltViewModel
class AuditoriaDatabaseViewModel @Inject constructor(
    private val movimientoRepository: MovimientoRepository,
    private val auditoriaService: AuditoriaService
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuditoriaDatabaseUiState>(AuditoriaDatabaseUiState.Loading)
    val uiState: StateFlow<AuditoriaDatabaseUiState> = _uiState.asStateFlow()

    fun cargarDatosAuditoria(tabla: String = "movimientos") {
        viewModelScope.launch {
            try {
                println("🔍 AUDITORIA: Cargando datos de auditoría para tabla: $tabla")
                _uiState.value = AuditoriaDatabaseUiState.Loading
                
                // Obtener datos de auditoría según la tabla seleccionada
                val auditoriaReciente = when (tabla) {
                    "movimientos", "todos" -> {
                        val auditoria = auditoriaService.obtenerAuditoriaPorTabla("movimientos")
                        println("🔍 AUDITORIA: Auditoría de movimientos obtenida: ${auditoria.size}")
                        auditoria
                    }
                    "presupuestos" -> {
                        val auditoria = auditoriaService.obtenerAuditoriaPorTabla("presupuestos")
                        println("🔍 AUDITORIA: Auditoría de presupuestos obtenida: ${auditoria.size}")
                        auditoria
                    }
                    "categorias" -> {
                        val auditoria = auditoriaService.obtenerAuditoriaPorTabla("categorias")
                        println("🔍 AUDITORIA: Auditoría de categorías obtenida: ${auditoria.size}")
                        auditoria
                    }
                    "clasificacion" -> {
                        val auditoria = auditoriaService.obtenerAuditoriaPorTabla("clasificacion")
                        println("🔍 AUDITORIA: Auditoría de clasificación obtenida: ${auditoria.size}")
                        auditoria
                    }
                    else -> {
                        val auditoria = auditoriaService.obtenerAuditoriaReciente()
                        println("🔍 AUDITORIA: Auditoría general obtenida: ${auditoria.size}")
                        auditoria
                    }
                }
                
                // Obtener movimientos recientes para mostrar detalles
                val movimientosRecientes = movimientoRepository.obtenerMovimientosRecientes()
                println("🔍 AUDITORIA: Movimientos recientes obtenidos: ${movimientosRecientes.size}")
                
                _uiState.value = AuditoriaDatabaseUiState.Success(
                    AuditoriaDatabaseData(
                        auditoriaReciente = auditoriaReciente,
                        movimientosRecientes = movimientosRecientes,
                        tablaSeleccionada = tabla
                    )
                )
                
            } catch (e: Exception) {
                println("❌ AUDITORIA: Error al cargar datos de auditoría: ${e.message}")
                _uiState.value = AuditoriaDatabaseUiState.Error(e.message ?: "Error al cargar auditoría")
            }
        }
    }
} 