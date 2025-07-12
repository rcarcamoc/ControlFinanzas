package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

// Modelos para insights avanzados
data class InsightComportamiento(
    val tipo: TipoInsight,
    val titulo: String,
    val descripcion: String,
    val valor: Double,
    val unidad: String,
    val severidad: SeveridadInsight,
    val accionRecomendada: String,
    val categoriaId: Long? = null,
    val categoriaNombre: String? = null
)

enum class TipoInsight {
    GASTO_RECURRENTE,
    GASTO_INUSUAL,
    TENDENCIA_NEGATIVA,
    TENDENCIA_POSITIVA,
    OPORTUNIDAD_AHORRO,
    RIESGO_PRESUPUESTO,
    PATRON_TEMPORAL,
    COMPARACION_HISTORICA,
    AGRUPACION_SIMILAR,
    ANOMALIA_DETECTADA
}

enum class SeveridadInsight {
    BAJA,      // Verde - Información
    MEDIA,     // Amarillo - Advertencia
    ALTA,      // Rojo - Crítico
    POSITIVA   // Azul - Bueno
}

data class AgrupacionTransacciones(
    val nombre: String,
    val tipo: TipoAgrupacion,
    val transacciones: List<TransaccionAgrupada>,
    val total: Double,
    val cantidad: Int,
    val promedio: Double,
    val patron: String? = null,
    val categoriaId: Long? = null,
    val categoriaNombre: String? = null
)

enum class TipoAgrupacion {
    POR_DESCRIPCION_SIMILAR,
    POR_MONTO_RANGO,
    POR_DIA_SEMANA,
    POR_HORA_DIA,
    POR_CATEGORIA,
    POR_PATRON_TEMPORAL,
    POR_FRECUENCIA,
    POR_ESTABLECIMIENTO
}

data class TransaccionAgrupada(
    val id: Long,
    val descripcion: String,
    val descripcionLimpia: String?,
    val monto: Double,
    val fecha: Date,
    val categoriaId: Long?,
    val categoriaNombre: String?
)

data class AnalisisPatronTemporal(
    val patron: String,
    val frecuencia: Int,
    val montoPromedio: Double,
    val diasSemana: List<Int>,
    val horasDia: List<Int>,
    val tendencia: String,
    val categoriaId: Long?,
    val categoriaNombre: String?
)

data class RecomendacionPersonalizada(
    val tipo: TipoRecomendacion,
    val titulo: String,
    val descripcion: String,
    val impactoEstimado: Double,
    val dificultad: DificultadImplementacion,
    val prioridad: PrioridadRecomendacion,
    val accionConcreta: String,
    val categoriaId: Long? = null
)

enum class TipoRecomendacion {
    REDUCIR_GASTO,
    OPTIMIZAR_PRESUPUESTO,
    CAMBIAR_HABITO,
    APROVECHAR_OPORTUNIDAD,
    PLANIFICAR_MEJOR,
    DIVERSIFICAR_GASTOS
}

enum class DificultadImplementacion {
    FACIL,      // Cambio inmediato
    MEDIA,      // Requiere planificación
    DIFICIL     // Cambio de hábito
}

enum class PrioridadRecomendacion {
    BAJA,       // Mejora menor
    MEDIA,      // Mejora significativa
    ALTA        // Impacto importante
}

