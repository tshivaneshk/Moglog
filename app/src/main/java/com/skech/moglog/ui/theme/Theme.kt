package com.skech.moglog.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.skech.moglog.data.local.ThemeMode
import com.skech.moglog.ui.designsystem.DarkMoglogColors
import com.skech.moglog.ui.designsystem.DefaultMoglogTypography
import com.skech.moglog.ui.designsystem.LightMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MaterialInterTypography

private val DarkMaterialScheme = darkColorScheme(
    primary = DarkMoglogColors.accent,
    secondary = DarkMoglogColors.statusPlanned,
    tertiary = DarkMoglogColors.statusWatching,
    background = DarkMoglogColors.background,
    surface = DarkMoglogColors.secondaryBackground,
    surfaceVariant = DarkMoglogColors.tertiaryBackground,
    onPrimary = DarkMoglogColors.onAccent,
    onSecondary = DarkMoglogColors.label,
    onBackground = DarkMoglogColors.label,
    onSurface = DarkMoglogColors.label,
    onSurfaceVariant = DarkMoglogColors.secondaryLabel,
    error = DarkMoglogColors.destructive,
    onError = DarkMoglogColors.label
)

private val LightMaterialScheme = lightColorScheme(
    primary = LightMoglogColors.accent,
    secondary = LightMoglogColors.statusPlanned,
    tertiary = LightMoglogColors.statusWatching,
    background = LightMoglogColors.background,
    surface = LightMoglogColors.secondaryBackground,
    surfaceVariant = LightMoglogColors.tertiaryBackground,
    onPrimary = LightMoglogColors.onAccent,
    onSecondary = LightMoglogColors.label,
    onBackground = LightMoglogColors.label,
    onSurface = LightMoglogColors.label,
    onSurfaceVariant = LightMoglogColors.secondaryLabel,
    error = LightMoglogColors.destructive,
    onError = LightMoglogColors.onAccent
)

@Composable
fun MoglogTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val moglogColors = if (useDark) DarkMoglogColors else LightMoglogColors
    val materialScheme = if (useDark) DarkMaterialScheme else LightMaterialScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = moglogColors.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !useDark
                insetsController.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(
        LocalMoglogColors provides moglogColors,
        LocalMoglogTypography provides DefaultMoglogTypography
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = MaterialInterTypography,
            content = content
        )
    }
}
