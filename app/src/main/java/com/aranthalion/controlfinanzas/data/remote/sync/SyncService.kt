package com.aranthalion.controlfinanzas.data.remote.sync

import android.content.Context
import android.util.Log
import com.aranthalion.controlfinanzas.data.local.AppDatabase
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.local.entity.*
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncService @Inject constructor(
    private val db: AppDatabase,
    private val config: ConfiguracionPreferences,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "SyncService"
    }

    suspend fun sincronizar(): Result<String> = withContext(Dispatchers.IO) {
        if (!config.syncEnabled || config.syncHouseholdId.isBlank()) {
            return@withContext Result.failure(Exception("Sincronización no configurada u hogar ID vacío"))
        }

        try {
            Log.d(TAG, "🔄 Iniciando sincronización con el servidor: ${config.syncServerUrl}")

            // 1. Obtener datos locales
            val localTxs = db.movimientoDao().obtenerMovimientos()
            val localCategories = db.categoriaDao().obtenerCategorias()
            val localBudgets = db.presupuestoCategoriaDao().obtenerTodosLosPresupuestos()
            val localSalaries = db.sueldoDao().obtenerTodosLosSueldos()
            val localPatterns = db.clasificacionAutomaticaDao().obtenerTodosLosPatrones()
            val localDebts = db.cuentaPorCobrarDao().obtenerTodasLasCuentas()
            val localUsers = db.usuarioDao().obtenerTodosLosUsuarios()

            // Mapas de ayuda para resolver IDs a nombres
            val categoryIdToName = localCategories.associate { it.id to it.nombre }
            val userIdToName = localUsers.associate { it.id to "${it.nombre} ${it.apellido}".trim() }

            val overwriteServer = config.syncOverwriteAction == "overwrite_server"

            // 2. Preparar el Payload JSON para enviar al servidor web
            val payload = mapOf(
                "householdId" to config.syncHouseholdId,
                "lastSyncTimestamp" to config.lastSyncTimestamp.toString(),
                "overwrite" to overwriteServer,
                "transactions" to localTxs.map { tx ->
                    mapOf(
                        "idUnico" to tx.idUnico,
                        "amount" to tx.monto,
                        "date" to tx.fecha.time,
                        "type" to tx.tipo,
                        "description" to tx.descripcion,
                        "categoryName" to (categoryIdToName[tx.categoriaId] ?: ""),
                        "cardType" to (tx.tipoTarjeta ?: ""),
                        "billingPeriod" to tx.periodoFacturacion,
                        "ignored" to (tx.tipo == "OMITIR"),
                        "createdAt" to tx.fechaCreacion,
                        "updatedAt" to tx.fechaActualizacion
                    )
                },
                "budgets" to localBudgets.map { b ->
                    mapOf(
                        "categoryName" to (categoryIdToName[b.categoriaId] ?: ""),
                        "amount" to b.monto,
                        "period" to b.periodo,
                        "updatedAt" to System.currentTimeMillis() // Presupuestos locales no tienen updatedAt explícito en Room
                    )
                },
                "salaries" to localSalaries.map { s ->
                    mapOf(
                        "nombrePersona" to s.nombrePersona,
                        "periodo" to s.periodo,
                        "sueldo" to s.sueldo,
                        "updatedAt" to System.currentTimeMillis()
                    )
                },
                "patterns" to localPatterns.map { p ->
                    mapOf(
                        "pattern" to p.patron,
                        "categoryName" to (categoryIdToName[p.categoriaId] ?: ""),
                        "confidence" to p.nivelConfianza,
                        "frequency" to p.frecuencia,
                        "updatedAt" to p.ultimaActualizacion
                    )
                },
                "debts" to localDebts.map { d ->
                    mapOf(
                        "debtorName" to (userIdToName[d.usuarioId] ?: "Desconocido"),
                        "creditorName" to "Papá", // Asumimos acreedor local por defecto
                        "amount" to d.monto,
                        "reason" to d.motivo,
                        "status" to d.estado,
                        "billingPeriod" to (d.periodoCobro ?: ""),
                        "notes" to (d.notas ?: ""),
                        "createdAt" to d.fechaCreacion,
                        "updatedAt" to d.fechaActualizacion
                    )
                }
            )

            val jsonBody = gson.toJson(payload)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val email = config.syncEmail
            val password = config.syncPassword
            val credentials = "$email:$password"
            val base64Credentials = android.util.Base64.encodeToString(
                credentials.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )

            val request = Request.Builder()
                .url(config.syncServerUrl)
                .addHeader("Authorization", "Basic $base64Credentials")
                .post(requestBody)
                .build()

            // 3. Ejecutar la llamada HTTP
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Error del servidor: HTTP ${response.code}"))
                }

                val responseBody = response.body?.string() ?: ""
                val syncData: Map<String, Any> = gson.fromJson(responseBody, object : TypeToken<Map<String, Any>>() {}.type)

                // 4. Procesar la respuesta del servidor (aplicar cambios del servidor localmente)
                val serverTimestamp = (syncData["serverTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                
                // Obtener listas actualizadas
                val remoteTxs = parseList(syncData["transactions"]) { gson.fromJson(gson.toJson(it), RemoteTransaction::class.java) }
                val remoteBudgets = parseList(syncData["budgets"]) { gson.fromJson(gson.toJson(it), RemoteBudget::class.java) }
                val remoteSalaries = parseList(syncData["salaries"]) { gson.fromJson(gson.toJson(it), RemoteSalary::class.java) }
                val remotePatterns = parseList(syncData["patterns"]) { gson.fromJson(gson.toJson(it), RemotePattern::class.java) }
                val remoteDebts = parseList(syncData["debts"]) { gson.fromJson(gson.toJson(it), RemoteDebt::class.java) }

                // Resolver Categorías locales e inyectar nuevas si no existen
                val currentCategories = db.categoriaDao().obtenerCategorias().toMutableList()
                val getOrCreateLocalCategoryId: suspend (String) -> Long? = { name: String ->
                    if (name.isBlank()) null
                    else {
                        val match = currentCategories.find { it.nombre.equals(name, ignoreCase = true) }
                        if (match != null) {
                            match.id
                        } else {
                            val newCat = Categoria(nombre = name, tipo = "Gasto")
                            db.categoriaDao().agregarCategoria(newCat)
                            val inserted = db.categoriaDao().obtenerCategoriaPorNombre(name)
                            if (inserted != null) {
                                currentCategories.add(inserted)
                                inserted.id
                            } else null
                        }
                    }
                }

                // Resolver Usuarios locales e inyectar nuevos
                val currentUsers = db.usuarioDao().obtenerTodosLosUsuarios().toMutableList()
                val getOrCreateLocalUserId: suspend (String) -> Long = { name: String ->
                    val match = currentUsers.find { "${it.nombre} ${it.apellido}".trim().equals(name, ignoreCase = true) }
                    if (match != null) {
                        match.id
                    } else {
                        val newUsr = UsuarioEntity(nombre = name, apellido = "", activo = true)
                        val id = db.usuarioDao().insertarUsuario(newUsr)
                        val inserted = db.usuarioDao().obtenerUsuarioPorId(id)
                        if (inserted != null) {
                            currentUsers.add(inserted)
                            inserted.id
                        } else id
                    }
                }

                // 5. Aplicar Transacciones Remotas
                remoteTxs.forEach { rTx ->
                    val catId = getOrCreateLocalCategoryId(rTx.categoryName)
                    val local = db.movimientoDao().obtenerMovimientos().find { it.idUnico == rTx.idUnico }
                    
                    val entity = MovimientoEntity(
                        id = local?.id ?: 0L,
                        tipo = rTx.type,
                        monto = rTx.amount,
                        descripcion = rTx.description,
                        descripcionLimpia = limpiarDescripcion(rTx.description),
                        fecha = Date(rTx.date),
                        periodoFacturacion = rTx.billingPeriod,
                        categoriaId = catId,
                        tipoTarjeta = rTx.cardType.ifBlank { null },
                        idUnico = rTx.idUnico,
                        fechaCreacion = rTx.createdAt,
                        fechaActualizacion = rTx.updatedAt,
                        metodoActualizacion = "SYNC",
                        daoResponsable = "SyncService"
                    )

                    db.movimientoDao().agregarMovimiento(entity)
                }

                // 6. Aplicar Presupuestos Remotos
                remoteBudgets.forEach { rB ->
                    val catId = getOrCreateLocalCategoryId(rB.categoryName)
                    if (catId != null) {
                        val local = db.presupuestoCategoriaDao().obtenerPresupuestosPorPeriodo(rB.period)
                            .find { it.categoriaId == catId }
                        val entity = PresupuestoCategoriaEntity(
                            id = local?.id ?: 0L,
                            categoriaId = catId,
                            monto = rB.amount,
                            periodo = rB.period
                        )
                        db.presupuestoCategoriaDao().insertarPresupuesto(entity)
                    }
                }

                // 7. Aplicar Sueldos Remotos
                remoteSalaries.forEach { rS ->
                    val local = db.sueldoDao().obtenerSueldoPorPersonaYPeriodo(rS.nombrePersona, rS.periodo)
                    val entity = SueldoEntity(
                        id = local?.id ?: 0L,
                        nombrePersona = rS.nombrePersona,
                        periodo = rS.periodo,
                        sueldo = rS.sueldo
                    )
                    db.sueldoDao().insertarSueldo(entity)
                }

                // 8. Aplicar Patrones Remotos
                remotePatterns.forEach { rP ->
                    val catId = getOrCreateLocalCategoryId(rP.categoryName)
                    if (catId != null) {
                        val local = db.clasificacionAutomaticaDao().obtenerPatronPorDescripcion(rP.pattern)
                        val entity = ClasificacionAutomaticaEntity(
                            id = local?.id ?: 0L,
                            patron = rP.pattern,
                            categoriaId = catId,
                            nivelConfianza = rP.confidence,
                            frecuencia = rP.frequency,
                            ultimaActualizacion = rP.updatedAt
                        )
                        if (local != null) {
                            db.clasificacionAutomaticaDao().actualizarPatron(entity)
                        } else {
                            db.clasificacionAutomaticaDao().insertarPatron(entity)
                        }
                    }
                }

                // 9. Aplicar Deudas Remotas
                remoteDebts.forEach { rD ->
                    val debtorId = getOrCreateLocalUserId(rD.debtorName)
                    
                    // Buscar si existe una deuda local por coincidencia de motivo y monto
                    val local = db.cuentaPorCobrarDao().obtenerTodasLasCuentas().find {
                        it.motivo == rD.reason && it.monto == rD.amount && it.fechaCreacion == rD.createdAt
                    }

                    val entity = CuentaPorCobrarEntity(
                        id = local?.id ?: 0L,
                        motivo = rD.reason,
                        monto = rD.amount,
                        usuarioId = debtorId,
                        fechaCobro = if (rD.status == "COBRADO") Date(rD.updatedAt) else null,
                        periodoCobro = rD.billingPeriod.ifBlank { null },
                        estado = rD.status,
                        notas = rD.notes.ifBlank { null },
                        fechaCreacion = rD.createdAt,
                        fechaActualizacion = rD.updatedAt
                    )

                    db.cuentaPorCobrarDao().insertarCuenta(entity)
                }

                // Guardar la marca de tiempo de sincronización exitosa
                config.lastSyncTimestamp = serverTimestamp
                config.syncOverwriteAction = ""
                Log.d(TAG, "✅ Sincronización exitosa. Marca de tiempo: $serverTimestamp")
                return@withContext Result.success("Sincronización completada exitosamente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en sincronización", e)
            return@withContext Result.failure(e)
        }
    }

    private fun <T> parseList(data: Any?, parser: (Map<*, *>) -> T): List<T> {
        val list = data as? List<*> ?: return emptyList()
        return list.mapNotNull {
            val map = it as? Map<*, *>
            if (map != null) parser(map) else null
        }
    }

    private fun limpiarDescripcion(descripcion: String): String {
        return descripcion
            .replace(Regex("[0-9]+"), "")
            .replace(Regex("[^A-Za-zÁÉÍÓÚáéíóúÑñüÜ\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }

    // Clases auxiliares para el parseo
    private data class RemoteTransaction(
        val idUnico: String,
        val amount: Double,
        val date: Long,
        val type: String,
        val description: String,
        val categoryName: String,
        val cardType: String,
        val billingPeriod: String,
        val ignored: Boolean,
        val createdAt: Long,
        val updatedAt: Long
    )

    private data class RemoteBudget(
        val categoryName: String,
        val amount: Double,
        val period: String,
        val updatedAt: Long
    )

    private data class RemoteSalary(
        val nombrePersona: String,
        val periodo: String,
        val sueldo: Double,
        val updatedAt: Long
    )

    private data class RemotePattern(
        val pattern: String,
        val categoryName: String,
        val confidence: Double,
        val frequency: Int,
        val updatedAt: Long
    )

    private data class RemoteDebt(
        val debtorName: String,
        val creditorName: String,
        val amount: Double,
        val reason: String,
        val status: String,
        val billingPeriod: String,
        val notes: String,
        val createdAt: Long,
        val updatedAt: Long
    )
}
