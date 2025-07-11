package com.aranthalion.controlfinanzas.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.aranthalion.controlfinanzas.presentation.components.CustomIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aranthalion.controlfinanzas.ui.theme.SidebarBackground
import com.aranthalion.controlfinanzas.ui.theme.SidebarForeground
import com.aranthalion.controlfinanzas.ui.theme.SidebarAccent
import com.aranthalion.controlfinanzas.ui.theme.SidebarAccentForeground
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val navItems = listOf(
    NavItem("home", "Dashboard", CustomIcons.Home),
    NavItem("transacciones", "Transacciones", CustomIcons.List),
    NavItem("presupuestos", "Presupuestos y Categorías", CustomIcons.Star),
    NavItem("usuarios", "Usuarios", CustomIcons.Person),
    NavItem("cuentas_por_cobrar", "Cuentas por Cobrar", CustomIcons.Star),

    NavItem("importar_excel", "Importar", CustomIcons.Info),
    NavItem("dashboardAnalisis", "Análisis", CustomIcons.Star),
    NavItem("aporte_proporcional", "Aporte", CustomIcons.Person),
    NavItem("analisis_gasto_categoria", "Análisis Gastos", CustomIcons.Info),
    NavItem("configuracion", "Configuración", CustomIcons.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    navController: NavController,
    content: @Composable () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 600.dp
    
    // Estado del sidebar
    var isSidebarExpanded by remember { mutableStateOf(!isSmallScreen) }
    
    // No mostrar sidebar en la pantalla de primer uso
    val isFirstRunScreen = currentRoute == "first_run"
    
    if (isFirstRunScreen) {
        // Mostrar solo el contenido sin sidebar para la pantalla de primer uso
        content()
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar colapsible
            if (isSidebarExpanded) {
                Column(
                    modifier = Modifier
                        .width(256.dp)
                        .fillMaxHeight()
                        .background(SidebarBackground)
                        .padding(16.dp)
                ) {
                    // Logo/Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = CustomIcons.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "FinaVision",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SidebarForeground
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Selector de período global
                    Text(
                        text = "Período",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = SidebarForeground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    PeriodoSelectorGlobal(
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Navigation Items
                    Text(
                        text = "Navegación",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = SidebarForeground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    navItems.forEach { item ->
                        val isActive = currentRoute == item.route
                        NavigationItem(
                            item = item,
                            isActive = isActive,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                // Cerrar sidebar en pantallas pequeñas después de navegar
                                if (isSmallScreen) {
                                    isSidebarExpanded = false
                                }
                            }
                        )
                    }
                }
            }
            
            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Header con botón de menú
                TopAppBar(
                    title = {
                        Text(
                            text = navItems.find { it.route == currentRoute }?.label ?: "FinaVision",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSidebarExpanded = !isSidebarExpanded }) {
                            Icon(
                                imageVector = if (isSidebarExpanded) CustomIcons.Close else CustomIcons.Menu,
                                contentDescription = if (isSidebarExpanded) "Cerrar menú" else "Abrir menú"
                            )
                        }
                    },
                    actions = {
                        // User menu placeholder
                        IconButton(onClick = { /* TODO: User menu */ }) {
                            Icon(
                                imageVector = CustomIcons.Person,
                                contentDescription = "Usuario"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
                
                // Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun NavigationItem(
    item: NavItem,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isActive) {
        SidebarAccent
    } else {
        SidebarBackground
    }
    
    val textColor = if (isActive) {
        SidebarAccentForeground
    } else {
        SidebarForeground
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = backgroundColor,
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color = textColor
            )
        }
    }
} 