package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.domain.clasificacion.ClasificacionAutomaticaRepository
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.CategoriaRepository
import javax.inject.Inject

data class ClasificacionResultado(
    val categoriaId: Long,
    val categoriaNombre: String,
    val confianza: Double,
    val metodo: String
)

class GestionarClasificacionAutomaticaUseCase @Inject constructor(
    private val clasificacionRepository: ClasificacionAutomaticaRepository,
    private val movimientoRepository: MovimientoRepository,
    private val categoriaRepository: CategoriaRepository
) {
    
    suspend fun clasificarMovimiento(descripcion: String, monto: Double): ClasificacionResultado {
        // Implementación simplificada
        return ClasificacionResultado(
            categoriaId = 1L,
            categoriaNombre = "Sin categorizar",
            confianza = 0.0,
            metodo = "Por defecto"
        )
    }
}
