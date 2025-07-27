package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.entity.Categoria as CategoriaEntity
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.aranthalion.controlfinanzas.data.repository.AuditoriaService

class CategoriaRepositoryImpl @Inject constructor(
    private val categoriaDao: CategoriaDao,
    private val auditoriaService: AuditoriaService
) : CategoriaRepository {

    private fun toEntity(categoria: com.aranthalion.controlfinanzas.domain.categoria.Categoria): CategoriaEntity {
        return CategoriaEntity(
            id = categoria.id,
            nombre = categoria.nombre,
            descripcion = categoria.descripcion,
            tipo = categoria.tipo,
            presupuestoMensual = categoria.presupuestoMensual
        )
    }

    private fun toDomain(entity: CategoriaEntity): com.aranthalion.controlfinanzas.domain.categoria.Categoria {
        return com.aranthalion.controlfinanzas.domain.categoria.Categoria(
            id = entity.id,
            nombre = entity.nombre,
            descripcion = entity.descripcion,
            tipo = entity.tipo,
            presupuestoMensual = entity.presupuestoMensual
        )
    }

    override fun getAllCategorias(): Flow<List<com.aranthalion.controlfinanzas.domain.categoria.Categoria>> {
        return categoriaDao.getAllCategorias().map { entities ->
            entities.map { toDomain(it) }
        }
    }

    override fun getCategoriasByTipo(tipo: String): Flow<List<com.aranthalion.controlfinanzas.domain.categoria.Categoria>> {
        return categoriaDao.getCategoriasByTipo(tipo).map { entities ->
            entities.map { toDomain(it) }
        }
    }

    override suspend fun insertCategoria(categoria: com.aranthalion.controlfinanzas.domain.categoria.Categoria) {
        println("📝 CATEGORIA_AUDITORIA: Insertando categoría - Nombre: ${categoria.nombre}")
        
        // Verificar si la categoría ya existe
        if (existeCategoria(categoria.nombre)) {
            println("⚠️ CATEGORIA_AUDITORIA: Categoría ya existe - Nombre: ${categoria.nombre}")
            return
        }
        
        categoriaDao.agregarCategoria(toEntity(categoria))
        
        // Registrar auditoría
        auditoriaService.registrarOperacion(
            tabla = "categorias",
            operacion = "INSERT",
            entidadId = categoria.id,
            detalles = "Categoría insertada - Nombre: ${categoria.nombre}, Tipo: ${categoria.tipo}",
            daoResponsable = "CategoriaDao"
        )
        
        println("✅ CATEGORIA_AUDITORIA: Categoría insertada exitosamente")
    }

    override suspend fun updateCategoria(categoria: com.aranthalion.controlfinanzas.domain.categoria.Categoria) {
        println("📝 CATEGORIA_AUDITORIA: Actualizando categoría - ID: ${categoria.id}, Nombre: ${categoria.nombre}")
        categoriaDao.actualizarCategoria(toEntity(categoria))
        
        // Registrar auditoría
        auditoriaService.registrarOperacion(
            tabla = "categorias",
            operacion = "UPDATE",
            entidadId = categoria.id,
            detalles = "Categoría actualizada - ID: ${categoria.id}, Nombre: ${categoria.nombre}, Tipo: ${categoria.tipo}",
            daoResponsable = "CategoriaDao"
        )
        
        println("✅ CATEGORIA_AUDITORIA: Categoría actualizada exitosamente")
    }

    override suspend fun deleteCategoria(categoria: com.aranthalion.controlfinanzas.domain.categoria.Categoria) {
        println("📝 CATEGORIA_AUDITORIA: Eliminando categoría - ID: ${categoria.id}, Nombre: ${categoria.nombre}")
        
        // Registrar auditoría antes de eliminar
        auditoriaService.registrarOperacion(
            tabla = "categorias",
            operacion = "DELETE",
            entidadId = categoria.id,
            detalles = "Categoría eliminada - ID: ${categoria.id}, Nombre: ${categoria.nombre}, Tipo: ${categoria.tipo}",
            daoResponsable = "CategoriaDao"
        )
        
        categoriaDao.eliminarCategoria(toEntity(categoria))
        println("✅ CATEGORIA_AUDITORIA: Categoría eliminada exitosamente")
    }

    override suspend fun insertDefaultCategorias() {
        val categoriasDefault = listOf(
            CategoriaEntity(nombre = "Arriendo", tipo = "Gasto", presupuestoMensual = 590000.0),
            CategoriaEntity(nombre = "Tarjeta titular", tipo = "Gasto", presupuestoMensual = 300000.0),
            CategoriaEntity(nombre = "Vacaciones", tipo = "Gasto", presupuestoMensual = 0.0),
            CategoriaEntity(nombre = "Supermercado", tipo = "Gasto", presupuestoMensual = 500000.0),
            CategoriaEntity(nombre = "Gastos comunes", tipo = "Gasto", presupuestoMensual = 100000.0),
            CategoriaEntity(nombre = "Choquito", tipo = "Gasto", presupuestoMensual = 0.0),
            CategoriaEntity(nombre = "Bencina", tipo = "Gasto", presupuestoMensual = 1000000.0),
            CategoriaEntity(nombre = "Veguita", tipo = "Gasto", presupuestoMensual = 60000.0),
            CategoriaEntity(nombre = "Gatos", tipo = "Gasto", presupuestoMensual = 50000.0),
            CategoriaEntity(nombre = "Uber", tipo = "Gasto", presupuestoMensual = 20000.0),
            CategoriaEntity(nombre = "Seguro", tipo = "Gasto", presupuestoMensual = 50000.0),
            CategoriaEntity(nombre = "Salir a comer", tipo = "Gasto", presupuestoMensual = 50000.0),
            CategoriaEntity(nombre = "Almacen", tipo = "Gasto", presupuestoMensual = 60000.0),
            CategoriaEntity(nombre = "Gas", tipo = "Gasto", presupuestoMensual = 75000.0),
            CategoriaEntity(nombre = "Peajes", tipo = "Gasto", presupuestoMensual = 50000.0),
            CategoriaEntity(nombre = "Delivery", tipo = "Gasto", presupuestoMensual = 20000.0),
            CategoriaEntity(nombre = "Luz", tipo = "Gasto", presupuestoMensual = 35000.0),
            CategoriaEntity(nombre = "Internet", tipo = "Gasto", presupuestoMensual = 17000.0),
            CategoriaEntity(nombre = "Streaming", tipo = "Gasto", presupuestoMensual = 15000.0),
            CategoriaEntity(nombre = "Bubi", tipo = "Gasto", presupuestoMensual = 50000.0),
            CategoriaEntity(nombre = "Agua", tipo = "Gasto", presupuestoMensual = 20000.0),
            CategoriaEntity(nombre = "Farmacia", tipo = "Gasto", presupuestoMensual = 10000.0),
            CategoriaEntity(nombre = "Casa", tipo = "Gasto", presupuestoMensual = 20000.0),
            CategoriaEntity(nombre = "Medico", tipo = "Gasto", presupuestoMensual = 15000.0),
            CategoriaEntity(nombre = "Regalos", tipo = "Gasto", presupuestoMensual = 15000.0),
            CategoriaEntity(nombre = "Credito", tipo = "Gasto", presupuestoMensual = 0.0),
            CategoriaEntity(nombre = "Antojos", tipo = "Gasto", presupuestoMensual = 0.0),
            CategoriaEntity(nombre = "Imprevistos", tipo = "Gasto", presupuestoMensual = 0.0)
            // Las categorías de ingreso pueden agregarse aquí si se desea con presupuesto 0.0
        )
        
        println("📝 CATEGORIA_DEBUG: Iniciando inserción de categorías por defecto")
        var categoriasInsertadas = 0
        
        // Solo insertar categorías que no existan
        categoriasDefault.forEach { categoria ->
            if (!existeCategoria(categoria.nombre)) {
                categoriaDao.agregarCategoria(categoria)
                categoriasInsertadas++
                println("✅ Categoría insertada: ${categoria.nombre} con presupuesto: ${categoria.presupuestoMensual}")
            } else {
                println("⏭️ Categoría ya existe, omitiendo: ${categoria.nombre}")
            }
        }
        
        println("📊 CATEGORIA_DEBUG: Total de categorías insertadas: $categoriasInsertadas")
        
        // Verificar categorías insertadas
        val categoriasVerificadas = categoriaDao.obtenerCategorias()
        println("🔍 CATEGORIA_DEBUG: Total de categorías en BD: ${categoriasVerificadas.size}")
        categoriasVerificadas.forEach { cat ->
            println("🔍 CATEGORIA_DEBUG: ID=${cat.id}, Nombre=${cat.nombre}, Presupuesto=${cat.presupuestoMensual}")
        }
    }

    override suspend fun existeCategoria(nombre: String): Boolean {
        return categoriaDao.existeCategoria(nombre) > 0
    }

    override suspend fun limpiarYEliminarDuplicados() {
        val categorias = categoriaDao.obtenerCategorias()
        val normalizadas = mutableMapOf<String, CategoriaEntity>()
        for (cat in categorias) {
            val nombreNormalizado = cat.nombre.trim().lowercase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u").replace("ñ", "n")
            if (!normalizadas.containsKey(nombreNormalizado)) {
                // Actualizar nombre si es necesario
                if (cat.nombre != nombreNormalizado) {
                    val actualizada = cat.copy(nombre = nombreNormalizado)
                    categoriaDao.actualizarCategoria(actualizada)
                    normalizadas[nombreNormalizado] = actualizada
                } else {
                    normalizadas[nombreNormalizado] = cat
                }
            } else {
                // Eliminar duplicado
                categoriaDao.eliminarCategoria(cat)
            }
        }
    }

    override suspend fun obtenerCategorias(): List<com.aranthalion.controlfinanzas.domain.categoria.Categoria> = categoriaDao.obtenerCategorias().map { toDomain(it) }
} 