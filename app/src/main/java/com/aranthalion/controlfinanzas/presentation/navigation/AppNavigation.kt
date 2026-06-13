package com.aranthalion.controlfinanzas.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aranthalion.controlfinanzas.presentation.screens.TransaccionesScreen
import com.aranthalion.controlfinanzas.presentation.screens.HomeScreen
import com.aranthalion.controlfinanzas.presentation.screens.ImportarExcelScreen
import com.aranthalion.controlfinanzas.presentation.screens.ConfiguracionScreen

import com.aranthalion.controlfinanzas.presentation.screens.DashboardAnalisisScreen
import com.aranthalion.controlfinanzas.presentation.screens.AporteProporcionalScreen
import com.aranthalion.controlfinanzas.presentation.screens.PresupuestosYCategoriasScreen
import com.aranthalion.controlfinanzas.presentation.screens.FirstRunScreen
import com.aranthalion.controlfinanzas.presentation.screens.UsuariosScreen
import com.aranthalion.controlfinanzas.presentation.screens.CuentasPorCobrarScreen
import com.aranthalion.controlfinanzas.presentation.screens.AuditoriaDatabaseScreen
import com.aranthalion.controlfinanzas.presentation.screens.ClasificacionAutomaticaDebugScreen
import com.aranthalion.controlfinanzas.presentation.screens.TransaccionesImportExportScreen

import com.aranthalion.controlfinanzas.presentation.components.AppShell
import com.aranthalion.controlfinanzas.presentation.screens.TinderClasificacionScreen

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
            composable(
                route = "transacciones?categoriaId={categoriaId}",
                arguments = listOf(
                    androidx.navigation.navArgument("categoriaId") {
                        type = androidx.navigation.NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val catId = backStackEntry.arguments?.getLong("categoriaId") ?: -1L
                TransaccionesScreen(categoriaId = if (catId == -1L) null else catId)
            }
            composable("importar_excel") { 
                ImportarExcelScreen() 
            }
            composable("configuracion") {
                ConfiguracionScreen(navController = navController)
            }
            composable("email_sync") {
                com.aranthalion.controlfinanzas.presentation.screens.email.EmailSyncScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
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
            composable("usuarios") {
                UsuariosScreen(navController = navController)
            }
            composable("cuentas_por_cobrar") {
                CuentasPorCobrarScreen(navController = navController)
            }
            composable("auditoria_database") {
                AuditoriaDatabaseScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
            composable("debug_clasificacion") {
                ClasificacionAutomaticaDebugScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
            composable("import_export") {
                TransaccionesImportExportScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
            composable("tinder_clasificacion") {
                TinderClasificacionScreen(
                    onDismiss = {
                        navController.popBackStack()
                        // Opcional: recargar transacciones si es necesario
                    }
                )
            }
        }
    }
}