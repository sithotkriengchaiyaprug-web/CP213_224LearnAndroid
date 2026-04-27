package com.example.zerotouchbudget.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Light-Only Color Scheme ─────────────────────────────────────────────────
// Dynamic color disabled — we enforce our own design system

private val LightColorScheme = lightColorScheme(
    primary              = Accent,
    onPrimary            = OnAccent,
    primaryContainer     = AccentLight,
    onPrimaryContainer   = Accent,

    secondary            = TextSecondary,
    onSecondary          = Surface,
    secondaryContainer   = SurfaceVariant,
    onSecondaryContainer = TextPrimary,

    tertiary             = Success,
    onTertiary           = Surface,
    tertiaryContainer    = SuccessLight,
    onTertiaryContainer  = Success,

    error                = Danger,
    onError              = Surface,
    errorContainer       = DangerLight,
    onErrorContainer     = Danger,

    background           = Background,
    onBackground         = TextPrimary,
    surface              = Surface,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceVariant,
    onSurfaceVariant     = TextSecondary,
    outline              = TextSecondary.copy(alpha = 0.3f)
)

@Composable
fun ZerotouchbudgetTheme(
    content: @Composable () -> Unit
) {
    // Status bar — light background, dark icons
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}