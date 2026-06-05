package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import java.util.*

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
