package com.aranthalion.controlfinanzas.data.repository

import android.content.Context
import android.util.Log
import com.aranthalion.controlfinanzas.data.local.dao.ClasificacionAutomaticaDao
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.entity.ClasificacionAutomaticaEntity
import com.aranthalion.controlfinanzas.domain.clasificacion.ClasificacionAutomatica
import com.aranthalion.controlfinanzas.domain.clasificacion.ClasificacionAutomaticaRepository
import com.aranthalion.controlfinanzas.domain.clasificacion.SugerenciaClasificacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class ClasificacionAutomaticaRepositoryImpl @Inject constructor(
    private val clasificacionDao: ClasificacionAutomaticaDao,
    private val categoriaDao: CategoriaDao,
    private val context: Context
) : ClasificacionAutomaticaRepository {

    override suspend fun guardarPatron(patron: String, categoriaId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val patronNormalizado = patron.trim().lowercase()
                val patronExistente = clasificacionDao.obtenerPatronPorDescripcion(patronNormalizado)
                
                if (patronExistente != null && patronExistente.categoriaId == categoriaId) {
                    // Actualizar frecuencia y confianza
                    val nuevaFrecuencia = patronExistente.frecuencia + 1
                    val nuevaConfianza = calcularConfianza(nuevaFrecuencia, patronNormalizado.length)
                    clasificacionDao.actualizarFrecuenciaYConfianza(patronNormalizado, categoriaId, nuevaConfianza)
                } else if (patronExistente != null) {
                    // Conflicto: mismo patrón, diferente categoría
                    val nuevaConfianza = calcularConfianza(1, patronNormalizado.length)
                    val nuevoPatron = ClasificacionAutomaticaEntity(
                        patron = patronNormalizado,
                        categoriaId = categoriaId,
                        nivelConfianza = nuevaConfianza,
                        frecuencia = 1
                    )
                    clasificacionDao.insertarPatron(nuevoPatron)
                } else {
                    // Nuevo patrón
                    val nuevaConfianza = calcularConfianza(1, patronNormalizado.length)
                    val nuevoPatron = ClasificacionAutomaticaEntity(
                        patron = patronNormalizado,
                        categoriaId = categoriaId,
                        nivelConfianza = nuevaConfianza,
                        frecuencia = 1
                    )
                    clasificacionDao.insertarPatron(nuevoPatron)
                }
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "Error al guardar patrón: ${e.message}")
            }
        }
    }

    override suspend fun obtenerSugerencia(descripcion: String): SugerenciaClasificacion? {
        return withContext(Dispatchers.IO) {
            try {
                val descripcionNormalizada = descripcion.trim().lowercase()
                val mejorCoincidencia = clasificacionDao.buscarMejorCoincidencia(descripcionNormalizada)
                
                mejorCoincidencia?.let {
                    if (it.nivelConfianza >= 0.3) { // Umbral mínimo de confianza
                        SugerenciaClasificacion(
                            categoriaId = it.categoriaId,
                            nivelConfianza = it.nivelConfianza,
                            patron = it.patron
                        )
                    } else null
                }
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "Error al obtener sugerencia: ${e.message}")
                null
            }
        }
    }

    override suspend fun obtenerTodosLosPatrones(): List<ClasificacionAutomatica> {
        return withContext(Dispatchers.IO) {
            try {
                clasificacionDao.obtenerTodosLosPatrones().map { entity ->
                    ClasificacionAutomatica(
                        id = entity.id,
                        patron = entity.patron,
                        categoriaId = entity.categoriaId,
                        nivelConfianza = entity.nivelConfianza,
                        frecuencia = entity.frecuencia,
                        ultimaActualizacion = entity.ultimaActualizacion
                    )
                }
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "Error al obtener patrones: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun actualizarConfianza(patron: String, categoriaId: Long, nuevaConfianza: Double) {
        withContext(Dispatchers.IO) {
            try {
                clasificacionDao.actualizarFrecuenciaYConfianza(patron, categoriaId, nuevaConfianza)
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "Error al actualizar confianza: ${e.message}")
            }
        }
    }

    override suspend fun cargarDatosHistoricos() {
        withContext(Dispatchers.IO) {
            try {
                // Cargar datos del archivo de movimientos históricos
                cargarMovimientosHistoricos()
                
                // Cargar datos del archivo de gastos históricos
                cargarGastosHistoricos()
                
                Log.i("ClasificacionRepo", "Datos históricos cargados exitosamente")
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "Error al cargar datos históricos: ${e.message}")
            }
        }
    }

    private suspend fun cargarMovimientosHistoricos() {
        try {
            val inputStream = context.assets.open("Archivos/Movimientos_historicos/Historia.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line = reader.readLine() // Saltar encabezado
            
            while (line != null) {
                val parts = line.split(",")
                if (parts.size >= 2) {
                    val movimiento = parts[0].trim()
                    val categoria = parts[1].trim()
                    
                    // Buscar la categoría en la base de datos
                    val categoriaEntity = categoriaDao.obtenerCategoriaPorNombre(categoria)
                    categoriaEntity?.let { cat ->
                        guardarPatron(movimiento, cat.id)
                    }
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("ClasificacionRepo", "Error al cargar movimientos históricos: ${e.message}")
        }
    }

    private suspend fun cargarGastosHistoricos() {
        try {
            val inputStream = context.assets.open("Archivos/Gastos_historicos/historial_gastos.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line = reader.readLine() // Saltar encabezado
            
            while (line != null) {
                val parts = line.split(",")
                if (parts.size >= 1) {
                    val item = parts[0].trim()
                    
                    // Mapear items a categorías basándose en patrones conocidos
                    val categoriaId = mapearItemACategoria(item)
                    if (categoriaId != null) {
                        guardarPatron(item, categoriaId)
                    }
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("ClasificacionRepo", "Error al cargar gastos históricos: ${e.message}")
        }
    }

    private suspend fun mapearItemACategoria(item: String): Long? {
        val itemLower = item.lowercase()
        
        // Mapeo basado en patrones conocidos
        val mapeo = mapOf(
            "arriendo" to "Vivienda",
            "supermercado" to "Alimentación",
            "bencina" to "Transporte",
            "gas" to "Servicios",
            "luz" to "Servicios",
            "agua" to "Servicios",
            "internet" to "Servicios",
            "streaming" to "Entretenimiento",
            "farmacia" to "Salud",
            "medico" to "Salud",
            "seguro" to "Seguros",
            "peajes" to "Transporte",
            "uber" to "Transporte",
            "delivery" to "Alimentación",
            "salir a comer" to "Alimentación",
            "almacen" to "Alimentación",
            "gastos comunes" to "Vivienda",
            "casa" to "Vivienda",
            "bubi" to "Entretenimiento",
            "veguita" to "Alimentación",
            "gatos" to "Mascotas",
            "choquito" to "Alimentación",
            "regalos" to "Otros",
            "imprevistos" to "Otros",
            "antojos" to "Alimentación",
            "credito" to "Financiero",
            "vacaciones" to "Viajes"
        )
        
        for ((patron, categoriaNombre) in mapeo) {
            if (itemLower.contains(patron)) {
                val categoria = categoriaDao.obtenerCategoriaPorNombre(categoriaNombre)
                return categoria?.id
            }
        }
        
        return null
    }

    private fun calcularConfianza(frecuencia: Int, longitudPatron: Int): Double {
        // Fórmula para calcular confianza basada en frecuencia y longitud del patrón
        val confianzaFrecuencia = minOf(frecuencia / 10.0, 1.0) // Máximo 1.0
        val confianzaLongitud = minOf(longitudPatron / 20.0, 1.0) // Máximo 1.0
        return (confianzaFrecuencia * 0.7 + confianzaLongitud * 0.3).coerceIn(0.0, 1.0)
    }
} 