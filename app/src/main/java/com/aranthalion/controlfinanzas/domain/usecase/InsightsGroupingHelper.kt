package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import java.util.*
import kotlin.math.abs

object InsightsGroupingHelper {

    fun generarAgrupaciones(
        gastosDelPeriodo: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<AgrupacionTransacciones> {
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        // 1. Agrupación por descripción similar
        agrupaciones.addAll(agruparPorDescripcionSimilar(gastosDelPeriodo, categorias))
        
        // 2. Agrupación por rango de montos
        agrupaciones.addAll(agruparPorRangoMontos(gastosDelPeriodo, categorias))
        
        // 3. Agrupación por día de la semana
        agrupaciones.addAll(agruparPorDiaSemana(gastosDelPeriodo, categorias))
        
        // 4. Agrupación por hora del día
        agrupaciones.addAll(agruparPorHoraDia(gastosDelPeriodo, categorias))
        
        // 5. Agrupación por frecuencia
        agrupaciones.addAll(agruparPorFrecuencia(gastosDelPeriodo, categorias))
        
        return agrupaciones.sortedByDescending { it.total }
    }

    private fun agruparPorDescripcionSimilar(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<AgrupacionTransacciones> {
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        // Agrupar por descripción limpia
        val grupos = gastos.groupBy { it.descripcionLimpia ?: it.descripcion }
        
        grupos.forEach { (descripcion, transacciones) ->
            if (transacciones.size >= 2) {
                val categoria = categorias.find { it.id == transacciones.first().categoriaId }
                val total = transacciones.sumOf { abs(it.monto) }
                val promedio = total / transacciones.size
                
                agrupaciones.add(
                    AgrupacionTransacciones(
                        nombre = "Transacciones similares: $descripcion",
                        tipo = TipoAgrupacion.POR_DESCRIPCION_SIMILAR,
                        transacciones = transacciones.map { 
                            TransaccionAgrupada(
                                id = it.id,
                                descripcion = it.descripcion,
                                descripcionLimpia = it.descripcionLimpia,
                                monto = it.monto,
                                fecha = it.fecha,
                                categoriaId = it.categoriaId,
                                categoriaNombre = categoria?.nombre
                            )
                        },
                        total = total,
                        cantidad = transacciones.size,
                        promedio = promedio,
                        patron = descripcion,
                        categoriaId = categoria?.id,
                        categoriaNombre = categoria?.nombre
                    )
                )
            }
        }
        
        return agrupaciones
    }
    
    private fun agruparPorRangoMontos(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<AgrupacionTransacciones> {
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        val rangos = listOf(
            Triple("Gastos pequeños", 0.0, 5000.0),
            Triple("Gastos medianos", 5000.0, 20000.0),
            Triple("Gastos grandes", 20000.0, 100000.0),
            Triple("Gastos muy grandes", 100000.0, Double.MAX_VALUE)
        )
        
        rangos.forEach { (nombre, min, max) ->
            val transaccionesEnRango = gastos.filter { 
                val monto = abs(it.monto)
                monto >= min && monto < max
            }
            
            if (transaccionesEnRango.isNotEmpty()) {
                val total = transaccionesEnRango.sumOf { abs(it.monto) }
                val promedio = total / transaccionesEnRango.size
                
                agrupaciones.add(
                    AgrupacionTransacciones(
                        nombre = nombre,
                        tipo = TipoAgrupacion.POR_MONTO_RANGO,
                        transacciones = transaccionesEnRango.map { 
                            val categoria = categorias.find { cat -> cat.id == it.categoriaId }
                            TransaccionAgrupada(
                                id = it.id,
                                descripcion = it.descripcion,
                                descripcionLimpia = it.descripcionLimpia,
                                monto = it.monto,
                                fecha = it.fecha,
                                categoriaId = it.categoriaId,
                                categoriaNombre = categoria?.nombre
                            )
                        },
                        total = total,
                        cantidad = transaccionesEnRango.size,
                        promedio = promedio,
                        patron = "$${min.toInt()}-${if (max == Double.MAX_VALUE) "∞" else max.toInt()}"
                    )
                )
            }
        }
        
        return agrupaciones
    }
    
    private fun agruparPorDiaSemana(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<AgrupacionTransacciones> {
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        val gastosPorDia = gastos.groupBy { 
            val calendar = Calendar.getInstance()
            calendar.time = it.fecha
            calendar.get(Calendar.DAY_OF_WEEK)
        }
        
        gastosPorDia.forEach { (diaSemana, transacciones) ->
            val nombreDia = obtenerNombreDia(diaSemana)
            val total = transacciones.sumOf { abs(it.monto) }
            val promedio = total / transacciones.size
            
            agrupaciones.add(
                AgrupacionTransacciones(
                    nombre = "Gastos en $nombreDia",
                    tipo = TipoAgrupacion.POR_DIA_SEMANA,
                    transacciones = transacciones.map { 
                        val categoria = categorias.find { cat -> cat.id == it.categoriaId }
                        TransaccionAgrupada(
                            id = it.id,
                            descripcion = it.descripcion,
                            descripcionLimpia = it.descripcionLimpia,
                            monto = it.monto,
                            fecha = it.fecha,
                            categoriaId = it.categoriaId,
                            categoriaNombre = categoria?.nombre
                        )
                    },
                    total = total,
                    cantidad = transacciones.size,
                    promedio = promedio,
                    patron = nombreDia
                )
            )
        }
        
        return agrupaciones
    }
    
    private fun agruparPorHoraDia(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<AgrupacionTransacciones> {
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        val gastosPorHora = gastos.groupBy { 
            val calendar = Calendar.getInstance()
            calendar.time = it.fecha
            calendar.get(Calendar.HOUR_OF_DAY)
        }
        
        gastosPorHora.forEach { (hora, transacciones) ->
            val nombreHora = when {
                hora < 6 -> "Madrugada (0-6h)"
                hora < 12 -> "Mañana (6-12h)"
                hora < 18 -> "Tarde (12-18h)"
                else -> "Noche (18-24h)"
            }
            
            val total = transacciones.sumOf { abs(it.monto) }
            val promedio = total / transacciones.size
            
            agrupaciones.add(
                AgrupacionTransacciones(
                    nombre = "Gastos en $nombreHora",
                    tipo = TipoAgrupacion.POR_HORA_DIA,
                    transacciones = transacciones.map { 
                        val categoria = categorias.find { cat -> cat.id == it.categoriaId }
                        TransaccionAgrupada(
                            id = it.id,
                            descripcion = it.descripcion,
                            descripcionLimpia = it.descripcionLimpia,
                            monto = it.monto,
                            fecha = it.fecha,
                            categoriaId = it.categoriaId,
                            categoriaNombre = categoria?.nombre
                        )
                    },
                    total = total,
                    cantidad = transacciones.size,
                    promedio = promedio,
                    patron = nombreHora
                )
            )
        }
        
        return agrupaciones
    }
    
    private fun agruparPorFrecuencia(
        gastos: List<MovimientoEntity>,
        categorias: List<Categoria>
    ): List<AgrupacionTransacciones> {
        val agrupaciones = mutableListOf<AgrupacionTransacciones>()
        
        val gastosPorCategoria = gastos.groupBy { it.categoriaId }
        
        gastosPorCategoria.forEach { (categoriaId, transacciones) ->
            val categoria = categorias.find { it.id == categoriaId }
            val total = transacciones.sumOf { abs(it.monto) }
            val promedio = total / transacciones.size
            
            val frecuencia = when {
                transacciones.size >= 10 -> "Muy frecuente"
                transacciones.size >= 5 -> "Frecuente"
                transacciones.size >= 2 -> "Ocasional"
                else -> "Único"
            }
            
            agrupaciones.add(
                AgrupacionTransacciones(
                    nombre = "${categoria?.nombre ?: "Sin categoría"} - $frecuencia",
                    tipo = TipoAgrupacion.POR_FRECUENCIA,
                    transacciones = transacciones.map { 
                        TransaccionAgrupada(
                            id = it.id,
                            descripcion = it.descripcion,
                            descripcionLimpia = it.descripcionLimpia,
                            monto = it.monto,
                            fecha = it.fecha,
                            categoriaId = it.categoriaId,
                            categoriaNombre = categoria?.nombre
                        )
                    },
                    total = total,
                    cantidad = transacciones.size,
                    promedio = promedio,
                    patron = frecuencia,
                    categoriaId = categoria?.id,
                    categoriaNombre = categoria?.nombre
                )
            )
        }
        
        return agrupaciones
    }

    private fun obtenerNombreDia(diaSemana: Int): String {
        return when (diaSemana) {
            Calendar.SUNDAY -> "Domingo"
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            else -> "Desconocido"
        }
    }
}
