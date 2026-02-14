package me.siddheshkothadi.codexdroid.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

private val LocalCodexColors = staticCompositionLocalOf<CodexColors> { LightCodexColors }

object CodexTheme {
    val colors: CodexColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCodexColors.current
}

private fun CodexColors.toMaterialColorScheme(
    darkTheme: Boolean,
): ColorScheme {
    val base =
        if (darkTheme) darkColorScheme() else lightColorScheme()
    val inverseColors = if (darkTheme) LightCodexColors else DarkCodexColors
    return base.copy(
        primary = accentUi,
        onPrimary = onAccentAction,
        primaryContainer = bgSecondary,
        onPrimaryContainer = textPrimary,
        inversePrimary = accentUi,
        secondary = textSecondary,
        onSecondary = textInverted,
        secondaryContainer = bgTertiary,
        onSecondaryContainer = textPrimary,
        tertiary = accentSuccess,
        onTertiary = textInverted,
        tertiaryContainer = bgTertiary,
        onTertiaryContainer = textPrimary,
        background = bgPrimary,
        onBackground = textPrimary,
        surface = bgPrimary,
        onSurface = textPrimary,
        surfaceVariant = bgSecondary,
        onSurfaceVariant = textSecondary,
        surfaceTint = accentUi,
        inverseSurface = inverseColors.bgPrimary,
        inverseOnSurface = inverseColors.textPrimary,
        error = accentError,
        onError = textInverted,
        errorContainer = bgSecondary,
        onErrorContainer = textPrimary,
        outline = borderDefault,
        outlineVariant = borderSubtle,
        scrim = Color.Black.copy(alpha = 0.40f),
    )
}

@Composable
fun CodexDroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val codexColors = if (darkTheme) DarkCodexColors else LightCodexColors
    val colorScheme = codexColors.toMaterialColorScheme(darkTheme = darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalCodexColors provides codexColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
