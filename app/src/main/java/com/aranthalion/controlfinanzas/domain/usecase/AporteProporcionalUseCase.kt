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
    val totalGastos: Double,
    val totalTarjetaTitular: Double,
    val totalADistribuir: Double,
    val porcentajeGastosSobreSueldo: Double,
    val totalSueldos: Double,
    val aportes: List<AporteProporcional>,
    val aportePapaConTarjetaTitular: Double?,
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
                totalGastos = 0.0,
                totalTarjetaTitular = 0.0,
                totalADistribuir = 0.0,
                porcentajeGastosSobreSueldo = 0.0,
                totalSueldos = 0.0,
                aportes = emptyList(),
                aportePapaConTarjetaTitular = null
            )
        }

        // Obtener todos los movimientos y categorías
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        val categoriaTarjetaTitular = categorias.find { 
            it.nombre.equals("Tarjeta titular", ignoreCase = true) 
        }

        // Calcular total de gastos del período (excluyendo transacciones omitidas)
        val totalGastos = movimientos.filter { 
            it.tipo == "GASTO" && 
            it.periodoFacturacion == periodo &&
            it.tipo != "OMITIR" // Excluir transacciones omitidas
        }.sumOf { it.monto }
        
        // Calcular gastos por categoría (excluyendo transacciones omitidas)
        val gastosPorCategoria = movimientos
            .filter { 
                it.tipo == "GASTO" &&
                it.periodoFacturacion == periodo &&
                it.tipo != "OMITIR" // Excluir transacciones omitidas
            }
            .groupBy { it.categoriaId }
            .mapValues { (_, movimientos) -> movimientos.sumOf { it.monto } }

        // Total tarjeta titular
        val totalTarjetaTitular = movimientos.filter { 
            it.tipo == "GASTO" &&
            it.periodoFacturacion == periodo &&
            it.categoriaId == categoriaTarjetaTitular?.id
        }.sumOf { abs(it.monto) }
        // Total a distribuir
        val totalADistribuir = totalGastos - totalTarjetaTitular
        // Total sueldos
        val totalSueldos = sueldos.sumOf { it.sueldo }
        // % de gastos sobre sueldo (sobre el total a distribuir)
        val porcentajeGastosSobreSueldo = if (totalSueldos > 0) (totalADistribuir / totalSueldos) * 100 else 0.0

        // Calcular aportes proporcionales (solo proporcionales)
        val aportes = sueldos.map { sueldo ->
            val porcentajeAporte = if (totalSueldos > 0) (sueldo.sueldo / totalSueldos) * 100 else 0.0
            val montoAporte = if (totalSueldos > 0) (sueldo.sueldo / totalSueldos) * totalADistribuir else 0.0
            AporteProporcional(
                nombrePersona = sueldo.nombrePersona,
                sueldo = sueldo.sueldo,
                porcentajeAporte = porcentajeAporte,
                montoAporte = montoAporte
            )
        }
        // Calcular el total de Papá + tarjeta titular
        val papa = aportes.find { it.nombrePersona.equals("papá", ignoreCase = true) }
        val aportePapaConTarjetaTitular = papa?.let { it.montoAporte + totalTarjetaTitular }
        return ResumenAporteProporcional(
            periodo = periodo,
            totalGastos = totalGastos,
            totalTarjetaTitular = totalTarjetaTitular,
            totalADistribuir = totalADistribuir,
            porcentajeGastosSobreSueldo = porcentajeGastosSobreSueldo,
            totalSueldos = totalSueldos,
            aportes = aportes,
            aportePapaConTarjetaTitular = aportePapaConTarjetaTitular
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

        // Buscar la categoría "Salario"
        val categorias = movimientoRepository.obtenerCategorias()
        val categoriaSalario = categorias.find { 
            it.nombre.equals("Salario", ignoreCase = true) 
        }

        // Crear movimiento de ingreso asociado al sueldo
        val movimientoIngreso = MovimientoEntity(
            tipo = "INGRESO",
            monto = sueldo,
            descripcion = "Sueldo de $nombrePersona",
            fecha = Date(),
            periodoFacturacion = periodo,
            categoriaId = categoriaSalario?.id, // Asignar la categoría Salario
            tipoTarjeta = null,
            idUnico = "sueldo_${nombrePersona}_$periodo"
        )
        movimientoRepository.agregarMovimiento(movimientoIngreso)
    }

    /**
     * Actualiza un sueldo existente y su movimiento asociado
     */
    suspend fun actualizarSueldo(sueldo: SueldoEntity) {
        // Actualizar la entidad sueldo
        sueldoRepository.actualizarSueldo(sueldo)
        
        // Buscar y actualizar el movimiento asociado
        val movimientos = movimientoRepository.obtenerMovimientos()
        val movimientoAsociado = movimientos.find { 
            it.idUnico == "sueldo_${sueldo.nombrePersona}_${sueldo.periodo}" 
        }
        
        if (movimientoAsociado != null) {
            // Buscar la categoría "Salario"
            val categorias = movimientoRepository.obtenerCategorias()
            val categoriaSalario = categorias.find { 
                it.nombre.equals("Salario", ignoreCase = true) 
            }
            
            val movimientoActualizado = movimientoAsociado.copy(
                monto = sueldo.sueldo,
                descripcion = "Sueldo de ${sueldo.nombrePersona}",
                categoriaId = categoriaSalario?.id
            )
            movimientoRepository.actualizarMovimiento(movimientoActualizado)
        }
    }

    /**
     * Elimina un sueldo y su movimiento asociado
     */
    suspend fun eliminarSueldo(sueldo: SueldoEntity) {
        // Eliminar la entidad sueldo
        sueldoRepository.eliminarSueldo(sueldo)
        
        // Buscar y eliminar el movimiento asociado
        val movimientos = movimientoRepository.obtenerMovimientos()
        val movimientoAsociado = movimientos.find { 
            it.idUnico == "sueldo_${sueldo.nombrePersona}_${sueldo.periodo}" 
        }
        
        if (movimientoAsociado != null) {
            movimientoRepository.eliminarMovimiento(movimientoAsociado)
        }
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

    /**
     * Corrige los movimientos de sueldo existentes que no tienen la categoría "Salario" asignada
     */
    suspend fun corregirMovimientosSueldoSinCategoria() {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        val categoriaSalario = categorias.find { 
            it.nombre.equals("Salario", ignoreCase = true) 
        }
        
        if (categoriaSalario == null) {
            throw IllegalStateException("La categoría 'Salario' no existe en la base de datos")
        }
        
        val movimientosSueldoSinCategoria = movimientos.filter { 
            it.idUnico.startsWith("sueldo_") && it.categoriaId == null 
        }
        
        movimientosSueldoSinCategoria.forEach { movimiento ->
            val movimientoCorregido = movimiento.copy(categoriaId = categoriaSalario.id)
            movimientoRepository.actualizarMovimiento(movimientoCorregido)
        }
    }

    /**
     * Verifica el estado de los movimientos de sueldo y devuelve información de diagnóstico
     */
    suspend fun diagnosticarMovimientosSueldo(): String {
        val movimientos = movimientoRepository.obtenerMovimientos()
        val categorias = movimientoRepository.obtenerCategorias()
        val categoriaSalario = categorias.find { 
            it.nombre.equals("Salario", ignoreCase = true) 
        }
        
        val movimientosSueldo = movimientos.filter { it.idUnico.startsWith("sueldo_") }
        val movimientosSueldoSinCategoria = movimientosSueldo.filter { it.categoriaId == null }
        val movimientosSueldoConCategoria = movimientosSueldo.filter { it.categoriaId != null }
        
        return buildString {
            appendLine("=== Diagnóstico de Movimientos de Sueldo ===")
            appendLine("Total movimientos de sueldo: ${movimientosSueldo.size}")
            appendLine("Movimientos con categoría asignada: ${movimientosSueldoConCategoria.size}")
            appendLine("Movimientos sin categoría: ${movimientosSueldoSinCategoria.size}")
            appendLine("Categoría 'Salario' existe: ${categoriaSalario != null}")
            
            if (categoriaSalario != null) {
                appendLine("ID de categoría 'Salario': ${categoriaSalario.id}")
            }
            
            if (movimientosSueldoSinCategoria.isNotEmpty()) {
                appendLine("\nMovimientos sin categoría:")
                movimientosSueldoSinCategoria.forEach { movimiento ->
                    appendLine("- ${movimiento.descripcion} (${movimiento.idUnico})")
                }
            }
        }
    }
} 