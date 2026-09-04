package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VeloColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = VeloBackground,
    primaryContainer = VeloSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricViolet,
    onSecondary = TextPrimary,
    secondaryContainer = VeloSurfaceVariant,
    onSecondaryContainer = NeonPurple,
    tertiary = MintGreen,
    onTertiary = VeloBackground,
    background = VeloBackground,
    onBackground = TextPrimary,
    surface = VeloSurface,
    onSurface = TextPrimary,
    surfaceVariant = VeloSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = VeloCardBorder,
    error = CoralError,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VeloBackground.toArgb()
            window.navigationBarColor = VeloBackground.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = VeloColorScheme,
        typography = Typography,
        content = content
    )
}
