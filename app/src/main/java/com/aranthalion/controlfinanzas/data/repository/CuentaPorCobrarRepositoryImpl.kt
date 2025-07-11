package com.aranthalion.controlfinanzas.data.repository

import com.aranthalion.controlfinanzas.data.local.dao.CuentaPorCobrarDao
import com.aranthalion.controlfinanzas.data.local.dao.UsuarioDao
import com.aranthalion.controlfinanzas.data.local.entity.CuentaPorCobrarEntity
import com.aranthalion.controlfinanzas.domain.cuenta.CuentaPorCobrar
import com.aranthalion.controlfinanzas.domain.cuenta.CuentaPorCobrarRepository
import com.aranthalion.controlfinanzas.domain.cuenta.EstadoCuenta
import javax.inject.Inject

class CuentaPorCobrarRepositoryImpl @Inject constructor(
    private val cuentaPorCobrarDao: CuentaPorCobrarDao,
    private val usuarioDao: UsuarioDao
) : CuentaPorCobrarRepository {

    override suspend fun obtenerTodasLasCuentas(): List<CuentaPorCobrar> {
        val cuentas = cuentaPorCobrarDao.obtenerTodasLasCuentas()
        return cuentas.map { it.toDomain() }
    }

    override suspend fun obtenerCuentasPendientes(): List<CuentaPorCobrar> {
        val cuentas = cuentaPorCobrarDao.obtenerCuentasPendientes()
        return cuentas.map { it.toDomain() }
    }

    override suspend fun obtenerCuentasPorUsuario(usuarioId: Long): List<CuentaPorCobrar> {
        val cuentas = cuentaPorCobrarDao.obtenerCuentasPorUsuario(usuarioId)
        return cuentas.map { it.toDomain() }
    }

    override suspend fun obtenerCuentasPorPeriodo(periodo: String): List<CuentaPorCobrar> {
        val cuentas = cuentaPorCobrarDao.obtenerCuentasPorPeriodo(periodo)
        return cuentas.map { it.toDomain() }
    }

    override suspend fun obtenerCuentaPorId(id: Long): CuentaPorCobrar? {
        return cuentaPorCobrarDao.obtenerCuentaPorId(id)?.toDomain()
    }

    override suspend fun buscarCuentasPorMotivo(motivo: String): List<CuentaPorCobrar> {
        val cuentas = cuentaPorCobrarDao.buscarCuentasPorMotivo(motivo)
        return cuentas.map { it.toDomain() }
    }

    override suspend fun insertarCuenta(cuenta: CuentaPorCobrar): Long {
        return cuentaPorCobrarDao.insertarCuenta(cuenta.toEntity())
    }

    override suspend fun actualizarCuenta(cuenta: CuentaPorCobrar) {
        cuentaPorCobrarDao.actualizarCuenta(cuenta.toEntity())
    }

    override suspend fun eliminarCuenta(cuenta: CuentaPorCobrar) {
        cuentaPorCobrarDao.eliminarCuenta(cuenta.toEntity())
    }

    override suspend fun cambiarEstadoCuenta(id: Long, estado: EstadoCuenta) {
        cuentaPorCobrarDao.cambiarEstadoCuenta(id, estado.name)
    }

    override suspend fun obtenerTotalPendiente(): Double? {
        return cuentaPorCobrarDao.obtenerTotalPendiente()
    }

    override suspend fun obtenerTotalPendientePorUsuario(usuarioId: Long): Double? {
        return cuentaPorCobrarDao.obtenerTotalPendientePorUsuario(usuarioId)
    }

    override suspend fun obtenerCantidadCuentasPendientes(): Int {
        return cuentaPorCobrarDao.obtenerCantidadCuentasPendientes()
    }

    private suspend fun CuentaPorCobrarEntity.toDomain(): CuentaPorCobrar {
        val usuario = usuarioDao.obtenerUsuarioPorId(usuarioId)
        return CuentaPorCobrar(
            id = id,
            motivo = motivo,
            monto = monto,
            usuarioId = usuarioId,
            usuarioNombre = usuario?.let { "${it.nombre} ${it.apellido}" },
            fechaCobro = fechaCobro,
            periodoCobro = periodoCobro,
            estado = EstadoCuenta.valueOf(estado),
            notas = notas,
            fechaCreacion = fechaCreacion,
            fechaActualizacion = fechaActualizacion
        )
    }

    private fun CuentaPorCobrar.toEntity(): CuentaPorCobrarEntity {
        return CuentaPorCobrarEntity(
            id = id,
            motivo = motivo,
            monto = monto,
            usuarioId = usuarioId,
            fechaCobro = fechaCobro,
            periodoCobro = periodoCobro,
            estado = estado.name,
            notas = notas,
            fechaCreacion = fechaCreacion,
            fechaActualizacion = fechaActualizacion
        )
    }
} 