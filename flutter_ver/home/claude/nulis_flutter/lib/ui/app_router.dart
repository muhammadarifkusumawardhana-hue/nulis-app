// lib/ui/app_router.dart
//
// Setara dengan navigasi sealed class Screen di MainActivity.kt.
// go_router lebih ringan dari Navigation Compose dan mendukung
// deep link secara built-in.

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'home/home_page.dart';
import 'editor/editor_page.dart';
import 'settings/settings_page.dart';

// ── Route names ───────────────────────────────────────────────────
// Setara dengan sealed class Screen { Home, Editor }

class AppRoutes {
  AppRoutes._();
  static const home     = '/';
  static const editor   = '/editor';
  static const settings = '/settings';
}

// ── Router ────────────────────────────────────────────────────────

final appRouter = GoRouter(
  initialLocation: AppRoutes.home,
  routes: [
    GoRoute(
      path: AppRoutes.home,
      pageBuilder: (context, state) => _fadePage(
        key: state.pageKey,
        child: const HomePage(),
      ),
    ),
    GoRoute(
      path: AppRoutes.editor,
      pageBuilder: (context, state) {
        // Setara dengan Screen.Editor(noteId, folderId, key) di Kotlin
        final noteId   = int.tryParse(state.uri.queryParameters['noteId'] ?? '') ?? -1;
        final folderId = int.tryParse(state.uri.queryParameters['folderId'] ?? '') ?? -1;
        return _fadePage(
          key: state.pageKey,
          child: EditorPage(noteId: noteId, folderId: folderId),
        );
      },
    ),
    GoRoute(
      path: AppRoutes.settings,
      pageBuilder: (context, state) => _slidePage(
        key: state.pageKey,
        child: const SettingsPage(),
      ),
    ),
  ],
);

// ── Page transitions ───────────────────────────────────────────────
// Setara dengan AnimatedContent + tween(300) di MainActivity.kt

CustomTransitionPage<void> _fadePage({
  required LocalKey key,
  required Widget child,
}) {
  return CustomTransitionPage<void>(
    key: key,
    child: child,
    transitionDuration: const Duration(milliseconds: 280),
    transitionsBuilder: (context, animation, secondaryAnimation, child) {
      return FadeTransition(
        opacity: CurveTween(curve: Curves.easeInOut).animate(animation),
        child: child,
      );
    },
  );
}

CustomTransitionPage<void> _slidePage({
  required LocalKey key,
  required Widget child,
}) {
  return CustomTransitionPage<void>(
    key: key,
    child: child,
    transitionDuration: const Duration(milliseconds: 300),
    transitionsBuilder: (context, animation, secondaryAnimation, child) {
      return SlideTransition(
        position: Tween(
          begin: const Offset(1.0, 0.0),
          end: Offset.zero,
        ).chain(CurveTween(curve: Curves.easeOutCubic)).animate(animation),
        child: child,
      );
    },
  );
}

// ── Navigation helpers ────────────────────────────────────────────
// Extension untuk navigasi yang lebih bersih di widget tree

extension AppNavigation on BuildContext {
  void goHome() => go(AppRoutes.home);

  void goEditor({int noteId = -1, int folderId = -1}) {
    go('${AppRoutes.editor}?noteId=$noteId&folderId=$folderId');
  }

  void goSettings() => go(AppRoutes.settings);
}
