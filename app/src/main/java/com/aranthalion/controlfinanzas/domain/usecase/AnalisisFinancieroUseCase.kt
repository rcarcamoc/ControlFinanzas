package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

class AnalisisFinancieroUseCase @Inject constructor(
    private val movimientoRepository: MovimientoRepository,
    private val presupuestoRepository: PresupuestoCategoriaRepository,
    private val configuracionPreferences: ConfiguracionPreferences
) {
    
    suspend fun obtenerResumenFinanciero(fechaInicio: Date, fechaFin: Date): ResumenFinanciero {
        val movimientos = movimientoRepository.obtenerMovimientosPorPeriodo(fechaInicio, fechaFin)
        val movimientosFiltrados = movimientos.filter { it.tipo != TipoMovimiento.OMITIR.name }
        return AnalisisFinancieroHelper.calcularResumen(movimientosFiltrados)
    }

    /**
     * Obtiene tendencias mensuales de los últimos N meses
     */
    suspend fun obtenerTendenciasMensuales(cantidadMeses: Int = 6): List<TendenciaMensual> {
        val tendencias = mutableListOf<TendenciaMensual>()
        val calendar = Calendar.getInstance()
        
        for (i in 0 until cantidadMeses) {
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.MONTH, -i)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val periodo = String.format("%04d-%02d", year, month)
            
            val resumen = obtenerResumenFinancieroPorPeriodo(periodo)
            tendencias.add(
                TendenciaMensual(
                    periodo = periodo,
                    ingresos = resumen.ingresos,
                    gastos = resumen.gastos,
                    balance = resumen.balance,
                    tasaAhorro = resumen.tasaAhorro
                )
            )
        }
        
        return tendencias.reversed()
    }

    /**
     * Analiza las categorías con mayor impacto en gastos
     */
    suspend fun obtenerAnalisisCategorias(periodo: String): List<AnalisisCategoria> {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        val gastosDelPeriodo = movimientos.filter { 
            it.tipo == TipoMovimiento.GASTO.name && 
            it.periodoFacturacion == periodo &&
            it.scope == activeScope
        }
        
        val totalGastos = gastosDelPeriodo.sumOf { abs(it.monto) }
        
        return gastosDelPeriodo
            .groupBy { it.categoriaId }
            .map { (categoriaId, movimientos) ->
                val categoria = categorias.find { it.id == categoriaId }
                val totalGastado = movimientos.sumOf { abs(it.monto) }
                val porcentajeDelTotal = if (totalGastos > 0) (totalGastado / totalGastos) * 100 else 0.0
                val promedioDiario = totalGastado / 30.0 // Aproximación mensual
                
                AnalisisCategoria(
                    categoriaId = categoriaId ?: 0,
                    nombreCategoria = categoria?.nombre ?: "Sin categoría",
                    totalGastado = totalGastado,
                    porcentajeDelTotal = porcentajeDelTotal,
                    promedioDiario = promedioDiario,
                    tendencia = calcularTendenciaCategoria(categoriaId, periodo)
                )
            }
            .sortedByDescending { it.totalGastado }
    }

    /**
     * Genera predicciones de gasto para el próximo mes
     */
    suspend fun obtenerPrediccionesGasto(periodoActual: String): List<PrediccionGasto> {
        val categorias = movimientoRepository.obtenerCategorias()
        val movimientos = movimientoRepository.obtenerMovimientos()
        val predicciones = mutableListOf<PrediccionGasto>()
        
        for (categoria in categorias) {
            val historial = AnalisisFinancieroHelper.extraerHistorialCategoria(movimientos, categoria.id, 3) // Últimos 3 meses
            if (historial.isNotEmpty()) {
                val prediccion = AnalisisFinancieroHelper.calcularPrediccionLineal(historial)
                val intervalo = AnalisisFinancieroHelper.calcularIntervaloConfianza(historial, prediccion)
                val confiabilidad = AnalisisFinancieroHelper.calcularConfiabilidad(historial)
                
                predicciones.add(
                    PrediccionGasto(
                        categoriaId = categoria.id,
                        nombreCategoria = categoria.nombre,
                        prediccion = prediccion,
                        intervaloConfianza = intervalo,
                        confiabilidad = confiabilidad
                    )
                )
            }
        }
        
        return predicciones.sortedByDescending { it.prediccion }
    }

    /**
     * Genera KPIs jerárquicos para dashboard ejecutivo
     */
    suspend fun obtenerKPIsJerarquicos(periodo: String): List<KPIFinanciero> {
        val kpis = mutableListOf<KPIFinanciero>()
        
        // Nivel 1: Resumen ejecutivo
        val resumen = obtenerResumenFinancieroPorPeriodo(periodo)
        kpis.add(KPIFinanciero(1, "Balance Neto", 
            FormatUtils.formatMoneyCLP(resumen.balance), 
            "Resultado financiero del período",
            if (resumen.balance > 0) "POSITIVO" else "NEGATIVO",
            if (resumen.balance > 0) "success" else "error"
        ))
        
        kpis.add(KPIFinanciero(1, "Tasa de Ahorro", 
            "${resumen.tasaAhorro.toInt()}%", 
            "Porcentaje de ingresos ahorrados",
            if (resumen.tasaAhorro > 20) "EXCELENTE" else if (resumen.tasaAhorro > 10) "BUENO" else "MEJORAR",
            if (resumen.tasaAhorro > 20) "success" else if (resumen.tasaAhorro > 10) "warning" else "error"
        ))
        
        // Nivel 2: Tendencias
        val tendencias = obtenerTendenciasMensuales(3)
        if (tendencias.size >= 2) {
            val tendenciaGastos = AnalisisFinancieroHelper.calcularTendencia(tendencias.map { it.gastos })
            kpis.add(KPIFinanciero(2, "Tendencia Gastos", 
                tendenciaGastos, 
                "Evolución de gastos últimos 3 meses",
                tendenciaGastos
            ))
        }
        
        // Nivel 3: Detalle por categorías
        val analisisCategorias = obtenerAnalisisCategorias(periodo)
        val topCategoria = analisisCategorias.firstOrNull()
        if (topCategoria != null) {
            kpis.add(KPIFinanciero(3, "Mayor Gasto", 
                topCategoria.nombreCategoria, 
                "${topCategoria.porcentajeDelTotal.toInt()}% del total",
                topCategoria.tendencia
            ))
        }
        
        return kpis
    }

    /**
     * Analiza tendencias temporales con moving averages y detección de outliers
     */
    suspend fun obtenerAnalisisTendenciaTemporal(
        categoriaId: Long? = null, 
        meses: Int = 6
    ): List<AnalisisTendenciaTemporal> {
        val historial = mutableListOf<AnalisisTendenciaTemporal>()
        val calendar = Calendar.getInstance()
        val movimientos = movimientoRepository.obtenerMovimientos()
        
        for (i in 0 until meses) {
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.MONTH, -i)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val periodo = String.format("%04d-%02d", year, month)
            
            val valor = if (categoriaId != null) {
                AnalisisFinancieroHelper.extraerGastoCategoriaPeriodo(movimientos, categoriaId, periodo)
            } else {
                obtenerResumenFinancieroPorPeriodo(periodo).gastos
            }
            
            historial.add(
                AnalisisTendenciaTemporal(
                    periodo = periodo,
                    valor = valor,
                    movingAverage = 0.0, // Se calculará después
                    tendencia = "ESTABLE",
                    volatilidad = 0.0,
                    esOutlier = false
                )
            )
        }
        
        val valores = historial.map { it.valor }
        val movingAverage = AnalisisFinancieroHelper.calcularMovingAverage(valores, 3)
        val volatilidad = AnalisisFinancieroHelper.calcularVolatilidad(valores)
        val outliers = AnalisisFinancieroHelper.detectarOutliers(valores)
        
        return historial.mapIndexed { index, item ->
            item.copy(
                movingAverage = movingAverage.getOrNull(index) ?: item.valor,
                volatilidad = volatilidad,
                esOutlier = outliers.contains(index),
                tendencia = AnalisisFinancieroHelper.calcularTendencia(valores.take(index + 1))
            )
        }.reversed()
    }

    /**
     * Analiza la volatilidad de gastos por categoría
     */
    suspend fun obtenerAnalisisVolatilidad(periodo: String): List<AnalisisVolatilidad> {
        val categorias = movimientoRepository.obtenerCategorias()
        val movimientos = movimientoRepository.obtenerMovimientos()
        val analisis = mutableListOf<AnalisisVolatilidad>()
        
        for (categoria in categorias) {
            val historial = AnalisisFinancieroHelper.extraerHistorialCategoria(movimientos, categoria.id, 6) // Últimos 6 meses
            if (historial.size >= 3) {
                val desviacionEstandar = AnalisisFinancieroHelper.calcularDesviacionEstandar(historial)
                val promedio = historial.average()
                val coeficienteVariacion = if (promedio > 0) desviacionEstandar / promedio else 0.0
                
                analisis.add(
                    AnalisisVolatilidad(
                        categoriaId = categoria.id,
                        nombreCategoria = categoria.nombre,
                        desviacionEstandar = desviacionEstandar,
                        coeficienteVariacion = coeficienteVariacion,
                        nivelVolatilidad = when {
                            coeficienteVariacion < 0.3 -> "BAJA"
                            coeficienteVariacion < 0.7 -> "MEDIA"
                            else -> "ALTA"
                        }
                    )
                )
            }
        }
        
        return analisis.sortedByDescending { it.coeficienteVariacion }
    }

    /**
     * Compara el período actual con el anterior
     */
    suspend fun obtenerComparacionPeriodo(periodoActual: String): ComparacionPeriodo? {
        val calendar = Calendar.getInstance()
        val partes = periodoActual.split("-")
        val year = partes[0].toInt()
        val month = partes[1].toInt()
        
        calendar.set(year, month - 2, 1) // Mes anterior
        val periodoAnterior = String.format("%04d-%02d", 
            calendar.get(Calendar.YEAR), 
            calendar.get(Calendar.MONTH) + 1
        )
        
        val resumenActual = obtenerResumenFinancieroPorPeriodo(periodoActual)
        val resumenAnterior = obtenerResumenFinancieroPorPeriodo(periodoAnterior)
        
        val cambioIngresos = AnalisisFinancieroHelper.calcularCambioPorcentual(resumenAnterior.ingresos, resumenActual.ingresos)
        val cambioGastos = AnalisisFinancieroHelper.calcularCambioPorcentual(resumenAnterior.gastos, resumenActual.gastos)
        val cambioBalance = AnalisisFinancieroHelper.calcularCambioPorcentual(resumenAnterior.balance, resumenActual.balance)
        val cambioTasaAhorro = resumenActual.tasaAhorro - resumenAnterior.tasaAhorro
        
        return ComparacionPeriodo(
            periodoActual = periodoActual,
            periodoAnterior = periodoAnterior,
            cambioIngresos = cambioIngresos,
            cambioGastos = cambioGastos,
            cambioBalance = cambioBalance,
            cambioTasaAhorro = cambioTasaAhorro,
            tendencia = when {
                cambioBalance > 5 -> "MEJORANDO"
                cambioBalance < -5 -> "EMPEORANDO"
                else -> "ESTABLE"
            }
        )
    }

    /**
     * Detecta gastos inusuales (outliers)
     */
    suspend fun obtenerGastosInusuales(periodo: String): List<GastoInusual> {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        
        val gastosDelPeriodo = movimientos.filter { 
            it.tipo == TipoMovimiento.GASTO.name && 
            it.periodoFacturacion == periodo 
        }
        
        val gastosInusuales = mutableListOf<GastoInusual>()
        
        for (gasto in gastosDelPeriodo) {
            val categoria = categorias.find { it.id == gasto.categoriaId }
            val historialCategoria = AnalisisFinancieroHelper.extraerHistorialCategoria(movimientos, gasto.categoriaId ?: 0, 6)
            
            if (historialCategoria.isNotEmpty()) {
                val promedio = historialCategoria.average()
                val desviacion = AnalisisFinancieroHelper.calcularDesviacionEstandar(historialCategoria)
                val montoAbsoluto = abs(gasto.monto)
                val zScore = if (desviacion > 0) (montoAbsoluto - promedio) / desviacion else 0.0
                
                if (zScore > 2.0) { // Outlier si está más de 2 desviaciones estándar
                    gastosInusuales.add(
                        GastoInusual(
                            movimientoId = gasto.id,
                            descripcion = gasto.descripcion,
                            monto = montoAbsoluto,
                            categoria = categoria?.nombre ?: "Sin categoría",
                            fecha = gasto.fecha,
                            desviacion = zScore,
                            factorInusual = zScore / 2.0
                        )
                    )
                }
            }
        }
        
        return gastosInusuales.sortedByDescending { it.factorInusual }
    }

    /**
     * Calcula métricas de rendimiento financiero
     */
    suspend fun obtenerMetricasRendimiento(periodo: String): MetricasRendimiento {
        val resumen = obtenerResumenFinancieroPorPeriodo(periodo)
        val presupuestos = presupuestoRepository.obtenerPresupuestosPorPeriodo(periodo)
        
        // Ratio de liquidez (ingresos / gastos)
        val ratioLiquidez = if (resumen.gastos > 0) resumen.ingresos / resumen.gastos else 0.0
        
        // Ratio de gastos fijos (gastos en categorías esenciales / ingresos)
        val gastosFijos = presupuestos.filter { it.categoriaId in listOf(1L, 2L, 3L) }
            .sumOf { it.monto }
        val ratioGastosFijos = if (resumen.ingresos > 0) gastosFijos / resumen.ingresos else 0.0
        
        // Ratio de ahorro
        val ratioAhorro = resumen.tasaAhorro / 100.0
        
        // Índice de estabilidad (basado en volatilidad de gastos)
        val tendencias = obtenerTendenciasMensuales(3)
        val volatilidad = if (tendencias.size >= 2) {
            AnalisisFinancieroHelper.calcularVolatilidad(tendencias.map { it.gastos })
        } else 0.0
        val indiceEstabilidad = (1 - volatilidad).coerceIn(0.0, 1.0)
        
        // Score financiero (0-100)
        val scoreFinanciero = AnalisisFinancieroHelper.calcularScoreFinanciero(
            ratioLiquidez, ratioGastosFijos, ratioAhorro, indiceEstabilidad
        )
        
        return MetricasRendimiento(
            ratioLiquidez = ratioLiquidez,
            ratioGastosFijos = ratioGastosFijos,
            ratioAhorro = ratioAhorro,
            indiceEstabilidad = indiceEstabilidad,
            scoreFinanciero = scoreFinanciero
        )
    }

    suspend fun obtenerResumenFinancieroPorPeriodo(periodo: String): ResumenFinanciero {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        val movimientosDelPeriodo = movimientos.filter { 
            it.periodoFacturacion == periodo &&
            it.scope == activeScope
        }
        val movimientosFiltrados = movimientosDelPeriodo.filter { it.tipo != TipoMovimiento.OMITIR.name }
        return AnalisisFinancieroHelper.calcularResumen(movimientosFiltrados)
    }

    /**
     * Obtiene histórico de gastos por categoría para análisis de tendencias
     */
    suspend fun obtenerHistoricoGasto(
        periodoActual: String,
        mesesHistorico: Int = 6
    ): List<ResumenHistoricoCategoria> {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        
        val historico = mutableListOf<ResumenHistoricoCategoria>()
        
        for (categoria in categorias) {
            val gastosCategoria = mutableListOf<Double>()
            val periodosAnalizados = mutableSetOf<String>()
            
            // Obtener gastos de los últimos N meses
            for (i in 0 until mesesHistorico) {
                val periodo = AnalisisFinancieroHelper.calcularPeriodoAnterior(periodoActual, i)
                val gastosDelPeriodo = movimientos.filter { 
                    it.periodoFacturacion == periodo &&
                    it.categoriaId == categoria.id &&
                    it.tipo == TipoMovimiento.GASTO.name &&
                    it.tipo != TipoMovimiento.OMITIR.name &&
                    it.scope == activeScope
                }
                
                val totalGasto = abs(gastosDelPeriodo.sumOf { it.monto })
                if (totalGasto > 0) {
                    gastosCategoria.add(totalGasto)
                    periodosAnalizados.add(periodo)
                }
            }
            
            if (gastosCategoria.isNotEmpty()) {
                val promedio = gastosCategoria.average()
                val desviacion = AnalisisFinancieroHelper.calcularDesviacionEstandar(gastosCategoria)
                val maximo = gastosCategoria.maxOrNull() ?: 0.0
                val minimo = gastosCategoria.minOrNull() ?: 0.0
                val tendencia = AnalisisFinancieroHelper.calcularTendenciaHistorica(gastosCategoria)
                
                historico.add(
                    ResumenHistoricoCategoria(
                        categoriaId = categoria.id,
                        nombreCategoria = categoria.nombre,
                        gastoPromedioMensual = promedio,
                        desviacionEstandar = desviacion,
                        gastoMaximo = maximo,
                        gastoMinimo = minimo,
                        mesesAnalizados = periodosAnalizados.size,
                        tendencia = tendencia
                    )
                )
            }
        }
        
        return historico.sortedByDescending { it.gastoPromedioMensual }
    }

    /**
     * Obtiene presupuestos con análisis de brecha vs gasto actual
     */
    suspend fun obtenerPresupuestosConBrecha(periodo: String): List<PresupuestoConBrecha> {
        val budgets = presupuestoRepository.obtenerPresupuestosPorPeriodo(periodo)
        val categorias = movimientoRepository.obtenerCategorias()
        val movimientos = movimientoRepository.obtenerMovimientos()
        val historico = obtenerHistoricoGasto(periodo)
        
        val result = mutableListOf<PresupuestoConBrecha>()
        
        for (presupuesto in budgets) {
            val categoria = categorias.find { it.id == presupuesto.categoriaId }
            if (categoria != null) {
                val gastosActuales = movimientos.filter { 
                    it.periodoFacturacion == periodo &&
                    it.categoriaId == presupuesto.categoriaId &&
                    it.tipo == TipoMovimiento.GASTO.name &&
                    it.tipo != TipoMovimiento.OMITIR.name &&
                    it.scope == presupuesto.scope
                }
                val gastoActual = abs(gastosActuales.sumOf { it.monto })
                
                // Calcular porcentaje gastado
                val porcentajeGastado = if (presupuesto.monto > 0) (gastoActual / presupuesto.monto) * 100 else 0.0
                
                // Calcular brecha vs presupuesto
                val brechaPresupuesto = presupuesto.monto - gastoActual
                
                // Calcular ritmo histórico
                val historicoCategoria = historico.find { it.categoriaId == presupuesto.categoriaId }
                val ritmoHistorico = if (historicoCategoria != null && historicoCategoria.gastoPromedioMensual > 0) {
                    (gastoActual / historicoCategoria.gastoPromedioMensual) * 100
                } else 100.0
                
                // Calcular días restantes y proyección
                val diasRestantes = AnalisisFinancieroHelper.calcularDiasRestantes(periodo)
                val diasTranscurridos = 30 - diasRestantes
                val proyeccionMensual = if (diasTranscurridos > 0) {
                    (gastoActual / diasTranscurridos) * 30
                } else gastoActual
                
                result.add(
                    PresupuestoConBrecha(
                        categoriaId = presupuesto.categoriaId,
                        nombreCategoria = categoria.nombre,
                        presupuesto = presupuesto.monto,
                        gastoActual = gastoActual,
                        porcentajeGastado = porcentajeGastado,
                        brechaPresupuesto = brechaPresupuesto,
                        ritmoHistorico = ritmoHistorico,
                        diasRestantes = diasRestantes,
                        proyeccionMensual = proyeccionMensual
                    )
                )
            }
        }
        
        return result.sortedByDescending { it.porcentajeGastado }
    }

    private suspend fun calcularTendenciaCategoria(categoriaId: Long?, periodo: String): String {
        return "ESTABLE"
    }
}
