package com.aranthalion.controlfinanzas

import com.aranthalion.controlfinanzas.domain.usecase.*
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class AnalisisFinancieroUseCaseTest {
    private lateinit var movimientoRepository: MovimientoRepository
    private lateinit var presupuestoRepository: PresupuestoCategoriaRepository
    private lateinit var useCase: AnalisisFinancieroUseCase

    @Before
    fun setup() {
        movimientoRepository = mockk(relaxed = true)
        presupuestoRepository = mockk(relaxed = true)
        useCase = AnalisisFinancieroUseCase(movimientoRepository, presupuestoRepository)
    }

    @Test
    fun `calcula tendencias mensuales correctamente`() = runBlocking {
        val movimientos = listOf(
            // Enero
            mockMovimiento(1, 1, "2024-01", TipoMovimiento.INGRESO, 1000.0),
            mockMovimiento(2, 2, "2024-01", TipoMovimiento.GASTO, -400.0),
            // Febrero
            mockMovimiento(3, 1, "2024-02", TipoMovimiento.INGRESO, 1200.0),
            mockMovimiento(4, 2, "2024-02", TipoMovimiento.GASTO, -600.0),
            // Marzo
            mockMovimiento(5, 1, "2024-03", TipoMovimiento.INGRESO, 1100.0),
            mockMovimiento(6, 2, "2024-03", TipoMovimiento.GASTO, -500.0)
        )
        coEvery { movimientoRepository.obtenerMovimientos() } returns movimientos
        coEvery { movimientoRepository.obtenerMovimientosPorPeriodo(any(), any()) } returns movimientos

        val tendencias = useCase.obtenerTendenciasMensuales(3)
        assertEquals(3, tendencias.size)
        assertTrue(tendencias.all { it.ingresos > 0 })
    }

    @Test
    fun `calcula volatilidad correctamente`() = runBlocking {
        val movimientos = listOf(
            mockMovimiento(1, 1, "2024-01", TipoMovimiento.GASTO, -100.0),
            mockMovimiento(2, 1, "2024-02", TipoMovimiento.GASTO, -200.0),
            mockMovimiento(3, 1, "2024-03", TipoMovimiento.GASTO, -300.0)
        )
        coEvery { movimientoRepository.obtenerMovimientos() } returns movimientos
        coEvery { movimientoRepository.obtenerCategorias() } returns listOf(
            mockCategoria(1, "Test")
        )
        val volatilidad = useCase.obtenerAnalisisVolatilidad("2024-03")
        assertEquals(1, volatilidad.size)
        assertTrue(volatilidad[0].desviacionEstandar > 0)
    }

    @Test
    fun `calcula score financiero correctamente`() = runBlocking {
        coEvery { movimientoRepository.obtenerMovimientos() } returns listOf(
            mockMovimiento(1, 1, "2024-03", TipoMovimiento.INGRESO, 1000.0),
            mockMovimiento(2, 2, "2024-03", TipoMovimiento.GASTO, -400.0)
        )
        coEvery { presupuestoRepository.obtenerPresupuestosPorPeriodo(any()) } returns listOf()
        val score = useCase.obtenerMetricasRendimiento("2024-03")
        assertTrue(score.scoreFinanciero in 0..100)
    }

    @Test
    fun `detecta gastos inusuales`() = runBlocking {
        val movimientos = listOf(
            mockMovimiento(1, 1, "2024-03", TipoMovimiento.GASTO, -100.0),
            mockMovimiento(2, 1, "2024-02", TipoMovimiento.GASTO, -100.0),
            mockMovimiento(3, 1, "2024-01", TipoMovimiento.GASTO, -1000.0) // outlier
        )
        coEvery { movimientoRepository.obtenerMovimientos() } returns movimientos
        coEvery { movimientoRepository.obtenerCategorias() } returns listOf(
            mockCategoria(1, "Test")
        )
        val outliers = useCase.obtenerGastosInusuales("2024-01")
        assertTrue(outliers.isNotEmpty())
        assertTrue(outliers[0].factorInusual > 1.0)
    }

    // Helpers
    private fun mockMovimiento(id: Long, categoriaId: Long, periodo: String, tipo: TipoMovimiento, monto: Double) =
        com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity(
            id = id,
            categoriaId = categoriaId,
            tipo = tipo.name,
            monto = monto,
            descripcion = "Test",
            fecha = Date(),
            periodoFacturacion = periodo,
            idUnico = "test_$id"
        )
    private fun mockCategoria(id: Long, nombre: String) =
        com.aranthalion.controlfinanzas.data.local.entity.Categoria(
            id = id,
            nombre = nombre,
            descripcion = "",
            tipo = "GASTO"
        )
} 