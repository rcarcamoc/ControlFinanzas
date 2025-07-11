package com.aranthalion.controlfinanzas.domain.cuenta

interface CuentaPorCobrarRepository {
    suspend fun obtenerTodasLasCuentas(): List<CuentaPorCobrar>
    suspend fun obtenerCuentasPendientes(): List<CuentaPorCobrar>
    suspend fun obtenerCuentasPorUsuario(usuarioId: Long): List<CuentaPorCobrar>
    suspend fun obtenerCuentasPorPeriodo(periodo: String): List<CuentaPorCobrar>
    suspend fun obtenerCuentaPorId(id: Long): CuentaPorCobrar?
    suspend fun buscarCuentasPorMotivo(motivo: String): List<CuentaPorCobrar>
    suspend fun insertarCuenta(cuenta: CuentaPorCobrar): Long
    suspend fun actualizarCuenta(cuenta: CuentaPorCobrar)
    suspend fun eliminarCuenta(cuenta: CuentaPorCobrar)
    suspend fun cambiarEstadoCuenta(id: Long, estado: EstadoCuenta)
    suspend fun obtenerTotalPendiente(): Double?
    suspend fun obtenerTotalPendientePorUsuario(usuarioId: Long): Double?
    suspend fun obtenerCantidadCuentasPendientes(): Int
} 