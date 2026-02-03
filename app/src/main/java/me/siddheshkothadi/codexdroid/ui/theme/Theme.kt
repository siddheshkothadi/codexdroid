package me.siddheshkothadi.codexdroid.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CodexDroidDarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PitchBlack,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = OnBluePrimaryContainer,
    secondary = SubtitleGray,
    onSecondary = PureWhite,
    background = PitchBlack,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = PureWhite,
    outline = BorderGray
)

private val CodexDroidLightColorScheme = lightColorScheme(
    primary = LightBluePrimary,
    onPrimary = PureWhite,
    primaryContainer = LightBluePrimaryContainer,
    onPrimaryContainer = OnLightBluePrimaryContainer,
    secondary = SubtitleGray,
    onSecondary = PitchBlack,
    background = PureWhite,
    onBackground = PitchBlack,
    surface = LightSurface,
    onSurface = PitchBlack,
    surfaceVariant = LightSurface,
    onSurfaceVariant = PitchBlack,
    outline = BorderGray.copy(alpha = 0.2f)
)

@Composable
fun CodexDroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Enable dynamic color by default for Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context).copy(
                    background = PitchBlack,
                    surface = DarkSurface,
                    surfaceVariant = DarkSurface,
                    onBackground = PureWhite,
                    onSurface = PureWhite,
                    onSurfaceVariant = PureWhite
                )
            } else {
                dynamicLightColorScheme(context).copy(
                    background = PureWhite,
                    surface = LightSurface,
                    surfaceVariant = LightSurface
                )
            }
        }
        darkTheme -> CodexDroidDarkColorScheme
        else -> CodexDroidLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
