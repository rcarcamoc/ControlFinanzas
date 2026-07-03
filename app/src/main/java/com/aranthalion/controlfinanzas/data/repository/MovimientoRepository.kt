package com.aranthalion.controlfinanzas.data.repository

import android.content.Context
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.dao.OfflineOperationDao
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.OfflineOperationEntity
import com.aranthalion.controlfinanzas.data.remote.api.FinanzasApiService
import com.aranthalion.controlfinanzas.data.remote.connectivity.ConnectivityMonitor
import com.aranthalion.controlfinanzas.data.remote.api.dto.CreateTransactionDto
import com.aranthalion.controlfinanzas.data.remote.api.dto.UpdateTransactionDto
import com.aranthalion.controlfinanzas.data.remote.api.dto.TransactionDto
import com.aranthalion.controlfinanzas.data.remote.api.dto.CategoryDto
import com.aranthalion.controlfinanzas.data.remote.api.dto.DashboardResponse
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.aranthalion.controlfinanzas.data.repository.AuditoriaService
import com.aranthalion.controlfinanzas.data.sync.OfflineQueueProcessor

class MovimientoRepository @Inject constructor(
    private val movimientoDao: MovimientoDao,
    private val categoriaDao: CategoriaDao,
    private val offlineOperationDao: OfflineOperationDao,
    private val context: Context,
    private val auditoriaService: AuditoriaService,
    private val normalizacionService: NormalizacionService,
    private val cacheService: CacheService,
    private val configuracionPreferences: com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences,
    private val gson: Gson,
    private val api: FinanzasApiService,
    private val connectivity: ConnectivityMonitor,
    private val queueProcessor: OfflineQueueProcessor
) {
    private suspend fun syncRemoteCategoriesToLocalCache(remoteCats: List<CategoryDto>) {
        try {
            val localCats = categoriaDao.obtenerCategorias()
            val localCatNames = localCats.map { it.nombre.lowercase().trim() }.toSet()
            
            remoteCats.forEach { rCat ->
                val rCatNameNorm = rCat.name.lowercase().trim()
                if (rCatNameNorm !in localCatNames) {
                    val newCat = Categoria(
                        nombre = rCat.name,
                        descripcion = "Creado desde servidor",
                        tipo = "Gasto"
                    )
                    categoriaDao.agregarCategoria(newCat)
                }
            }
        } catch (e: Exception) {
            println("⚠️ SYNC: Error synchronizing remote categories to Room: ${e.message}")
        }
    }    private fun generateLocalId(idUnico: String): Long {
        return idUnico.hashCode().toLong()
    }

    private suspend fun syncRemoteTransactionsToLocalCache(remoteTxs: List<TransactionDto>, targetPeriod: String? = null) {
        // Obsoleta: No se guarda en Room
    }

    suspend fun obtenerMovimientos(): List<MovimientoEntity> {
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        val householdId = configuracionPreferences.syncHouseholdId
        
        if (householdId.isBlank()) {
            throw java.io.IOException("Servidor no configurado. Vincula tu cuenta en Configuración.")
        }
        
        val response = api.refresh(householdId)
        syncRemoteCategoriesToLocalCache(response.categories)
        val allCats = categoriaDao.obtenerCategorias()
        val catNameToId = allCats.associate { it.nombre.lowercase().trim() to it.id }
        
        return response.transactions.map { t ->
            val catId = t.categoryName.lowercase().trim().let { catNameToId[it] }
            MovimientoEntity(
                id = generateLocalId(t.idUnico),
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
        }.filter { it.scope == activeScope }
    }

    /**
     * Obtiene TODOS los movimientos sin filtro de scope.
     * Usado por el EmailSyncWorker para detectar duplicados independientemente del scope activo.
     */
    suspend fun obtenerTodosLosMovimientos(): List<MovimientoEntity> {
        val householdId = configuracionPreferences.syncHouseholdId
        if (householdId.isBlank()) {
            throw java.io.IOException("Servidor no configurado. Vincula tu cuenta en Configuración.")
        }
        
        val response = api.refresh(householdId)
        syncRemoteCategoriesToLocalCache(response.categories)
        val allCats = categoriaDao.obtenerCategorias()
        val catNameToId = allCats.associate { it.nombre.lowercase().trim() to it.id }
        
        return response.transactions.map { t ->
            val catId = t.categoryName.lowercase().trim().let { catNameToId[it] }
            MovimientoEntity(
                id = generateLocalId(t.idUnico),
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
        }
    }

    /**
     * Obtiene un movimiento por su ID sin filtro de scope.
     * Usado para lookups en clasificación y edición donde el scope puede cambiar.
     */
    suspend fun obtenerMovimientoPorId(id: Long): MovimientoEntity? {
        val householdId = configuracionPreferences.syncHouseholdId
        if (householdId.isBlank()) return null
        val response = api.refresh(householdId)
        val allCats = categoriaDao.obtenerCategorias()
        val catNameToId = allCats.associate { it.nombre.lowercase().trim() to it.id }
        
        return response.transactions.map { t ->
            val catId = t.categoryName.lowercase().trim().let { catNameToId[it] }
            MovimientoEntity(
                id = generateLocalId(t.idUnico),
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
        }.firstOrNull { it.id == id }
    }

    /**
     * Obtiene todos los movimientos sin categoría de TODOS los scopes.
     * Usado por el asistente de clasificación para mostrar TODAS las pendientes.
     */
    suspend fun obtenerMovimientosSinCategoriaTodosScopes(): List<MovimientoEntity> {
        val householdId = configuracionPreferences.syncHouseholdId
        if (householdId.isBlank()) return emptyList()
        val response = api.refresh(householdId)
        val allCats = categoriaDao.obtenerCategorias()
        val catNameToId = allCats.associate { it.nombre.lowercase().trim() to it.id }
        
        return response.transactions.map { t ->
            val catId = t.categoryName.lowercase().trim().let { catNameToId[it] }
            MovimientoEntity(
                id = generateLocalId(t.idUnico),
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
        }.filter { it.categoriaId == null && it.tipo != "OMITIR" }
    }

    // Métodos optimizados del HITO 1
    suspend fun obtenerMovimientosOptimizado(): List<MovimientoEntity> {
        return obtenerMovimientos()
    }
    
    suspend fun obtenerMovimientosPorPeriodoOptimizado(periodo: String): List<MovimientoEntity> {
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        val householdId = configuracionPreferences.syncHouseholdId
        
        if (householdId.isBlank()) {
            throw java.io.IOException("Servidor no configurado. Vincula tu cuenta en Configuración.")
        }
        
        val response = api.refresh(householdId = householdId, billingPeriod = periodo)
        syncRemoteCategoriesToLocalCache(response.categories)
        val allCats = categoriaDao.obtenerCategorias()
        val catNameToId = allCats.associate { it.nombre.lowercase().trim() to it.id }
        
        return response.transactions.map { t ->
            val catId = t.categoryName.lowercase().trim().let { catNameToId[it] }
            MovimientoEntity(
                id = generateLocalId(t.idUnico),
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
        }.filter { it.scope == activeScope }
    }

    suspend fun obtenerMovimientosPorPeriodo(fechaInicio: Date, fechaFin: Date): List<MovimientoEntity> {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val periodo = sdf.format(fechaInicio)
        return obtenerMovimientosPorPeriodoOptimizado(periodo)
    }
 
    suspend fun obtenerCategorias(): List<Categoria> {
        val householdId = configuracionPreferences.syncHouseholdId
        if (householdId.isNotBlank() && connectivity.isOnline.value) {
            try {
                val response = api.refresh(householdId)
                syncRemoteCategoriesToLocalCache(response.categories)
            } catch (e: Exception) {
                // Silently ignore category prefetch errors
            }
        }
        return categoriaDao.obtenerCategorias()
    }
    
    // Método optimizado para categorías (HITO 1.3)
    suspend fun obtenerCategoriasOptimizado(): List<Categoria> {
        return obtenerCategorias()
    }

    suspend fun agregarMovimiento(movimiento: MovimientoEntity, metodo: String = "INSERT", dao: String = "MovimientoDao") {
        val categoryName = movimiento.categoriaId?.let { catId ->
            categoriaDao.obtenerCategorias().firstOrNull { it.id == catId }?.nombre
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val dateStr = sdf.format(movimiento.fecha)

        val createDto = CreateTransactionDto(
            amount = movimiento.monto,
            currency = "CLP",
            date = dateStr,
            type = if (movimiento.tipo == "INGRESO" || movimiento.tipo == "INCOME") "INCOME" else "EXPENSE",
            description = movimiento.descripcion,
            accountId = "",
            categoryId = null,
            categoryName = categoryName,
            householdId = configuracionPreferences.syncHouseholdId,
            billingPeriod = movimiento.periodoFacturacion,
            externalId = movimiento.idUnico
        )

        api.createTransaction(createDto)
    }

    suspend fun actualizarMovimiento(movimiento: MovimientoEntity, metodo: String = "UPDATE", dao: String = "MovimientoDao") {
        val categoryName = movimiento.categoriaId?.let { catId ->
            categoriaDao.obtenerCategorias().firstOrNull { it.id == catId }?.nombre
        }
        val updateDto = UpdateTransactionDto(
            ignored = movimiento.tipo == "OMITIR",
            scope = movimiento.scope,
            userId_internal = movimiento.userId_internal,
            categoryId = null,
            categoryName = categoryName
        )
        
        api.updateTransaction(movimiento.idUnico, updateDto)
    }

    suspend fun eliminarMovimiento(movimiento: MovimientoEntity) {
        if (movimiento.idUnico.isNotEmpty()) {
            api.deleteTransaction(movimiento.idUnico)
        }
    }

    suspend fun obtenerIdUnicos(): Set<String> {
        val householdId = configuracionPreferences.syncHouseholdId
        if (householdId.isBlank()) return emptySet()
        val response = api.refresh(householdId)
        return response.transactions.map { it.idUnico }.toSet()
    }

    suspend fun obtenerIdUnicosPorPeriodo(periodo: String?): Set<String> {
        val householdId = configuracionPreferences.syncHouseholdId
        if (householdId.isBlank()) return emptySet()
        val response = api.refresh(householdId = householdId, billingPeriod = periodo)
        return response.transactions.map { it.idUnico }.toSet()
    }

    suspend fun obtenerCategoriasPorIdUnico(periodo: String?): Map<String, Long?> {
        val householdId = configuracionPreferences.syncHouseholdId
        if (householdId.isBlank()) return emptyMap()
        val response = api.refresh(householdId = householdId, billingPeriod = periodo)
        val allCats = categoriaDao.obtenerCategorias()
        val catNameToId = allCats.associate { it.nombre.lowercase().trim() to it.id }
        return response.transactions.associate { t ->
            val catId = t.categoryName.lowercase().trim().let { catNameToId[it] }
            t.idUnico to catId
        }
    }

    suspend fun eliminarMovimientosPorPeriodo(periodo: String?) {
        if (periodo != null) {
            api.deleteTransactionsByPeriod(periodo)
        }
    }
    
    // Métodos de auditoría
    suspend fun obtenerMovimientosRecientes(): List<MovimientoEntity> {
        val movimientos = movimientoDao.obtenerMovimientosRecientes()
        println("🔍 AUDITORIA_REPO: Movimientos recientes obtenidos: ${movimientos.size}")
        movimientos.take(5).forEach { movimiento ->
            println("  - ID: ${movimiento.id}, Descripción: ${movimiento.descripcion}, Método: ${movimiento.metodoActualizacion}, DAO: ${movimiento.daoResponsable}")
        }
        return movimientos
    }
    
    suspend fun obtenerMovimientosPorMetodo(metodo: String): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientosPorMetodo(metodo)
    }
    
    suspend fun actualizarAuditoria(id: Long, metodo: String, dao: String) {
        val timestamp = System.currentTimeMillis()
        movimientoDao.actualizarAuditoria(id, timestamp, metodo, dao)
        println("📝 AUDITORÍA: Actualizando auditoría - ID: $id, Método: $metodo, DAO: $dao, Timestamp: $timestamp")
    }

    /**
     * Limpia todos los datos históricos de la base de datos
     * Útil para instalaciones completamente limpias
     */
    suspend fun limpiarTodosLosDatos() {
        try {
            println("🧹 Limpiando todos los datos históricos...")
            
            // Obtener todos los movimientos
            val movimientosExistentes = movimientoDao.obtenerMovimientos()
            
            if (movimientosExistentes.isNotEmpty()) {
                // Eliminar todos los movimientos
                movimientosExistentes.forEach { movimiento ->
                    movimientoDao.eliminarMovimiento(movimiento)
                }
                println("🗑️ Eliminados ${movimientosExistentes.size} movimientos de la base de datos")
            } else {
                println("ℹ️ No hay movimientos para eliminar")
            }
            
            // Invalidar cache
            cacheService.invalidarTodoCache()
            
            println("✅ Base de datos limpiada completamente")
            
        } catch (e: Exception) {
            println("❌ Error al limpiar datos: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Carga datos históricos hardcodeados (solo una vez)
     */
    suspend fun cargarDatosHistoricos() {
        DatosHistoricosPredefinidos.cargarDatosHistoricos(movimientoDao, categoriaDao)
    }
    
    // Métodos optimizados usando campos normalizados (HITO 1.1)
    
    /**
     * Obtiene movimientos sin categoría usando consultas optimizadas
     */
    suspend fun obtenerMovimientosSinCategoriaOptimizado(limit: Int = 50): List<MovimientoEntity> {
        return cacheService.getMovimientosSinCategoria {
            normalizacionService.obtenerMovimientosSinCategoriaOptimizado(limit)
        }
    }
    
    /**
     * Obtiene movimientos similares usando campos normalizados
     */
    suspend fun obtenerMovimientosSimilaresOptimizado(
        descripcion: String,
        limit: Int = 10
    ): List<MovimientoEntity> {
        return normalizacionService.obtenerMovimientosSimilaresOptimizado(descripcion, limit)
    }
    
    /**
     * Obtiene movimientos similares por monto y patrón
     */
    suspend fun obtenerMovimientosSimilaresPorMontoOptimizado(
        descripcion: String,
        monto: Double,
        limit: Int = 10
    ): List<MovimientoEntity> {
        return normalizacionService.obtenerMovimientosSimilaresPorMontoOptimizado(descripcion, monto, limit)
    }
    
    /**
     * Calcula similitud rápida entre dos descripciones
     */
    fun calcularSimilitudRapida(descripcion1: String, descripcion2: String): Double {
        return normalizacionService.calcularSimilitudRapida(descripcion1, descripcion2)
    }
    
    /**
     * Migra datos existentes para agregar campos normalizados
     */
    suspend fun migrarDatosExistentes(): Boolean {
        return normalizacionService.migrarDatosExistentes()
    }
    
    /**
     * Obtiene estadísticas de clasificación optimizadas
     */
    suspend fun obtenerEstadisticasClasificacion(): Map<String, Int> {
        return cacheService.getEstadisticas {
            normalizacionService.obtenerEstadisticasClasificacion()
        }
    }
    
    /**
     * Obtiene movimientos por categoría de monto optimizado
     */
    suspend fun obtenerMovimientosPorCategoriaMonto(categoriaMonto: String, limit: Int = 20): List<MovimientoEntity> {
        return normalizacionService.obtenerMovimientosPorCategoriaMonto(categoriaMonto, limit)
    }
    
    /**
     * Obtiene movimientos por mes optimizado
     */
    suspend fun obtenerMovimientosPorMes(mes: String, limit: Int = 20): List<MovimientoEntity> {
        return normalizacionService.obtenerMovimientosPorMes(mes, limit)
    }
    
    /**
     * Función auxiliar para obtener fechas de inicio y fin de un período
     */
    private fun obtenerFechasDePeriodo(periodo: String): Pair<Date, Date> {
        val (anio, mes) = periodo.split("-")
        val calendar = java.util.Calendar.getInstance()
        
        // Fecha de inicio: primer día del mes
        calendar.set(anio.toInt(), mes.toInt() - 1, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val fechaInicio = calendar.time
        
        // Fecha de fin: último día del mes
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.add(java.util.Calendar.MILLISECOND, -1)
        val fechaFin = calendar.time
        
        return Pair(fechaInicio, fechaFin)
    }

    suspend fun obtenerDashboardData(periodo: String): DashboardResponse {
        val householdId = configuracionPreferences.syncHouseholdId
        if (householdId.isBlank()) {
            throw java.io.IOException("Servidor no configurado. Vincula tu cuenta en Configuración.")
        }
        return api.getDashboardData(householdId, period = periodo)
    }
}