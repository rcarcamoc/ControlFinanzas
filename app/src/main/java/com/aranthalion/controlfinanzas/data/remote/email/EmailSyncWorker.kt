package com.aranthalion.controlfinanzas.data.remote.email

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aranthalion.controlfinanzas.MainActivity
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.domain.usecase.GestionarMovimientosUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class EmailSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val emailFetcherService: EmailFetcherService,
    private val gestionarMovimientosUseCase: GestionarMovimientosUseCase,
    private val configuracionPreferences: ConfiguracionPreferences
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("EmailSyncWorker", "🚀 Iniciando trabajo de sincronización en segundo plano...")

        if (!configuracionPreferences.emailSyncAutoEnabled) {
            Log.d("EmailSyncWorker", "ℹ️ Sincronización automática desactivada en preferencias.")
            return@withContext Result.success()
        }

        try {
            val movimientosDescargados = emailFetcherService.fetchTransactionsFromEmail()
            if (movimientosDescargados.isEmpty()) {
                Log.d("EmailSyncWorker", "ℹ️ No se encontraron movimientos nuevos en los correos.")
                return@withContext Result.success()
            }

            // Usar TODOS los movimientos sin filtro de scope para detectar duplicados
            // correctamente, independientemente del scope activo al momento del Worker.
            val existentes = gestionarMovimientosUseCase.obtenerTodosLosMovimientos()
            var guardados = 0
            for (mov in movimientosDescargados) {
                if (!esDuplicado(mov, existentes)) {
                    gestionarMovimientosUseCase.agregarMovimiento(mov)
                    guardados++
                }
            }

            if (guardados > 0) {
                Log.d("EmailSyncWorker", "✅ Sincronización completada: $guardados nuevos movimientos guardados.")
                mostrarNotificacion(applicationContext, guardados)
            } else {
                Log.d("EmailSyncWorker", "ℹ️ Todos los movimientos descargados eran duplicados.")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("EmailSyncWorker", "❌ Error al ejecutar sincronización en segundo plano", e)
            Result.retry()
        }
    }

    private fun esDuplicado(nuevo: MovimientoEntity, existentes: List<MovimientoEntity>): Boolean {
        if (existentes.any { it.idUnico == nuevo.idUnico }) return true

        val nuevoCal = java.util.Calendar.getInstance().apply { time = nuevo.fecha }
        val nuevoYear = nuevoCal.get(java.util.Calendar.YEAR)
        val nuevoMonth = nuevoCal.get(java.util.Calendar.MONTH)
        val nuevoDay = nuevoCal.get(java.util.Calendar.DAY_OF_MONTH)

        val descNueva = nuevo.descripcion.lowercase().trim()
        val comercioNuevo = descNueva
            .replace("importado correo:", "")
            .replace("importado excel:", "")
            .trim()

        for (existente in existentes) {
            if (existente.monto == nuevo.monto && existente.tipo == nuevo.tipo) {
                val extCal = java.util.Calendar.getInstance().apply { time = existente.fecha }
                val extYear = extCal.get(java.util.Calendar.YEAR)
                val extMonth = extCal.get(java.util.Calendar.MONTH)
                val extDay = extCal.get(java.util.Calendar.DAY_OF_MONTH)

                if (nuevoYear == extYear && nuevoMonth == extMonth && nuevoDay == extDay) {
                    val descExistente = existente.descripcion.lowercase().trim()
                    val comercioExistente = descExistente
                        .replace("importado correo:", "")
                        .replace("importado excel:", "")
                        .trim()

                    if (comercioExistente.isEmpty() || comercioNuevo.isEmpty() ||
                        comercioExistente.contains(comercioNuevo) ||
                        comercioNuevo.contains(comercioExistente) ||
                        comercioExistente.take(5) == comercioNuevo.take(5)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun mostrarNotificacion(context: Context, nuevosMovimientos: Int) {
        val channelId = "transacciones_canal"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Nuevos movimientos importados")
            .setContentText("Se han detectado $nuevosMovimientos transacciones nuevas por clasificar.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(1001, builder.build())
            } catch (e: SecurityException) {
                Log.e("EmailSyncWorker", "❌ No se pudo mostrar la notificación: falta permiso POST_NOTIFICATIONS", e)
            }
        }
    }
}
