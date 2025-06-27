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

@HiltAndroidApp
class ControlFinanzasApp : Application() {
    
    @Inject
    lateinit var clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
    
    @Inject
    lateinit var categoriasUseCase: GestionarCategoriasUseCase
    
    @Inject
    lateinit var movimientoRepository: MovimientoRepository
    
    override fun onCreate() {
        super.onCreate()
        Log.d("ControlFinanzasApp", "🚀 Iniciando aplicación ControlFinanzas")
        
        // Inicializar el sistema de clasificación automática
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ControlFinanzasApp", "📚 Inicializando sistema de clasificación automática...")
                
                // Primero cargar categorías por defecto
                Log.d("ControlFinanzasApp", "📋 Cargando categorías por defecto...")
                categoriasUseCase.insertDefaultCategorias()
                
                // Luego cargar datos históricos de clasificación
                clasificacionUseCase.cargarDatosHistoricos()
                Log.d("ControlFinanzasApp", "✅ Sistema de clasificación automática inicializado correctamente")
                
                // Cargar datos históricos del CSV
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
                
            } catch (e: Exception) {
                // Log del error pero no fallar la aplicación
                Log.e("ControlFinanzasApp", "❌ Error al inicializar sistema: ${e.message}")
                e.printStackTrace()
            }
        }
    }
} 