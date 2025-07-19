package com.aranthalion.controlfinanzas.data.repository

import android.util.Log
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.util.ExcelTransaction
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.presentation.components.TransaccionTinder
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TinderPreloadService @Inject constructor(
    private val movimientoDao: MovimientoDao,
    private val clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
) {
    
    private val preloadedTransacciones = mutableListOf<TransaccionTinder>()
    private var isPreloading = false
    private val preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Inicia la precarga de transacciones sin categoría en segundo plano
     */
    fun startPreloading() {
        if (isPreloading) {
            Log.d("TinderPreload", "⏭️ Precarga ya en progreso")
            return
        }
        
        isPreloading = true
        Log.d("TinderPreload", "🚀 Iniciando precarga en segundo plano")
        
        preloadScope.launch {
            try {
                // Obtener transacciones sin categoría
                val movimientos = movimientoDao.obtenerMovimientos()
                val transaccionesSinCategoria = movimientos.filter { it.categoriaId == null }
                
                Log.d("TinderPreload", "📊 Encontradas ${transaccionesSinCategoria.size} transacciones sin categoría")
                
                if (transaccionesSinCategoria.isNotEmpty()) {
                    // Convertir a ExcelTransaction
                    val excelTransactions = transaccionesSinCategoria.take(10).map { movimiento ->
                        ExcelTransaction(
                            fecha = movimiento.fecha,
                            codigoReferencia = movimiento.idUnico,
                            ciudad = null,
                            descripcion = movimiento.descripcion,
                            tipoTarjeta = movimiento.tipoTarjeta,
                            monto = movimiento.monto,
                            periodoFacturacion = movimiento.periodoFacturacion,
                            categoria = null,
                            categoriaId = null,
                            nivelConfianza = null
                        )
                    }
                    
                    // Procesar transacciones en segundo plano
                    val transaccionesTinder = mutableListOf<TransaccionTinder>()
                    
                    excelTransactions.forEach { transaccion ->
                        try {
                            val resultadoClasificacion = clasificacionUseCase.obtenerSugerenciaMejorada(transaccion.descripcion)
                            
                            // Crear TransaccionTinder básica (sin categoría específica por ahora)
                            val transaccionTinder = TransaccionTinder(
                                transaccion = transaccion,
                                categoriaSugerida = com.aranthalion.controlfinanzas.data.local.entity.Categoria(
                                    id = 1,
                                    nombre = "Sin clasificar",
                                    descripcion = "Categoría temporal",
                                    tipo = "Gasto"
                                ),
                                nivelConfianza = 0.0,
                                patron = "Sin patrón",
                                tipoCoincidencia = "PRECARGA"
                            )
                            
                            transaccionesTinder.add(transaccionTinder)
                            Log.d("TinderPreload", "✅ Precargada transacción: ${transaccion.descripcion}")
                            
                        } catch (e: Exception) {
                            Log.e("TinderPreload", "❌ Error precargando transacción: ${e.message}")
                        }
                    }
                    
                    preloadedTransacciones.clear()
                    preloadedTransacciones.addAll(transaccionesTinder)
                    
                    Log.d("TinderPreload", "🎉 Precarga completada: ${preloadedTransacciones.size} transacciones listas")
                }
                
            } catch (e: Exception) {
                Log.e("TinderPreload", "❌ Error en precarga: ${e.message}")
            } finally {
                isPreloading = false
            }
        }
    }
    
    /**
     * Obtiene las transacciones precargadas
     */
    fun getPreloadedTransacciones(): List<TransaccionTinder> {
        Log.d("TinderPreload", "📤 Entregando ${preloadedTransacciones.size} transacciones precargadas")
        return preloadedTransacciones.toList()
    }
    
    /**
     * Verifica si hay transacciones precargadas disponibles
     */
    fun hasPreloadedTransacciones(): Boolean {
        return preloadedTransacciones.isNotEmpty()
    }
    
    /**
     * Limpia las transacciones precargadas
     */
    fun clearPreloadedTransacciones() {
        preloadedTransacciones.clear()
        Log.d("TinderPreload", "🗑️ Transacciones precargadas limpiadas")
    }
    
    /**
     * Detiene la precarga
     */
    fun stopPreloading() {
        preloadScope.cancel()
        isPreloading = false
        Log.d("TinderPreload", "⏹️ Precarga detenida")
    }
} 