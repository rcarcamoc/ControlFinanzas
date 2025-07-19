package com.aranthalion.controlfinanzas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.aranthalion.controlfinanzas.data.util.ExcelProcessor
import com.aranthalion.controlfinanzas.data.repository.TinderPreloadService
import com.aranthalion.controlfinanzas.data.repository.MigracionInicialService
import com.aranthalion.controlfinanzas.presentation.configuracion.ConfiguracionViewModel
import com.aranthalion.controlfinanzas.presentation.navigation.AppNavigation
import com.aranthalion.controlfinanzas.ui.theme.ControlFinanzasTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import javax.inject.Inject
import androidx.core.view.WindowCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var migracionInicialService: MigracionInicialService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val configuracionViewModel: ConfiguracionViewModel = hiltViewModel()
            val tema by configuracionViewModel.temaSeleccionado.collectAsState()
            ControlFinanzasTheme(temaApp = tema) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var isFirstRun by remember { mutableStateOf(true) }
                    
                    // Verificar si es la primera ejecución
                    LaunchedEffect(Unit) {
                        val configPrefs = ConfiguracionPreferences(this@MainActivity)
                        isFirstRun = configPrefs.isFirstRun
                        
                        if (isFirstRun) {
                            navController.navigate("first_run") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                    
                    // Iniciar precarga de transacciones Tinder en segundo plano
                    LaunchedEffect(Unit) {
                        // La precarga se iniciará automáticamente cuando se necesite
                        // No podemos usar hiltViewModel() aquí
                    }
                    
                    // Iniciar migración de datos normalizados (HITO 1.1)
                    LaunchedEffect(Unit) {
                        migracionInicialService.iniciarMigracionEnBackground()
                    }
                    
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}