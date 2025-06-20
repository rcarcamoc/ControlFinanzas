package com.aranthalion.controlfinanzas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.aranthalion.controlfinanzas.data.util.ExcelProcessor
import com.aranthalion.controlfinanzas.presentation.navigation.AppNavigation
import com.aranthalion.controlfinanzas.ui.theme.ControlFinanzasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prueba de procesamiento de archivos Excel
        ExcelProcessor.pruebaProcesamientoArchivosExcel(this)
        setContent {
            ControlFinanzasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}