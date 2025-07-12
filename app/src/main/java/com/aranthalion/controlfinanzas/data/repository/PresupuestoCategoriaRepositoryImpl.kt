package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.PresupuestoCategoriaDao
import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity
import com.aranthalion.controlfinanzas.data.repository.AuditoriaService
import javax.inject.Inject

class PresupuestoCategoriaRepositoryImpl @Inject constructor(
    private val dao: PresupuestoCategoriaDao,
    private val auditoriaService: AuditoriaService
) : PresupuestoCategoriaRepository {
    override suspend fun insertarPresupuesto(presupuesto: PresupuestoCategoriaEntity) {
        println("📝 PRESUPUESTO_AUDITORIA: Insertando presupuesto - Categoría: ${presupuesto.categoriaId}, Período: ${presupuesto.periodo}, Monto: ${presupuesto.monto}")
        dao.insertarPresupuesto(presupuesto)
        
        // Registrar auditoría
        auditoriaService.registrarOperacion(
            tabla = "presupuestos",
            operacion = "INSERT",
            entidadId = presupuesto.id,
            detalles = "Presupuesto insertado - Categoría: ${presupuesto.categoriaId}, Período: ${presupuesto.periodo}, Monto: ${presupuesto.monto}",
            daoResponsable = "PresupuestoCategoriaDao"
        )
        
        println("✅ PRESUPUESTO_AUDITORIA: Presupuesto insertado exitosamente")
    }

    override suspend fun actualizarPresupuesto(presupuesto: PresupuestoCategoriaEntity) {
        println("📝 PRESUPUESTO_AUDITORIA: Actualizando presupuesto - Categoría: ${presupuesto.categoriaId}, Período: ${presupuesto.periodo}, Monto: ${presupuesto.monto}")
        dao.actualizarPresupuesto(presupuesto)
        
        // Registrar auditoría
        auditoriaService.registrarOperacion(
            tabla = "presupuestos",
            operacion = "UPDATE",
            entidadId = presupuesto.id,
            detalles = "Presupuesto actualizado - Categoría: ${presupuesto.categoriaId}, Período: ${presupuesto.periodo}, Monto: ${presupuesto.monto}",
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
        val presupuestos = dao.obtenerPresupuestosPorPeriodo(periodo)
        println("🔍 PRESUPUESTO_AUDITORIA: Obtenidos ${presupuestos.size} presupuestos para período: $periodo")
        return presupuestos
    }

    override suspend fun obtenerPresupuestoPorCategoriaYPeriodo(categoriaId: Long, periodo: String): PresupuestoCategoriaEntity? {
        val presupuesto = dao.obtenerPresupuestoPorCategoriaYPeriodo(categoriaId, periodo)
        println("🔍 PRESUPUESTO_AUDITORIA: Buscando presupuesto - Categoría: $categoriaId, Período: $periodo, Encontrado: ${presupuesto != null}")
        return presupuesto
    }

    override suspend fun obtenerSumaTotalPresupuesto(periodo: String): Double? {
        val suma = dao.obtenerSumaTotalPresupuesto(periodo)
        println("🔍 PRESUPUESTO_AUDITORIA: Suma total presupuesto período $periodo: $suma")
        return suma
    }
} 