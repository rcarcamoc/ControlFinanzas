package com.aranthalion.controlfinanzas.presentation.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TemaApp {
    NARANJA, AZUL, VERDE
}

@HiltViewModel
class ConfiguracionViewModel @Inject constructor() : ViewModel() {
    
    private val _temaSeleccionado = MutableStateFlow(TemaApp.NARANJA)
    val temaSeleccionado: StateFlow<TemaApp> = _temaSeleccionado.asStateFlow()
    
    fun cambiarTema(tema: TemaApp) {
        viewModelScope.launch {
            _temaSeleccionado.value = tema
            // Aquí se podría guardar en SharedPreferences para persistencia
        }
    }
    
    fun obtenerColoresTema(tema: TemaApp): Map<String, Int> {
        return when (tema) {
            TemaApp.NARANJA -> mapOf(
                "primary" to android.R.color.holo_orange_dark,
                "primaryVariant" to android.R.color.holo_orange_light,
                "secondary" to android.R.color.holo_orange_light,
                "background" to android.R.color.white,
                "surface" to android.R.color.white
            )
            TemaApp.AZUL -> mapOf(
                "primary" to android.R.color.holo_blue_dark,
                "primaryVariant" to android.R.color.holo_blue_light,
                "secondary" to android.R.color.holo_blue_light,
                "background" to android.R.color.white,
                "surface" to android.R.color.white
            )
            TemaApp.VERDE -> mapOf(
                "primary" to android.R.color.holo_green_dark,
                "primaryVariant" to android.R.color.holo_green_light,
                "secondary" to android.R.color.holo_green_light,
                "background" to android.R.color.white,
                "surface" to android.R.color.white
            )
        }
    }
} 