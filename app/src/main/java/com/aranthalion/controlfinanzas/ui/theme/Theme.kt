package com.aranthalion.controlfinanzas.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.aranthalion.controlfinanzas.presentation.configuracion.TemaApp
import androidx.compose.ui.graphics.Color

// Tema Naranja (basado en el prototipo)
private val NaranjaLightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    secondary = OrangeSecondary,
    tertiary = OrangeAccent,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF8F4F0),
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight
)

private val NaranjaDarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    secondary = OrangeSecondary,
    tertiary = OrangeAccent,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF4A3A2A),
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = BorderDark
)

// Tema Azul (nuevo)
private val AzulLightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3), // Blue 500
    secondary = Color(0xFF90CAF9), // Blue 200
    tertiary = Color(0xFF1976D2), // Blue 700
    background = Color(0xFFF3F8FD), // Very Light Blue
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color(0xFF1C1B1F),
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE3F2FD), // Light Blue 50
    onSurfaceVariant = Color(0xFF546E7A), // Blue Grey 600
    outline = Color(0xFFBBDEFB), // Blue 100
    outlineVariant = Color(0xFFE1F5FE) // Light Blue 50
)

private val AzulDarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9), // Blue 200
    secondary = Color(0xFF42A5F5), // Blue 400
    tertiary = Color(0xFF64B5F6), // Blue 300
    background = Color(0xFF0D47A1), // Blue 900
    surface = Color(0xFF1565C0), // Blue 800
    onPrimary = Color(0xFF1C1B1F),
    onSecondary = Color(0xFF1C1B1F),
    onTertiary = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE3F2FD), // Light Blue 50
    onSurface = Color(0xFFE3F2FD),
    surfaceVariant = Color(0xFF1976D2), // Blue 700
    onSurfaceVariant = Color(0xFFBBDEFB), // Blue 100
    outline = Color(0xFF42A5F5), // Blue 400
    outlineVariant = Color(0xFF1976D2) // Blue 700
)

// Tema Verde (nuevo)
private val VerdeLightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50), // Green 500
    secondary = Color(0xFFA5D6A7), // Green 200
    tertiary = Color(0xFF388E3C), // Green 700
    background = Color(0xFFF3FDF6), // Very Light Green
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color(0xFF1C1B1F),
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8F5E8), // Light Green 50
    onSurfaceVariant = Color(0xFF558B2F), // Light Green 800
    outline = Color(0xFFC8E6C9), // Green 100
    outlineVariant = Color(0xFFE8F5E8) // Light Green 50
)

private val VerdeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5D6A7), // Green 200
    secondary = Color(0xFF66BB6A), // Green 400
    tertiary = Color(0xFF81C784), // Green 300
    background = Color(0xFF1B5E20), // Green 900
    surface = Color(0xFF2E7D32), // Green 800
    onPrimary = Color(0xFF1C1B1F),
    onSecondary = Color(0xFF1C1B1F),
    onTertiary = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE8F5E8), // Light Green 50
    onSurface = Color(0xFFE8F5E8),
    surfaceVariant = Color(0xFF388E3C), // Green 700
    onSurfaceVariant = Color(0xFFC8E6C9), // Green 100
    outline = Color(0xFF66BB6A), // Green 400
    outlineVariant = Color(0xFF388E3C) // Green 700
)

@Composable
fun ControlFinanzasTheme(
    temaApp: TemaApp = TemaApp.NARANJA,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (temaApp) {
        TemaApp.NARANJA -> if (darkTheme) NaranjaDarkColorScheme else NaranjaLightColorScheme
        TemaApp.AZUL -> if (darkTheme) AzulDarkColorScheme else AzulLightColorScheme
        TemaApp.VERDE -> if (darkTheme) VerdeDarkColorScheme else VerdeLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}