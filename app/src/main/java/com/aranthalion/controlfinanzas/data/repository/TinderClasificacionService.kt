package com.aranthalion.controlfinanzas.data.repository

import android.util.Log
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.util.ExcelTransaction
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.presentation.components.TransaccionTinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TinderClasificacionService @Inject constructor(
    private val movimientoDao: MovimientoDao,
    private val clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
) {
    
    private val transaccionesProcesadas = mutableListOf<TransaccionTinder>()
    private val transaccionesAceptadas = mutableListOf<TransaccionTinder>()
    private val transaccionesRechazadas = mutableListOf<TransaccionTinder>()
    
    /**
     * Obtiene las transacciones pendientes de clasificación
     */
    suspend fun obtenerTransaccionesPendientes(): List<ExcelTransaction> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("TinderService", "🔍 Obteniendo transacciones pendientes...")
                
                // Por ahora, devolver una lista vacía ya que las transacciones
                // se cargan desde el ExcelProcessor
                // En el futuro, esto podría obtener transacciones sin clasificar de la BD
                
                Log.d("TinderService", "📊 Transacciones pendientes obtenidas: 0")
                emptyList()
                
            } catch (e: Exception) {
                Log.e("TinderService", "❌ Error al obtener transacciones pendientes: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * Obtiene las transacciones pendientes usando consultas optimizadas (HITO 1)
     */
    suspend fun obtenerTransaccionesPendientesOptimizado(): List<ExcelTransaction> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("TinderService", "🔍 Obteniendo transacciones pendientes optimizado...")
                
                // Usar consulta optimizada para movimientos sin categoría
                val movimientosSinCategoria = movimientoDao.obtenerMovimientosSinCategoriaOptimizado(limit = 50)
                
                // Convertir a ExcelTransaction
                val transacciones = movimientosSinCategoria.map { movimiento ->
                    ExcelTransaction(
                        fecha = movimiento.fecha,
                        codigoReferencia = null,
                        ciudad = null,
                        descripcion = movimiento.descripcion,
                        tipoTarjeta = movimiento.tipoTarjeta,
                        monto = movimiento.monto,
                        periodoFacturacion = movimiento.periodoFacturacion,
                        categoria = null
                    )
                }
                
                Log.d("TinderService", "📊 Transacciones pendientes optimizadas obtenidas: ${transacciones.size}")
                transacciones
                
            } catch (e: Exception) {
                Log.e("TinderService", "❌ Error al obtener transacciones pendientes optimizadas: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * Registra una transacción aceptada en el Tinder
     */
    suspend fun registrarAceptacion(transaccionTinder: TransaccionTinder) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("TinderService", "✅ Registrando aceptación: '${transaccionTinder.transaccion.descripcion}' -> ${transaccionTinder.categoriaSugerida.nombre}")
                
                // Aprender el patrón
                clasificacionUseCase.aprenderPatron(
                    transaccionTinder.transaccion.descripcion,
                    transaccionTinder.categoriaSugerida.id
                )
                
                // Guardar en la lista de aceptadas
                transaccionesAceptadas.add(transaccionTinder)
                
                // Convertir a MovimientoEntity y guardar en BD
                val movimiento = convertirAMovimientoEntity(transaccionTinder)
                // Buscar si ya existe un movimiento con el mismo idUnico
                val existentes = movimientoDao.obtenerMovimientos().filter { it.idUnico == movimiento.idUnico }
                if (existentes.isNotEmpty()) {
                    val existente = existentes.first()
                    val movimientoActualizado = movimiento.copy(id = existente.id)
                    movimientoDao.actualizarMovimiento(movimientoActualizado)
                    Log.d("TinderService", "⚠️ DUPLICADO: Movimiento actualizado - idUnico: ${movimiento.idUnico}")
                } else {
                    movimientoDao.agregarMovimiento(movimiento)
                    Log.d("TinderService", "📝 Movimiento agregado - idUnico: ${movimiento.idUnico}")
                }
                
                Log.d("TinderService", "✅ Transacción aceptada y guardada exitosamente")
                
            } catch (e: Exception) {
                Log.e("TinderService", "❌ Error al registrar aceptación: ${e.message}")
                throw e
            }
        }
    }
    
    /**
     * Registra una transacción rechazada en el Tinder
     */
    suspend fun registrarRechazo(transaccionTinder: TransaccionTinder) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("TinderService", "❌ Registrando rechazo: '${transaccionTinder.transaccion.descripcion}'")
                
                // Guardar en la lista de rechazadas
                transaccionesRechazadas.add(transaccionTinder)
                
                // Convertir a MovimientoEntity y guardar en BD sin categoría
                val movimiento = convertirAMovimientoEntity(transaccionTinder, asignarCategoria = false)
                movimientoDao.agregarMovimiento(movimiento)
                
                Log.d("TinderService", "❌ Transacción rechazada y guardada sin categoría")
                
            } catch (e: Exception) {
                Log.e("TinderService", "❌ Error al registrar rechazo: ${e.message}")
                throw e
            }
        }
    }
    
    /**
     * Convierte una TransaccionTinder a MovimientoEntity
     */
    private fun convertirAMovimientoEntity(
        transaccionTinder: TransaccionTinder,
        asignarCategoria: Boolean = true
    ): MovimientoEntity {
        return MovimientoEntity(
            tipo = "GASTO", // Por defecto todos los movimientos de tarjeta son gastos
            monto = transaccionTinder.transaccion.monto,
            descripcion = transaccionTinder.transaccion.descripcion,
            fecha = transaccionTinder.transaccion.fecha ?: java.util.Date(),
            periodoFacturacion = transaccionTinder.transaccion.periodoFacturacion ?: "2025-01",
            categoriaId = if (asignarCategoria) transaccionTinder.categoriaSugerida.id else null,
            tipoTarjeta = transaccionTinder.transaccion.tipoTarjeta,
            idUnico = generarIdUnico(transaccionTinder.transaccion)
        )
    }
    
    /**
     * Genera un ID único para la transacción
     */
    private fun generarIdUnico(transaccion: ExcelTransaction): String {
        val fecha = transaccion.fecha?.time ?: System.currentTimeMillis()
        val monto = transaccion.monto
        val descripcion = transaccion.descripcion
        return "$fecha-$monto-$descripcion".hashCode().toString()
    }
    
    /**
     * Obtiene estadísticas del procesamiento del Tinder
     */
    fun obtenerEstadisticas(): EstadisticasTinder {
        return EstadisticasTinder(
            totalProcesadas = transaccionesAceptadas.size + transaccionesRechazadas.size,
            aceptadas = transaccionesAceptadas.size,
            rechazadas = transaccionesRechazadas.size,
            pendientes = 0 // Se calcula dinámicamente
        )
    }
    
    /**
     * Limpia las listas de transacciones procesadas
     */
    fun limpiarTransacciones() {
        transaccionesProcesadas.clear()
        transaccionesAceptadas.clear()
        transaccionesRechazadas.clear()
        Log.d("TinderService", "🧹 Listas de transacciones limpiadas")
    }
    
    /**
     * Obtiene las transacciones aceptadas
     */
    fun obtenerTransaccionesAceptadas(): List<TransaccionTinder> {
        return transaccionesAceptadas.toList()
    }
    
    /**
     * Obtiene las transacciones rechazadas
     */
    fun obtenerTransaccionesRechazadas(): List<TransaccionTinder> {
        return transaccionesRechazadas.toList()
    }
}

data class EstadisticasTinder(
    val totalProcesadas: Int = 0,
    val aceptadas: Int = 0,
    val rechazadas: Int = 0,
    val pendientes: Int = 0
) 