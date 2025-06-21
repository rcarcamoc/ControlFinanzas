package com.aranthalion.controlfinanzas.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aranthalion.controlfinanzas.presentation.screens.CategoriasScreen
import com.aranthalion.controlfinanzas.presentation.screens.TransaccionesScreen
import com.aranthalion.controlfinanzas.presentation.screens.HomeScreen
import com.aranthalion.controlfinanzas.presentation.screens.ImportarExcelScreen
import com.aranthalion.controlfinanzas.presentation.screens.ConfiguracionScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("categorias") {
            CategoriasScreen(navController = navController)
        }
        composable("transacciones") {
            TransaccionesScreen(navController = navController)
        }
        composable("importar_excel") { ImportarExcelScreen() }
        composable("configuracion") {
            ConfiguracionScreen(navController = navController)
        }
    }
} 