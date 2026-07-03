package com.aranthalion.controlfinanzas

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.aranthalion.controlfinanzas.data.remote.email.EmailSyncWorker
import com.aranthalion.controlfinanzas.data.sync.CacheRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.domain.categoria.GestionarCategoriasUseCase
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class ControlFinanzasApp : Application(), Configuration.Provider {

    @Inject
    lateinit var clasificacionUseCase: GestionarClasificacionAutomaticaUseCase

    @Inject
    lateinit var categoriasUseCase: GestionarCategoriasUseCase

    @Inject
    lateinit var movimientoRepository: MovimientoRepository

    @Inject
    lateinit var configuracionPreferences: ConfiguracionPreferences

    // Obligatorio para integración Hilt + WorkManager
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Registra la WorkerFactory de Hilt para que WorkManager pueda
    // instanciar los Workers con inyección de dependencias en background
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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

        // Crear canal de notificaciones
        crearCanalDeNotificaciones()

        // Configurar sincronización en segundo plano si está activada
        if (configuracionPreferences.emailSyncAutoEnabled) {
            val interval = configuracionPreferences.emailSyncIntervalMinutes
            programarSincronizacionCorreo(this, interval)
        }
        if (configuracionPreferences.syncEnabled) {
            programarRefrescoCache(this)
        }
    }

    private fun crearCanalDeNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sincronización de Correo"
            val descriptionText = "Notificaciones para transacciones detectadas por correo electrónico"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("transacciones_canal", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("ControlFinanzasApp", "📢 Canal de notificaciones creado")
        }
    }

    companion object {
        fun programarSincronizacionCorreo(context: Context, intervalMinutes: Int) {
            // Asegurar que el intervalo mínimo sea 15 minutos (límite de WorkManager)
            val finalInterval = if (intervalMinutes < 15) 15 else intervalMinutes
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<EmailSyncWorker>(
                finalInterval.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag("EmailSyncWorkerTag")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "EmailSyncPeriodicWork",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d("ControlFinanzasApp", "✅ Sincronización en segundo plano programada cada $finalInterval minutos")
        }

        fun cancelarSincronizacionCorreo(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("EmailSyncPeriodicWork")
            Log.d("ControlFinanzasApp", "🚫 Sincronización en segundo plano cancelada")
        }

        fun programarRefrescoCache(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<CacheRefreshWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag("CacheRefreshWorkerTag")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "CacheRefreshPeriodicWork",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d("ControlFinanzasApp", "✅ Sincronización de caché programada cada 1 hora")
        }

        fun cancelarRefrescoCache(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("CacheRefreshPeriodicWork")
            Log.d("ControlFinanzasApp", "🚫 Sincronización de caché cancelada")
        }
    }
}