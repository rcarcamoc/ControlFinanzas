package com.aranthalion.controlfinanzas

import com.aranthalion.controlfinanzas.domain.usecase.*
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.domain.movimiento.TipoMovimiento
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class AnalisisFinancieroUseCaseTest {
    private lateinit var movimientoRepository: MovimientoRepository
    private lateinit var presupuestoRepository: PresupuestoCategoriaRepository
    private lateinit var configuracionPreferences: ConfiguracionPreferences
    private lateinit var useCase: AnalisisFinancieroUseCase

    @Before
    fun setup() {
        movimientoRepository = mockk(relaxed = true)
        presupuestoRepository = mockk(relaxed = true)
        configuracionPreferences = mockk(relaxed = true)
        every { configuracionPreferences.obtenerScopeGlobal() } returns "HOUSEHOLD"
        useCase = AnalisisFinancieroUseCase(movimientoRepository, presupuestoRepository, configuracionPreferences)
    }

    private fun getRelativePeriod(monthsAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthsAgo)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format("%04d-%02d", year, month)
    }

    @Test
    fun `calcula tendencias mensuales correctamente`() = runBlocking {
        val p0 = getRelativePeriod(0)
        val p1 = getRelativePeriod(1)
        val p3 = getRelativePeriod(3) // offset is cumulative in production loop
        
        val movimientos = listOf(
            mockMovimiento(1, 1, p0, TipoMovimiento.INGRESO, 1000.0),
            mockMovimiento(2, 2, p0, TipoMovimiento.GASTO, -400.0),
            mockMovimiento(3, 1, p1, TipoMovimiento.INGRESO, 1200.0),
            mockMovimiento(4, 2, p1, TipoMovimiento.GASTO, -600.0),
            mockMovimiento(5, 1, p3, TipoMovimiento.INGRESO, 1100.0),
            mockMovimiento(6, 2, p3, TipoMovimiento.GASTO, -500.0)
        )
        coEvery { movimientoRepository.obtenerMovimientos() } returns movimientos
        coEvery { movimientoRepository.obtenerMovimientosPorPeriodo(any(), any()) } returns movimientos

        val tendencias = useCase.obtenerTendenciasMensuales(3)
        assertEquals(3, tendencias.size)
        assertTrue(tendencias.all { it.ingresos > 0 })
    }

    @Test
    fun `calcula volatilidad correctamente`() = runBlocking {
        val p0 = getRelativePeriod(0)
        val p1 = getRelativePeriod(1)
        val p2 = getRelativePeriod(2)
        val p3 = getRelativePeriod(3)
        val p4 = getRelativePeriod(4)
        val p5 = getRelativePeriod(5)
        
        val movimientos = listOf(
            mockMovimiento(1, 1, p0, TipoMovimiento.GASTO, -100.0),
            mockMovimiento(2, 1, p1, TipoMovimiento.GASTO, -200.0),
            mockMovimiento(3, 1, p2, TipoMovimiento.GASTO, -300.0),
            mockMovimiento(4, 1, p3, TipoMovimiento.GASTO, -400.0),
            mockMovimiento(5, 1, p4, TipoMovimiento.GASTO, -500.0),
            mockMovimiento(6, 1, p5, TipoMovimiento.GASTO, -600.0)
        )
        coEvery { movimientoRepository.obtenerMovimientos() } returns movimientos
        coEvery { movimientoRepository.obtenerCategorias() } returns listOf(
            mockCategoria(1, "Test")
        )
        val volatilidad = useCase.obtenerAnalisisVolatilidad(p0)
        assertEquals(1, volatilidad.size)
        assertTrue(volatilidad[0].desviacionEstandar > 0)
    }

    @Test
    fun `calcula score financiero correctamente`() = runBlocking {
        val p0 = getRelativePeriod(0)
        coEvery { movimientoRepository.obtenerMovimientos() } returns listOf(
            mockMovimiento(1, 1, p0, TipoMovimiento.INGRESO, 1000.0),
            mockMovimiento(2, 2, p0, TipoMovimiento.GASTO, -400.0)
        )
        coEvery { presupuestoRepository.obtenerPresupuestosPorPeriodo(any()) } returns listOf()
        val score = useCase.obtenerMetricasRendimiento(p0)
        assertTrue(score.scoreFinanciero in 0..100)
    }

    @Test
    fun `detecta gastos inusuales`() = runBlocking {
        val p0 = getRelativePeriod(0)
        val p1 = getRelativePeriod(1)
        val p2 = getRelativePeriod(2)
        val p3 = getRelativePeriod(3)
        val p4 = getRelativePeriod(4)
        val p5 = getRelativePeriod(5)
        
        val movimientos = listOf(
            mockMovimiento(1, 1, p0, TipoMovimiento.GASTO, -10000.0), // outlier
            mockMovimiento(2, 1, p1, TipoMovimiento.GASTO, -100.0),
            mockMovimiento(3, 1, p2, TipoMovimiento.GASTO, -100.0),
            mockMovimiento(4, 1, p3, TipoMovimiento.GASTO, -100.0),
            mockMovimiento(5, 1, p4, TipoMovimiento.GASTO, -100.0),
            mockMovimiento(6, 1, p5, TipoMovimiento.GASTO, -100.0)
        )
        coEvery { movimientoRepository.obtenerMovimientos() } returns movimientos
        coEvery { movimientoRepository.obtenerCategorias() } returns listOf(
            mockCategoria(1, "Test")
        )
        val outliers = useCase.obtenerGastosInusuales(p0)
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