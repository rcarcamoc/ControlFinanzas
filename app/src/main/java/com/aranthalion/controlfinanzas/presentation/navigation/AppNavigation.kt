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

import com.aranthalion.controlfinanzas.presentation.screens.DashboardAnalisisScreen
import com.aranthalion.controlfinanzas.presentation.screens.AporteProporcionalScreen
import com.aranthalion.controlfinanzas.presentation.screens.AnalisisGastoPorCategoriaScreen
import com.aranthalion.controlfinanzas.presentation.screens.PresupuestosScreen
import com.aranthalion.controlfinanzas.presentation.screens.PresupuestosYCategoriasScreen
import com.aranthalion.controlfinanzas.presentation.screens.FirstRunScreen
import com.aranthalion.controlfinanzas.presentation.screens.UsuariosScreen
import com.aranthalion.controlfinanzas.presentation.screens.CuentasPorCobrarScreen
import com.aranthalion.controlfinanzas.presentation.screens.InsightsAvanzadosScreen
import com.aranthalion.controlfinanzas.presentation.screens.AuditoriaDatabaseScreen

import com.aranthalion.controlfinanzas.presentation.components.AppShell

@Composable
fun AppNavigation(navController: NavHostController) {
    AppShell(navController = navController) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("first_run") {
                FirstRunScreen(navController = navController)
            }
            composable("home") {
                HomeScreen(navController = navController)
            }
            composable("categorias") {
                PresupuestosYCategoriasScreen(navController = navController)
            }
            composable("transacciones") {
                TransaccionesScreen(navController = navController)
            }
            composable("importar_excel") { 
                ImportarExcelScreen() 
            }
            composable("configuracion") {
                ConfiguracionScreen(navController = navController)
            }

            composable("dashboardAnalisis") {
                DashboardAnalisisScreen(navController = navController)
            }
            composable("aporte_proporcional") {
                AporteProporcionalScreen(navController = navController)
            }
            composable("presupuestos") {
                PresupuestosYCategoriasScreen(navController = navController)
            }
            composable("analisis_gasto_categoria") {
                AnalisisGastoPorCategoriaScreen(navController = navController)
            }
            composable("usuarios") {
                UsuariosScreen(navController = navController)
            }
            composable("cuentas_por_cobrar") {
                CuentasPorCobrarScreen(navController = navController)
            }
            composable("insights_avanzados") {
                InsightsAvanzadosScreen(navController = navController)
            }
            composable("auditoria_database") {
                AuditoriaDatabaseScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
    }
} 