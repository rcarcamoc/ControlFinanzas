package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

class InsightsAvanzadosUseCase @Inject constructor(
    private val movimientoRepository: MovimientoRepository,
    private val presupuestoRepository: PresupuestoCategoriaRepository
) {
    
    /**
     * Genera insights avanzados basados en análisis de comportamiento
     */
    suspend fun generarInsightsAvanzados(periodo: String): List<InsightComportamiento> {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        
        val gastosDelPeriodo = movimientos.filter { 
            it.tipo == TipoMovimiento.GASTO.name && 
            it.periodoFacturacion == periodo &&
            it.tipo != TipoMovimiento.OMITIR.name
        }
        
        val insights = mutableListOf<InsightComportamiento>()
        
        // 1. Análisis de gastos recurrentes
        val gastosRecurrentes = detectarGastosRecurrentes(gastosDelPeriodo, categorias)
        insights.addAll(gastosRecurrentes)
        
        // 2. Análisis de gastos inusuales
        val gastosInusuales = detectarGastosInusuales(gastosDelPeriodo, categorias)
        insights.addAll(gastosInusuales)
        
        // 3. Análisis de tendencias
        val tendencias = analizarTendencias(gastosDelPeriodo, categorias)
        insights.addAll(tendencias)
        
        // 4. Análisis de patrones temporales
        val patronesTemporales = analizarPatronesTemporales(gastosDelPeriodo, categorias)
        insights.addAll(patronesTemporales)
        
        // 5. Análisis de oportunidades de ahorro
        val oportunidades = detectarOportunidadesAhorro(gastosDelPeriodo, categorias)
        insights.addAll(oportunidades)
        
        return insights.sortedByDescending { it.severidad.ordinal }
    }
    
    /**
     * Genera agrupaciones inteligentes de transacciones
     */
    suspend fun generarAgrupaciones(periodo: String): List<AgrupacionTransacciones> {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        
        val gastosDelPeriodo = movimientos.filter { 
            it.tipo == TipoMovimiento.GASTO.name && 
            it.periodoFacturacion == periodo &&
            it.tipo != TipoMovimiento.OMITIR.name
        }
        
        return InsightsGroupingHelper.generarAgrupaciones(gastosDelPeriodo, categorias)
    }
    
    /**
     * Genera recomendaciones personalizadas basadas en el comportamiento
     */
    suspend fun generarRecomendacionesPersonalizadas(periodo: String): List<RecomendacionPersonalizada> {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        val presupuestos = presupuestoRepository.obtenerPresupuestosPorPeriodo(periodo)
        
        val gastosDelPeriodo = movimientos.filter { 
            it.tipo == TipoMovimiento.GASTO.name && 
            it.periodoFacturacion == periodo &&
            it.tipo != TipoMovimiento.OMITIR.name
        }
        
        return InsightsRecommendationsHelper.generarRecomendacionesPersonalizadas(
            gastosDelPeriodo,
            presupuestos,
            categorias
        )
    }
    
    /**
     * Analiza patrones temporales en las transacciones
     */
    suspend fun analizarPatronesTemporales(periodo: String): List<AnalisisPatronTemporal> {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        
        val gastosDelPeriodo = movimientos.filter { 
            it.tipo == TipoMovimiento.GASTO.name && 
            it.periodoFacturacion == periodo &&
            it.tipo != TipoMovimiento.OMITIR.name
        }
        
        return detectarPatronesTemporales(gastosDelPeriodo, categorias)
    }
    
    /**
     * Genera un resumen completo de insights
     */
    suspend fun generarResumenInsights(periodo: String): ResumenInsights {
        val insights = generarInsightsAvanzados(periodo)
        val agrupaciones = generarAgrupaciones(periodo)
        val recomendaciones = generarRecomendacionesPersonalizadas(periodo)
        
        val insightsCriticos = insights.count { it.severidad == SeveridadInsight.ALTA }
        val areasMejora = insights.filter { it.severidad in listOf(SeveridadInsight.MEDIA, SeveridadInsight.ALTA) }
            .map { it.titulo }
        val fortalezas = insights.filter { it.severidad == SeveridadInsight.POSITIVA }
            .map { it.titulo }
        
        val scoreComportamiento = calcularScoreComportamiento(insights)
        
        return ResumenInsights(
            insightsGenerados = insights.size,
            insightsCriticos = insightsCriticos,
            agrupacionesEncontradas = agrupaciones.size,
            recomendacionesGeneradas = recomendaciones.size,
            scoreComportamiento = scoreComportamiento,
            areasMejora = areasMejora,
            fortalezas = fortalezas
        )
    }
    
    // Métodos privados de análisis
    
    private fun detectarGastosRecurrentes(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<InsightComportamiento> {
        val insights = mutableListOf<InsightComportamiento>()
        
        // Agrupar por descripción limpia
        val gruposPorDescripcion = gastos.groupBy { it.descripcionLimpia ?: it.descripcion }
        
        gruposPorDescripcion.forEach { (descripcion, transacciones) ->
            if (transacciones.size >= 3) {
                val categoria = categorias.find { it.id == transacciones.first().categoriaId }
                val montoPromedio = transacciones.map { abs(it.monto) }.average()
                val frecuencia = transacciones.size
                
                insights.add(
                    InsightComportamiento(
                        tipo = TipoInsight.GASTO_RECURRENTE,
                        titulo = "Gasto Recurrente Detectado",
                        descripcion = "Has realizado $frecuencia compras similares en '$descripcion' con un promedio de ${montoPromedio.toInt()}",
                        valor = montoPromedio,
                        unidad = "CLP promedio",
                        severidad = if (frecuencia > 5) SeveridadInsight.MEDIA else SeveridadInsight.BAJA,
                        accionRecomendada = "Considera establecer un presupuesto específico para este tipo de gasto",
                        categoriaId = categoria?.id,
                        categoriaNombre = categoria?.nombre
                    )
                )
            }
        }
        
        return insights
    }
    
    private fun detectarGastosInusuales(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<InsightComportamiento> {
        val insights = mutableListOf<InsightComportamiento>()
        
        // Calcular estadísticas por categoría
        val estadisticasPorCategoria = gastos.groupBy { it.categoriaId }
            .mapValues { (_, transacciones) ->
                val montos = transacciones.map { abs(it.monto) }
                val promedio = montos.average()
                val desviacion = calcularDesviacionEstandar(montos)
                Triple(promedio, desviacion, montos)
            }
        
        gastos.forEach { gasto ->
            val estadisticas = estadisticasPorCategoria[gasto.categoriaId]
            if (estadisticas != null) {
                val (promedio, desviacion, _) = estadisticas
                val montoAbsoluto = abs(gasto.monto)
                val zScore = if (desviacion > 0) (montoAbsoluto - promedio) / desviacion else 0.0
                
                if (zScore > 2.0) {
                    val categoria = categorias.find { it.id == gasto.categoriaId }
                    insights.add(
                        InsightComportamiento(
                            tipo = TipoInsight.GASTO_INUSUAL,
                            titulo = "Gasto Inusual Detectado",
                            descripcion = "Tu gasto de ${montoAbsoluto.toInt()} en '${gasto.descripcion}' es ${zScore.toInt()} veces mayor al promedio de la categoría",
                            valor = zScore,
                            unidad = "desviaciones estándar",
                            severidad = if (zScore > 3.0) SeveridadInsight.ALTA else SeveridadInsight.MEDIA,
                            accionRecomendada = "Revisa si este gasto fue necesario o si puedes optimizarlo",
                            categoriaId = categoria?.id,
                            categoriaNombre = categoria?.nombre
                        )
                    )
                }
            }
        }
        
        return insights
    }
    
    private fun analizarTendencias(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<InsightComportamiento> {
        val insights = mutableListOf<InsightComportamiento>()
        
        // Agrupar por categoría y analizar tendencias
        val gastosPorCategoria = gastos.groupBy { it.categoriaId }
        
        gastosPorCategoria.forEach { (categoriaId, transacciones) ->
            if (transacciones.size >= 3) {
                val categoria = categorias.find { it.id == categoriaId }
                val montosOrdenados = transacciones.sortedBy { it.fecha }.map { abs(it.monto) }
                val tendencia = calcularTendenciaLineal(montosOrdenados)
                
                if (tendencia > 0.1) {
                    insights.add(
                        InsightComportamiento(
                            tipo = TipoInsight.TENDENCIA_NEGATIVA,
                            titulo = "Tendencia Negativa Detectada",
                            descripcion = "Tus gastos en '${categoria?.nombre ?: "Sin categoría"}' están aumentando un ${(tendencia * 100).toInt()}% por transacción",
                            valor = tendencia * 100,
                            unidad = "% de aumento",
                            severidad = SeveridadInsight.MEDIA,
                            accionRecomendada = "Revisa si puedes reducir la frecuencia o el monto de estos gastos",
                            categoriaId = categoria?.id,
                            categoriaNombre = categoria?.nombre
                        )
                    )
                } else if (tendencia < -0.1) {
                    insights.add(
                        InsightComportamiento(
                            tipo = TipoInsight.TENDENCIA_POSITIVA,
                            titulo = "Tendencia Positiva Detectada",
                            descripcion = "Tus gastos en '${categoria?.nombre ?: "Sin categoría"}' están disminuyendo un ${abs(tendencia * 100).toInt()}% por transacción",
                            valor = abs(tendencia * 100),
                            unidad = "% de disminución",
                            severidad = SeveridadInsight.POSITIVA,
                            accionRecomendada = "¡Excelente! Mantén este comportamiento de ahorro",
                            categoriaId = categoria?.id,
                            categoriaNombre = categoria?.nombre
                        )
                    )
                }
            }
        }
        
        return insights
    }
    
    private fun analizarPatronesTemporales(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<InsightComportamiento> {
        val insights = mutableListOf<InsightComportamiento>()
        
        // Analizar patrones por día de la semana
        val gastosPorDia = gastos.groupBy { 
            val calendar = Calendar.getInstance()
            calendar.time = it.fecha
            calendar.get(Calendar.DAY_OF_WEEK)
        }
        
        val diaMasGasto = gastosPorDia.maxByOrNull { it.value.sumOf { abs(it.monto) } }
        if (diaMasGasto != null) {
            val nombreDia = obtenerNombreDia(diaMasGasto.key)
            val totalDia = diaMasGasto.value.sumOf { abs(it.monto) }
            val promedioOtrosDias = gastosPorDia.filter { it.key != diaMasGasto.key }
                .map { it.value.sumOf { abs(it.monto) } }
                .average()
            
            if (totalDia > promedioOtrosDias * 1.5) {
                insights.add(
                    InsightComportamiento(
                        tipo = TipoInsight.PATRON_TEMPORAL,
                        titulo = "Patrón Temporal Detectado",
                        descripcion = "Los $nombreDia son tus días de mayor gasto (${totalDia.toInt()} vs ${promedioOtrosDias.toInt()} promedio)",
                        valor = (totalDia / promedioOtrosDias - 1) * 100,
                        unidad = "% sobre promedio",
                        severidad = SeveridadInsight.MEDIA,
                        accionRecomendada = "Planifica mejor tus compras para evitar gastos excesivos en $nombreDia"
                    )
                )
            }
        }
        
        return insights
    }
    
    private fun detectarOportunidadesAhorro(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<InsightComportamiento> {
        val insights = mutableListOf<InsightComportamiento>()
        
        // Buscar categorías con gastos pequeños frecuentes
        val gastosPorCategoria = gastos.groupBy { it.categoriaId }
        
        gastosPorCategoria.forEach { (categoriaId, transacciones) ->
            val categoria = categorias.find { it.id == categoriaId }
            val gastosPequenos = transacciones.filter { abs(it.monto) < 5000 } // Gastos menores a 5k
            
            if (gastosPequenos.size >= 5) {
                val totalPequenos = gastosPequenos.sumOf { abs(it.monto) }
                insights.add(
                    InsightComportamiento(
                        tipo = TipoInsight.OPORTUNIDAD_AHORRO,
                        titulo = "Oportunidad de Ahorro",
                        descripcion = "Tienes ${gastosPequenos.size} gastos pequeños en '${categoria?.nombre ?: "Sin categoría"}' que suman ${totalPequenos.toInt()}",
                        valor = totalPequenos,
                        unidad = "CLP en gastos pequeños",
                        severidad = SeveridadInsight.BAJA,
                        accionRecomendada = "Considera agrupar estos gastos pequeños para obtener mejores precios",
                        categoriaId = categoria?.id,
                        categoriaNombre = categoria?.nombre
                    )
                )
            }
        }
        
        return insights
    }
    
    // Métodos auxiliares
    
    private fun detectarPatronesTemporales(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<AnalisisPatronTemporal> {
        val patrones = mutableListOf<AnalisisPatronTemporal>()
        
        // Agrupar por descripción limpia
        val gruposPorDescripcion = gastos.groupBy { it.descripcionLimpia ?: it.descripcion }
        
        gruposPorDescripcion.forEach { (descripcion, transacciones) ->
            if (transacciones.size >= 3) {
                val categoria = categorias.find { it.id == transacciones.first().categoriaId }
                val montoPromedio = transacciones.map { abs(it.monto) }.average()
                
                val diasSemana = transacciones.map { 
                    val calendar = Calendar.getInstance()
                    calendar.time = it.fecha
                    calendar.get(Calendar.DAY_OF_WEEK)
                }.distinct()
                
                val horasDia = transacciones.map { 
                    val calendar = Calendar.getInstance()
                    calendar.time = it.fecha
                    calendar.get(Calendar.HOUR_OF_DAY)
                }.distinct()
                
                val tendencia = calcularTendenciaLineal(transacciones.sortedBy { it.fecha }.map { abs(it.monto) })
                val tendenciaTexto = when {
                    tendencia > 0.1 -> "AUMENTO"
                    tendencia < -0.1 -> "DISMINUCION"
                    else -> "ESTABLE"
                }
                
                patrones.add(
                    AnalisisPatronTemporal(
                        patron = descripcion,
                        frecuencia = transacciones.size,
                        montoPromedio = montoPromedio,
                        diasSemana = diasSemana,
                        horasDia = horasDia,
                        tendencia = tendenciaTexto,
                        categoriaId = categoria?.id,
                        categoriaNombre = categoria?.nombre
                    )
                )
            }
        }
        
        return patrones
    }
    
    private fun calcularDesviacionEstandar(valores: List<Double>): Double {
        if (valores.isEmpty()) return 0.0
        val promedio = valores.average()
        val sumaCuadrados = valores.sumOf { (it - promedio) * (it - promedio) }
        return sqrt(sumaCuadrados / valores.size)
    }
    
    private fun calcularTendenciaLineal(valores: List<Double>): Double {
        if (valores.size < 2) return 0.0
        val n = valores.size
        val sumX = (0 until n).sum()
        val sumY = valores.sum()
        val sumXY = valores.mapIndexed { index, valor -> index * valor }.sum()
        val sumXX = (0 until n).sumOf { it * it }
        
        val pendiente = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
        return pendiente
    }
    
    private fun calcularScoreComportamiento(insights: List<InsightComportamiento>): Int {
        var score = 100
        
        insights.forEach { insight ->
            when (insight.severidad) {
                SeveridadInsight.ALTA -> score -= 20
                SeveridadInsight.MEDIA -> score -= 10
                SeveridadInsight.BAJA -> score -= 5
                SeveridadInsight.POSITIVA -> score += 10
            }
        }
        
        return score.coerceIn(0, 100)
    }
    
    private fun obtenerNombreDia(diaSemana: Int): String {
        return when (diaSemana) {
            Calendar.SUNDAY -> "Domingo"
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            else -> "Desconocido"
        }
    }
}