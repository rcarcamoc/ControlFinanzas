package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.local.entity.SueldoEntity
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.SueldoRepository
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity

data class AporteProporcional(
    val nombrePersona: String,
    val sueldo: Double,
    val porcentajeAporte: Double,
    val montoAporte: Double
)

data class ResumenAporteProporcional(
    val periodo: String,
    val totalGastosDistribuibles: Double,
    val totalSueldos: Double,
    val aportes: List<AporteProporcional>,
    val fechaCalculo: Date = Date()
)

class AporteProporcionalUseCase @Inject constructor(
    private val sueldoRepository: SueldoRepository,
    private val movimientoRepository: MovimientoRepository
) {
    
    /**
     * Calcula los aportes proporcionales para un período específico
     */
    suspend fun calcularAporteProporcional(periodo: String): ResumenAporteProporcional {
        // Obtener sueldos del período
        val sueldos = sueldoRepository.obtenerSueldosPorPeriodo(periodo)
        if (sueldos.isEmpty()) {
            return ResumenAporteProporcional(
                periodo = periodo,
                totalGastosDistribuibles = 0.0,
                totalSueldos = 0.0,
                aportes = emptyList()
            )
        }

        // Obtener gastos del período excluyendo "Tarjeta titular"
        val gastosDistribuibles = obtenerGastosDistribuibles(periodo)
        val totalGastosDistribuibles = gastosDistribuibles.sumOf { abs(it.monto) }
        val totalSueldos = sueldos.sumOf { it.sueldo }

        // Calcular aportes proporcionales
        val aportes = sueldos.map { sueldo ->
            val porcentajeAporte = if (totalSueldos > 0) (sueldo.sueldo / totalSueldos) * 100 else 0.0
            val montoAporte = if (totalSueldos > 0) (sueldo.sueldo / totalSueldos) * totalGastosDistribuibles else 0.0
            
            AporteProporcional(
                nombrePersona = sueldo.nombrePersona,
                sueldo = sueldo.sueldo,
                porcentajeAporte = porcentajeAporte,
                montoAporte = montoAporte
            )
        }.toMutableList()

        // Calcular total de gastos "Tarjeta titular"
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        val categoriaTarjetaTitular = categorias.find { 
            it.nombre.equals("Tarjeta titular", ignoreCase = true) 
        }
        val totalTarjetaTitular = movimientos.filter { movimiento ->
            movimiento.tipo == "GASTO" &&
            movimiento.periodoFacturacion == periodo &&
            movimiento.categoriaId == categoriaTarjetaTitular?.id
        }.sumOf { abs(it.monto) }

        // Si hay gastos de tarjeta titular, agregar fila extra para Papá
        if (totalTarjetaTitular > 0) {
            aportes.add(
                AporteProporcional(
                    nombrePersona = "Papá + Tarjeta Titular",
                    sueldo = 0.0,
                    porcentajeAporte = 0.0,
                    montoAporte = totalTarjetaTitular
                )
            )
        }

        return ResumenAporteProporcional(
            periodo = periodo,
            totalGastosDistribuibles = totalGastosDistribuibles,
            totalSueldos = totalSueldos,
            aportes = aportes
        )
    }

    /**
     * Obtiene los gastos distribuibles (excluyendo "Tarjeta titular")
     */
    private suspend fun obtenerGastosDistribuibles(periodo: String): List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity> {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        
        // Encontrar la categoría "Tarjeta titular"
        val categoriaTarjetaTitular = categorias.find { 
            it.nombre.equals("Tarjeta titular", ignoreCase = true) 
        }
        
        return movimientos.filter { movimiento ->
            movimiento.tipo == "GASTO" &&
            movimiento.periodoFacturacion == periodo &&
            movimiento.categoriaId != categoriaTarjetaTitular?.id
        }
    }

    /**
     * Obtiene el historial de aportes proporcionales
     */
    suspend fun obtenerHistorialAportes(periodos: List<String>): List<ResumenAporteProporcional> {
        return periodos.mapNotNull { periodo ->
            try {
                calcularAporteProporcional(periodo)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Guarda un sueldo para una persona en un período
     */
    suspend fun guardarSueldo(nombrePersona: String, periodo: String, sueldo: Double) {
        val sueldoEntity = SueldoEntity(
            nombrePersona = nombrePersona,
            periodo = periodo,
            sueldo = sueldo
        )
        sueldoRepository.insertarSueldo(sueldoEntity)

        // Crear movimiento de ingreso asociado al sueldo
        val movimientoIngreso = MovimientoEntity(
            tipo = "INGRESO",
            monto = sueldo,
            descripcion = "Sueldo de $nombrePersona",
            fecha = Date(),
            periodoFacturacion = periodo,
            categoriaId = null, // O puedes asignar una categoría específica si existe
            tipoTarjeta = null,
            idUnico = "sueldo_${nombrePersona}_$periodo"
        )
        movimientoRepository.agregarMovimiento(movimientoIngreso)
    }

    /**
     * Actualiza un sueldo existente
     */
    suspend fun actualizarSueldo(sueldo: SueldoEntity) {
        sueldoRepository.actualizarSueldo(sueldo)
    }

    /**
     * Elimina un sueldo
     */
    suspend fun eliminarSueldo(sueldo: SueldoEntity) {
        sueldoRepository.eliminarSueldo(sueldo)
    }

    /**
     * Obtiene los períodos disponibles
     */
    suspend fun obtenerPeriodosDisponibles(): List<String> {
        return sueldoRepository.obtenerPeriodosDisponibles()
    }

    /**
     * Obtiene las personas disponibles
     */
    suspend fun obtenerPersonasDisponibles(): List<String> {
        return sueldoRepository.obtenerPersonasDisponibles()
    }

    /**
     * Obtiene los sueldos de un período específico
     */
    suspend fun obtenerSueldosPorPeriodo(periodo: String): List<SueldoEntity> {
        return sueldoRepository.obtenerSueldosPorPeriodo(periodo)
    }
} 