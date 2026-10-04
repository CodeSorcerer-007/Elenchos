package com.example.elenchos.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LabPrimary,
    onPrimary = LabSurface,
    primaryContainer = LabPrimaryLight,
    onPrimaryContainer = LabPrimaryDark,
    secondary = LabTextSecondary,
    onSecondary = LabSurface,
    secondaryContainer = LabSurfaceVariant,
    onSecondaryContainer = LabTextPrimary,
    tertiary = LabSeverityP2,
    onTertiary = LabSurface,
    background = LabBackground,
    onBackground = LabTextPrimary,
    surface = LabSurface,
    onSurface = LabTextPrimary,
    surfaceVariant = LabSurfaceVariant,
    onSurfaceVariant = LabTextSecondary,
    outline = LabCardBorder,
    outlineVariant = LabBorderSubtle
)

@Composable
fun ElenchosTheme(
    content: @Composable () -> Unit
) {
    // Specification: Light-only premium developer laboratory aesthetic
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = LabBackground.toArgb()
            window.navigationBarColor = LabSurface.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
