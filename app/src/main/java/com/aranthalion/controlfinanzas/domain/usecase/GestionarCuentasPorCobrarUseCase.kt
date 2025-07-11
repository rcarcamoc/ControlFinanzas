package com.aranthalion.controlfinanzas.domain.usecase

import com.aranthalion.controlfinanzas.domain.cuenta.CuentaPorCobrar
import com.aranthalion.controlfinanzas.domain.cuenta.CuentaPorCobrarRepository
import com.aranthalion.controlfinanzas.domain.cuenta.EstadoCuenta
import com.aranthalion.controlfinanzas.domain.usuario.UsuarioRepository
import javax.inject.Inject

class GestionarCuentasPorCobrarUseCase @Inject constructor(
    private val cuentaPorCobrarRepository: CuentaPorCobrarRepository,
    private val usuarioRepository: UsuarioRepository
) {
    suspend fun obtenerTodasLasCuentas(): List<CuentaPorCobrar> {
        return cuentaPorCobrarRepository.obtenerTodasLasCuentas()
    }

    suspend fun obtenerCuentasPendientes(): List<CuentaPorCobrar> {
        return cuentaPorCobrarRepository.obtenerCuentasPendientes()
    }

    suspend fun obtenerCuentasPorUsuario(usuarioId: Long): List<CuentaPorCobrar> {
        return cuentaPorCobrarRepository.obtenerCuentasPorUsuario(usuarioId)
    }

    suspend fun obtenerCuentasPorPeriodo(periodo: String): List<CuentaPorCobrar> {
        return cuentaPorCobrarRepository.obtenerCuentasPorPeriodo(periodo)
    }

    suspend fun obtenerCuentaPorId(id: Long): CuentaPorCobrar? {
        return cuentaPorCobrarRepository.obtenerCuentaPorId(id)
    }

    suspend fun buscarCuentasPorMotivo(motivo: String): List<CuentaPorCobrar> {
        return cuentaPorCobrarRepository.buscarCuentasPorMotivo(motivo)
    }

    suspend fun insertarCuenta(cuenta: CuentaPorCobrar): Long {
        return cuentaPorCobrarRepository.insertarCuenta(cuenta)
    }

    suspend fun actualizarCuenta(cuenta: CuentaPorCobrar) {
        cuentaPorCobrarRepository.actualizarCuenta(cuenta)
    }

    suspend fun eliminarCuenta(cuenta: CuentaPorCobrar) {
        cuentaPorCobrarRepository.eliminarCuenta(cuenta)
    }

    suspend fun cambiarEstadoCuenta(id: Long, estado: EstadoCuenta) {
        cuentaPorCobrarRepository.cambiarEstadoCuenta(id, estado)
    }

    suspend fun obtenerTotalPendiente(): Double? {
        return cuentaPorCobrarRepository.obtenerTotalPendiente()
    }

    suspend fun obtenerTotalPendientePorUsuario(usuarioId: Long): Double? {
        return cuentaPorCobrarRepository.obtenerTotalPendientePorUsuario(usuarioId)
    }

    suspend fun obtenerCantidadCuentasPendientes(): Int {
        return cuentaPorCobrarRepository.obtenerCantidadCuentasPendientes()
    }

    suspend fun obtenerUsuariosDisponibles(): List<com.aranthalion.controlfinanzas.domain.usuario.Usuario> {
        return usuarioRepository.obtenerUsuariosActivos()
    }

    suspend fun validarCuenta(cuenta: CuentaPorCobrar): List<String> {
        val errores = mutableListOf<String>()
        
        if (cuenta.motivo.isBlank()) {
            errores.add("El motivo es obligatorio")
        }
        
        if (cuenta.monto <= 0) {
            errores.add("El monto debe ser mayor a 0")
        }
        
        if (cuenta.usuarioId <= 0) {
            errores.add("Debe seleccionar un usuario")
        }
        
        // Verificar que el usuario existe
        val usuario = usuarioRepository.obtenerUsuarioPorId(cuenta.usuarioId)
        if (usuario == null) {
            errores.add("El usuario seleccionado no existe")
        }
        
        return errores
    }
} 