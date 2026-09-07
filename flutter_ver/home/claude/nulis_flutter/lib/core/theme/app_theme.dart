// lib/core/theme/app_theme.dart
//
// Setara dengan Theme.kt di Kotlin.
// Palet warna sage green dipertahankan persis sama.

import 'package:flutter/material.dart';

// ── Palette ───────────────────────────────────────────────────────
// Sama persis dengan nilai di Theme.kt

class NulisPalette {
  NulisPalette._();

  static const sage50  = Color(0xFFF0F7F0);
  static const sage100 = Color(0xFFDCEEDC);
  static const sage200 = Color(0xFFC2DEC2);
  static const sage600 = Color(0xFF3D7A3D);
  static const sage700 = Color(0xFF2C5F2C);
  static const sage800 = Color(0xFF1A3D1A);
  static const terracotta = Color(0xFFB85C38);
  static const cream = Color(0xFFFAF8F3);
  static const monoBlack = Color(0xFF121212);
  static const monoWhite = Color(0xFFF5F5F5);

  // Dark mode
  static const darkBg   = Color(0xFF111A11);
  static const darkSurf = Color(0xFF1C2B1C);
  static const darkSurf2 = Color(0xFF243224);
  static const darkPrim = Color(0xFF90C490);
  static const darkOnPrim = Color(0xFF0D2E0D);
  static const darkText = Color(0xFFE8F0E8);
}

// ── Color Schemes ─────────────────────────────────────────────────

const _lightScheme = ColorScheme(
  brightness: Brightness.light,
  primary: NulisPalette.sage600,
  onPrimary: Colors.white,
  primaryContainer: NulisPalette.sage100,
  onPrimaryContainer: NulisPalette.sage800,
  secondary: NulisPalette.terracotta,
  onSecondary: Colors.white,
  secondaryContainer: Color(0xFFFFE8DF),
  onSecondaryContainer: Color(0xFF6B2E14),
  tertiary: Color(0xFF5B6E5B),
  onTertiary: Colors.white,
  tertiaryContainer: Color(0xFFDEEBDE),
  onTertiaryContainer: NulisPalette.sage800,
  error: Color(0xFFBA1A1A),
  onError: Colors.white,
  errorContainer: Color(0xFFFFDAD6),
  onErrorContainer: Color(0xFF410002),
  surface: NulisPalette.cream,
  onSurface: NulisPalette.sage800,
  surfaceContainerLow: NulisPalette.cream,
  surfaceContainer: NulisPalette.sage100,
  surfaceContainerHigh: NulisPalette.sage200,
  onSurfaceVariant: NulisPalette.sage700,
  outline: Color(0xFF8BAC8B),
  outlineVariant: NulisPalette.sage200,
  shadow: Colors.black,
  scrim: Colors.black,
  inverseSurface: NulisPalette.sage800,
  onInverseSurface: NulisPalette.sage50,
  inversePrimary: NulisPalette.darkPrim,
);

const _darkScheme = ColorScheme(
  brightness: Brightness.dark,
  primary: NulisPalette.darkPrim,
  onPrimary: NulisPalette.darkOnPrim,
  primaryContainer: Color(0xFF1E3D1E),
  onPrimaryContainer: Color(0xFFB8DDB8),
  secondary: Color(0xFFE8A080),
  onSecondary: Color(0xFF4A1E08),
  secondaryContainer: Color(0xFF6B3018),
  onSecondaryContainer: Color(0xFFFFD0BC),
  tertiary: Color(0xFF9BB89B),
  onTertiary: Color(0xFF1A2E1A),
  tertiaryContainer: Color(0xFF2C3D2C),
  onTertiaryContainer: Color(0xFFB8D0B8),
  error: Color(0xFFFFB4AB),
  onError: Color(0xFF690005),
  errorContainer: Color(0xFF93000A),
  onErrorContainer: Color(0xFFFFDAD6),
  surface: NulisPalette.darkSurf,
  onSurface: NulisPalette.darkText,
  surfaceContainerLow: NulisPalette.darkSurf,
  surfaceContainer: NulisPalette.darkSurf2,
  surfaceContainerHigh: Color(0xFF2C3D2C),
  onSurfaceVariant: Color(0xFFA8C0A8),
  outline: Color(0xFF4A6B4A),
  outlineVariant: Color(0xFF2D472D),
  shadow: Colors.black,
  scrim: Colors.black,
  inverseSurface: NulisPalette.darkText,
  onInverseSurface: NulisPalette.darkSurf,
  inversePrimary: NulisPalette.sage600,
);

