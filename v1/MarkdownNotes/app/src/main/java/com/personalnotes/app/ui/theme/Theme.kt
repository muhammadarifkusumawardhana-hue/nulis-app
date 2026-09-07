package com.personalnotes.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PurplePrimary = Color(0xFF6200EE)
private val PurpleSecondary = Color(0xFF03DAC6)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D5FF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = PurpleSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFCEFAF5),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF4F0F9),
    onSurfaceVariant = Color(0xFF49454F),
    background = Color(0xFFFFFBFE),
    outline = Color(0xFF79747E)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF4ED8C7),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF1E1B22),
    onSurfaceVariant = Color(0xFFCAC4D0),
    background = Color(0xFF141218),
    outline = Color(0xFF938F99)
)

@Composable
fun PersonalNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
