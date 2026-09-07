// lib/core/providers/settings_provider.dart
//
// Setara dengan AppSettingsState + SharedPreferences di MainActivity.kt.
// Riverpod AsyncNotifier menggantikan LaunchedEffect + prefs.edit().

import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

// ── Model ────────────────────────────────────────────────────────

class AppSettings {
  final bool darkMode;
  final String language;       // 'en' | 'id'
  final Color accentColor;
  final String nickname;
  final bool isGridView;
  final String fontTheme;      // 'Modern' | 'Classic' | 'Mono'
  final bool isNoteLockEnabled;
  final String noteLockPin;
  final bool isBiometricEnabled;
  final String securityQuestion;
  final String securityAnswer;
  final int lockTimeoutSeconds;
  final String lastBackup;

  const AppSettings({
    this.darkMode = false,
    this.language = 'en',
    this.accentColor = const Color(0xFF3D7A3D),
    this.nickname = '',
    this.isGridView = false,
    this.fontTheme = 'Modern',
    this.isNoteLockEnabled = false,
    this.noteLockPin = '',
    this.isBiometricEnabled = false,
    this.securityQuestion = '',
    this.securityAnswer = '',
    this.lockTimeoutSeconds = 0,
    this.lastBackup = '-',
  });

  AppSettings copyWith({
    bool? darkMode,
    String? language,
    Color? accentColor,
    String? nickname,
    bool? isGridView,
    String? fontTheme,
    bool? isNoteLockEnabled,
    String? noteLockPin,
    bool? isBiometricEnabled,
    String? securityQuestion,
    String? securityAnswer,
    int? lockTimeoutSeconds,
    String? lastBackup,
  }) {
    return AppSettings(
      darkMode: darkMode ?? this.darkMode,
      language: language ?? this.language,
      accentColor: accentColor ?? this.accentColor,
      nickname: nickname ?? this.nickname,
      isGridView: isGridView ?? this.isGridView,
      fontTheme: fontTheme ?? this.fontTheme,
      isNoteLockEnabled: isNoteLockEnabled ?? this.isNoteLockEnabled,
      noteLockPin: noteLockPin ?? this.noteLockPin,
      isBiometricEnabled: isBiometricEnabled ?? this.isBiometricEnabled,
      securityQuestion: securityQuestion ?? this.securityQuestion,
      securityAnswer: securityAnswer ?? this.securityAnswer,
      lockTimeoutSeconds: lockTimeoutSeconds ?? this.lockTimeoutSeconds,
      lastBackup: lastBackup ?? this.lastBackup,
    );
  }
}

// ── Notifier ─────────────────────────────────────────────────────

class SettingsNotifier extends AsyncNotifier<AppSettings> {
  static const _keys = _PrefsKeys();

  @override
  Future<AppSettings> build() async {
    final prefs = await SharedPreferences.getInstance();
    return _load(prefs);
  }

  AppSettings _load(SharedPreferences p) {
    return AppSettings(
      darkMode: p.getBool(_keys.darkMode) ?? false,
      language: p.getString(_keys.language) ?? 'en',
      accentColor: Color(p.getInt(_keys.accentColor) ?? const Color(0xFF3D7A3D).value),
      nickname: p.getString(_keys.nickname) ?? '',
      isGridView: p.getBool(_keys.isGridView) ?? false,
      fontTheme: p.getString(_keys.fontTheme) ?? 'Modern',
      isNoteLockEnabled: p.getBool(_keys.isNoteLockEnabled) ?? false,
      noteLockPin: p.getString(_keys.noteLockPin) ?? '',
      isBiometricEnabled: p.getBool(_keys.isBiometricEnabled) ?? false,
      securityQuestion: p.getString(_keys.securityQuestion) ?? '',
      securityAnswer: p.getString(_keys.securityAnswer) ?? '',
      lockTimeoutSeconds: p.getInt(_keys.lockTimeoutSeconds) ?? 0,
      lastBackup: p.getString(_keys.lastBackup) ?? '-',
    );
  }

  Future<void> _save(AppSettings s) async {
    final prefs = await SharedPreferences.getInstance();
    await Future.wait([
      prefs.setBool(_keys.darkMode, s.darkMode),
      prefs.setString(_keys.language, s.language),
      prefs.setInt(_keys.accentColor, s.accentColor.value),
      prefs.setString(_keys.nickname, s.nickname),
      prefs.setBool(_keys.isGridView, s.isGridView),
      prefs.setString(_keys.fontTheme, s.fontTheme),
      prefs.setBool(_keys.isNoteLockEnabled, s.isNoteLockEnabled),
      prefs.setString(_keys.noteLockPin, s.noteLockPin),
      prefs.setBool(_keys.isBiometricEnabled, s.isBiometricEnabled),
      prefs.setString(_keys.securityQuestion, s.securityQuestion),
      prefs.setString(_keys.securityAnswer, s.securityAnswer),
      prefs.setInt(_keys.lockTimeoutSeconds, s.lockTimeoutSeconds),
      prefs.setString(_keys.lastBackup, s.lastBackup),
    ]);
  }

  Future<void> _update(AppSettings Function(AppSettings) updater) async {
    final current = state.valueOrNull;
    if (current == null) return;
    final next = updater(current);
    state = AsyncData(next);
    await _save(next);
  }

  // ── Public setters ───────────────────────────────────────────
  Future<void> setDarkMode(bool v) => _update((s) => s.copyWith(darkMode: v));
  Future<void> setLanguage(String v) => _update((s) => s.copyWith(language: v));
  Future<void> setAccentColor(Color v) => _update((s) => s.copyWith(accentColor: v));
  Future<void> setNickname(String v) => _update((s) => s.copyWith(nickname: v));
  Future<void> setGridView(bool v) => _update((s) => s.copyWith(isGridView: v));
  Future<void> setFontTheme(String v) => _update((s) => s.copyWith(fontTheme: v));
  Future<void> setNoteLockEnabled(bool v) => _update((s) => s.copyWith(isNoteLockEnabled: v));
  Future<void> setNoteLockPin(String v) => _update((s) => s.copyWith(noteLockPin: v));
  Future<void> setBiometricEnabled(bool v) => _update((s) => s.copyWith(isBiometricEnabled: v));
  Future<void> setSecurityQuestion(String v) => _update((s) => s.copyWith(securityQuestion: v));
  Future<void> setSecurityAnswer(String v) => _update((s) => s.copyWith(securityAnswer: v));
  Future<void> setLockTimeout(int v) => _update((s) => s.copyWith(lockTimeoutSeconds: v));
  Future<void> setLastBackup(String v) => _update((s) => s.copyWith(lastBackup: v));
}

// ── Provider ─────────────────────────────────────────────────────

final settingsProvider =
    AsyncNotifierProvider<SettingsNotifier, AppSettings>(
  SettingsNotifier.new,
  name: 'settingsProvider',
);

// ── Key constants ─────────────────────────────────────────────────

class _PrefsKeys {
  const _PrefsKeys();
  final darkMode = 'darkMode';
  final language = 'language';
  final accentColor = 'accentColor';
  final nickname = 'nickname';
  final isGridView = 'isGridView';
  final fontTheme = 'fontTheme';
  final isNoteLockEnabled = 'isNoteLockEnabled';
  final noteLockPin = 'noteLockPin';
  final isBiometricEnabled = 'isBiometricEnabled';
  final securityQuestion = 'securityQuestion';
  final securityAnswer = 'securityAnswer';
  final lockTimeoutSeconds = 'lockTimeoutSeconds';
  final lastBackup = 'lastBackup';
}
