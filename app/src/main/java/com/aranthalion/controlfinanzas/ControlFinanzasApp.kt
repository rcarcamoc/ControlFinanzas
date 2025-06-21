package com.aranthalion.controlfinanzas

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase

@HiltAndroidApp
class ControlFinanzasApp : Application() {
    
    @Inject
    lateinit var clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar el sistema de clasificación automática
        CoroutineScope(Dispatchers.IO).launch {
            try {
                clasificacionUseCase.cargarDatosHistoricos()
            } catch (e: Exception) {
                // Log del error pero no fallar la aplicación
                e.printStackTrace()
            }
        }
    }
} 