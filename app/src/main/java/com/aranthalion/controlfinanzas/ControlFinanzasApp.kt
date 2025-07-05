package com.aranthalion.controlfinanzas

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.domain.categoria.GestionarCategoriasUseCase
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences

@HiltAndroidApp
class ControlFinanzasApp : Application() {
    
    @Inject
    lateinit var clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
    
    @Inject
    lateinit var categoriasUseCase: GestionarCategoriasUseCase
    
    @Inject
    lateinit var movimientoRepository: MovimientoRepository
    
    @Inject
    lateinit var configuracionPreferences: ConfiguracionPreferences
    
    override fun onCreate() {
        super.onCreate()
        Log.d("ControlFinanzasApp", "🚀 Iniciando aplicación ControlFinanzas")
        
        // Solo cargar datos históricos si no es la primera ejecución y se han cargado datos históricos
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verificar si es la primera ejecución
                if (!configuracionPreferences.isFirstRun) {
                    Log.d("ControlFinanzasApp", "📚 Inicializando sistema de clasificación automática...")
                    
                    // Solo cargar categorías por defecto si no existen
                    Log.d("ControlFinanzasApp", "📋 Verificando categorías por defecto...")
                    categoriasUseCase.insertDefaultCategorias()
                    
                    // Solo cargar datos históricos si se han marcado como cargados
                    if (configuracionPreferences.historicalDataLoaded) {
                        Log.d("ControlFinanzasApp", "📊 Cargando datos históricos del CSV...")
                        movimientoRepository.cargarDatosHistoricos()
                        Log.d("ControlFinanzasApp", "✅ Datos históricos cargados correctamente")
                        
                        // Diagnosticar estado actual de datos históricos
                        Log.d("ControlFinanzasApp", "🔍 Diagnosticando datos históricos...")
                        movimientoRepository.diagnosticarDatosHistoricos()
                        
                        // Limpiar y recargar datos históricos con nuevas descripciones
                        Log.d("ControlFinanzasApp", "🔄 Limpiando y recargando datos históricos...")
                        movimientoRepository.limpiarYRecargarDatosHistoricos()
                        Log.d("ControlFinanzasApp", "✅ Datos históricos actualizados correctamente")
                    } else {
                        Log.d("ControlFinanzasApp", "ℹ️ No se cargan datos históricos (instalación limpia)")
                    }
                    
                    // Cargar sistema de clasificación automática
                    clasificacionUseCase.cargarDatosHistoricos()
                    Log.d("ControlFinanzasApp", "✅ Sistema de clasificación automática inicializado correctamente")
                } else {
                    Log.d("ControlFinanzasApp", "🆕 Primera ejecución detectada - esperando configuración del usuario")
                }
                
            } catch (e: Exception) {
                // Log del error pero no fallar la aplicación
                Log.e("ControlFinanzasApp", "❌ Error al inicializar sistema: ${e.message}")
                e.printStackTrace()
            }
        }
    }
} 