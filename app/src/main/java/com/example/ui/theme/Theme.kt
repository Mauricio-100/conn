package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppColorScheme = staticCompositionLocalOf<AppColorScheme> {
    ShadcnLightColorScheme
}

@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val scheme = appTheme.toColorScheme(isSystemDark = isSystemDark)

    val colorScheme = if (scheme.isDark) {
        darkColorScheme(
            primary = scheme.primary,
            onPrimary = scheme.onPrimary,
            primaryContainer = scheme.surfaceVariant,
            onPrimaryContainer = scheme.onSurface,
            secondary = scheme.secondary,
            onSecondary = scheme.onSecondary,
            secondaryContainer = scheme.surfaceVariant,
            onSecondaryContainer = scheme.onSurface,
            tertiary = scheme.tertiary,
            onTertiary = scheme.onTertiary,
            background = scheme.background,
            onBackground = scheme.onBackground,
            surface = scheme.surface,
            onSurface = scheme.onSurface,
            surfaceVariant = scheme.surfaceVariant,
            onSurfaceVariant = scheme.onSurfaceVariant,
            outline = scheme.outline,
            outlineVariant = scheme.outlineVariant,
            error = scheme.error,
            onError = scheme.onError
        )
    } else {
        lightColorScheme(
            primary = scheme.primary,
            onPrimary = scheme.onPrimary,
            primaryContainer = scheme.surfaceVariant,
            onPrimaryContainer = scheme.onSurface,
            secondary = scheme.secondary,
            onSecondary = scheme.onSecondary,
            secondaryContainer = scheme.surfaceVariant,
            onSecondaryContainer = scheme.onSurface,
            tertiary = scheme.tertiary,
            onTertiary = scheme.onTertiary,
            background = scheme.background,
            onBackground = scheme.onBackground,
            surface = scheme.surface,
            onSurface = scheme.onSurface,
            surfaceVariant = scheme.surfaceVariant,
            onSurfaceVariant = scheme.onSurfaceVariant,
            outline = scheme.outline,
            outlineVariant = scheme.outlineVariant,
            error = scheme.error,
            onError = scheme.onError
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !scheme.isDark
        }
    }

    CompositionLocalProvider(
        LocalAppColorScheme provides scheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
