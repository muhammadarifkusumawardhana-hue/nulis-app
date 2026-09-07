package com.nu.lis.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── Palette — organic green pastel ──────────────────────────────
val Sage50        = Color(0xFFF0F7F0)
val Sage100       = Color(0xFFDCEEDC)
val Sage200       = Color(0xFFC2DEC2)
val Sage600       = Color(0xFF3D7A3D)
val Sage700       = Color(0xFF2C5F2C)
val Sage800       = Color(0xFF1A3D1A)
val Terracotta    = Color(0xFFB85C38)
val Cream         = Color(0xFFFAF8F3)
val MonoBlack     = Color(0xFF121212)
val MonoWhite     = Color(0xFFF5F5F5)

val DarkBg        = Color(0xFF111A11)
val DarkSurf      = Color(0xFF1C2B1C)
val DarkSurf2     = Color(0xFF243224)
val DarkPrim      = Color(0xFF90C490)
val DarkOnPrim    = Color(0xFF0D2E0D)
val DarkText      = Color(0xFFE8F0E8)

// ── Color Schemes ────────────────────────────────────────────────
val LightScheme = lightColorScheme(
    primary               = Sage600,
    onPrimary             = Color.White,
    primaryContainer      = Sage100,
    onPrimaryContainer    = Sage800,
    secondary             = Terracotta,
    onSecondary           = Color.White,
    secondaryContainer    = Color(0xFFFFE8DF),
    onSecondaryContainer  = Color(0xFF6B2E14),
    tertiary              = Color(0xFF5B6E5B),
    background            = Sage50,
    onBackground          = Sage800,
    surface               = Cream,
    onSurface             = Sage800,
    surfaceVariant        = Sage100,
    onSurfaceVariant      = Sage700,
    outline               = Color(0xFF8BAC8B),
    outlineVariant        = Sage200,
    surfaceContainer      = Sage100,
    surfaceContainerLow   = Cream,
    surfaceContainerHigh  = Sage200,
    error                 = Color(0xFFBA1A1A),
    onError               = Color.White,
)

val DarkScheme = darkColorScheme(
    primary               = DarkPrim,
    onPrimary             = DarkOnPrim,
    primaryContainer      = Color(0xFF1E3D1E),
    onPrimaryContainer    = Color(0xFFB8DDB8),
    secondary             = Color(0xFFE8A080),
    onSecondary           = Color(0xFF4A1E08),
    secondaryContainer    = Color(0xFF6B3018),
    onSecondaryContainer  = Color(0xFFFFD0BC),
    tertiary              = Color(0xFF9BB89B),
    background            = DarkBg,
    onBackground          = DarkText,
    surface               = DarkSurf,
    onSurface             = DarkText,
    surfaceVariant        = DarkSurf2,
    onSurfaceVariant      = Color(0xFFA8C0A8),
    outline               = Color(0xFF4A6B4A),
    outlineVariant        = Color(0xFF2D472D),
    surfaceContainer      = DarkSurf2,
    surfaceContainerLow   = DarkSurf,
    surfaceContainerHigh  = Color(0xFF2C3D2C),
    error                 = Color(0xFFFFB4AB),
    onError               = Color(0xFF690005),
)

// ── Typography — Google Sans lookalike using system sans-serif ────────────────────
val NotesTypography = Typography(
    displayLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 48.sp,
        lineHeight    = 54.sp,
        letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 36.sp,
        lineHeight    = 42.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 28.sp,
        lineHeight    = 34.sp
    ),
    headlineLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 24.sp,
        lineHeight    = 30.sp
    ),
    headlineMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 20.sp,
        lineHeight    = 27.sp
    ),
    headlineSmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 18.sp,
        lineHeight    = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 18.sp,
        lineHeight    = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 15.sp,
        lineHeight    = 21.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 13.sp,
        lineHeight    = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontSize      = 16.sp,
        lineHeight    = 26.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontSize      = 14.sp,
        lineHeight    = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontSize      = 12.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 12.sp,
        lineHeight    = 18.sp,
        letterSpacing = 1.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 10.sp,
        lineHeight    = 14.sp,
        letterSpacing = 0.5.sp
    )
)

