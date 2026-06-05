package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity
import kotlin.math.abs

object InsightsRecommendationsHelper {

    fun generarRecomendacionesPersonalizadas(
        gastosDelPeriodo: List<MovimientoEntity>,
        presupuestos: List<PresupuestoCategoriaEntity>,
        categorias: List<Categoria>
    ): List<RecomendacionPersonalizada> {
        val recomendaciones = mutableListOf<RecomendacionPersonalizada>()
        
        // 1. Recomendaciones basadas en presupuestos
        recomendaciones.addAll(generarRecomendacionesPresupuesto(gastosDelPeriodo, presupuestos, categorias))
        
        // 2. Recomendaciones basadas en patrones
        recomendaciones.addAll(generarRecomendacionesPatrones(gastosDelPeriodo, categorias))
        
        // 3. Recomendaciones basadas en oportunidades
        recomendaciones.addAll(generarRecomendacionesOportunidades(gastosDelPeriodo, categorias))
        
        return recomendaciones.sortedByDescending { it.prioridad.ordinal }
    }

    private fun generarRecomendacionesPresupuesto(
        gastos: List<MovimientoEntity>,
        presupuestos: List<PresupuestoCategoriaEntity>,
        categorias: List<Categoria>
    ): List<RecomendacionPersonalizada> {
        val recomendaciones = mutableListOf<RecomendacionPersonalizada>()
        
        presupuestos.forEach { presupuesto ->
            val gastosCategoria = gastos.filter { it.categoriaId == presupuesto.categoriaId }
            val totalGastado = gastosCategoria.sumOf { abs(it.monto) }
            val porcentajeGastado = (totalGastado / presupuesto.monto) * 100
            
            if (porcentajeGastado > 80) {
                val categoria = categorias.find { it.id == presupuesto.categoriaId }
                recomendaciones.add(
                    RecomendacionPersonalizada(
                        tipo = TipoRecomendacion.REDUCIR_GASTO,
                        titulo = "Controlar gastos en ${categoria?.nombre}",
                        descripcion = "Has gastado el ${porcentajeGastado.toInt()}% del presupuesto. Considera reducir gastos en esta categoría.",
                        impactoEstimado = presupuesto.monto * 0.2,
                        dificultad = DificultadImplementacion.MEDIA,
                        prioridad = if (porcentajeGastado > 90) PrioridadRecomendacion.ALTA else PrioridadRecomendacion.MEDIA,
                        accionConcreta = "Revisa los últimos gastos en esta categoría y identifica cuáles puedes reducir o eliminar",
                        categoriaId = presupuesto.categoriaId
                    )
                )
            }
        }
        
        return recomendaciones
    }
    
    private fun generarRecomendacionesPatrones(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<RecomendacionPersonalizada> {
        val recomendaciones = mutableListOf<RecomendacionPersonalizada>()
        
        // Detectar gastos pequeños frecuentes
        val gastosPequenos = gastos.filter { abs(it.monto) < 3000 }
        if (gastosPequenos.size >= 8) {
            val totalPequenos = gastosPequenos.sumOf { abs(it.monto) }
            recomendaciones.add(
                RecomendacionPersonalizada(
                    tipo = TipoRecomendacion.OPTIMIZAR_PRESUPUESTO,
                    titulo = "Optimizar gastos pequeños",
                    descripcion = "Tienes ${gastosPequenos.size} gastos pequeños que suman ${totalPequenos.toInt()}. Considera agruparlos.",
                    impactoEstimado = totalPequenos * 0.15,
                    dificultad = DificultadImplementacion.FACIL,
                    prioridad = PrioridadRecomendacion.MEDIA,
                    accionConcreta = "Planifica tus compras para hacer menos transacciones pero de mayor valor"
                )
            )
        }
        
        return recomendaciones
    }
    
    private fun generarRecomendacionesOportunidades(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<RecomendacionPersonalizada> {
        val recomendaciones = mutableListOf<RecomendacionPersonalizada>()
        
        // Detectar categorías con buen comportamiento
        val gastosPorCategoria = gastos.groupBy { it.categoriaId }
        val categoriasConBuenComportamiento = gastosPorCategoria.filter { (_, transacciones) ->
            val promedio = transacciones.map { abs(it.monto) }.average()
            promedio < 10000 // Promedio bajo
        }
        
        if (categoriasConBuenComportamiento.isNotEmpty()) {
            val categoria = categorias.find { it.id == categoriasConBuenComportamiento.keys.first() }
            recomendaciones.add(
                RecomendacionPersonalizada(
                    tipo = TipoRecomendacion.APROVECHAR_OPORTUNIDAD,
                    titulo = "Mantener buen comportamiento",
                    descripcion = "Excelente control en ${categoria?.nombre}. Mantén este patrón de gasto.",
                    impactoEstimado = 0.0,
                    dificultad = DificultadImplementacion.FACIL,
                    prioridad = PrioridadRecomendacion.BAJA,
                    accionConcreta = "Continúa con el mismo nivel de gasto en esta categoría",
                    categoriaId = categoria?.id
                )
            )
        }
        
        return recomendaciones
    }
}
