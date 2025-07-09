package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClasificacionPendienteViewModel @Inject constructor(
    private val movimientoRepository: MovimientoRepository,
    private val clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ClasificacionPendienteUiState>(ClasificacionPendienteUiState.Loading)
    val uiState: StateFlow<ClasificacionPendienteUiState> = _uiState.asStateFlow()

    init {
        cargarTransaccionesPendientes()
    }

    private fun cargarTransaccionesPendientes() {
        viewModelScope.launch {
            try {
                android.util.Log.d("ClasificacionVM", "🔄 Cargando transacciones pendientes...")
                _uiState.value = ClasificacionPendienteUiState.Loading
                
                // Obtener todas las transacciones sin categoría asignada
                val todasLasTransacciones = movimientoRepository.obtenerMovimientos()
                android.util.Log.d("ClasificacionVM", "📊 Total de transacciones obtenidas: ${todasLasTransacciones.size}")
                
                val transaccionesPendientes = todasLasTransacciones.filter { 
                    it.categoriaId == null || it.categoriaId == 0L 
                }
                android.util.Log.d("ClasificacionVM", "📋 Transacciones pendientes de clasificación: ${transaccionesPendientes.size}")
                
                _uiState.value = ClasificacionPendienteUiState.Success(transaccionesPendientes)
                android.util.Log.d("ClasificacionVM", "✅ Transacciones cargadas exitosamente")
            } catch (e: Exception) {
                android.util.Log.e("ClasificacionVM", "❌ Error al cargar transacciones: ${e.message}")
                e.printStackTrace()
                _uiState.value = ClasificacionPendienteUiState.Error(
                    "Error al cargar transacciones: ${e.message}"
                )
            }
        }
    }

    fun clasificarTransaccion(transaccion: MovimientoEntity, categoriaId: Long) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ClasificacionVM", "🔄 Iniciando clasificación de transacción: ${transaccion.descripcion}")
                android.util.Log.d("ClasificacionVM", "📋 ID de transacción: ${transaccion.id}")
                android.util.Log.d("ClasificacionVM", "🏷️ Categoría seleccionada ID: $categoriaId")
                
                // Actualizar la transacción con la categoría seleccionada
                val transaccionActualizada = transaccion.copy(categoriaId = categoriaId)
                android.util.Log.d("ClasificacionVM", "📝 Transacción actualizada: ${transaccionActualizada.categoriaId}")
                
                try {
                    movimientoRepository.actualizarMovimiento(transaccionActualizada)
                    android.util.Log.d("ClasificacionVM", "✅ Transacción actualizada en la base de datos")
                } catch (e: Exception) {
                    android.util.Log.e("ClasificacionVM", "❌ Error al actualizar movimiento: ${e.message}")
                    throw e
                }
                
                // Aprender del patrón para futuras clasificaciones
                try {
                    android.util.Log.d("ClasificacionVM", "🧠 Aprendiendo patrón: '${transaccion.descripcion}' -> $categoriaId")
                    clasificacionUseCase.aprenderPatron(transaccion.descripcion, categoriaId)
                    android.util.Log.d("ClasificacionVM", "✅ Patrón aprendido exitosamente")
                } catch (e: Exception) {
                    android.util.Log.e("ClasificacionVM", "⚠️ Error al aprender patrón: ${e.message}")
                    e.printStackTrace()
                    // No fallar la clasificación si el aprendizaje falla
                }
                
                // Recargar la lista de transacciones pendientes
                android.util.Log.d("ClasificacionVM", "🔄 Recargando lista de transacciones...")
                cargarTransaccionesPendientes()
                android.util.Log.d("ClasificacionVM", "✅ Clasificación completada exitosamente")
            } catch (e: Exception) {
                android.util.Log.e("ClasificacionVM", "❌ Error al clasificar transacción: ${e.message}")
                android.util.Log.e("ClasificacionVM", "❌ Stack trace: ${e.stackTraceToString()}")
                e.printStackTrace()
                _uiState.value = ClasificacionPendienteUiState.Error(
                    "Error al clasificar transacción: ${e.message}"
                )
            }
        }
    }

    fun recargarTransacciones() {
        cargarTransaccionesPendientes()
    }
} 