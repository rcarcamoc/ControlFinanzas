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

    override fun getAllCategorias(): Flow<List<Categoria>> {
        return categoriaDao.getAllCategorias().map { entities ->
            entities.map { entity ->
                Categoria(
                    id = entity.id.toInt(),
                    nombre = entity.nombre,
                    descripcion = entity.descripcion
                )
            }
        }
    }

    override fun getCategoriasByTipo(tipo: String): Flow<List<Categoria>> {
        return categoriaDao.getCategoriasByTipo(tipo).map { entities ->
            entities.map { entity ->
                Categoria(
                    id = entity.id.toInt(),
                    nombre = entity.nombre,
                    descripcion = entity.descripcion
                )
            }
        }
    }

    override suspend fun insertCategoria(categoria: Categoria) {
        val entity = CategoriaEntity(
            id = categoria.id.toLong(),
            nombre = categoria.nombre,
            descripcion = categoria.descripcion,
            tipo = "Gasto" // Por defecto
        )
        categoriaDao.agregarCategoria(entity)
    }

    override suspend fun deleteCategoria(categoria: Categoria) {
        val entity = CategoriaEntity(
            id = categoria.id.toLong(),
            nombre = categoria.nombre,
            descripcion = categoria.descripcion,
            tipo = "Gasto" // Por defecto
        )
        categoriaDao.eliminarCategoria(entity)
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
} 