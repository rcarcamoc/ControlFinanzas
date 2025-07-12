package com.aranthalion.controlfinanzas.data.repository

import android.content.Context
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.aranthalion.controlfinanzas.data.repository.AuditoriaService

class MovimientoRepository @Inject constructor(
    private val movimientoDao: MovimientoDao,
    private val categoriaDao: CategoriaDao,
    private val context: Context,
    private val auditoriaService: AuditoriaService
) {
    suspend fun obtenerMovimientos(): List<MovimientoEntity> {
        val movimientos = movimientoDao.obtenerMovimientos()
        println("🔍 DEBUG: MovimientoRepository.obtenerMovimientos() - Total: ${movimientos.size}")
        movimientos.take(3).forEach { movimiento ->
            println("  - ${movimiento.descripcion}: ${movimiento.fecha} (tipo: ${movimiento.tipo})")
        }
        return movimientos
    }

    suspend fun obtenerMovimientosPorPeriodo(fechaInicio: Date, fechaFin: Date): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientosPorPeriodo(fechaInicio, fechaFin)
    }

    // suspend fun obtenerMovimientosPorPeriodo(periodo: String): List<MovimientoEntity> {
    //     println("🔍 DEBUG: MovimientoRepository.obtenerMovimientosPorPeriodo($periodo)")
    //     // Implementación anterior comentada
    //     // val (fechaInicio, fechaFin) = obtenerFechasDePeriodo(periodo)
    //     // return movimientoDao.obtenerMovimientosPorPeriodo(fechaInicio, fechaFin)
    //     // return emptyList()
    // }

    suspend fun obtenerCategorias(): List<Categoria> {
        return categoriaDao.obtenerCategorias()
    }

    suspend fun agregarMovimiento(movimiento: MovimientoEntity, metodo: String = "INSERT", dao: String = "MovimientoDao") {
        val descripcionLimpia = limpiarDescripcion(movimiento.descripcion)
        val timestamp = System.currentTimeMillis()
        val movimientoConAuditoria = movimiento.copy(
            descripcionLimpia = descripcionLimpia,
            fechaCreacion = timestamp,
            fechaActualizacion = timestamp,
            metodoActualizacion = metodo,
            daoResponsable = dao
        )
        movimientoDao.agregarMovimiento(movimientoConAuditoria)
        
        // Registrar auditoría
        auditoriaService.registrarOperacion(
            tabla = "movimientos",
            operacion = "INSERT",
            entidadId = movimientoConAuditoria.id,
            detalles = "Movimiento agregado: ${movimientoConAuditoria.descripcion} - Monto: ${movimientoConAuditoria.monto} - Tipo: ${movimientoConAuditoria.tipo}",
            daoResponsable = dao
        )
        
        println("📝 AUDITORÍA: Movimiento agregado - ID: ${movimiento.id}, Método: $metodo, DAO: $dao, Timestamp: $timestamp")
    }

    suspend fun actualizarMovimiento(movimiento: MovimientoEntity, metodo: String = "UPDATE", dao: String = "MovimientoDao") {
        val timestamp = System.currentTimeMillis()
        val movimientoConAuditoria = movimiento.copy(
            fechaActualizacion = timestamp,
            metodoActualizacion = metodo,
            daoResponsable = dao
        )
        movimientoDao.actualizarMovimiento(movimientoConAuditoria)
        
        // Registrar auditoría
        auditoriaService.registrarOperacion(
            tabla = "movimientos",
            operacion = "UPDATE",
            entidadId = movimiento.id,
            detalles = "Movimiento actualizado: ${movimiento.descripcion} - Monto: ${movimiento.monto} - Categoría: ${movimiento.categoriaId}",
            daoResponsable = dao
        )
        
        println("📝 AUDITORÍA: Movimiento actualizado - ID: ${movimiento.id}, Método: $metodo, DAO: $dao, Timestamp: $timestamp")
    }

    suspend fun eliminarMovimiento(movimiento: MovimientoEntity) {
        val timestamp = System.currentTimeMillis()
        println("📝 AUDITORÍA: Eliminando movimiento individual - ID: ${movimiento.id}, Descripción: ${movimiento.descripcion}")
        
        // Registrar auditoría antes de eliminar
        auditoriaService.registrarOperacion(
            tabla = "movimientos",
            operacion = "DELETE_INDIVIDUAL",
            entidadId = movimiento.id,
            detalles = "Movimiento eliminado: ${movimiento.descripcion} - Monto: ${movimiento.monto} - Tipo: ${movimiento.tipo}",
            daoResponsable = "MovimientoDao"
        )
        
        // Ahora eliminar el movimiento
        movimientoDao.eliminarMovimiento(movimiento)
        println("✅ AUDITORÍA: Movimiento eliminado exitosamente")
    }

    suspend fun obtenerIdUnicos(): Set<String> {
        return movimientoDao.obtenerIdUnicos().toSet()
    }

    suspend fun obtenerIdUnicosPorPeriodo(periodo: String?): Set<String> {
        return movimientoDao.obtenerIdUnicosPorPeriodo(periodo).toSet()
    }

    suspend fun obtenerCategoriasPorIdUnico(periodo: String?): Map<String, Long?> {
        return movimientoDao.obtenerCategoriasPorIdUnico(periodo).associate { it.idUnico to it.categoriaId }
    }

    suspend fun eliminarMovimientosPorPeriodo(periodo: String?) {
        val timestamp = System.currentTimeMillis()
        println("📝 AUDITORÍA: Eliminando movimientos por período - Período: $periodo, Timestamp: $timestamp")
        
        // Obtener los movimientos que se van a eliminar para registrar auditoría
        val movimientosAEliminar = movimientoDao.obtenerMovimientos().filter { 
            it.periodoFacturacion == periodo 
        }
        
        println("📝 AUDITORÍA: Movimientos a eliminar: ${movimientosAEliminar.size}")
        
        // Registrar auditoría para cada movimiento antes de eliminarlo
        movimientosAEliminar.forEach { movimiento ->
            auditoriaService.registrarOperacion(
                tabla = "movimientos",
                operacion = "DELETE_PERIODO",
                entidadId = movimiento.id,
                detalles = "Movimiento eliminado por período $periodo: ${movimiento.descripcion} - Monto: ${movimiento.monto}",
                daoResponsable = "MovimientoDao"
            )
            println("📝 AUDITORÍA: Registrada eliminación para movimiento ID: ${movimiento.id}, Descripción: ${movimiento.descripcion}")
        }
        
        // Ahora eliminar los movimientos
        movimientoDao.eliminarMovimientosPorPeriodo(periodo)
        println("✅ AUDITORÍA: Eliminación completada para período: $periodo")
    }
    
    // Métodos de auditoría
    suspend fun obtenerMovimientosRecientes(): List<MovimientoEntity> {
        val movimientos = movimientoDao.obtenerMovimientosRecientes()
        println("🔍 AUDITORIA_REPO: Movimientos recientes obtenidos: ${movimientos.size}")
        movimientos.take(5).forEach { movimiento ->
            println("  - ID: ${movimiento.id}, Descripción: ${movimiento.descripcion}, Método: ${movimiento.metodoActualizacion}, DAO: ${movimiento.daoResponsable}")
        }
        return movimientos
    }
    
    suspend fun obtenerMovimientosPorMetodo(metodo: String): List<MovimientoEntity> {
        return movimientoDao.obtenerMovimientosPorMetodo(metodo)
    }
    
    suspend fun actualizarAuditoria(id: Long, metodo: String, dao: String) {
        val timestamp = System.currentTimeMillis()
        movimientoDao.actualizarAuditoria(id, timestamp, metodo, dao)
        println("📝 AUDITORÍA: Actualizando auditoría - ID: $id, Método: $metodo, DAO: $dao, Timestamp: $timestamp")
    }

    /**
     * Diagnostica el estado de los datos históricos
     * Muestra información útil sobre los movimientos históricos existentes
     */
    suspend fun diagnosticarDatosHistoricos() {
        val movimientosExistentes = movimientoDao.obtenerMovimientos()
        val movimientosHistoricos = movimientosExistentes.filter { 
            it.idUnico.startsWith("historico_") 
        }
        
        println("🔍 DIAGNÓSTICO DE DATOS HISTÓRICOS:")
        println("Total de movimientos: ${movimientosExistentes.size}")
        println("Movimientos históricos: ${movimientosHistoricos.size}")
        
        if (movimientosHistoricos.isNotEmpty()) {
            val conDescripcionAntigua = movimientosHistoricos.filter { 
                it.descripcion == "Carga histórica" 
            }
            val conDescripcionNueva = movimientosHistoricos.filter { 
                it.descripcion != "Carga histórica" 
            }
            
            println("  - Con descripción 'Carga histórica': ${conDescripcionAntigua.size}")
            println("  - Con descripción nueva: ${conDescripcionNueva.size}")
            
            if (conDescripcionAntigua.isNotEmpty()) {
                println("  📝 Ejemplos de descripciones antiguas:")
                conDescripcionAntigua.take(3).forEach { movimiento ->
                    println("    * ${movimiento.idUnico} -> '${movimiento.descripcion}'")
                }
            }
            
            if (conDescripcionNueva.isNotEmpty()) {
                println("  ✅ Ejemplos de descripciones nuevas:")
                conDescripcionNueva.take(3).forEach { movimiento ->
                    println("    * ${movimiento.idUnico} -> '${movimiento.descripcion}'")
                }
            }
        } else {
            println("  ℹ️ No hay movimientos históricos en la base de datos")
        }
    }

    /**
     * Limpia todos los movimientos históricos y los recarga con las nuevas descripciones
     * Útil cuando se desinstala y reinstala la app pero la base de datos persiste
     * 
     * ⚠️ DESHABILITADO: Esta función puede borrar datos del usuario
     * Solo usar manualmente si el usuario lo solicita explícitamente
     */
    suspend fun limpiarYRecargarDatosHistoricos() {
        // DESHABILITADO: No borrar datos automáticamente
        println("⚠️ Función limpiarYRecargarDatosHistoricos deshabilitada para preservar datos del usuario")
        println("ℹ️ Si necesitas limpiar datos históricos, hazlo manualmente desde la configuración")
        
        /*
        try {
            println("🧹 Limpiando movimientos históricos existentes...")
            
            // Obtener todos los movimientos históricos
            val movimientosExistentes = movimientoDao.obtenerMovimientos()
            val movimientosHistoricos = movimientosExistentes.filter { 
                it.idUnico.startsWith("historico_") 
            }
            
            if (movimientosHistoricos.isNotEmpty()) {
                // Eliminar todos los movimientos históricos
                movimientosHistoricos.forEach { movimiento ->
                    movimientoDao.eliminarMovimiento(movimiento)
                }
                println("🗑️ Eliminados ${movimientosHistoricos.size} movimientos históricos")
            }
            
            // Recargar datos históricos con las nuevas descripciones
            println("📊 Recargando datos históricos con nuevas descripciones...")
            cargarDatosHistoricos()
            
            println("✅ Datos históricos limpiados y recargados correctamente")
            
        } catch (e: Exception) {
            println("❌ Error al limpiar y recargar datos históricos: ${e.message}")
            e.printStackTrace()
        }
        */
    }

    /**
     * Actualiza las descripciones de movimientos históricos existentes
     * de "Carga histórica" a los nombres de categorías correspondientes
     */
    suspend fun actualizarDescripcionesHistoricas() {
        val movimientosExistentes = movimientoDao.obtenerMovimientos()
        val movimientosHistoricos = movimientosExistentes.filter { 
            it.descripcion == "Carga histórica" && it.idUnico.startsWith("historico_") 
        }
        
        if (movimientosHistoricos.isEmpty()) {
            println("No hay movimientos históricos con descripción 'Carga histórica' para actualizar")
            return
        }
        
        val categorias = categoriaDao.obtenerCategorias()
        var actualizados = 0
        
        movimientosHistoricos.forEach { movimiento ->
            // Extraer el nombre de la categoría del ID único
            val idUnico = movimiento.idUnico
            val partes = idUnico.split("_")
            if (partes.size >= 4) {
                val categoriaNombre = partes[2] // El nombre de la categoría está en la posición 2
                
                // Buscar la categoría por nombre
                val categoria = categorias.find { 
                    it.nombre.equals(categoriaNombre, ignoreCase = true) 
                }
                
                // Actualizar el movimiento
                val movimientoActualizado = movimiento.copy(
                    descripcion = categoriaNombre,
                    categoriaId = categoria?.id
                )
                
                movimientoDao.actualizarMovimiento(movimientoActualizado)
                actualizados++
                println("Actualizado: ${movimiento.idUnico} -> descripción: $categoriaNombre")
            }
        }
        
        println("Se actualizaron $actualizados movimientos históricos")
    }

    /**
     * Limpia todos los datos históricos de la base de datos
     * Útil para instalaciones completamente limpias
     */
    suspend fun limpiarTodosLosDatos() {
        try {
            println("🧹 Limpiando todos los datos históricos...")
            
            // Obtener todos los movimientos
            val movimientosExistentes = movimientoDao.obtenerMovimientos()
            
            if (movimientosExistentes.isNotEmpty()) {
                // Eliminar todos los movimientos
                movimientosExistentes.forEach { movimiento ->
                    movimientoDao.eliminarMovimiento(movimiento)
                }
                println("🗑️ Eliminados ${movimientosExistentes.size} movimientos de la base de datos")
            } else {
                println("ℹ️ No hay movimientos para eliminar")
            }
            
            println("✅ Base de datos limpiada completamente")
            
        } catch (e: Exception) {
            println("❌ Error al limpiar datos: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Carga datos históricos hardcodeados (solo una vez)
     */
    suspend fun cargarDatosHistoricos() {
        // Verificar si ya se han cargado datos históricos
        val movimientosExistentes = movimientoDao.obtenerMovimientos()
        if (movimientosExistentes.any { it.idUnico.startsWith("historico_") }) {
            println("Los datos históricos ya han sido cargados anteriormente")
            return
        }

        val categorias = categoriaDao.obtenerCategorias()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val movimientos = mutableListOf<MovimientoEntity>()

        // Datos hardcodeados del archivo historial_gastos.csv
        val datosHistoricos = listOf(
            // 2025-03
            Triple("arriendo", "2025-03-01", 590000.0),
            Triple("Tarjeta titular", "2025-03-01", 535048.0),
            Triple("vacaciones", "2025-03-01", 392923.0),
            Triple("supermercado", "2025-03-01", 378614.0),
            Triple("gastos comunes", "2025-03-01", 97038.0),
            Triple("choquito", "2025-03-01", 72696.0),
            Triple("Bencina", "2025-03-01", 70032.0),
            Triple("veguita", "2025-03-01", 54160.0),
            Triple("Gatos", "2025-03-01", 50980.0),
            Triple("uber", "2025-03-01", 50752.0),
            Triple("Seguro", "2025-03-01", 50070.0),
            Triple("salir a comer", "2025-03-01", 47960.0),
            Triple("Almacen", "2025-03-01", 39414.0),
            Triple("gas", "2025-03-01", 39344.0),
            Triple("PEAJES", "2025-03-01", 39050.0),
            Triple("delivery", "2025-03-01", 29200.0),
            Triple("luz", "2025-03-01", 17699.0),
            Triple("internet", "2025-03-01", 15990.0),
            Triple("streaming", "2025-03-01", 15170.0),
            Triple("bubi", "2025-03-01", 14990.0),
            Triple("agua", "2025-03-01", 13120.0),
            Triple("farmacia", "2025-03-01", 2360.0),
            
            // 2025-01
            Triple("Tarjeta titular", "2025-01-01", 1818920.0),
            Triple("supermercado", "2025-01-01", 656098.0),
            Triple("arriendo", "2025-01-01", 590000.0),
            Triple("Bencina", "2025-01-01", 186754.0),
            Triple("gastos comunes", "2025-01-01", 93179.0),
            Triple("salir a comer", "2025-01-01", 84021.0),
            Triple("Almacen", "2025-01-01", 76480.0),
            Triple("casa", "2025-01-01", 68733.0),
            Triple("PEAJES", "2025-01-01", 66109.0),
            Triple("bubi", "2025-01-01", 51390.0),
            Triple("Seguro", "2025-01-01", 49586.0),
            Triple("veguita", "2025-01-01", 47300.0),
            Triple("Gatos", "2025-01-01", 40990.0),
            Triple("gas", "2025-01-01", 38372.0),
            Triple("luz", "2025-01-01", 22102.0),
            Triple("agua", "2025-01-01", 19590.0),
            Triple("internet", "2025-01-01", 15990.0),
            Triple("streaming", "2025-01-01", 15170.0),
            Triple("farmacia", "2025-01-01", 10882.0),
            Triple("choquito", "2025-01-01", 10000.0),
            Triple("Medico", "2025-01-01", 1450.0),
            
            // 2024-12
            Triple("arriendo", "2024-12-01", 590000.0),
            Triple("supermercado", "2024-12-01", 550797.0),
            Triple("Tarjeta titular", "2024-12-01", 435402.0),
            Triple("gastos comunes", "2024-12-01", 106852.0),
            Triple("casa", "2024-12-01", 90509.0),
            Triple("Almacen", "2024-12-01", 81720.0),
            Triple("gas", "2024-12-01", 57250.0),
            Triple("Bencina", "2024-12-01", 55589.0),
            Triple("Seguro", "2024-12-01", 49373.0),
            Triple("PEAJES", "2024-12-01", 41201.0),
            Triple("salir a comer", "2024-12-01", 36988.0),
            Triple("veguita", "2024-12-01", 32200.0),
            Triple("Regalos", "2024-12-01", 28500.0),
            Triple("luz", "2024-12-01", 26918.0),
            Triple("farmacia", "2024-12-01", 25400.0),
            Triple("Medico", "2024-12-01", 23300.0),
            Triple("agua", "2024-12-01", 22370.0),
            Triple("internet", "2024-12-01", 15990.0),
            Triple("streaming", "2024-12-01", 15170.0),
            Triple("delivery", "2024-12-01", 13850.0),
            Triple("bubi", "2024-12-01", 11970.0),
            Triple("choquito", "2024-12-01", 10000.0),
            
            // 2024-11
            Triple("agua", "2024-11-01", 47430.0),
            Triple("arriendo", "2024-11-01", 590000.0),
            Triple("Bencina", "2024-11-01", 207344.0),
            Triple("bubi", "2024-11-01", 83600.0),
            Triple("casa", "2024-11-01", 159198.0),
            Triple("delivery", "2024-11-01", 112382.0),
            Triple("Almacen", "2024-11-01", 94270.0),
            Triple("farmacia", "2024-11-01", 55001.0),
            Triple("gas", "2024-11-01", 69205.0),
            Triple("gastos comunes", "2024-11-01", 109000.0),
            Triple("Gatos", "2024-11-01", 53143.0),
            Triple("internet", "2024-11-01", 15990.0),
            Triple("luz", "2024-11-01", 26737.0),
            Triple("PEAJES", "2024-11-01", 103601.0),
            Triple("Regalos", "2024-11-01", 39700.0),
            Triple("salir a comer", "2024-11-01", 216688.0),
            Triple("Seguro", "2024-11-01", 98107.0),
            Triple("streaming", "2024-11-01", 15170.0),
            Triple("supermercado", "2024-11-01", 558273.0),
            Triple("Tarjeta titular", "2024-11-01", 380804.0),
            Triple("veguita", "2024-11-01", 54470.0),
            
            // 2024-10
            Triple("arriendo", "2024-10-01", 590000.0),
            Triple("Bencina", "2024-10-01", 185870.0),
            Triple("bubi", "2024-10-01", 129600.0),
            Triple("casa", "2024-10-01", 156864.0),
            Triple("Credito", "2024-10-01", 94492.0),
            Triple("delivery", "2024-10-01", 139701.0),
            Triple("farmacia", "2024-10-01", 18490.0),
            Triple("gas", "2024-10-01", 39197.0),
            Triple("gastos comunes", "2024-10-01", 90000.0),
            Triple("internet", "2024-10-01", 9698.0),
            Triple("luz", "2024-10-01", 27915.0),
            Triple("PEAJES", "2024-10-01", 30246.0),
            Triple("salir a comer", "2024-10-01", 99902.0),
            Triple("Seguro", "2024-10-01", 48846.0),
            Triple("supermercado", "2024-10-01", 636583.0),
            Triple("veguita", "2024-10-01", 50100.0),
            
            // 2024-09
            Triple("veguita", "2024-09-01", 2350.0),
            Triple("choquito", "2024-09-01", 10000.0),
            Triple("internet", "2024-09-01", 14461.0),
            Triple("Medico", "2024-09-01", 20140.0),
            Triple("Antojos", "2024-09-01", 26640.0),
            Triple("luz", "2024-09-01", 31704.0),
            Triple("farmacia", "2024-09-01", 33701.0),
            Triple("PEAJES", "2024-09-01", 38277.0),
            Triple("Imprevistos", "2024-09-01", 39560.0),
            Triple("Gatos", "2024-09-01", 50346.0),
            Triple("gastos comunes", "2024-09-01", 62566.0),
            Triple("salir a comer", "2024-09-01", 73185.0),
            Triple("gas", "2024-09-01", 90503.0),
            Triple("Credito", "2024-09-01", 94492.0),
            Triple("Bencina", "2024-09-01", 150000.0),
            Triple("casa", "2024-09-01", 168505.0),
            Triple("bubi", "2024-09-01", 176520.0),
            Triple("delivery", "2024-09-01", 255242.0),
            Triple("arriendo", "2024-09-01", 590000.0),
            Triple("supermercado", "2024-09-01", 615448.0),
            
            // 2024-08
            Triple("Medico", "2024-08-01", 6700.0),
            Triple("luz", "2024-08-01", 18276.0),
            Triple("internet", "2024-08-01", 24876.0),
            Triple("Regalos", "2024-08-01", 32490.0),
            Triple("Imprevistos", "2024-08-01", 53610.0),
            Triple("farmacia", "2024-08-01", 57954.0),
            Triple("gastos comunes", "2024-08-01", 62566.0),
            Triple("delivery", "2024-08-01", 68799.0),
            Triple("veguita", "2024-08-01", 70833.0),
            Triple("bubi", "2024-08-01", 82060.0),
            Triple("PEAJES", "2024-08-01", 86452.0),
            Triple("gas", "2024-08-01", 145993.0),
            Triple("bENCINA", "2024-08-01", 210000.0),
            Triple("salir a comer", "2024-08-01", 239904.0),
            Triple("arriendo", "2024-08-01", 590000.0),
            Triple("supermercado", "2024-08-01", 728966.0)
        )

        // Crear movimientos desde los datos hardcodeados
        datosHistoricos.forEach { (categoriaNombre, fechaStr, monto) ->
            // Buscar la categoría por nombre
            val categoria = categorias.find { 
                it.nombre.equals(categoriaNombre, ignoreCase = true) 
            }
            
            // Crear el movimiento
            val movimiento = MovimientoEntity(
                tipo = "GASTO", // Todos los datos históricos son gastos
                monto = -monto, // Negativo porque son gastos
                descripcion = categoriaNombre, // Usar el nombre de la categoría como descripción
                fecha = dateFormat.parse(fechaStr) ?: Date(),
                periodoFacturacion = fechaStr.substring(0, 7), // Solo YYYY-MM
                categoriaId = categoria?.id,
                idUnico = "historico_${fechaStr}_${categoriaNombre}_${monto}"
            )
            
            movimientos.add(movimiento)
        }
        
        // Insertar todos los movimientos
        if (movimientos.isNotEmpty()) {
            movimientos.forEach { movimiento ->
                movimientoDao.agregarMovimiento(movimiento)
            }
            println("Se cargaron ${movimientos.size} movimientos históricos hardcodeados")
        }
    }

    /**
     * Limpia y normaliza la descripción de una transacción para facilitar el análisis y sugerencias.
     */
    private fun limpiarDescripcion(descripcion: String): String {
        // Ejemplo simple: quitar números, caracteres especiales y espacios extra
        return descripcion
            .replace(Regex("[0-9]+"), "")
            .replace(Regex("[^A-Za-zÁÉÍÓÚáéíóúÑñüÜ\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }
} 