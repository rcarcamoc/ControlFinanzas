package com.aranthalion.controlfinanzas.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.remote.ai.AiAnalisisService
import com.aranthalion.controlfinanzas.data.remote.ai.DatosAnalisisMes
import com.aranthalion.controlfinanzas.data.remote.ai.ResumenIa
import com.aranthalion.controlfinanzas.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.remote.api.dto.DashboardResponse

enum class GranularidadTendencia {
    MENSUAL, SEMANAL
}

data class GastoHormigaItem(
    val descripcion: String,
    val totalAcumulado: Double,
    val cantidadCompras: Int,
    val promedioCompra: Double
)

data class ItemTendencia(
    val etiqueta: String,
    val ingresos: Double,
    val gastos: Double,
    val balance: Double
)

enum class TipoAlerta {
    INFO, WARNING, DANGER, SUCCESS
}

data class AlertaAnalisis(
    val tipo: TipoAlerta,
    val titulo: String,
    val descripcion: String
)

sealed class DashboardAnalisisUiState {
    object Loading : DashboardAnalisisUiState()
    data class Success(
        val periodo: String,
        val resumenIa: ResumenIa?,
        val cargandoResumenIa: Boolean,
        val errorResumenIa: String?,
        
        // Dónde gasto
        val distribucionCategorias: List<AnalisisCategoria>,
        
        // Presupuestos
        val presupuestosConBrecha: List<PresupuestoConBrecha>,
        
        // Ritmo de gasto
        val diaActual: Int,
        val diasTotales: Int,
        val presupuestoTotal: Double,
        val gastoActual: Double,
        val proyeccionFinMes: Double,
        val porcentajePresupuestoGastado: Double,
        val porcentajePeriodoTranscurrido: Double,
        val diferenciaRitmo: Double,
        
        // Gastos Hormiga
        val gastosHormiga: List<GastoHormigaItem>,
        val gastosHormigaTotal: Double,
        
        // Tendencia
        val tendenciaMensual: List<ItemTendencia>,
        val tendenciaSemanal: List<ItemTendencia>,
        val granularidadTendencia: GranularidadTendencia = GranularidadTendencia.MENSUAL,
        
        // Alertas
        val alertas: List<AlertaAnalisis>,

        // Movimientos para drill-down
        val movimientos: List<MovimientoEntity> = emptyList()
    ) : DashboardAnalisisUiState()
    data class Error(val mensaje: String, val onRetry: () -> Unit) : DashboardAnalisisUiState()
}

