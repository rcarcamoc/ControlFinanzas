package com.aranthalion.controlfinanzas.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// PALETA UNIFICADA: FINANZAS FAMILIARES
// Basada en Piedra/Arena (Zen) y Arcilla Cálida
// ==========================================

// Colores Base Zen (Piedra y Arena)
val ZenStoneBackgroundLight = Color(0xFFFAFAF9) // Stone-50 (Fondo claro cálido)
val ZenStoneBackgroundDark = Color(0xFF1C1917)  // Stone-900 (Fondo oscuro cálido)

val ZenStonePrimary = Color(0xFF44403C)          // Stone-700 (Gris carbón cálido)
val ZenStonePrimaryDark = Color(0xFFF5F5F4)      // Stone-100 (Texto en modo oscuro)

val ZenStoneSecondary = Color(0xFFF5F5F4)        // Stone-100 (Bordes y elementos secundarios)
val ZenStoneSecondaryDark = Color(0xFF292524)    // Stone-800

val ZenStoneBorderLight = Color(0xFFE7E5E4)      // Stone-200 (Bordes finos)
val ZenStoneBorderDark = Color(0xFF3F3F46)       // Gris oscuro para bordes

// El Acento de Color: Arcilla/Terracota Cálida (Familiar y acogedor)
val ZenClayAccent = Color(0xFFC26D40)            // Terracota (Acento familiar activo)
val ZenClayLight = Color(0xFFF7E6DC)             // Arena arcillosa suave (Fondo de elementos activos)
val ZenClayAccentDark = Color(0xFFE8BFA8)        // Arcilla suave para modo oscuro

// ==========================================
// MAPEO DE COLORES LEGACY PARA COMPATIBILIDAD
// ==========================================

// Colores principales del tema naranja mapped to Arcilla y Piedra
val OrangePrimary = ZenClayAccent
val OrangeSecondary = ZenClayLight
val OrangeAccent = ZenClayAccentDark

// Colores de fondo
val BackgroundLight = ZenStoneBackgroundLight
val BackgroundDark = ZenStoneBackgroundDark

// Colores de superficie
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = ZenStoneSecondaryDark

// Colores de texto
val TextPrimaryLight = ZenStonePrimary
val TextPrimaryDark = ZenStonePrimaryDark

// Colores de texto secundario
val TextSecondaryLight = Color(0xFF78716C) // Stone-500
val TextSecondaryDark = Color(0xFFA8A29E)  // Stone-400

// Colores de borde
val BorderLight = ZenStoneBorderLight
val BorderDark = ZenStoneBorderDark

// Colores de entrada
val InputLight = ZenStoneSecondary
val InputDark = ZenStoneSecondaryDark

// Colores de estado
val Success = Color(0xFF10B981) // Emerald Green (Zen)
val Error = Color(0xFFEF4444)   // Rose Red (Zen)
val Warning = Color(0xFFF59E0B) // Amber (Zen)

// Colores de gráficos (para charts) - Paleta del dashboard unificado
val Chart1 = ZenClayAccent      // Arcilla / Terracota
val Chart2 = Color(0xFF8B5CF6)   // Violeta
val Chart3 = Color(0xFF10B981)   // Verde Esmeralda
val Chart4 = Color(0xFFF59E0B)   // Ámbar
val Chart5 = Color(0xFF6366F1)   // Índigo

// Colores del sidebar / navegación lateral
val SidebarBackground = ZenStoneSecondary
val SidebarForeground = ZenStonePrimary
val SidebarAccent = ZenClayLight
val SidebarAccentForeground = ZenClayAccent

// Colores legacy para compatibilidad
val Purple80 = OrangePrimary
val PurpleGrey80 = OrangeSecondary
val Pink80 = OrangeAccent

val Purple40 = OrangeAccent
val PurpleGrey40 = OrangeSecondary
val Pink40 = OrangePrimary