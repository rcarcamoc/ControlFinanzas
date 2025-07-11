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
        
        // Inicialización segura que NO borra datos del usuario
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verificar si es la primera ejecución
                if (!configuracionPreferences.isFirstRun) {
                    Log.d("ControlFinanzasApp", "📚 Inicializando sistema de clasificación automática...")
                    
                    // Solo cargar categorías por defecto si no existen (NO sobrescribir)
                    Log.d("ControlFinanzasApp", "📋 Verificando categorías por defecto...")
                    categoriasUseCase.insertDefaultCategorias()
                    
                    // SOLO cargar datos históricos si es la primera vez y el usuario lo ha solicitado
                    // NO limpiar ni recargar automáticamente
                    if (configuracionPreferences.historicalDataLoaded && !configuracionPreferences.obtenerDatosCargados()) {
                        Log.d("ControlFinanzasApp", "📊 Cargando datos históricos del CSV (solo primera vez)...")
                        movimientoRepository.cargarDatosHistoricos()
                        configuracionPreferences.guardarDatosCargados(true)
                        Log.d("ControlFinanzasApp", "✅ Datos históricos cargados correctamente")
                    } else {
                        Log.d("ControlFinanzasApp", "ℹ️ Datos históricos ya cargados o no solicitados - preservando datos del usuario")
                    }
                    
                    // Cargar sistema de clasificación automática (solo patrones predefinidos, NO sobrescribir)
                    if (!configuracionPreferences.obtenerClasificacionCargada()) {
                        Log.d("ControlFinanzasApp", "🤖 Cargando patrones de clasificación predefinidos...")
                        clasificacionUseCase.cargarDatosHistoricos()
                        configuracionPreferences.guardarClasificacionCargada(true)
                        Log.d("ControlFinanzasApp", "✅ Sistema de clasificación automática inicializado correctamente")
                    } else {
                        Log.d("ControlFinanzasApp", "ℹ️ Patrones de clasificación ya cargados - preservando patrones del usuario")
                    }
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