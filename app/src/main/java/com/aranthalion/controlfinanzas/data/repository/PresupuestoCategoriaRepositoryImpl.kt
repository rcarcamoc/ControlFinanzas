package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.PresupuestoCategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity
import com.aranthalion.controlfinanzas.data.repository.AuditoriaService
import com.aranthalion.controlfinanzas.data.remote.api.FinanzasApiService
import com.aranthalion.controlfinanzas.data.remote.connectivity.ConnectivityMonitor
import com.aranthalion.controlfinanzas.data.remote.api.dto.CreateBudgetDto
import javax.inject.Inject

class PresupuestoCategoriaRepositoryImpl @Inject constructor(
    private val dao: PresupuestoCategoriaDao,
    private val categoriaDao: CategoriaDao,
    private val auditoriaService: AuditoriaService,
    private val configuracionPreferences: com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences,
    private val api: FinanzasApiService,
    private val connectivity: ConnectivityMonitor
) : PresupuestoCategoriaRepository {
    override suspend fun insertarPresupuesto(presupuesto: PresupuestoCategoriaEntity) {
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        val presupuestoConScope = presupuesto.copy(scope = activeScope)
        println("📝 PRESUPUESTO_AUDITORIA: Insertando presupuesto - Categoría: ${presupuestoConScope.categoriaId}, Período: ${presupuestoConScope.periodo}, Monto: ${presupuestoConScope.monto}, Scope: $activeScope")
        
        if (!connectivity.isOnline.value) {
            throw java.io.IOException("La aplicación está en modo de lectura porque no tiene conexión a internet.")
        }

        // Buscar el nombre de la categoría para resolverlo en el servidor por nombre
        val categoryName = categoriaDao.obtenerCategorias().firstOrNull { it.id == presupuesto.categoriaId }?.nombre
        val parts = presupuesto.periodo.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 6
        val householdId = configuracionPreferences.syncHouseholdId

        val createDto = CreateBudgetDto(
            categoryId = null,
            categoryName = categoryName,
            limit = presupuesto.monto,
            month = month,
            year = year,
            householdId = if (householdId.isBlank()) null else householdId
        )

        try {
            api.createBudget(createDto)
        } catch (e: Exception) {
            println("⚠️ PRESUPUESTO_AUDITORIA: Error al guardar presupuesto en servidor: ${e.message}")
            throw e
        }

        dao.insertarPresupuesto(presupuestoConScope)
        
        // Registrar auditoría
        auditoriaService.registrarOperacion(
            tabla = "presupuestos",
            operacion = "INSERT",
            entidadId = presupuestoConScope.id,
            detalles = "Presupuesto insertado - Categoría: ${presupuestoConScope.categoriaId}, Período: ${presupuestoConScope.periodo}, Monto: ${presupuestoConScope.monto}, Scope: $activeScope",
            daoResponsable = "PresupuestoCategoriaDao"
        )
        
        println("✅ PRESUPUESTO_AUDITORIA: Presupuesto insertado exitosamente")
    }

    override suspend fun actualizarPresupuesto(presupuesto: PresupuestoCategoriaEntity) {
        println("📝 PRESUPUESTO_AUDITORIA: Actualizando presupuesto - Categoría: ${presupuesto.categoriaId}, Período: ${presupuesto.periodo}, Monto: ${presupuesto.monto}, Scope: ${presupuesto.scope}")
        
        if (!connectivity.isOnline.value) {
            throw java.io.IOException("La aplicación está en modo de lectura porque no tiene conexión a internet.")
        }

        // Buscar el nombre de la categoría
        val categoryName = categoriaDao.obtenerCategorias().firstOrNull { it.id == presupuesto.categoriaId }?.nombre
        val parts = presupuesto.periodo.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 6
        val householdId = configuracionPreferences.syncHouseholdId

        val createDto = CreateBudgetDto(
            categoryId = null,
            categoryName = categoryName,
            limit = presupuesto.monto,
            month = month,
            year = year,
            householdId = if (householdId.isBlank()) null else householdId
        )

        try {
            api.createBudget(createDto)
        } catch (e: Exception) {
            println("⚠️ PRESUPUESTO_AUDITORIA: Error al actualizar presupuesto en servidor: ${e.message}")
            throw e
        }

        dao.actualizarPresupuesto(presupuesto)
        
        // Registrar auditoría
        auditoriaService.registrarOperacion(
            tabla = "presupuestos",
            operacion = "UPDATE",
            entidadId = presupuesto.id,
            detalles = "Presupuesto actualizado - Categoría: ${presupuesto.categoriaId}, Período: ${presupuesto.periodo}, Monto: ${presupuesto.monto}, Scope: ${presupuesto.scope}",
            daoResponsable = "PresupuestoCategoriaDao"
        )
        
        println("✅ PRESUPUESTO_AUDITORIA: Presupuesto actualizado exitosamente")
    }

    override suspend fun eliminarPresupuesto(presupuesto: PresupuestoCategoriaEntity) {
        println("📝 PRESUPUESTO_AUDITORIA: Eliminando presupuesto - Categoría: ${presupuesto.categoriaId}, Período: ${presupuesto.periodo}, Monto: ${presupuesto.monto}")
        
        if (!connectivity.isOnline.value) {
            throw java.io.IOException("La aplicación está en modo de lectura porque no tiene conexión a internet.")
        }
        
        // Registrar auditoría antes de eliminar
        auditoriaService.registrarOperacion(
            tabla = "presupuestos",
            operacion = "DELETE",
            entidadId = presupuesto.id,
            detalles = "Presupuesto eliminado - Categoría: ${presupuesto.categoriaId}, Período: ${presupuesto.periodo}, Monto: ${presupuesto.monto}",
            daoResponsable = "PresupuestoCategoriaDao"
        )
        
        dao.eliminarPresupuesto(presupuesto)
        println("✅ PRESUPUESTO_AUDITORIA: Presupuesto eliminado exitosamente")
    }

    override suspend fun obtenerPresupuestosPorPeriodo(periodo: String): List<PresupuestoCategoriaEntity> {
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        val presupuestos = dao.obtenerPresupuestosPorPeriodoYScope(periodo, activeScope)
        println("🔍 PRESUPUESTO_AUDITORIA: Obtenidos ${presupuestos.size} presupuestos para período: $periodo, Scope: $activeScope")
        return presupuestos
    }

    override suspend fun obtenerPresupuestoPorCategoriaYPeriodo(categoriaId: Long, periodo: String): PresupuestoCategoriaEntity? {
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        val presupuesto = dao.obtenerPresupuestoPorCategoriaYPeriodoYScope(categoriaId, periodo, activeScope)
        println("🔍 PRESUPUESTO_AUDITORIA: Buscando presupuesto - Categoría: $categoriaId, Período: $periodo, Scope: $activeScope, Encontrado: ${presupuesto != null}")
        return presupuesto
    }

    override suspend fun obtenerSumaTotalPresupuesto(periodo: String): Double? {
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        val suma = dao.obtenerSumaTotalPresupuestoYScope(periodo, activeScope)
        println("🔍 PRESUPUESTO_AUDITORIA: Suma total presupuesto período $periodo, Scope: $activeScope: $suma")
        return suma
    }
}