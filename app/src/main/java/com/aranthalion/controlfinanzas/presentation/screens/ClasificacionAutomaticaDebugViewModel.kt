package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import com.aranthalion.controlfinanzas.domain.clasificacion.ClasificacionAutomatica
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.presentation.components.ClasificacionMetrics
import com.aranthalion.controlfinanzas.presentation.components.CategoriaMetric
import com.aranthalion.controlfinanzas.presentation.components.PatronMetric
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClasificacionAutomaticaDebugViewModel @Inject constructor(
    private val clasificacionUseCase: GestionarClasificacionAutomaticaUseCase,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ClasificacionDebugUiState())
    val uiState: StateFlow<ClasificacionDebugUiState> = _uiState.asStateFlow()
    
    init {
        cargarDatos()
    }
    
    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val patrones = clasificacionUseCase.obtenerTodosLosPatrones()
                val categorias = categoriaRepository.obtenerCategorias()
                
                val estadisticas = calcularEstadisticas(patrones, categorias)
                val metrics = calcularMetrics(patrones, categorias)
                val patronesFiltrados = aplicarFiltros(patrones)
                
                _uiState.value = _uiState.value.copy(
                    patrones = patronesFiltrados,
                    categorias = categorias,
                    estadisticas = estadisticas,
                    metrics = metrics,
                    isLoading = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar datos: ${e.message}"
                )
            }
        }
    }
    
    fun recargarDatos() {
        cargarDatos()
    }
    
    fun setFiltroCategoria(categoriaId: Long?) {
        _uiState.value = _uiState.value.copy(filtroCategoria = categoriaId)
        aplicarFiltrosYActualizar()
    }
    
    fun setFiltroConfianzaMinima(confianza: Double) {
        _uiState.value = _uiState.value.copy(filtroConfianzaMinima = confianza)
        aplicarFiltrosYActualizar()
    }
    
    fun setOrdenarPor(orden: OrdenPatrones) {
        _uiState.value = _uiState.value.copy(ordenarPor = orden)
        aplicarFiltrosYActualizar()
    }
    
    fun editarPatron(patron: ClasificacionAutomatica) {
        // TODO: Implementar edición de patrones
        // Por ahora solo recargamos los datos
        cargarDatos()
    }
    
    fun eliminarPatron(patron: ClasificacionAutomatica) {
        viewModelScope.launch {
            try {
                // TODO: Implementar eliminación de patrones
                // Por ahora solo recargamos los datos
                cargarDatos()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al eliminar patrón: ${e.message}"
                )
            }
        }
    }
    
    private fun aplicarFiltrosYActualizar() {
        viewModelScope.launch {
            try {
                val patrones = clasificacionUseCase.obtenerTodosLosPatrones()
                val categorias = _uiState.value.categorias
                val patronesFiltrados = aplicarFiltros(patrones)
                val metrics = calcularMetrics(patrones, categorias)
                
                _uiState.value = _uiState.value.copy(
                    patrones = patronesFiltrados,
                    metrics = metrics
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al aplicar filtros: ${e.message}"
                )
            }
        }
    }
    
    private fun aplicarFiltros(patrones: List<ClasificacionAutomatica>): List<ClasificacionAutomatica> {
        var patronesFiltrados = patrones
        
        // Filtro por categoría
        val filtroCategoria = _uiState.value.filtroCategoria
        if (filtroCategoria != null) {
            patronesFiltrados = patronesFiltrados.filter { it.categoriaId == filtroCategoria }
        }
        
        // Filtro por confianza mínima
        val filtroConfianza = _uiState.value.filtroConfianzaMinima
        if (filtroConfianza > 0) {
            patronesFiltrados = patronesFiltrados.filter { it.nivelConfianza >= filtroConfianza }
        }
        
        // Ordenamiento
        val orden = _uiState.value.ordenarPor
        patronesFiltrados = when (orden) {
            OrdenPatrones.CONFIANZA -> patronesFiltrados.sortedByDescending { it.nivelConfianza }
            OrdenPatrones.FRECUENCIA -> patronesFiltrados.sortedByDescending { it.frecuencia }
            OrdenPatrones.FECHA -> patronesFiltrados.sortedByDescending { it.ultimaActualizacion }
            OrdenPatrones.PATRON -> patronesFiltrados.sortedBy { it.patron }
        }
        
        return patronesFiltrados
    }
    
    private fun calcularEstadisticas(
        patrones: List<ClasificacionAutomatica>,
        categorias: List<Categoria>
    ): EstadisticasClasificacion {
        if (patrones.isEmpty()) {
            return EstadisticasClasificacion()
        }
        
        val totalPatrones = patrones.size
        val patronesActivos = patrones.count { it.nivelConfianza >= 0.3 }
        val promedioConfianza = patrones.map { it.nivelConfianza }.average()
        
        // Categoría más usada
        val categoriaMasUsada = patrones
            .groupBy { it.categoriaId }
            .maxByOrNull { it.value.size }
            ?.let { categoriaId ->
                categorias.find { it.id == categoriaId.key }?.nombre ?: "Desconocida"
            } ?: "N/A"
        
        // Patrón más efectivo (mayor confianza)
        val patronMasEfectivo = patrones
            .maxByOrNull { it.nivelConfianza }
            ?.patron ?: "N/A"
        
        // Precisión promedio (simulada basada en confianza)
        val precisionPromedio = patrones
            .filter { it.nivelConfianza >= 0.3 }
            .map { it.nivelConfianza }
            .average()
            .coerceIn(0.0, 1.0)
        
        return EstadisticasClasificacion(
            totalPatrones = totalPatrones,
            patronesActivos = patronesActivos,
            promedioConfianza = promedioConfianza,
            categoriaMasUsada = categoriaMasUsada,
            patronMasEfectivo = patronMasEfectivo,
            precisionPromedio = precisionPromedio
        )
    }
    
    private fun calcularMetrics(
        patrones: List<ClasificacionAutomatica>,
        categorias: List<Categoria>
    ): ClasificacionMetrics {
        if (patrones.isEmpty()) {
            return ClasificacionMetrics(
                totalTransacciones = 0,
                transaccionesClasificadas = 0,
                transaccionesSinClasificar = 0,
                precisionPromedio = 0.0,
                patronesActivos = 0,
                categoriasMasUsadas = emptyList(),
                patronesMasEfectivos = emptyList()
            )
        }
        
        val totalTransacciones = patrones.sumOf { it.frecuencia }
        val transaccionesClasificadas = patrones.filter { it.nivelConfianza >= 0.3 }.sumOf { it.frecuencia }
        val transaccionesSinClasificar = totalTransacciones - transaccionesClasificadas
        val precisionPromedio = patrones.map { it.nivelConfianza }.average()
        val patronesActivos = patrones.count { it.nivelConfianza >= 0.3 }
        
        // Categorías más usadas
        val categoriasMasUsadas = patrones
            .groupBy { it.categoriaId }
            .map { (categoriaId, patronesCategoria) ->
                val categoria = categorias.find { it.id == categoriaId }
                val cantidad = patronesCategoria.sumOf { it.frecuencia }
                val porcentaje = if (totalTransacciones > 0) {
                    (cantidad.toDouble() / totalTransacciones.toDouble()) * 100
                } else 0.0
                
                CategoriaMetric(
                    nombre = categoria?.nombre ?: "Desconocida",
                    cantidad = cantidad,
                    porcentaje = porcentaje
                )
            }
            .sortedByDescending { it.cantidad }
            .take(5)
        
        // Patrones más efectivos
        val patronesMasEfectivos = patrones
            .filter { it.nivelConfianza >= 0.3 }
            .map { patron ->
                val categoria = categorias.find { it.id == patron.categoriaId }
                PatronMetric(
                    patron = patron.patron,
                    categoria = categoria?.nombre ?: "Desconocida",
                    precision = patron.nivelConfianza,
                    frecuencia = patron.frecuencia
                )
            }
            .sortedByDescending { it.precision }
            .take(5)
        
        return ClasificacionMetrics(
            totalTransacciones = totalTransacciones,
            transaccionesClasificadas = transaccionesClasificadas,
            transaccionesSinClasificar = transaccionesSinClasificar,
            precisionPromedio = precisionPromedio,
            patronesActivos = patronesActivos,
            categoriasMasUsadas = categoriasMasUsadas,
            patronesMasEfectivos = patronesMasEfectivos
        )
    }
} 