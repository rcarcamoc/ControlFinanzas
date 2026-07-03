package com.aranthalion.controlfinanzas.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aranthalion.controlfinanzas.data.local.AppDatabase
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.local.entity.*
import com.aranthalion.controlfinanzas.data.remote.api.FinanzasApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date

@HiltWorker
class CacheRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: FinanzasApiService,
    private val db: AppDatabase,
    private val prefs: ConfiguracionPreferences,
    private val queueProcessor: OfflineQueueProcessor
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CacheRefreshWorker"
    }

    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "🔄 CacheRefreshWorker starting...")
            // 1. Process offline operations queue first
            queueProcessor.processQueue()

            // 2. Fetch fresh cache from the server
            val householdId = prefs.syncHouseholdId
            if (householdId.isBlank()) {
                return Result.failure()
            }

            val since = prefs.lastSyncTimestamp.takeIf { it > 0 }
            val overwriteServer = prefs.syncOverwriteAction == "overwrite_server"
            
            val response = api.refresh(
                householdId = householdId,
                since = since,
                overwrite = if (overwriteServer) true else null
            )

            // Clear the overwrite action once processed
            if (prefs.syncOverwriteAction.isNotEmpty()) {
                prefs.syncOverwriteAction = ""
            }

            // 3. Apply changes to Room
            // If it's a full sync, clear existing data
            if (prefs.lastSyncTimestamp == 0L) {
                db.movimientoDao().deleteAllMovimientos()
                db.categoriaDao().eliminarTodasLasCategorias()
                db.presupuestoCategoriaDao().eliminarTodosLosPresupuestos()
                db.sueldoDao().eliminarTodosLosSueldos()
                db.cuentaPorCobrarDao().eliminarTodasLasCuentas()
            }

            // Insert categories
            val localCats = db.categoriaDao().obtenerCategorias()
            response.categories.forEach { c ->
                val existing = localCats.firstOrNull { 
                    it.nombre.trim().lowercase() == c.name.trim().lowercase()
                }
                if (existing == null) {
                    db.categoriaDao().agregarCategoria(
                        Categoria(
                            id = 0, // Auto-generated
                            nombre = c.name,
                            tipo = "Gasto"
                        )
                    )
                }
            }

            // Map categories name to ID
            val allCats = db.categoriaDao().obtenerCategorias()
            val catNameToId = allCats.associate { it.nombre to it.id }

            // Insert transactions
            val localMovimientos = db.movimientoDao().obtenerMovimientos()
            response.transactions.forEach { t ->
                val catId = catNameToId[t.categoryName]
                val existing = localMovimientos.firstOrNull { it.idUnico == t.idUnico }
                val newEntity = MovimientoEntity(
                    id = existing?.id ?: 0,
                    monto = t.amount,
                    fecha = Date(t.date),
                    tipo = t.type,
                    descripcion = t.description,
                    categoriaId = catId,
                    idUnico = t.idUnico,
                    tipoTarjeta = t.cardType,
                    periodoFacturacion = t.billingPeriod,
                    scope = t.scope,
                    userId_internal = t.userId_internal,
                    fechaCreacion = t.createdAt,
                    fechaActualizacion = t.updatedAt
                )
                if (existing != null) {
                    db.movimientoDao().actualizarMovimiento(newEntity)
                } else {
                    db.movimientoDao().agregarMovimiento(newEntity)
                }
            }

            // Delete transactions that are soft deleted on server
            response.deletedIds.forEach { idUnico ->
                db.movimientoDao().eliminarPorIdUnico(idUnico)
            }

            // Save last sync timestamp
            prefs.lastSyncTimestamp = response.serverTimestamp
            Log.d(TAG, "✅ CacheRefreshWorker completed successfully. Timestamp: ${response.serverTimestamp}")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ CacheRefreshWorker failed: ${e.message}", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
