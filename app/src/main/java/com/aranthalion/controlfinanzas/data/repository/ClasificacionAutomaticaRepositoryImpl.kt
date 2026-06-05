package com.aranthalion.controlfinanzas.data.repository

import android.content.Context
import android.util.Log
import com.aranthalion.controlfinanzas.data.local.dao.ClasificacionAutomaticaDao
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.entity.ClasificacionAutomaticaEntity
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.domain.clasificacion.*
import com.aranthalion.controlfinanzas.data.util.ClasificacionNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClasificacionAutomaticaRepositoryImpl @Inject constructor(
    private val clasificacionDao: ClasificacionAutomaticaDao,
    private val categoriaDao: CategoriaDao,
    private val movimientoDao: MovimientoDao,
    private val context: Context
) : ClasificacionAutomaticaRepository {

    // Cache de clasificaciones históricas
    private var cacheClasificaciones: Map<String, Long> = emptyMap()
    private var cacheActualizado = false

    override suspend fun obtenerSugerenciaMejorada(descripcion: String): ResultadoClasificacion {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ClasificacionRepo", "🔍 Buscando sugerencia mejorada para: '$descripcion'")
                
                // Asegurar que el cache esté actualizado
                if (!cacheActualizado) {
                    actualizarCacheClasificaciones()
                }
                
                // 1. Buscar coincidencia exacta (100% confianza)
                val coincidenciaExacta = ClasificacionNormalizer.buscarCoincidenciaExacta(descripcion, cacheClasificaciones)
                if (coincidenciaExacta != null) {
                    Log.d("ClasificacionRepo", "✅ Coincidencia exacta encontrada: ${coincidenciaExacta.first}")
                    return@withContext ResultadoClasificacion.AltaConfianza(
                        categoriaId = coincidenciaExacta.first,
                        confianza = coincidenciaExacta.second,
                        patron = descripcion,
                        tipoCoincidencia = TipoCoincidencia.EXACTA
                    )
                }
                
                // 2. Buscar coincidencia parcial (90% confianza)
                val coincidenciaParcial = ClasificacionNormalizer.buscarCoincidenciaParcial(descripcion, cacheClasificaciones)
                if (coincidenciaParcial != null) {
                    Log.d("ClasificacionRepo", "✅ Coincidencia parcial encontrada: ${coincidenciaParcial.first}")
                    return@withContext ResultadoClasificacion.AltaConfianza(
                        categoriaId = coincidenciaParcial.first,
                        confianza = coincidenciaParcial.second,
                        patron = descripcion,
                        tipoCoincidencia = TipoCoincidencia.PARCIAL
                    )
                }
                
                // 3. Buscar coincidencia fuzzy (60-80% confianza)
                val coincidenciaFuzzy = ClasificacionNormalizer.buscarCoincidenciaFuzzy(descripcion, cacheClasificaciones)
                if (coincidenciaFuzzy != null && ClasificacionNormalizer.esConfianzaSuficiente(coincidenciaFuzzy.second)) {
                    Log.d("ClasificacionRepo", "✅ Coincidencia fuzzy encontrada: ${coincidenciaFuzzy.first} (${(coincidenciaFuzzy.second * 100).toInt()}%)")
                    return@withContext ResultadoClasificacion.AltaConfianza(
                        categoriaId = coincidenciaFuzzy.first,
                        confianza = coincidenciaFuzzy.second,
                        patron = descripcion,
                        tipoCoincidencia = determinarTipoCoincidencia(coincidenciaFuzzy.second)
                    )
                }
                
                // 4. Buscar en patrones aprendidos (sistema anterior)
                val sugerenciaPatron = obtenerSugerencia(descripcion)
                if (sugerenciaPatron != null && ClasificacionNormalizer.esConfianzaSuficiente(sugerenciaPatron.nivelConfianza)) {
                    Log.d("ClasificacionRepo", "✅ Sugerencia de patrón encontrada: ${sugerenciaPatron.categoriaId} (${(sugerenciaPatron.nivelConfianza * 100).toInt()}%)")
                    return@withContext ResultadoClasificacion.AltaConfianza(
                        categoriaId = sugerenciaPatron.categoriaId,
                        confianza = sugerenciaPatron.nivelConfianza,
                        patron = sugerenciaPatron.patron,
                        tipoCoincidencia = TipoCoincidencia.PATRON
                    )
                }
                
                // 5. Si no hay coincidencias suficientes, devolver sugerencias de baja confianza
                val sugerenciasBajaConfianza = obtenerSugerenciasBajaConfianza(descripcion)
                if (sugerenciasBajaConfianza.isNotEmpty()) {
                    Log.d("ClasificacionRepo", "⚠️ Sugerencias de baja confianza encontradas: ${sugerenciasBajaConfianza.size}")
                    return@withContext ResultadoClasificacion.BajaConfianza(
                        sugerencias = sugerenciasBajaConfianza,
                        confianzaMaxima = sugerenciasBajaConfianza.maxOfOrNull { it.nivelConfianza } ?: 0.0
                    )
                }
                
                // 6. Sin coincidencias
                Log.d("ClasificacionRepo", "❌ No se encontraron coincidencias para: '$descripcion'")
                return@withContext ResultadoClasificacion.SinCoincidencias(
                    descripcion = descripcion,
                    razon = "No hay datos históricos suficientes para clasificar esta transacción"
                )
                
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "❌ Error al obtener sugerencia mejorada: ${e.message}")
                return@withContext ResultadoClasificacion.SinCoincidencias(
                    descripcion = descripcion,
                    razon = "Error en el sistema de clasificación: ${e.message}"
                )
            }
        }
    }

    private fun determinarTipoCoincidencia(confianza: Double): TipoCoincidencia {
        return when {
            confianza >= 1.0 -> TipoCoincidencia.EXACTA
            confianza >= 0.9 -> TipoCoincidencia.PARCIAL
            confianza >= 0.8 -> TipoCoincidencia.FUZZY_ALTA
            confianza >= 0.6 -> TipoCoincidencia.FUZZY_MEDIA
            else -> TipoCoincidencia.PATRON
        }
    }

    override suspend fun actualizarCacheClasificaciones() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("ClasificacionRepo", "🔄 Actualizando cache de clasificaciones...")
                
                // Obtener todas las transacciones ya clasificadas
                val movimientosClasificados = movimientoDao.obtenerMovimientosConCategoria()
                
                // Crear cache de clasificaciones históricas
                val nuevoCache = mutableMapOf<String, Long>()
                
                for (movimiento in movimientosClasificados) {
                    if (movimiento.categoriaId != null) {
                        val descripcionNormalizada = ClasificacionNormalizer.normalizarDescripcion(movimiento.descripcion)
                        nuevoCache[descripcionNormalizada] = movimiento.categoriaId
                        
                        // También agregar variaciones para mejorar la búsqueda
                        val variaciones = ClasificacionNormalizer.generarVariaciones(movimiento.descripcion)
                        for (variacion in variaciones) {
                            if (variacion != descripcionNormalizada) {
                                nuevoCache[variacion] = movimiento.categoriaId
                            }
                        }
                    }
                }
                
                cacheClasificaciones = nuevoCache
                cacheActualizado = true
                
                Log.d("ClasificacionRepo", "✅ Cache actualizado: ${cacheClasificaciones.size} clasificaciones históricas")
                
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "❌ Error al actualizar cache: ${e.message}")
            }
        }
    }

    override suspend fun obtenerEstadisticasClasificacion(): EstadisticasClasificacion {
        return withContext(Dispatchers.IO) {
            try {
                val movimientos = movimientoDao.obtenerMovimientos()
                val categorias = categoriaDao.obtenerCategorias()
                
                val totalTransacciones = movimientos.size
                val transaccionesClasificadas = movimientos.count { it.categoriaId != null }
                val sinClasificar = totalTransacciones - transaccionesClasificadas
                
                // Calcular estadísticas por tipo de coincidencia
                var clasificacionesExactas = 0
                var clasificacionesParciales = 0
                var clasificacionesFuzzy = 0
                
                // Calcular precisión promedio (simulada basada en confianza)
                val precisionPromedio = if (transaccionesClasificadas > 0) {
                    // Simular precisión basada en el número de transacciones clasificadas
                    (transaccionesClasificadas.toDouble() / totalTransacciones.toDouble()) * 0.85
                } else 0.0
                
                // Comercios más frecuentes
                val comerciosFrecuentes = movimientos
                    .filter { it.categoriaId != null }
                    .groupBy { ClasificacionNormalizer.normalizarDescripcion(it.descripcion) }
                    .map { (descripcion, movimientos) ->
                        val categoria = categorias.find { it.id == movimientos.first().categoriaId }
                        ComercioFrecuente(
                            nombreNormalizado = descripcion,
                            nombreOriginal = movimientos.first().descripcion,
                            categoriaId = movimientos.first().categoriaId!!,
                            frecuencia = movimientos.size,
                            confianzaPromedio = 0.9 // Simulado
                        )
                    }
                    .sortedByDescending { it.frecuencia }
                    .take(10)
                
                // Categorías más usadas
                val categoriasMasUsadas = movimientos
                    .filter { it.categoriaId != null }
                    .groupBy { it.categoriaId }
                    .map { (categoriaId, movimientos) ->
                        val categoria = categorias.find { it.id == categoriaId }
                        CategoriaUso(
                            categoriaId = categoriaId!!,
                            nombre = categoria?.nombre ?: "Desconocida",
                            frecuencia = movimientos.size,
                            porcentaje = (movimientos.size.toDouble() / totalTransacciones.toDouble()) * 100
                        )
                    }
                    .sortedByDescending { it.frecuencia }
                    .take(10)
                
                EstadisticasClasificacion(
                    totalTransacciones = totalTransacciones,
                    clasificacionesExactas = clasificacionesExactas,
                    clasificacionesParciales = clasificacionesParciales,
                    clasificacionesFuzzy = clasificacionesFuzzy,
                    sinClasificar = sinClasificar,
                    precisionPromedio = precisionPromedio,
                    comerciosMasFrecuentes = comerciosFrecuentes,
                    categoriasMasUsadas = categoriasMasUsadas
                )
                
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "❌ Error al obtener estadísticas: ${e.message}")
                EstadisticasClasificacion()
            }
        }
    }

    private suspend fun obtenerSugerenciasBajaConfianza(descripcion: String): List<SugerenciaClasificacion> {
        val sugerencias = mutableListOf<SugerenciaClasificacion>()
        
        // Buscar en patrones aprendidos con confianza baja
        val patrones = clasificacionDao.obtenerTodosLosPatrones()
        for (patron in patrones) {
            if (patron.nivelConfianza < 0.6 && patron.nivelConfianza > 0.3) {
                sugerencias.add(
                    SugerenciaClasificacion(
                        categoriaId = patron.categoriaId,
                        nivelConfianza = patron.nivelConfianza,
                        patron = patron.patron
                    )
                )
            }
        }
        
        return sugerencias.sortedByDescending { it.nivelConfianza }.take(3)
    }

    // Métodos existentes (mantener compatibilidad)
    override suspend fun guardarPatron(patron: String, categoriaId: Long) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("ClasificacionRepo", "💾 Guardando patrón: '$patron' -> Categoría ID: $categoriaId")
                val patronNormalizado = ClasificacionNormalizer.normalizarDescripcion(patron)
                
                // Verificar si ya existe este patrón exacto
                val existe = clasificacionDao.existePatron(patronNormalizado, categoriaId)
                if (existe > 0) {
                    Log.d("ClasificacionRepo", "⏭️ Patrón ya existe, actualizando frecuencia: '$patronNormalizado'")
                    val patronExistente = clasificacionDao.obtenerPatronPorDescripcion(patronNormalizado)
                    if (patronExistente != null) {
                        val nuevaFrecuencia = patronExistente.frecuencia + 1
                        val nuevaConfianza = calcularConfianza(nuevaFrecuencia, patronNormalizado.length)
                        clasificacionDao.actualizarFrecuenciaYConfianza(patronNormalizado, categoriaId, nuevaConfianza)
                        Log.d("ClasificacionRepo", "✅ Frecuencia actualizada: $nuevaFrecuencia, Confianza: $nuevaConfianza")
                    }
                    return@withContext
                }
                
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
                
                // Invalidar cache para forzar actualización
                cacheActualizado = false
                
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
                val descripcionNormalizada = ClasificacionNormalizer.normalizarDescripcion(descripcion)
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
                
                // Cargar datos del archivo CSV de historial
                ClasificacionCSVLoader.cargarDatosDesdeCSV(context, categoriaDao) { descripcion, catId ->
                    guardarPatron(descripcion, catId)
                }
                
                // Actualizar cache
                actualizarCacheClasificaciones()
                
                Log.d("ClasificacionRepo", "✅ Datos históricos cargados exitosamente")
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "❌ Error al cargar datos históricos: ${e.message}")
            }
        }
    }

    private fun calcularConfianza(frecuencia: Int, longitudPatron: Int): Double {
        // Fórmula mejorada para calcular confianza
        val confianzaFrecuencia = minOf(frecuencia / 5.0, 1.0) // Más sensible a la frecuencia
        val confianzaLongitud = minOf(longitudPatron / 15.0, 1.0) // Ajustado para patrones más cortos
        val confianzaFinal = (confianzaFrecuencia * 0.8 + confianzaLongitud * 0.2).coerceIn(0.0, 1.0)
        Log.d("ClasificacionRepo", "🧮 Cálculo de confianza: Frecuencia=$frecuencia, Longitud=$longitudPatron, ConfianzaFrecuencia=$confianzaFrecuencia, ConfianzaLongitud=$confianzaLongitud, Final=$confianzaFinal")
        return confianzaFinal
    }

    // Métodos stub para cumplir con la interfaz
    override suspend fun agregarRegla(regla: ReglaClasificacion) {}
    override suspend fun obtenerReglas(): List<ReglaClasificacion> = emptyList()
    override suspend fun actualizarRegla(regla: ReglaClasificacion) {}
    override suspend fun eliminarRegla(reglaId: Long) {}
    override suspend fun obtenerPatronesAprendidos(): List<PatronAprendido> = emptyList()
    override suspend fun obtenerPatronPorDescripcion(descripcion: String): PatronAprendido? = null
    override suspend fun agregarPatron(patron: PatronAprendido) {}
    override suspend fun actualizarPatron(patron: PatronAprendido) {}
    override suspend fun obtenerTotalClasificaciones(): Int = 0
    override suspend fun obtenerClasificacionesAutomaticas(): Int = 0
    override suspend fun obtenerPrecisionPromedio(): Double = 0.0
    override suspend fun obtenerCategoriasMasUsadas(): List<CategoriaUso> = emptyList()
    override suspend fun obtenerPatronesMasEfectivos(): List<PatronEfectivo> = emptyList()
    override suspend fun registrarClasificacion(descripcion: String, categoriaId: Long, esCorrecta: Boolean) {}

    /**
     * Limpia duplicados existentes en la base de datos de clasificación automática
     */
    override suspend fun limpiarDuplicados() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("ClasificacionRepo", "🧹 Iniciando limpieza de duplicados...")
                
                val todosLosPatrones = clasificacionDao.obtenerTodosLosPatrones()
                val patronesUnicos = mutableMapOf<String, ClasificacionAutomaticaEntity>()
                var duplicadosEliminados = 0
                
                for (patron in todosLosPatrones) {
                    val clave = "${patron.patron}_${patron.categoriaId}"
                    if (patronesUnicos.containsKey(clave)) {
                        // Duplicado encontrado, mantener el que tiene mayor frecuencia
                        val existente = patronesUnicos[clave]!!
                        if (patron.frecuencia > existente.frecuencia) {
                            patronesUnicos[clave] = patron
                            Log.d("ClasificacionRepo", "🔄 Reemplazando duplicado: '${patron.patron}' (frecuencia: ${patron.frecuencia} > ${existente.frecuencia})")
                        } else {
                            Log.d("ClasificacionRepo", "🗑️ Eliminando duplicado: '${patron.patron}' (frecuencia: ${patron.frecuencia} <= ${existente.frecuencia})")
                        }
                        duplicadosEliminados++
                    } else {
                        patronesUnicos[clave] = patron
                    }
                }
                
                // Recrear la tabla con solo patrones únicos
                clasificacionDao.eliminarTodosLosPatrones()
                
                for (patron in patronesUnicos.values) {
                    clasificacionDao.insertarPatron(patron)
                }
                
                Log.d("ClasificacionRepo", "✅ Limpieza completada: ${patronesUnicos.size} patrones únicos, $duplicadosEliminados duplicados eliminados")
                
                // Invalidar cache
                cacheActualizado = false
                
            } catch (e: Exception) {
                Log.e("ClasificacionRepo", "❌ Error al limpiar duplicados: ${e.message}")
            }
        }
    }
} 