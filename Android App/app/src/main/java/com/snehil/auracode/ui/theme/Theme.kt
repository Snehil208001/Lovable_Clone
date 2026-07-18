package com.snehil.auracode.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AuraColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = PrimaryForeground,
    primaryContainer = EmeraldDeep,
    onPrimaryContainer = Foreground,
    secondary = MutedForeground,
    onSecondary = Foreground,
    secondaryContainer = Secondary,
    onSecondaryContainer = Foreground,
    tertiary = ChartCyan,
    onTertiary = Background,
    background = Background,
    onBackground = Foreground,
    surface = CardSurface,
    onSurface = Foreground,
    surfaceVariant = Secondary,
    onSurfaceVariant = MutedForeground,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = Secondary,
    error = Destructive,
    onError = Foreground,
    outline = BorderColor,
    outlineVariant = Accent
)

@Composable
fun AuraCodeTheme(
    // Dark-only design system, matching the web app.
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Dark theme: keep light-on-dark system bar icons. Bar backgrounds
            // are transparent via edge-to-edge and show the dark app background.
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = false
            insets.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = AuraColorScheme,
        typography = Typography,
        content = content
    )
}
