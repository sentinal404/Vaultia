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

private val DarkColorScheme = darkColorScheme(
    primary = ClayPrimary,
    onPrimary = ClaySurfaceLight,
    secondary = ClaySecondary,
    background = ClayBgDark,
    surface = ClaySurfaceDark,
    surfaceVariant = ClayCardDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = StatusError
)

private val LightColorScheme = lightColorScheme(
    primary = ClayPrimary,
    onPrimary = ClaySurfaceLight,
    secondary = ClaySecondary,
    background = ClayBgLight,
    surface = ClaySurfaceLight,
    surfaceVariant = ClayCardLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = StatusError
)

@Composable
fun VaultiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
