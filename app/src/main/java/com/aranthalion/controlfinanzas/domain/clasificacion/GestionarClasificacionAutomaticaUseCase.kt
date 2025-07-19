package com.aranthalion.controlfinanzas.domain.clasificacion

import android.util.Log
import javax.inject.Inject

class GestionarClasificacionAutomaticaUseCase @Inject constructor(
    private val repository: ClasificacionAutomaticaRepository
) {
    
    /**
     * Registra un nuevo patrón o actualiza uno existente basado en la clasificación manual del usuario
     */
    suspend fun aprenderPatron(descripcion: String, categoriaId: Long) {
        Log.d("ClasificacionUseCase", "🔄 Aprendiendo patrón: '$descripcion' -> Categoría ID: $categoriaId")
        val patrones = extraerPatrones(descripcion)
        Log.d("ClasificacionUseCase", "�� Patrones extraídos (${patrones.size}): $patrones")
        
        if (patrones.isEmpty()) {
            Log.w("ClasificacionUseCase", "⚠️ No se extrajeron patrones válidos de: '$descripcion'")
            return
        }
        
        var patronesGuardados = 0
        var patronesDuplicados = 0
        
        patrones.forEach { patron ->
            try {
                repository.guardarPatron(patron, categoriaId)
                patronesGuardados++
                Log.d("ClasificacionUseCase", "✅ Patrón guardado: '$patron'")
            } catch (e: Exception) {
                patronesDuplicados++
                Log.w("ClasificacionUseCase", "⚠️ Error al guardar patrón '$patron': ${e.message}")
            }
        }
        
        Log.d("ClasificacionUseCase", "📊 Resumen aprendizaje: $patronesGuardados guardados, $patronesDuplicados duplicados/errores")
        Log.d("ClasificacionUseCase", "✅ Patrón aprendido exitosamente")
    }
    
    /**
     * Busca el patrón más relevante y devuelve una sugerencia de categoría con su nivel de confianza
     * MÉTODO LEGACY - Usar obtenerSugerenciaMejorada() en su lugar
     */
    suspend fun sugerirCategoria(descripcion: String): SugerenciaClasificacion? {
        Log.d("ClasificacionUseCase", "🔍 Buscando sugerencia para: '$descripcion'")
        val sugerencia = repository.obtenerSugerencia(descripcion)
        if (sugerencia != null) {
            Log.d("ClasificacionUseCase", "✅ Sugerencia encontrada: Categoría ID ${sugerencia.categoriaId}, Confianza: ${sugerencia.nivelConfianza}, Patrón: '${sugerencia.patron}'")
        } else {
            Log.d("ClasificacionUseCase", "❌ No se encontró sugerencia para: '$descripcion'")
        }
        return sugerencia
    }
    
    /**
     * NUEVO MÉTODO: Sistema mejorado de clasificación automática
     * Usa cache, fuzzy matching y umbrales configurables
     */
    suspend fun obtenerSugerenciaMejorada(descripcion: String): ResultadoClasificacion {
        Log.d("ClasificacionUseCase", "🔍 Buscando sugerencia mejorada para: '$descripcion'")
        val resultado = repository.obtenerSugerenciaMejorada(descripcion)
        
        when (resultado) {
            is ResultadoClasificacion.AltaConfianza -> {
                Log.d("ClasificacionUseCase", "✅ Alta confianza: Categoría ID ${resultado.categoriaId}, Confianza: ${(resultado.confianza * 100).toInt()}%, Tipo: ${resultado.tipoCoincidencia}")
            }
            is ResultadoClasificacion.BajaConfianza -> {
                Log.d("ClasificacionUseCase", "⚠️ Baja confianza: ${resultado.sugerencias.size} sugerencias, Máxima: ${(resultado.confianzaMaxima * 100).toInt()}%")
            }
            is ResultadoClasificacion.SinCoincidencias -> {
                Log.d("ClasificacionUseCase", "❌ Sin coincidencias: ${resultado.razon}")
            }
        }
        
        return resultado
    }
    
    /**
     * Carga los datos históricos para entrenar el sistema
     */
    suspend fun cargarDatosHistoricos() {
        Log.d("ClasificacionUseCase", "📚 Iniciando carga de datos históricos...")
        repository.cargarDatosHistoricos()
        Log.d("ClasificacionUseCase", "✅ Datos históricos cargados")
    }
    
    /**
     * Actualiza el cache de clasificaciones
     */
    suspend fun actualizarCacheClasificaciones() {
        Log.d("ClasificacionUseCase", "🔄 Actualizando cache de clasificaciones...")
        repository.actualizarCacheClasificaciones()
        Log.d("ClasificacionUseCase", "✅ Cache actualizado")
    }
    
    /**
     * Obtiene estadísticas del sistema de clasificación
     */
    suspend fun obtenerEstadisticasClasificacion(): EstadisticasClasificacion {
        Log.d("ClasificacionUseCase", "📊 Obteniendo estadísticas de clasificación...")
        val estadisticas = repository.obtenerEstadisticasClasificacion()
        Log.d("ClasificacionUseCase", "✅ Estadísticas obtenidas: ${estadisticas.totalTransacciones} transacciones totales")
        return estadisticas
    }
    
    /**
     * Extrae patrones de una descripción para mejorar el aprendizaje
     * Optimizado para reducir duplicados y mejorar calidad
     */
    private fun extraerPatrones(descripcion: String): List<String> {
        val patrones = mutableListOf<String>()
        val descripcionLimpia = descripcion.trim()
        
        // Solo agregar patrones si la descripción tiene contenido válido
        if (descripcionLimpia.isBlank() || descripcionLimpia.length < 3) {
            return emptyList()
        }
        
        // Patrón completo (solo si tiene más de 5 caracteres)
        if (descripcionLimpia.length >= 5) {
            patrones.add(descripcionLimpia)
        }
        
        // Palabras individuales (solo si tienen más de 4 caracteres y no son comunes)
        val palabras = descripcionLimpia.split(" ")
            .filter { it.length > 4 }
            .filter { !esPalabraComun(it) }
        
        // Agregar solo las palabras más relevantes (máximo 3)
        patrones.addAll(palabras.take(3))
        
        // Combinaciones de 2 palabras (solo si ambas palabras son relevantes)
        if (palabras.size >= 2) {
            for (i in 0 until palabras.size - 1) {
                val combinacion = "${palabras[i]} ${palabras[i + 1]}"
                if (combinacion.length >= 8) { // Solo combinaciones suficientemente largas
                    patrones.add(combinacion)
                }
            }
        }
        
        // Filtrar duplicados y patrones muy similares
        return patrones.distinct().filter { patron ->
            // No incluir patrones que sean subcadenas de otros patrones más largos
            !patrones.any { otroPatron ->
                otroPatron != patron && otroPatron.contains(patron) && otroPatron.length > patron.length + 2
            }
        }
    }
    
    /**
     * Verifica si una palabra es común y debe ser filtrada
     */
    private fun esPalabraComun(palabra: String): Boolean {
        val palabrasComunes = setOf(
            "tienda", "servicio", "online", "web", "com", "cl", "santiago", "chile",
            "compra", "pago", "transferencia", "debito", "credito", "tarjeta",
            "sucursal", "local", "centro", "mall", "plaza", "avenida", "calle",
            "banco", "cajero", "atm", "pos", "terminal", "punto", "venta"
        )
        return palabrasComunes.contains(palabra.lowercase())
    }
    
    /**
     * Verifica si una sugerencia tiene confianza suficiente para ser mostrada
     */
    fun esConfianzaSuficiente(confianza: Double): Boolean {
        return confianza >= 0.6 // 60% de confianza mínima
    }
    
    /**
     * Obtiene todos los patrones aprendidos
     */
    suspend fun obtenerTodosLosPatrones(): List<ClasificacionAutomatica> {
        Log.d("ClasificacionUseCase", "📋 Obteniendo todos los patrones...")
        val patrones = repository.obtenerTodosLosPatrones()
        Log.d("ClasificacionUseCase", "✅ Patrones obtenidos: ${patrones.size}")
        return patrones
    }
    
    /**
     * Actualiza la confianza de un patrón específico
     */
    suspend fun actualizarConfianza(patron: String, categoriaId: Long, nuevaConfianza: Double) {
        Log.d("ClasificacionUseCase", "🔄 Actualizando confianza: '$patron' -> $nuevaConfianza")
        repository.actualizarConfianza(patron, categoriaId, nuevaConfianza)
        Log.d("ClasificacionUseCase", "✅ Confianza actualizada")
    }
    
    /**
     * Limpia duplicados existentes en la base de datos de clasificación automática
     */
    suspend fun limpiarDuplicados() {
        Log.d("ClasificacionUseCase", "🧹 Iniciando limpieza de duplicados...")
        repository.limpiarDuplicados()
        Log.d("ClasificacionUseCase", "✅ Limpieza de duplicados completada")
    }
} 