// ── App State for Settings ───────────────────────────────────────
class AppSettingsState(
    darkMode: Boolean,
    language: String,
    accentColor: Color,
    nickname: String = "",
    isGridView: Boolean = false,
    fontTheme: String = "Modern",
    customColorHsv: FloatArray = floatArrayOf(120f, 0.5f, 0.47f), // Default Sage600 approx
    isNoteLockEnabled: Boolean = false,
    noteLockPin: String = "",
    isBiometricEnabled: Boolean = false,
    securityQuestion: String = "",
    securityAnswer: String = "",
    lockTimeoutSeconds: Int = 0, // 0 means "when note closed", -1 means "when app closed"
    recentColorsHsv: List<FloatArray> = emptyList()
) {
    var darkMode by mutableStateOf(darkMode)
    var language by mutableStateOf(language)
    var accentColor by mutableStateOf(accentColor)
    var nickname by mutableStateOf(nickname)
    var isGridView by mutableStateOf(isGridView)
    var fontTheme by mutableStateOf(fontTheme)
    var customColorHsv by mutableStateOf(customColorHsv)
    var isNoteLockEnabled by mutableStateOf(isNoteLockEnabled)
    var noteLockPin by mutableStateOf(noteLockPin)
    var isBiometricEnabled by mutableStateOf(isBiometricEnabled)
    var securityQuestion by mutableStateOf(securityQuestion)
    var securityAnswer by mutableStateOf(securityAnswer)
    var lockTimeoutSeconds by mutableStateOf(lockTimeoutSeconds)
    var recentColorsHsv by mutableStateOf(recentColorsHsv)

    fun addRecentColor(hsv: FloatArray) {
        val newList = recentColorsHsv.toMutableList()
        // Remove if exists (to move to front)
        newList.removeAll { it.contentEquals(hsv) }
        newList.add(0, hsv.copyOf())
        if (newList.size > 5) {
            recentColorsHsv = newList.take(5)
        } else {
            recentColorsHsv = newList
        }
    }

    companion object {
        val Saver: Saver<AppSettingsState, *> = listSaver(
            save = { 
                listOf(
                    it.darkMode, 
                    it.language, 
                    it.accentColor.toArgb(), 
                    it.nickname, 
                    it.isGridView,
                    it.fontTheme,
                    it.customColorHsv[0],
                    it.customColorHsv[1],
                    it.customColorHsv[2],
                    it.isNoteLockEnabled,
                    it.noteLockPin,
                    it.isBiometricEnabled,
                    it.securityQuestion,
                    it.securityAnswer,
                    it.lockTimeoutSeconds,
                    it.recentColorsHsv.flatMap { hsv -> listOf(hsv[0], hsv[1], hsv[2]) }
                ) 
            },
            restore = {
                val recentColors = mutableListOf<FloatArray>()
                val colorsData = it[15] as List<Float>
                for (i in colorsData.indices step 3) {
                    recentColors.add(floatArrayOf(colorsData[i], colorsData[i+1], colorsData[i+2]))
                }
                AppSettingsState(
                    darkMode = it[0] as Boolean,
                    language = it[1] as String,
                    accentColor = Color(it[2] as Int),
                    nickname = it[3] as String,
                    isGridView = it[4] as Boolean,
                    fontTheme = it[5] as String,
                    customColorHsv = floatArrayOf(it[6] as Float, it[7] as Float, it[8] as Float),
                    isNoteLockEnabled = it[9] as Boolean,
                    noteLockPin = it[10] as String,
                    isBiometricEnabled = it[11] as Boolean,
                    securityQuestion = it[12] as String,
                    securityAnswer = it[13] as String,
                    lockTimeoutSeconds = it[14] as Int,
                    recentColorsHsv = recentColors
                )
            }
        )
    }
}

val LocalAppSettings = staticCompositionLocalOf<AppSettingsState> {
    error("No AppSettingsState provided")
}

