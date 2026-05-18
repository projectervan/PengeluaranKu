package com.finansialku.app.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = OnPrimary,
    primaryContainer = GreenPrimaryLight,
    onPrimaryContainer = GreenPrimaryDark,
    secondary = RedExpense,
    onSecondary = OnSecondary,
    secondaryContainer = RedExpenseLight,
    onSecondaryContainer = RedExpenseDark,
    background = GrayBackground,
    onBackground = OnBackground,
    surface = White,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceVariant = SurfaceVariant,
    outline = Outline
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = GreenPrimaryDark,
    primaryContainer = GreenPrimary,
    onPrimaryContainer = OnPrimary,
    secondary = RedExpenseLight,
    onSecondary = RedExpenseDark,
    secondaryContainer = RedExpense,
    onSecondaryContainer = OnSecondary,
    background = OnBackground,
    onBackground = GrayBackground,
    surface = OnSurface,
    onSurface = GrayBackground,
    onSurfaceVariant = GrayLight,
    surfaceVariant = OnSurfaceVariant,
    outline = GrayLight
)

@Composable
fun FinansialKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
