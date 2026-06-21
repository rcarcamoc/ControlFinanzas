package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.PresupuestoCategoriaDao
import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity
import com.aranthalion.controlfinanzas.data.repository.AuditoriaService
import javax.inject.Inject

class PresupuestoCategoriaRepositoryImpl @Inject constructor(
    private val dao: PresupuestoCategoriaDao,
    private val auditoriaService: AuditoriaService,
    private val configuracionPreferences: com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
) : PresupuestoCategoriaRepository {
    override suspend fun insertarPresupuesto(presupuesto: PresupuestoCategoriaEntity) {
        val activeScope = configuracionPreferences.obtenerScopeGlobal()
        val presupuestoConScope = presupuesto.copy(scope = activeScope)
        println("📝 PRESUPUESTO_AUDITORIA: Insertando presupuesto - Categoría: ${presupuestoConScope.categoriaId}, Período: ${presupuestoConScope.periodo}, Monto: ${presupuestoConScope.monto}, Scope: $activeScope")
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