// ── Font Themes ───────────────────────────────────────────────────
// Setara dengan fontTheme choices di SettingsScreen.kt

TextTheme _buildTextTheme(String fontTheme) {
  final family = switch (fontTheme) {
    'Classic' => 'serif',
    'Mono'    => 'monospace',
    _         => null, // Modern = system sans-serif
  };
  final base = family != null
      ? TextTheme(
          bodyLarge: TextStyle(fontFamily: family),
          bodyMedium: TextStyle(fontFamily: family),
          bodySmall: TextStyle(fontFamily: family),
          titleLarge: TextStyle(fontFamily: family),
          titleMedium: TextStyle(fontFamily: family),
          titleSmall: TextStyle(fontFamily: family),
          labelLarge: TextStyle(fontFamily: family),
          labelMedium: TextStyle(fontFamily: family),
        )
      : const TextTheme();
  return base;
}

// ── Theme Builder ─────────────────────────────────────────────────

class AppTheme {
  AppTheme._();

  static ThemeData light({
    String fontTheme = 'Modern',
    Color? accentColor,
  }) {
    final scheme = accentColor != null
        ? _lightScheme.copyWith(primary: accentColor)
        : _lightScheme;
    return _build(scheme, fontTheme);
  }

  static ThemeData dark({
    String fontTheme = 'Modern',
    Color? accentColor,
  }) {
    final scheme = accentColor != null
        ? _darkScheme.copyWith(primary: accentColor)
        : _darkScheme;
    return _build(scheme, fontTheme);
  }

  static ThemeData _build(ColorScheme scheme, String fontTheme) {
    final textTheme = _buildTextTheme(fontTheme);
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      textTheme: textTheme,
      scaffoldBackgroundColor: scheme.surface,

      // AppBar transparan seperti di Theme.kt
      appBarTheme: AppBarTheme(
        backgroundColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        foregroundColor: scheme.onSurface,
        titleTextStyle: TextStyle(
          color: scheme.onSurface,
          fontSize: 20,
          fontWeight: FontWeight.w600,
        ),
      ),

      // Card
      cardTheme: CardTheme(
        color: scheme.surfaceContainerLow,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(color: scheme.outlineVariant, width: 0.5),
        ),
      ),

      // Chips (untuk tags dan folders)
      chipTheme: ChipThemeData(
        backgroundColor: scheme.surfaceContainer,
        selectedColor: scheme.primaryContainer,
        labelStyle: TextStyle(fontSize: 12, color: scheme.onSurface),
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
      ),

      // FAB
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: scheme.primaryContainer,
        foregroundColor: scheme.onPrimaryContainer,
        elevation: 2,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
      ),

      // Input fields (untuk search bar dan editor)
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: scheme.surfaceContainer,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: scheme.outlineVariant, width: 0.5),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: scheme.primary, width: 1.5),
        ),
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      ),

      // Dividers halus
      dividerTheme: DividerThemeData(
        color: scheme.outlineVariant,
        thickness: 0.5,
      ),

      // Bottom nav
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: scheme.surface,
        indicatorColor: scheme.primaryContainer,
        labelTextStyle: WidgetStateProperty.all(
          const TextStyle(fontSize: 12),
        ),
      ),
    );
  }
}
