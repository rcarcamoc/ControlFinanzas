package com.aranthalion.controlfinanzas.presentation.global

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
open class PeriodoGlobalViewModel @Inject constructor(
    private val configuracionPreferences: ConfiguracionPreferences
) : ViewModel() {
    
    companion object {
        private val _periodoSeleccionado = MutableStateFlow("")
        val staticPeriodoSeleccionado: StateFlow<String> = _periodoSeleccionado.asStateFlow()

        private val _scopeSeleccionado = MutableStateFlow("")
        val staticScopeSeleccionado: StateFlow<String> = _scopeSeleccionado.asStateFlow()

        private var isInitialized = false
    }

    open val periodoSeleccionado: StateFlow<String> = staticPeriodoSeleccionado
    open val scopeSeleccionado: StateFlow<String> = staticScopeSeleccionado
    
    private val _periodosDisponibles = MutableStateFlow(generarPeriodosDisponibles())
    open val periodosDisponibles: StateFlow<List<String>> = _periodosDisponibles.asStateFlow()

    init {
        synchronized(this) {
            if (!isInitialized) {
                val periodoGuardado = configuracionPreferences.obtenerPeriodoGlobal() ?: obtenerPeriodoInicial()
                _periodoSeleccionado.value = periodoGuardado
                _scopeSeleccionado.value = configuracionPreferences.obtenerScopeGlobal()
                isInitialized = true
            }
        }
    }
    
    fun cambiarPeriodo(periodo: String) {
        _periodoSeleccionado.value = periodo
        // Guardar en preferencias
        viewModelScope.launch {
            configuracionPreferences.guardarPeriodoGlobal(periodo)
        }
    }

    fun cambiarScope(scope: String) {
        _scopeSeleccionado.value = scope
        viewModelScope.launch {
            configuracionPreferences.guardarScopeGlobal(scope)
        }
    }
    
    private fun obtenerPeriodoInicial(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return dateFormat.format(Date())
    }
    
    private fun generarPeriodosDisponibles(): List<String> {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val periodos = mutableListOf<String>()
        // Últimos 11 meses + mes actual + 2 futuros
        for (i in -11..2) {
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, 1)
            cal.add(Calendar.MONTH, i)
            val periodo = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            periodos.add(periodo)
        }
        return periodos.sortedDescending()
    }
} 