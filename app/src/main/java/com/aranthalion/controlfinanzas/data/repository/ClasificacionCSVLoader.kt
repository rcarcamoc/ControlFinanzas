package com.aranthalion.controlfinanzas.data.repository

import android.content.Context
import android.util.Log
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.util.ClasificacionNormalizer
import java.io.BufferedReader
import java.io.InputStreamReader

object ClasificacionCSVLoader {
    suspend fun cargarDatosDesdeCSV(
        context: Context,
        categoriaDao: CategoriaDao,
        guardarPatron: suspend (String, Long) -> Unit
    ) {
        try {
            val inputStream = context.assets.open("Archivos/Movimientos_historicos/Historia.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            var lineNumber = 0
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                lineNumber++
                if (lineNumber == 1) continue // Saltar header
                
                line?.let { csvLine ->
                    val parts = csvLine.split(",")
                    if (parts.size >= 2) {
                        val descripcion = parts[0].trim()
                        val categoriaNombre = parts[1].trim()
                        
                        // Buscar categoría por nombre
                        val categorias = categoriaDao.obtenerCategorias()
                        val categoria = categorias.find { 
                            it.nombre.equals(categoriaNombre, ignoreCase = true) 
                        }
                        
                        if (categoria != null) {
                            // Guardar patrón normalizado
                            val descripcionNormalizada = ClasificacionNormalizer.normalizarDescripcion(descripcion)
                            guardarPatron(descripcionNormalizada, categoria.id)
                        }
                    }
                }
            }
            
            reader.close()
            Log.d("ClasificacionRepo", "📊 Datos CSV cargados: $lineNumber líneas procesadas")
            
        } catch (e: Exception) {
            Log.e("ClasificacionRepo", "❌ Error al cargar CSV: ${e.message}")
        }
    }
}
