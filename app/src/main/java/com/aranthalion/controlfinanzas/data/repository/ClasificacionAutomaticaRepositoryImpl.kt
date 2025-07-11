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
                Log.d("ClasificacionRepo", "💾 Guardando patrón: '$patron' -> Categoría ID: $categoriaId")
                val patronNormalizado = patron.trim().lowercase()
                val patronExistente = clasificacionDao.obtenerPatronPorDescripcion(patronNormalizado)
                
                if (patronExistente != null && patronExistente.categoriaId == categoriaId) {
                    // Actualizar frecuencia y confianza
                    val nuevaFrecuencia = patronExistente.frecuencia + 1
                    val nuevaConfianza = calcularConfianza(nuevaFrecuencia, patronNormalizado.length)
                    Log.d("ClasificacionRepo", "🔄 Actualizando patrón existente: Frecuencia $nuevaFrecuencia, Confianza $nuevaConfianza")
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
                    Log.d("ClasificacionRepo", "⚠️ Conflicto de categorías, creando nuevo patrón: Confianza $nuevaConfianza")
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
                    Log.d("ClasificacionRepo", "🆕 Creando nuevo patrón: Confianza $nuevaConfianza")
                    clasificacionDao.insertarPatron(nuevoPatron)
                }
                Log.d("ClasificacionRepo", "✅ Patrón guardado exitosamente")
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "❌ Error al guardar patrón: ${e.message}")
            }
        }
    }

    override suspend fun obtenerSugerencia(descripcion: String): SugerenciaClasificacion? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ClasificacionRepo", "🔍 Buscando sugerencia para: '$descripcion'")
                val descripcionNormalizada = descripcion.trim().lowercase()
                val mejorCoincidencia = clasificacionDao.buscarMejorCoincidencia(descripcionNormalizada)
                
                if (mejorCoincidencia != null) {
                    Log.d("ClasificacionRepo", "🎯 Mejor coincidencia encontrada: '${mejorCoincidencia.patron}' -> Categoría ID: ${mejorCoincidencia.categoriaId}, Confianza: ${mejorCoincidencia.nivelConfianza}")
                    
                    if (mejorCoincidencia.nivelConfianza >= 0.3) { // Umbral mínimo de confianza
                        Log.d("ClasificacionRepo", "✅ Sugerencia válida (confianza >= 0.3)")
                        SugerenciaClasificacion(
                            categoriaId = mejorCoincidencia.categoriaId,
                            nivelConfianza = mejorCoincidencia.nivelConfianza,
                            patron = mejorCoincidencia.patron
                        )
                    } else {
                        Log.d("ClasificacionRepo", "❌ Confianza insuficiente: ${mejorCoincidencia.nivelConfianza} < 0.3")
                        null
                    }
                } else {
                    Log.d("ClasificacionRepo", "❌ No se encontró coincidencia en la base de datos")
                    null
                }
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "❌ Error al obtener sugerencia: ${e.message}")
                null
            }
        }
    }

    override suspend fun obtenerTodosLosPatrones(): List<ClasificacionAutomatica> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ClasificacionRepo", "📋 Obteniendo todos los patrones...")
                val patrones = clasificacionDao.obtenerTodosLosPatrones().map { entity ->
                    ClasificacionAutomatica(
                        id = entity.id,
                        patron = entity.patron,
                        categoriaId = entity.categoriaId,
                        nivelConfianza = entity.nivelConfianza,
                        frecuencia = entity.frecuencia,
                        ultimaActualizacion = entity.ultimaActualizacion
                    )
                }
                Log.d("ClasificacionRepo", "📊 Total de patrones obtenidos: ${patrones.size}")
                patrones.forEach { patron ->
                    Log.d("ClasificacionRepo", "  - '${patron.patron}' -> Categoría ID: ${patron.categoriaId}, Confianza: ${patron.nivelConfianza}, Frecuencia: ${patron.frecuencia}")
                }
                patrones
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "❌ Error al obtener patrones: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun actualizarConfianza(patron: String, categoriaId: Long, nuevaConfianza: Double) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("ClasificacionRepo", "🔄 Actualizando confianza: '$patron' -> $nuevaConfianza")
                clasificacionDao.actualizarFrecuenciaYConfianza(patron, categoriaId, nuevaConfianza)
                Log.d("ClasificacionRepo", "✅ Confianza actualizada exitosamente")
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "❌ Error al actualizar confianza: ${e.message}")
            }
        }
    }

    override suspend fun cargarDatosHistoricos() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("ClasificacionRepo", "📚 Iniciando carga de datos históricos...")
                
                // Verificar cantidad actual de patrones
                val patronesAntes = clasificacionDao.obtenerCantidadPatrones()
                Log.d("ClasificacionRepo", "📊 Patrones antes de la carga: $patronesAntes")
                
                // NO limpiar patrones existentes - preservar datos del usuario
                // Solo cargar patrones predefinidos si no existen
                if (patronesAntes == 0) {
                    Log.d("ClasificacionRepo", "📖 Cargando patrones predefinidos (base de datos vacía)...")
                    cargarPatronesPredefinidos()
                } else {
                    Log.d("ClasificacionRepo", "ℹ️ Patrones existentes detectados - preservando datos del usuario")
                }
                
                // Verificar cantidad final de patrones
                val patronesDespues = clasificacionDao.obtenerCantidadPatrones()
                Log.d("ClasificacionRepo", "📊 Patrones después de la carga: $patronesDespues")
                Log.i("ClasificacionRepo", "✅ Datos históricos cargados exitosamente (preservando datos del usuario)")
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "❌ Error al cargar datos históricos: ${e.message}")
            }
        }
    }

    private suspend fun cargarPatronesPredefinidos() {
        try {
            Log.d("ClasificacionRepo", "📖 Cargando patrones predefinidos...")
            
            // Obtener todas las categorías para mapear por nombre
            val categorias = categoriaDao.obtenerCategorias()
            val categoriasMap = categorias.associateBy { it.nombre.lowercase() }
            
            Log.d("ClasificacionRepo", "📋 Categorías disponibles: ${categorias.map { it.nombre }}")
            
            // Patrones predefinidos basados en el archivo Historia.csv
            val patronesPredefinidos = listOf(
                // Datos exactos del archivo Historia.csv
                "UNIRED CL AGUAS AND SANTIAGO" to "Agua",
                "ALICIA 11001SANTIAG" to "Almacen",
                "DISTRIBUIDORA LOS C SANTIAGO" to "Almacen",
                "EVENTOS HOLZ PADRE HURTAD" to "Almacen",
                "KIOSCO UBERLINDA MA SANTIAGO" to "Almacen",
                "LA MARTITA SANTIAGO" to "Almacen",
                "MERPAGO*CONFITERIA SANTIAGO" to "Almacen",
                "MINIMARKET FIONA NUNOA" to "Almacen",
                "PANIFICADORA NUEVA SANTIAGO" to "Almacen",
                "PAYSCAN*SNACK CENTE SANTIAGO" to "Almacen",
                "SANTUARIO PADRE HUR SANTIAGO" to "Almacen",
                "SERVICIOS Y COMERCI PROVIDENCIA" to "Almacen",
                "SumUp * Rita Oliva Santiago" to "Almacen",
                "VELERITO SANTIAGO" to "Almacen",
                "arariendo" to "Arriendo",
                "arriendo" to "Arriendo",
                "COPEC APP SANTIAGO" to "Bencina",
                "COPEC APP COMPRAS" to "Bencina",
                "COPEC APP REV.COMPRAS" to "Bencina",
                "SHELL.FILE181 SANTIAGO" to "Bencina",
                "ANLUKE COMPRAS" to "Bubi",
                "COLLOKY MALL PASEO COMPRAS" to "Bubi",
                "COLLOKY O BUENAVENT COMPRAS" to "Bubi",
                "FALABELLA COSTANERA SAN NC 01-03" to "Bubi",
                "GOOGLE PLAY YOUTUBE COMPRAS" to "Bubi",
                "ORLANDO HIDALGO COMPRAS" to "Bubi",
                "OUTLET EASTON COMPRAS" to "Bubi",
                "PILLIN COMPRAS" to "Bubi",
                "TUU*estiloKidsPeluq PROVIDENCIA" to "Bubi",
                "TUU*estiloKidsPeluq COMPRAS" to "Bubi",
                "TUU*estiloKidsPeluq PROVIDENCIA" to "Bubi",
                "CASA IDEAS PORTAL L COMPRAS" to "Casa",
                
                // Patrones adicionales de gastos históricos para mayor cobertura
                "arriendo" to "Arriendo",
                "supermercado" to "Supermercado",
                "gastos comunes" to "Gastos comunes",
                "choquito" to "Choquito",
                "bencina" to "Bencina",
                "veguita" to "Veguita",
                "gatos" to "Gatos",
                "uber" to "Uber",
                "seguro" to "Seguro",
                "salir a comer" to "Salir a comer",
                "almacen" to "Almacen",
                "gas" to "Gas",
                "peajes" to "Peajes",
                "delivery" to "Delivery",
                "luz" to "Luz",
                "internet" to "Internet",
                "streaming" to "Streaming",
                "bubi" to "Bubi",
                "agua" to "Agua",
                "farmacia" to "Farmacia",
                "casa" to "Casa",
                "medico" to "Medico",
                "regalos" to "Regalos",
                "credito" to "Credito",
                "vacaciones" to "Vacaciones",
                "antojos" to "Antojos",
                "imprevistos" to "Imprevistos"
            )
            
            var contador = 0
            for ((patron, categoriaNombre) in patronesPredefinidos) {
                Log.d("ClasificacionRepo", "🔄 Procesando patrón: '$patron' -> '$categoriaNombre'")
                val categoriaId = categoriasMap[categoriaNombre.lowercase()]?.id
                if (categoriaId != null) {
                    guardarPatron(patron, categoriaId)
                    contador++
                    Log.d("ClasificacionRepo", "✅ Patrón predefinido: '$patron' -> '$categoriaNombre' (ID: $categoriaId)")
                } else {
                    Log.w("ClasificacionRepo", "⚠️ Categoría no encontrada: '$categoriaNombre' para patrón: '$patron'")
                }
            }
            
            Log.d("ClasificacionRepo", "✅ Patrones predefinidos cargados: $contador patrones")
        } catch (e: Exception) {
            Log.e("ClasificacionRepo", "❌ Error al cargar patrones predefinidos: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun buscarCategoriaPorNombreInsensible(nombre: String): com.aranthalion.controlfinanzas.data.local.entity.Categoria? {
        // Obtener todas las categorías y buscar por nombre insensible a mayúsculas/minúsculas
        val todasLasCategorias = categoriaDao.obtenerCategorias()
        return todasLasCategorias.find { it.nombre.equals(nombre, ignoreCase = true) }
    }

    private suspend fun mapearItemACategoria(item: String): Long? {
        val itemLower = item.lowercase()
        
        // Mapeo basado en patrones conocidos - usando los nombres exactos de las categorías por defecto
        val mapeo = mapOf(
            "arriendo" to "Arriendo",
            "supermercado" to "Supermercado",
            "bencina" to "Bencina",
            "gas" to "Gas",
            "luz" to "Luz",
            "agua" to "Agua",
            "internet" to "Internet",
            "streaming" to "Streaming",
            "farmacia" to "Farmacia",
            "medico" to "Medico",
            "seguro" to "Seguro",
            "peajes" to "Peajes",
            "uber" to "Uber",
            "delivery" to "Delivery",
            "salir a comer" to "Salir a comer",
            "almacen" to "Almacen",
            "gastos comunes" to "Gastos comunes",
            "casa" to "Casa",
            "bubi" to "Bubi",
            "veguita" to "Veguita",
            "gatos" to "Gatos",
            "choquito" to "Choquito",
            "regalos" to "Regalos",
            "imprevistos" to "Imprevistos",
            "antojos" to "Antojos",
            "credito" to "Credito",
            "vacaciones" to "Vacaciones"
        )
        
        for ((patron, categoriaNombre) in mapeo) {
            if (itemLower.contains(patron)) {
                val categoria = buscarCategoriaPorNombreInsensible(categoriaNombre)
                Log.d("ClasificacionRepo", "🎯 Mapeo encontrado: '$item' -> '$categoriaNombre' (ID: ${categoria?.id})")
                return categoria?.id
            }
        }
        
        return null
    }

    private fun calcularConfianza(frecuencia: Int, longitudPatron: Int): Double {
        // Fórmula para calcular confianza basada en frecuencia y longitud del patrón
        val confianzaFrecuencia = minOf(frecuencia / 10.0, 1.0) // Máximo 1.0
        val confianzaLongitud = minOf(longitudPatron / 20.0, 1.0) // Máximo 1.0
        val confianzaFinal = (confianzaFrecuencia * 0.7 + confianzaLongitud * 0.3).coerceIn(0.0, 1.0)
        Log.d("ClasificacionRepo", "🧮 Cálculo de confianza: Frecuencia=$frecuencia, Longitud=$longitudPatron, ConfianzaFrecuencia=$confianzaFrecuencia, ConfianzaLongitud=$confianzaLongitud, Final=$confianzaFinal")
        return confianzaFinal
    }

    // Métodos stub para cumplir con la interfaz
    override suspend fun agregarRegla(regla: com.aranthalion.controlfinanzas.domain.clasificacion.ReglaClasificacion) {
        // TODO: Implementar lógica real
    }
    override suspend fun obtenerReglas(): List<com.aranthalion.controlfinanzas.domain.clasificacion.ReglaClasificacion> = emptyList()
    override suspend fun actualizarRegla(regla: com.aranthalion.controlfinanzas.domain.clasificacion.ReglaClasificacion) {}
    override suspend fun eliminarRegla(reglaId: Long) {}
    override suspend fun obtenerPatronesAprendidos(): List<com.aranthalion.controlfinanzas.domain.clasificacion.PatronAprendido> = emptyList()
    override suspend fun obtenerPatronPorDescripcion(descripcion: String): com.aranthalion.controlfinanzas.domain.clasificacion.PatronAprendido? = null
    override suspend fun agregarPatron(patron: com.aranthalion.controlfinanzas.domain.clasificacion.PatronAprendido) {}
    override suspend fun actualizarPatron(patron: com.aranthalion.controlfinanzas.domain.clasificacion.PatronAprendido) {}
    override suspend fun obtenerTotalClasificaciones(): Int = 0
    override suspend fun obtenerClasificacionesAutomaticas(): Int = 0
    override suspend fun obtenerPrecisionPromedio(): Double = 0.0
    override suspend fun obtenerCategoriasMasUsadas(): List<com.aranthalion.controlfinanzas.domain.clasificacion.CategoriaUso> = emptyList()
    override suspend fun obtenerPatronesMasEfectivos(): List<com.aranthalion.controlfinanzas.domain.clasificacion.PatronEfectivo> = emptyList()
    override suspend fun registrarClasificacion(descripcion: String, categoriaId: Long, esCorrecta: Boolean) {}
} 