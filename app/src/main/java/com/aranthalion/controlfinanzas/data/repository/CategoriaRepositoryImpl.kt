package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.entity.Categoria as CategoriaEntity
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoriaRepositoryImpl @Inject constructor(
    private val categoriaDao: CategoriaDao
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
        categoriaDao.agregarCategoria(toEntity(categoria))
    }

    override suspend fun updateCategoria(categoria: com.aranthalion.controlfinanzas.domain.categoria.Categoria) {
        categoriaDao.actualizarCategoria(toEntity(categoria))
    }

    override suspend fun deleteCategoria(categoria: com.aranthalion.controlfinanzas.domain.categoria.Categoria) {
        categoriaDao.eliminarCategoria(toEntity(categoria))
    }

    override suspend fun insertDefaultCategorias() {
        val categoriasDefault = listOf(
            CategoriaEntity(nombre = "Arriendo", tipo = "Gasto"),
            CategoriaEntity(nombre = "Tarjeta titular", tipo = "Gasto"),
            CategoriaEntity(nombre = "Vacaciones", tipo = "Gasto"),
            CategoriaEntity(nombre = "Supermercado", tipo = "Gasto"),
            CategoriaEntity(nombre = "Gastos comunes", tipo = "Gasto"),
            CategoriaEntity(nombre = "Choquito", tipo = "Gasto"),
            CategoriaEntity(nombre = "Bencina", tipo = "Gasto"),
            CategoriaEntity(nombre = "Veguita", tipo = "Gasto"),
            CategoriaEntity(nombre = "Gatos", tipo = "Gasto"),
            CategoriaEntity(nombre = "Uber", tipo = "Gasto"),
            CategoriaEntity(nombre = "Seguro", tipo = "Gasto"),
            CategoriaEntity(nombre = "Salir a comer", tipo = "Gasto"),
            CategoriaEntity(nombre = "Almacen", tipo = "Gasto"),
            CategoriaEntity(nombre = "Gas", tipo = "Gasto"),
            CategoriaEntity(nombre = "Peajes", tipo = "Gasto"),
            CategoriaEntity(nombre = "Delivery", tipo = "Gasto"),
            CategoriaEntity(nombre = "Luz", tipo = "Gasto"),
            CategoriaEntity(nombre = "Internet", tipo = "Gasto"),
            CategoriaEntity(nombre = "Streaming", tipo = "Gasto"),
            CategoriaEntity(nombre = "Bubi", tipo = "Gasto"),
            CategoriaEntity(nombre = "Agua", tipo = "Gasto"),
            CategoriaEntity(nombre = "Farmacia", tipo = "Gasto"),
            CategoriaEntity(nombre = "Casa", tipo = "Gasto"),
            CategoriaEntity(nombre = "Medico", tipo = "Gasto"),
            CategoriaEntity(nombre = "Regalos", tipo = "Gasto"),
            CategoriaEntity(nombre = "Credito", tipo = "Gasto"),
            CategoriaEntity(nombre = "Antojos", tipo = "Gasto"),
            CategoriaEntity(nombre = "Imprevistos", tipo = "Gasto"),
            CategoriaEntity(nombre = "Salario", tipo = "Ingreso"),
            CategoriaEntity(nombre = "Freelance", tipo = "Ingreso"),
            CategoriaEntity(nombre = "Inversiones", tipo = "Ingreso"),
            CategoriaEntity(nombre = "Otros Ingresos", tipo = "Ingreso")
        )
        categoriasDefault.forEach { categoriaDao.agregarCategoria(it) }
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