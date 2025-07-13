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
import androidx.compose.foundation.layout.navigationBarsPadding

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val category: String = "General"
)

data class NavCategory(
    val name: String,
    val items: List<NavItem>
)

val navItems = listOf(
    // Principal
    NavItem("home", "Dashboard", CustomIcons.Dashboard, "Principal"),
    NavItem("transacciones", "Transacciones", CustomIcons.Transaction, "Principal"),
    NavItem("importar_excel", "Importar Excel", CustomIcons.Import, "Principal"),
    
    // Gestión
    NavItem("presupuestos", "Presupuestos y Categorías", CustomIcons.Category, "Gestión"),
    NavItem("usuarios", "Usuarios", CustomIcons.User, "Gestión"),
    NavItem("cuentas_por_cobrar", "Cuentas por Cobrar", CustomIcons.CreditCard, "Gestión"),
    
    // Análisis
    NavItem("dashboardAnalisis", "Análisis General", CustomIcons.Analytics, "Análisis"),
    NavItem("aporte_proporcional", "Aporte Proporcional", CustomIcons.Group, "Análisis"),
    NavItem("analisis_gasto_categoria", "Análisis por Categoría", CustomIcons.Assessment, "Análisis"),
    NavItem("insights_avanzados", "Insights Avanzados", CustomIcons.Lightbulb, "Análisis"),
    
    // Herramientas
    NavItem("auditoria_database", "Auditoría DB", CustomIcons.Storage, "Herramientas"),
    NavItem("debug_clasificacion", "Debug Clasificación", CustomIcons.BugReport, "Herramientas"),
    NavItem("configuracion", "Configuración", CustomIcons.Settings, "Herramientas")
)

val navCategories = listOf(
    NavCategory("Principal", navItems.filter { it.category == "Principal" }),
    NavCategory("Gestión", navItems.filter { it.category == "Gestión" }),
    NavCategory("Análisis", navItems.filter { it.category == "Análisis" }),
    NavCategory("Herramientas", navItems.filter { it.category == "Herramientas" })
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
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(SidebarBackground)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
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
                    
                    // Navigation Items por categorías
                    navCategories.forEach { category ->
                        if (category.items.isNotEmpty()) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = SidebarForeground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                            )
                            
                            category.items.forEach { item ->
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
                        .navigationBarsPadding()
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
            .padding(vertical = 2.dp),
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
                modifier = Modifier.size(20.dp)
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