// ── Composable theme ─────────────────────────────────────────────
@Composable
fun NotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val settings = LocalAppSettings.current
    val isDark = settings.darkMode
    val accent = settings.accentColor
    
    val isMono = accent == Color.Black || accent == Color.White || accent == MonoBlack || accent == MonoWhite

    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White

    // Generate dynamic color schemes based on the selected accent color
    // We derive background and surface colors from the accent to create a cohesive look
    val colors = if (isDark) {
        val baseDark = if (isMono) Color.Black else Color(0xFF0F140F) // Very dark neutral
        val tintedDark = if (isMono) baseDark else accent.copy(alpha = 0.08f).compositeOver(baseDark)
        val tintedSurface = if (isMono) Color(0xFF1E1E1E) else accent.copy(alpha = 0.12f).compositeOver(baseDark)
        
        darkColorScheme(
            primary               = if (isMono) Color.White else accent,
            onPrimary             = if (isMono) Color.Black else onAccent,
            primaryContainer      = if (isMono) Color(0xFF333333) else accent.copy(alpha = 0.25f),
            onPrimaryContainer    = if (isMono) Color.White else accent,
            secondary             = if (isMono) Color.LightGray else accent.copy(alpha = 0.8f),
            onSecondary           = Color.Black,
            secondaryContainer    = if (isMono) Color(0xFF3A3A3A) else accent.copy(alpha = 0.2f),
            onSecondaryContainer  = Color.White,
            tertiary              = if (isMono) Color.Gray else accent.copy(alpha = 0.6f),
            onTertiary            = Color.White,
            background            = tintedDark,
            onBackground          = Color.White,
            surface               = tintedSurface,
            onSurface             = Color.White,
            surfaceVariant        = if (isMono) Color(0xFF2C2C2C) else accent.copy(alpha = 0.15f).compositeOver(baseDark),
            onSurfaceVariant      = Color(0xFFB0B0B0),
            outline               = if (isMono) Color.DarkGray else accent.copy(alpha = 0.4f),
            outlineVariant        = if (isMono) Color(0xFF333333) else accent.copy(alpha = 0.2f),
            surfaceContainer      = tintedSurface,
            surfaceContainerLow   = tintedDark,
            surfaceContainerHigh  = if (isMono) Color(0xFF252525) else accent.copy(alpha = 0.18f).compositeOver(baseDark),
            error                 = Color(0xFFFFB4AB),
            onError               = Color(0xFF690005),
        )
    } else {
        val baseLight = if (isMono) Color.White else Color(0xFFF7F9F7) // Very light neutral
        val tintedLight = if (isMono) baseLight else accent.copy(alpha = 0.04f).compositeOver(baseLight)
        val tintedSurface = Color.White
        
        lightColorScheme(
            primary               = if (isMono) Color.Black else accent,
            onPrimary             = if (isMono) Color.White else onAccent,
            primaryContainer      = if (isMono) Color(0xFFEEEEEE) else accent.copy(alpha = 0.12f),
            onPrimaryContainer    = if (isMono) Color.Black else accent,
            secondary             = if (isMono) Color.DarkGray else accent.copy(alpha = 0.7f),
            onSecondary           = Color.White,
            secondaryContainer    = if (isMono) Color(0xFFF5F5F5) else accent.copy(alpha = 0.1f),
            onSecondaryContainer  = Color.Black,
            tertiary              = if (isMono) Color.Gray else accent.copy(alpha = 0.5f),
            onTertiary            = Color.White,
            background            = tintedLight,
            onBackground          = Color.Black,
            surface               = tintedSurface,
            onSurface             = Color.Black,
            surfaceVariant        = if (isMono) Color(0xFFF0F0F0) else accent.copy(alpha = 0.08f).compositeOver(baseLight),
            onSurfaceVariant      = Color(0xFF404040),
            outline               = if (isMono) Color.LightGray else accent.copy(alpha = 0.3f),
            outlineVariant        = if (isMono) Color(0xFFE0E0E0) else accent.copy(alpha = 0.15f),
            surfaceContainer      = if (isMono) Color(0xFFF8F8F8) else accent.copy(alpha = 0.06f).compositeOver(baseLight),
            surfaceContainerLow   = Color.White,
            surfaceContainerHigh  = if (isMono) Color(0xFFF0F0F0) else accent.copy(alpha = 0.1f).compositeOver(baseLight),
            error                 = Color(0xFFBA1A1A),
            onError               = Color.White,
        )
    }
    
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !isDark
        }
    }

    val selectedFontFamily = when (settings.fontTheme) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }

    // ── Typography Dynamic Color Linking ─────────────────────────
    val typography = NotesTypography.copy(
        displayLarge   = NotesTypography.displayLarge.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        displayMedium  = NotesTypography.displayMedium.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        displaySmall   = NotesTypography.displaySmall.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        headlineLarge  = NotesTypography.headlineLarge.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        headlineMedium = NotesTypography.headlineMedium.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        headlineSmall  = NotesTypography.headlineSmall.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        titleLarge     = NotesTypography.titleLarge.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        titleMedium    = NotesTypography.titleMedium.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        titleSmall     = NotesTypography.titleSmall.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        bodyLarge      = NotesTypography.bodyLarge.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        bodyMedium     = NotesTypography.bodyMedium.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        bodySmall      = NotesTypography.bodySmall.copy(color = colors.onSurface, fontFamily = selectedFontFamily),
        labelLarge     = NotesTypography.labelLarge.copy(color = colors.onSurfaceVariant, fontFamily = selectedFontFamily),
        labelMedium    = NotesTypography.labelMedium.copy(color = colors.onSurfaceVariant, fontFamily = selectedFontFamily),
        labelSmall     = NotesTypography.labelSmall.copy(color = colors.onSurfaceVariant, fontFamily = selectedFontFamily),
    )

    MaterialTheme(
        colorScheme = colors,
        typography  = typography,
        content     = content
    )
}