@HiltViewModel
class DashboardAnalisisViewModel @Inject constructor(
    private val analisisFinancieroUseCase: AnalisisFinancieroUseCase,
    private val gestionarMovimientosUseCase: GestionarMovimientosUseCase,
    private val aiAnalisisService: AiAnalisisService,
    private val movimientoRepository: MovimientoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardAnalisisUiState>(DashboardAnalisisUiState.Loading)
    val uiState: StateFlow<DashboardAnalisisUiState> = _uiState.asStateFlow()

    private var lastPeriodo: String? = null
    private var currentGranularidad = GranularidadTendencia.MENSUAL
    fun cargarAnalisis(periodo: String, force: Boolean = false) {
        if (!force && lastPeriodo == periodo && _uiState.value is DashboardAnalisisUiState.Success) {
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = DashboardAnalisisUiState.Loading
                lastPeriodo = periodo
                
                // Calcular días transcurridos y restantes (declarados una sola vez para ambos bloques)
                val partes = periodo.split("-")
                val anio = partes[0].toInt()
                val mes = partes[1].toInt()
                val calendar = Calendar.getInstance()
                val esPeriodoActual = calendar.get(Calendar.YEAR) == anio && (calendar.get(Calendar.MONTH) + 1) == mes

                // Posicionar el calendario en el mes del período para obtener los días correctos
                val calPeriodo = Calendar.getInstance()
                calPeriodo.set(anio, mes - 1, 1)
                val diasTotales = calPeriodo.getActualMaximum(Calendar.DAY_OF_MONTH)
                val diaActual = if (esPeriodoActual) {
                    calendar.get(Calendar.DAY_OF_MONTH)
                } else {
                    diasTotales
                }

                var successState: DashboardAnalisisUiState.Success? = null

                // Intentar obtener los datos del Dashboard pre-procesados desde el servidor
                try {
                    val dashboardData = movimientoRepository.obtenerDashboardData(periodo)
                    
                    val distribucionCategorias = dashboardData.categories.map { cat ->
                        val localCatId = cat.categoryId.toLongOrNull() ?: 0L
                        AnalisisCategoria(
                            categoriaId = localCatId,
                            nombreCategoria = cat.name,
                            totalGastado = cat.amount,
                            porcentajeDelTotal = if (dashboardData.totalExpense > 0) (cat.amount / dashboardData.totalExpense) * 100 else 0.0,
                            promedioDiario = if (diaActual > 0) cat.amount / diaActual else 0.0,
                            tendencia = "ESTABLE"
                        )
                    }

                    val budgetsWithBreach = dashboardData.budgets.map { b ->
                        val pct = if (b.limit > 0) (b.amount / b.limit) * 100 else 0.0
                        val brecha = b.limit - b.amount
                        val diasRestantes = diasTotales - diaActual
                        val proyeccion = if (diaActual > 0) (b.amount / diaActual) * diasTotales else 0.0
                        PresupuestoConBrecha(
                            categoriaId = 0L,
                            nombreCategoria = b.categoryName,
                            presupuesto = b.limit,
                            gastoActual = b.amount,
                            porcentajeGastado = pct,
                            brechaPresupuesto = brecha,
                            ritmoHistorico = 100.0,
                            diasRestantes = diasRestantes,
                            proyeccionMensual = proyeccion
                        )
                    }

                    val presupuestoTotal = budgetsWithBreach.sumOf { it.presupuesto }
                    val gastoActual = dashboardData.totalExpense
                    
                    val dailyRate = if (diaActual > 0) gastoActual / diaActual else 0.0
                    val proyeccionFinMes = dailyRate * diasTotales

                    val porcentajePeriodoTranscurrido = (diaActual.toDouble() / diasTotales) * 100
                    val porcentajePresupuestoGastado = if (presupuestoTotal > 0) (gastoActual / presupuestoTotal) * 100 else 0.0
                    val diferenciaRitmo = porcentajePresupuestoGastado - porcentajePeriodoTranscurrido

                    // Gastos Hormiga y tendencia semanal se calculan rápido de forma local usando cache
                    val movimientosPeriodo = gestionarMovimientosUseCase.obtenerMovimientosPorPeriodoOptimizado(periodo)
                    
                    val gastosDelPeriodo = movimientosPeriodo.filter { 
                        it.tipo == "GASTO" && 
                        abs(it.monto) <= 10000 && 
                        it.tipo != "OMITIR" 
                    }
                    
                    val groupedHormiga = gastosDelPeriodo.groupBy { it.descripcionNormalizada.lowercase().trim() }
                    val gastosHormiga = groupedHormiga.map { (desc, txs) ->
                        val total = txs.sumOf { abs(it.monto) }
                        val count = txs.size
                        val prom = total / count
                        GastoHormigaItem(
                            descripcion = txs.firstOrNull()?.descripcion ?: desc,
                            totalAcumulado = total,
                            cantidadCompras = count,
                            promedioCompra = prom
                        )
                    }.filter { it.cantidadCompras >= 3 }
                     .sortedByDescending { it.totalAcumulado }

                    val gastosHormigaTotal = gastosHormiga.sumOf { it.totalAcumulado }

                    val mesesEsp = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                    val tendenciasMensualesMapeadas = dashboardData.trends.map {
                        val pPartes = it.period.split("-")
                        val pAnio = pPartes[0].substring(2)
                        val pMesIdx = pPartes[1].toInt() - 1
                        val etiqueta = "${mesesEsp[pMesIdx]} $pAnio"
                        ItemTendencia(
                            etiqueta = etiqueta,
                            ingresos = it.income,
                            gastos = it.expense,
                            balance = it.income - it.expense
                        )
                    }

                    // Tendencia semanal (desde base local rápida)
                    val todosMovimientos = gestionarMovimientosUseCase.obtenerMovimientos()
                    
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                    }
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)

                    val semanas = mutableListOf<Pair<Date, Date>>()
                    for (i in 0 until 6) {
                        val inicioSemana = cal.time
                        val calFin = cal.clone() as Calendar
                        calFin.add(Calendar.DAY_OF_YEAR, 7)
                        calFin.add(Calendar.MILLISECOND, -1)
                        val finSemana = calFin.time
                        
                        semanas.add(Pair(inicioSemana, finSemana))
                        cal.add(Calendar.WEEK_OF_YEAR, -1)
                    }
                    
                    val etiquetasRelativas = listOf(
                        "Hace 5s",
                        "Hace 4s",
                        "Hace 3s",
                        "Hace 2s",
                        "Sem ant",
                        "Esta sem"
                    )
                    
                    val semanasCronologicas = semanas.reversed()
                    
                    val tendenciasSemanales = semanasCronologicas.mapIndexed { index, (inicio, fin) ->
                        val movsSemana = todosMovimientos.filter { 
                            it.fecha.time >= inicio.time && it.fecha.time <= fin.time &&
                            it.tipo != "OMITIR" 
                        }
                        val ingresosSemana = movsSemana.filter { it.tipo == "INGRESO" }.sumOf { it.monto }
                        val gastosSemana = movsSemana.filter { it.tipo == "GASTO" }.sumOf { abs(it.monto) }
                        
                        ItemTendencia(
                            etiqueta = etiquetasRelativas.getOrElse(index) { "Sem" },
                            ingresos = ingresosSemana,
                            gastos = gastosSemana,
                            balance = ingresosSemana - gastosSemana
                        )
                    }

                    val alertas = mutableListOf<AlertaAnalisis>()
                    
                    // Presupuestos excedidos
                    val excedidos = budgetsWithBreach.filter { it.gastoActual > it.presupuesto }
                    if (excedidos.isNotEmpty()) {
                        val cats = excedidos.joinToString(", ") { it.nombreCategoria }
                        alertas.add(
                            AlertaAnalisis(
                                tipo = TipoAlerta.DANGER,
                                titulo = "Presupuestos Excedidos",
                                descripcion = "Te has pasado del límite mensual en: $cats."
                            )
                        )
                    }

                    // Ritmo de gasto muy alto
                    if (diferenciaRitmo > 15) {
                        alertas.add(
                            AlertaAnalisis(
                                tipo = TipoAlerta.WARNING,
                                titulo = "Ritmo Acelerado de Gasto",
                                descripcion = "Llevas el ${porcentajePeriodoTranscurrido.toInt()}% del mes transcurrido y has consumido el ${porcentajePresupuestoGastado.toInt()}% del presupuesto total."
                            )
                        )
                    }

                    // Tasa de ahorro baja
                    val tasaAhorro = if (dashboardData.totalIncome > 0) ((dashboardData.totalIncome - dashboardData.totalExpense) / dashboardData.totalIncome) * 100 else 0.0
                    if (tasaAhorro < 10 && dashboardData.totalIncome > 0) {
                        alertas.add(
                            AlertaAnalisis(
                                tipo = TipoAlerta.INFO,
                                titulo = "Tasa de Ahorro Baja",
                                descripcion = "Tu tasa de ahorro actual es de ${tasaAhorro.toInt()}%. Intenta recortar algunos gastos no esenciales."
                            )
                        )
                    }

                    successState = DashboardAnalisisUiState.Success(
                        periodo = periodo,
                        resumenIa = null,
                        cargandoResumenIa = true,
                        errorResumenIa = null,
                        distribucionCategorias = distribucionCategorias.take(5),
                        presupuestosConBrecha = budgetsWithBreach,
                        diaActual = diaActual,
                        diasTotales = diasTotales,
                        presupuestoTotal = presupuestoTotal,
                        gastoActual = gastoActual,
                        proyeccionFinMes = proyeccionFinMes,
                        porcentajePresupuestoGastado = porcentajePresupuestoGastado,
                        porcentajePeriodoTranscurrido = porcentajePeriodoTranscurrido,
                        diferenciaRitmo = diferenciaRitmo,
                        gastosHormiga = gastosHormiga,
                        gastosHormigaTotal = gastosHormigaTotal,
                        tendenciaMensual = tendenciasMensualesMapeadas,
                        tendenciaSemanal = tendenciasSemanales,
                        granularidadTendencia = currentGranularidad,
                        alertas = alertas,
                        movimientos = movimientosPeriodo
                    )
                } catch (apiEx: Exception) {
                    apiEx.printStackTrace()
                }

                if (successState != null) {
                    _uiState.value = successState!!
                    // 8. Cargar Resumen de IA asíncronamente
                    cargarResumenIa(
                        successState!!,
                        ingresos = if (successState!!.presupuestosConBrecha.isNotEmpty()) successState!!.presupuestoTotal else successState!!.gastoActual * 1.2,
                        successState!!.distribucionCategorias,
                        successState!!.presupuestosConBrecha,
                        successState!!.gastosHormigaTotal,
                        successState!!.gastosHormiga.size
                    )
                    return@launch
                }

                // --- FALLBACK A CALCULOS LOCALES ---
                // 1. Cargar datos básicos en paralelo
                val resumenDeferred = async { analisisFinancieroUseCase.obtenerResumenFinancieroPorPeriodo(periodo) }
                val categoriasDeferred = async { analisisFinancieroUseCase.obtenerAnalisisCategorias(periodo) }
                val presupuestosDeferred = async { analisisFinancieroUseCase.obtenerPresupuestosConBrecha(periodo) }
                val movimientosPeriodoDeferred = async { gestionarMovimientosUseCase.obtenerMovimientosPorPeriodoOptimizado(periodo) }
                val tendenciasMensualesDeferred = async { analisisFinancieroUseCase.obtenerTendenciasMensuales(6) }
                val todosMovimientosDeferred = async { gestionarMovimientosUseCase.obtenerMovimientos() }

                val resumen = resumenDeferred.await()
                val analisisCategorias = categoriasDeferred.await()
                val presupuestosConBrecha = presupuestosDeferred.await()
                val movimientosPeriodo = movimientosPeriodoDeferred.await()
                val tendenciasMensuales = tendenciasMensualesDeferred.await()
                val todosMovimientos = todosMovimientosDeferred.await()

                // 3. Ritmo de gasto
                val presupuestoTotal = presupuestosConBrecha.sumOf { it.presupuesto }
                val gastoActual = abs(resumen.gastos)
                
                val dailyRate = if (diaActual > 0) gastoActual / diaActual else 0.0
                val proyeccionFinMes = dailyRate * diasTotales

                val porcentajePeriodoTranscurrido = (diaActual.toDouble() / diasTotales) * 100
                val porcentajePresupuestoGastado = if (presupuestoTotal > 0) (gastoActual / presupuestoTotal) * 100 else 0.0
                val diferenciaRitmo = porcentajePresupuestoGastado - porcentajePeriodoTranscurrido

                // 4. Gastos Hormiga
                // Transacciones <= 10.000 CLP, repetidas >= 3 veces en el período
                val gastosDelPeriodo = movimientosPeriodo.filter { 
                    it.tipo == "GASTO" && 
                    abs(it.monto) <= 10000 && 
                    it.tipo != "OMITIR" 
                }
                
                val groupedHormiga = gastosDelPeriodo.groupBy { it.descripcionNormalizada.lowercase().trim() }
                val gastosHormiga = groupedHormiga.map { (desc, txs) ->
                    val total = txs.sumOf { abs(it.monto) }
                    val count = txs.size
                    val prom = total / count
                    GastoHormigaItem(
                        descripcion = txs.firstOrNull()?.descripcion ?: desc,
                        totalAcumulado = total,
                        cantidadCompras = count,
                        promedioCompra = prom
                    )
                }.filter { it.cantidadCompras >= 3 }
                 .sortedByDescending { it.totalAcumulado }

                val gastosHormigaTotal = gastosHormiga.sumOf { it.totalAcumulado }

                // 5. Tendencia Mensual
                val mesesEsp = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                val tendenciasMensualesMapeadas = tendenciasMensuales.map {
                    val pPartes = it.periodo.split("-")
                    val pAnio = pPartes[0].substring(2)
                    val pMesIdx = pPartes[1].toInt() - 1
                    val etiqueta = "${mesesEsp[pMesIdx]} $pAnio"
                    ItemTendencia(
                        etiqueta = etiqueta,
                        ingresos = it.ingresos,
                        gastos = abs(it.gastos),
                        balance = it.balance
                    )
                }

                // 6. Tendencia Semanal (Lunes a Domingo, 6 semanas, etiquetas compactas)
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                val semanas = mutableListOf<Pair<Date, Date>>()
                for (i in 0 until 6) {
                    val inicioSemana = cal.time
                    val calFin = cal.clone() as Calendar
                    calFin.add(Calendar.DAY_OF_YEAR, 7)
                    calFin.add(Calendar.MILLISECOND, -1)
                    val finSemana = calFin.time
                    
                    semanas.add(Pair(inicioSemana, finSemana))
                    cal.add(Calendar.WEEK_OF_YEAR, -1)
                }
                
                val etiquetasRelativas = listOf(
                    "Hace 5s",
                    "Hace 4s",
                    "Hace 3s",
                    "Hace 2s",
                    "Sem ant",
                    "Esta sem"
                )
                
                val semanasCronologicas = semanas.reversed()
                
                val tendenciasSemanales = semanasCronologicas.mapIndexed { index, (inicio, fin) ->
                    val movsSemana = todosMovimientos.filter { 
                        it.fecha.time >= inicio.time && it.fecha.time <= fin.time &&
                        it.tipo != "OMITIR" 
                    }
                    val ingresosSemana = movsSemana.filter { it.tipo == "INGRESO" }.sumOf { it.monto }
                    val gastosSemana = movsSemana.filter { it.tipo == "GASTO" }.sumOf { abs(it.monto) }
                    
                    ItemTendencia(
                        etiqueta = etiquetasRelativas.getOrElse(index) { "Sem" },
                        ingresos = ingresosSemana,
                        gastos = gastosSemana,
                        balance = ingresosSemana - gastosSemana
                    )
                }

                // 7. Alertas
                val alertas = mutableListOf<AlertaAnalisis>()
                
                // Presupuestos excedidos
                val excedidos = presupuestosConBrecha.filter { it.gastoActual > it.presupuesto }
                if (excedidos.isNotEmpty()) {
                    val cats = excedidos.joinToString(", ") { it.nombreCategoria }
                    alertas.add(
                        AlertaAnalisis(
                            tipo = TipoAlerta.DANGER,
                            titulo = "Presupuestos Excedidos",
                            descripcion = "Te has pasado del límite mensual en: $cats."
                        )
                    )
                }

                // Ritmo de gasto muy alto
                if (diferenciaRitmo > 15) {
                    alertas.add(
                        AlertaAnalisis(
                            tipo = TipoAlerta.WARNING,
                            titulo = "Ritmo Acelerado de Gasto",
                            descripcion = "Llevas el ${porcentajePeriodoTranscurrido.toInt()}% del mes transcurrido y has consumido el ${porcentajePresupuestoGastado.toInt()}% del presupuesto total."
                        )
                    )
                }

                // Tasa de ahorro baja
                if (resumen.tasaAhorro < 10 && resumen.ingresos > 0) {
                    alertas.add(
                        AlertaAnalisis(
                            tipo = TipoAlerta.INFO,
                            titulo = "Tasa de Ahorro Baja",
                            descripcion = "Tu tasa de ahorro actual es de ${resumen.tasaAhorro.toInt()}%. Intenta recortar algunos gastos no esenciales."
                        )
                    )
                }

                successState = DashboardAnalisisUiState.Success(
                    periodo = periodo,
                    resumenIa = null,
                    cargandoResumenIa = true,
                    errorResumenIa = null,
                    distribucionCategorias = analisisCategorias.take(5),
                    presupuestosConBrecha = presupuestosConBrecha,
                    diaActual = diaActual,
                    diasTotales = diasTotales,
                    presupuestoTotal = presupuestoTotal,
                    gastoActual = gastoActual,
                    proyeccionFinMes = proyeccionFinMes,
                    porcentajePresupuestoGastado = porcentajePresupuestoGastado,
                    porcentajePeriodoTranscurrido = porcentajePeriodoTranscurrido,
                    diferenciaRitmo = diferenciaRitmo,
                    gastosHormiga = gastosHormiga,
                    gastosHormigaTotal = gastosHormigaTotal,
                    tendenciaMensual = tendenciasMensualesMapeadas,
                    tendenciaSemanal = tendenciasSemanales,
                    granularidadTendencia = currentGranularidad,
                    alertas = alertas,
                    movimientos = movimientosPeriodo
                )
                
                _uiState.value = successState!!
                
                // 8. Cargar Resumen de IA asíncronamente
                cargarResumenIa(successState!!, resumen.ingresos, analisisCategorias, presupuestosConBrecha, gastosHormigaTotal, gastosHormiga.size)

            } catch (e: Exception) {
                _uiState.value = DashboardAnalisisUiState.Error(
                    mensaje = e.message ?: "Error al cargar análisis financiero",
                    onRetry = { cargarAnalisis(periodo, force = true) }
                )
            }
        }
    }

    private fun cargarResumenIa(
        state: DashboardAnalisisUiState.Success,
        ingresos: Double,
        analisisCategorias: List<AnalisisCategoria>,
        presupuestosConBrecha: List<PresupuestoConBrecha>,
        gastosHormigaTotal: Double,
        gastosHormigaCantidad: Int
    ) {
        viewModelScope.launch {
            try {
                val topCat = analisisCategorias.firstOrNull()
                val excedidosList = presupuestosConBrecha.filter { it.gastoActual > it.presupuesto }.map { it.nombreCategoria }
                val tasaAhorro = if (ingresos > 0) ((ingresos - state.gastoActual) / ingresos) * 100 else 0.0

                val datosIa = DatosAnalisisMes(
                    periodo = state.periodo,
                    diaActual = state.diaActual,
                    diasTotales = state.diasTotales,
                    presupuestoTotal = state.presupuestoTotal,
                    gastoActual = state.gastoActual,
                    ingresos = ingresos,
                    proyeccionFinMes = state.proyeccionFinMes,
                    categoriaMasAlta = topCat?.nombreCategoria ?: "",
                    cambioCategoriaMasAlta = topCat?.porcentajeDelTotal ?: 0.0,
                    gastosHormigaTotal = gastosHormigaTotal,
                    gastosHormigaCantidad = gastosHormigaCantidad,
                    tendenciaVsMesAnterior = 0.0,
                    presupuestosExcedidos = excedidosList,
                    tasaAhorro = tasaAhorro
                )

                val resumenIa = aiAnalisisService.generarResumenMes(datosIa)
                
                val currentState = _uiState.value
                if (currentState is DashboardAnalisisUiState.Success && currentState.periodo == state.periodo) {
                    _uiState.value = currentState.copy(
                        resumenIa = resumenIa,
                        cargandoResumenIa = false,
                        errorResumenIa = null
                    )
                }
            } catch (e: Exception) {
                val currentState = _uiState.value
                if (currentState is DashboardAnalisisUiState.Success && currentState.periodo == state.periodo) {
                    _uiState.value = currentState.copy(
                        cargandoResumenIa = false,
                        errorResumenIa = e.message ?: "Error al generar resumen con IA"
                    )
                }
            }
        }
    }

    fun cambiarGranularidadTendencia(granularidad: GranularidadTendencia) {
        currentGranularidad = granularidad
        val currentState = _uiState.value
        if (currentState is DashboardAnalisisUiState.Success) {
            _uiState.value = currentState.copy(granularidadTendencia = granularidad)
        }
    }
}