data class ResumenInsights(
    val insightsGenerados: Int,
    val insightsCriticos: Int,
    val agrupacionesEncontradas: Int,
    val recomendacionesGeneradas: Int,
    val scoreComportamiento: Int, // 0-100
    val areasMejora: List<String>,
    val fortalezas: List<String>
)

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
        
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        // 1. Agrupación por descripción similar
        val agrupacionesDescripcion = agruparPorDescripcionSimilar(gastosDelPeriodo, categorias)
        agrupaciones.addAll(agrupacionesDescripcion)
        
        // 2. Agrupación por rango de montos
        val agrupacionesMonto = agruparPorRangoMontos(gastosDelPeriodo, categorias)
        agrupaciones.addAll(agrupacionesMonto)
        
        // 3. Agrupación por día de la semana
        val agrupacionesDiaSemana = agruparPorDiaSemana(gastosDelPeriodo, categorias)
        agrupaciones.addAll(agrupacionesDiaSemana)
        
        // 4. Agrupación por hora del día
        val agrupacionesHora = agruparPorHoraDia(gastosDelPeriodo, categorias)
        agrupaciones.addAll(agrupacionesHora)
        
        // 5. Agrupación por frecuencia
        val agrupacionesFrecuencia = agruparPorFrecuencia(gastosDelPeriodo, categorias)
        agrupaciones.addAll(agrupacionesFrecuencia)
        
        return agrupaciones.sortedByDescending { it.total }
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
        
        val recomendaciones = mutableListOf<RecomendacionPersonalizada>()
        
        // 1. Recomendaciones basadas en presupuestos
        val recomendacionesPresupuesto = generarRecomendacionesPresupuesto(gastosDelPeriodo, presupuestos, categorias)
        recomendaciones.addAll(recomendacionesPresupuesto)
        
        // 2. Recomendaciones basadas en patrones
        val recomendacionesPatrones = generarRecomendacionesPatrones(gastosDelPeriodo, categorias)
        recomendaciones.addAll(recomendacionesPatrones)
        
        // 3. Recomendaciones basadas en oportunidades
        val recomendacionesOportunidades = generarRecomendacionesOportunidades(gastosDelPeriodo, categorias)
        recomendaciones.addAll(recomendacionesOportunidades)
        
        return recomendaciones.sortedByDescending { it.prioridad.ordinal }
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
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
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
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
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
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
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
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
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
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
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
    
    // Métodos de agrupación
    
    private fun agruparPorDescripcionSimilar(
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
    ): List<AgrupacionTransacciones> {
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        // Agrupar por descripción limpia
        val grupos = gastos.groupBy { it.descripcionLimpia ?: it.descripcion }
        
        grupos.forEach { (descripcion, transacciones) ->
            if (transacciones.size >= 2) {
                val categoria = categorias.find { it.id == transacciones.first().categoriaId }
                val total = transacciones.sumOf { abs(it.monto) }
                val promedio = total / transacciones.size
                
                agrupaciones.add(
                    AgrupacionTransacciones(
                        nombre = "Transacciones similares: $descripcion",
                        tipo = TipoAgrupacion.POR_DESCRIPCION_SIMILAR,
                        transacciones = transacciones.map { 
                            TransaccionAgrupada(
                                id = it.id,
                                descripcion = it.descripcion,
                                descripcionLimpia = it.descripcionLimpia,
                                monto = it.monto,
                                fecha = it.fecha,
                                categoriaId = it.categoriaId,
                                categoriaNombre = categoria?.nombre
                            )
                        },
                        total = total,
                        cantidad = transacciones.size,
                        promedio = promedio,
                        patron = descripcion,
                        categoriaId = categoria?.id,
                        categoriaNombre = categoria?.nombre
                    )
                )
            }
        }
        
        return agrupaciones
    }
    
    private fun agruparPorRangoMontos(
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
    ): List<AgrupacionTransacciones> {
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        val rangos = listOf(
            Triple("Gastos pequeños", 0.0, 5000.0),
            Triple("Gastos medianos", 5000.0, 20000.0),
            Triple("Gastos grandes", 20000.0, 100000.0),
            Triple("Gastos muy grandes", 100000.0, Double.MAX_VALUE)
        )
        
        rangos.forEach { (nombre, min, max) ->
            val transaccionesEnRango = gastos.filter { 
                val monto = abs(it.monto)
                monto >= min && monto < max
            }
            
            if (transaccionesEnRango.isNotEmpty()) {
                val total = transaccionesEnRango.sumOf { abs(it.monto) }
                val promedio = total / transaccionesEnRango.size
                
                agrupaciones.add(
                    AgrupacionTransacciones(
                        nombre = nombre,
                        tipo = TipoAgrupacion.POR_MONTO_RANGO,
                        transacciones = transaccionesEnRango.map { 
                            val categoria = categorias.find { cat -> cat.id == it.categoriaId }
                            TransaccionAgrupada(
                                id = it.id,
                                descripcion = it.descripcion,
                                descripcionLimpia = it.descripcionLimpia,
                                monto = it.monto,
                                fecha = it.fecha,
                                categoriaId = it.categoriaId,
                                categoriaNombre = categoria?.nombre
                            )
                        },
                        total = total,
                        cantidad = transaccionesEnRango.size,
                        promedio = promedio,
                        patron = "$${min.toInt()}-${if (max == Double.MAX_VALUE) "∞" else max.toInt()}"
                    )
                )
            }
        }
        
        return agrupaciones
    }
    
    private fun agruparPorDiaSemana(
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
    ): List<AgrupacionTransacciones> {
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        val gastosPorDia = gastos.groupBy { 
            val calendar = Calendar.getInstance()
            calendar.time = it.fecha
            calendar.get(Calendar.DAY_OF_WEEK)
        }
        
        gastosPorDia.forEach { (diaSemana, transacciones) ->
            val nombreDia = obtenerNombreDia(diaSemana)
            val total = transacciones.sumOf { abs(it.monto) }
            val promedio = total / transacciones.size
            
            agrupaciones.add(
                AgrupacionTransacciones(
                    nombre = "Gastos en $nombreDia",
                    tipo = TipoAgrupacion.POR_DIA_SEMANA,
                    transacciones = transacciones.map { 
                        val categoria = categorias.find { cat -> cat.id == it.categoriaId }
                        TransaccionAgrupada(
                            id = it.id,
                            descripcion = it.descripcion,
                            descripcionLimpia = it.descripcionLimpia,
                            monto = it.monto,
                            fecha = it.fecha,
                            categoriaId = it.categoriaId,
                            categoriaNombre = categoria?.nombre
                        )
                    },
                    total = total,
                    cantidad = transacciones.size,
                    promedio = promedio,
                    patron = nombreDia
                )
            )
        }
        
        return agrupaciones
    }
    
    private fun agruparPorHoraDia(
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
    ): List<AgrupacionTransacciones> {
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        val gastosPorHora = gastos.groupBy { 
            val calendar = Calendar.getInstance()
            calendar.time = it.fecha
            calendar.get(Calendar.HOUR_OF_DAY)
        }
        
        gastosPorHora.forEach { (hora, transacciones) ->
            val nombreHora = when {
                hora < 6 -> "Madrugada (0-6h)"
                hora < 12 -> "Mañana (6-12h)"
                hora < 18 -> "Tarde (12-18h)"
                else -> "Noche (18-24h)"
            }
            
            val total = transacciones.sumOf { abs(it.monto) }
            val promedio = total / transacciones.size
            
            agrupaciones.add(
                AgrupacionTransacciones(
                    nombre = "Gastos en $nombreHora",
                    tipo = TipoAgrupacion.POR_HORA_DIA,
                    transacciones = transacciones.map { 
                        val categoria = categorias.find { cat -> cat.id == it.categoriaId }
                        TransaccionAgrupada(
                            id = it.id,
                            descripcion = it.descripcion,
                            descripcionLimpia = it.descripcionLimpia,
                            monto = it.monto,
                            fecha = it.fecha,
                            categoriaId = it.categoriaId,
                            categoriaNombre = categoria?.nombre
                        )
                    },
                    total = total,
                    cantidad = transacciones.size,
                    promedio = promedio,
                    patron = nombreHora
                )
            )
        }
        
        return agrupaciones
    }
    
    private fun agruparPorFrecuencia(
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
    ): List<AgrupacionTransacciones> {
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        val gastosPorCategoria = gastos.groupBy { it.categoriaId }
        
        gastosPorCategoria.forEach { (categoriaId, transacciones) ->
            val categoria = categorias.find { it.id == categoriaId }
            val total = transacciones.sumOf { abs(it.monto) }
            val promedio = total / transacciones.size
            
            val frecuencia = when {
                transacciones.size >= 10 -> "Muy frecuente"
                transacciones.size >= 5 -> "Frecuente"
                transacciones.size >= 2 -> "Ocasional"
                else -> "Único"
            }
            
            agrupaciones.add(
                AgrupacionTransacciones(
                    nombre = "${categoria?.nombre ?: "Sin categoría"} - $frecuencia",
                    tipo = TipoAgrupacion.POR_FRECUENCIA,
                    transacciones = transacciones.map { 
                        TransaccionAgrupada(
                            id = it.id,
                            descripcion = it.descripcion,
                            descripcionLimpia = it.descripcionLimpia,
                            monto = it.monto,
                            fecha = it.fecha,
                            categoriaId = it.categoriaId,
                            categoriaNombre = categoria?.nombre
                        )
                    },
                    total = total,
                    cantidad = transacciones.size,
                    promedio = promedio,
                    patron = frecuencia,
                    categoriaId = categoria?.id,
                    categoriaNombre = categoria?.nombre
                )
            )
        }
        
        return agrupaciones
    }
    
    // Métodos de recomendaciones
    
    private fun generarRecomendacionesPresupuesto(
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        presupuestos: List<com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
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
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
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
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
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
    
    // Métodos auxiliares
    
    private fun detectarPatronesTemporales(
        gastos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>,
        categorias: List<com.aranthalion.controlfinanzas.data.local.entity.Categoria>
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