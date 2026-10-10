package com.vaultia.app.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Obsidian Aurora Dark Scheme
private val DarkColorScheme = darkColorScheme(
    primary = ObsidianPrimaryLavender,
    onPrimary = ObsidianBgMain,
    secondary = ObsidianElectricCyan,
    onSecondary = ObsidianBgMain,
    tertiary = ObsidianChampagneAccent,
    background = ObsidianBgMain,
    surface = ObsidianSurfaceElevated,
    surfaceVariant = ObsidianSurfaceSecondary,
    onBackground = ObsidianTextPrimary,
    onSurface = ObsidianTextPrimary,
    onSurfaceVariant = ObsidianTextSecondary,
    outline = ObsidianBorder,
    error = ObsidianErrorCrimson
)

// Pearl Titanium Luxury Light Scheme (Default on First Launch)
private val LightColorScheme = lightColorScheme(
    primary = PearlPrimaryViolet,
    onPrimary = PearlSurfaceElevated,
    secondary = PearlIceCyan,
    onSecondary = PearlTextPrimary,
    tertiary = PearlSoftChampagne,
    background = PearlBgMain,
    surface = PearlSurfaceElevated,
    surfaceVariant = PearlBgSecondary,
    onBackground = PearlTextPrimary,
    onSurface = PearlTextPrimary,
    onSurfaceVariant = PearlTextSecondary,
    outline = PearlBorderSubtle,
    error = PearlErrorCrimson
)

@Composable
fun VaultiaTheme(
    themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when {
        darkTheme != null -> darkTheme
        themeMode == "DARK" -> true
        themeMode == "LIGHT" -> false
        else -> isSystemDark
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
