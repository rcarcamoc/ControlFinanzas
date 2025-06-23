package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class DashboardAnalisisUiState(
    val saldoActual: Double = 0.0,
    val tasaAhorro: Double = 0.0,
    val evolucionHistorica: List<Pair<String, Double>> = emptyList(),
    val distribucionCategorias: List<Pair<String, Double>> = emptyList()
)

@HiltViewModel
class DashboardAnalisisViewModel @Inject constructor(): ViewModel() {
    var uiState = mutableStateOf(DashboardAnalisisUiState())
        private set

    init {
        // Simular datos
        uiState.value = DashboardAnalisisUiState(
            saldoActual = 120000.0,
            tasaAhorro = 18.5,
            evolucionHistorica = listOf(
                "Ene" to 100000.0,
                "Feb" to 120000.0,
                "Mar" to 110000.0,
                "Abr" to 130000.0
            ),
            distribucionCategorias = listOf(
                "Alimentación" to 40000.0,
                "Transporte" to 20000.0,
                "Salud" to 15000.0,
                "Ocio" to 25000.0
            )
        )
    }
} 