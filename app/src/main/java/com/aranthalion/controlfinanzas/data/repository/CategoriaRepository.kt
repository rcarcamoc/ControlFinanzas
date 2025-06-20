package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import javax.inject.Inject

class CategoriaRepository @Inject constructor(
    private val categoriaDao: CategoriaDao
) {
    suspend fun obtenerCategorias(): List<Categoria> = categoriaDao.obtenerCategorias()
    suspend fun agregarCategoria(categoria: Categoria) = categoriaDao.agregarCategoria(categoria)
    suspend fun actualizarCategoria(categoria: Categoria) = categoriaDao.actualizarCategoria(categoria)
    suspend fun eliminarCategoria(categoria: Categoria) = categoriaDao.eliminarCategoria(categoria)
    suspend fun insertarCategoriasDefault() {
        val categoriasDefault = listOf(
            Categoria(nombre = "Arriendo", tipo = "Gasto"),
            Categoria(nombre = "Tarjeta titular", tipo = "Gasto"),
            Categoria(nombre = "Vacaciones", tipo = "Gasto"),
            Categoria(nombre = "Supermercado", tipo = "Gasto"),
            Categoria(nombre = "Gastos comunes", tipo = "Gasto"),
            Categoria(nombre = "Choquito", tipo = "Gasto"),
            Categoria(nombre = "Bencina", tipo = "Gasto"),
            Categoria(nombre = "Veguita", tipo = "Gasto"),
            Categoria(nombre = "Gatos", tipo = "Gasto"),
            Categoria(nombre = "Uber", tipo = "Gasto"),
            Categoria(nombre = "Seguro", tipo = "Gasto"),
            Categoria(nombre = "Salir a comer", tipo = "Gasto"),
            Categoria(nombre = "Almacen", tipo = "Gasto"),
            Categoria(nombre = "Gas", tipo = "Gasto"),
            Categoria(nombre = "Peajes", tipo = "Gasto"),
            Categoria(nombre = "Delivery", tipo = "Gasto"),
            Categoria(nombre = "Luz", tipo = "Gasto"),
            Categoria(nombre = "Internet", tipo = "Gasto"),
            Categoria(nombre = "Streaming", tipo = "Gasto"),
            Categoria(nombre = "Bubi", tipo = "Gasto"),
            Categoria(nombre = "Agua", tipo = "Gasto"),
            Categoria(nombre = "Farmacia", tipo = "Gasto"),
            Categoria(nombre = "Casa", tipo = "Gasto"),
            Categoria(nombre = "Medico", tipo = "Gasto"),
            Categoria(nombre = "Regalos", tipo = "Gasto"),
            Categoria(nombre = "Credito", tipo = "Gasto"),
            Categoria(nombre = "Antojos", tipo = "Gasto"),
            Categoria(nombre = "Imprevistos", tipo = "Gasto"),
            Categoria(nombre = "Salario", tipo = "Ingreso"),
            Categoria(nombre = "Freelance", tipo = "Ingreso"),
            Categoria(nombre = "Inversiones", tipo = "Ingreso"),
            Categoria(nombre = "Otros Ingresos", tipo = "Ingreso")
        )
        categoriasDefault.forEach { categoriaDao.agregarCategoria(it) }
    }
} 