// lib/main.dart
//
// Entry point — setara dengan MainActivity.kt.
// ProviderScope menggantikan peran AppContainer (singleton DI).

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/providers/settings_provider.dart';
import 'core/theme/app_theme.dart';
import 'ui/app_router.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Edge-to-edge — setara dengan WindowCompat.setDecorFitsSystemWindows
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      systemNavigationBarColor: Colors.transparent,
    ),
  );
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

  runApp(
    const ProviderScope(
      child: NulisApp(),
    ),
  );
}

class NulisApp extends ConsumerWidget {
  const NulisApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settingsAsync = ref.watch(settingsProvider);

    return settingsAsync.when(
      loading: () => const MaterialApp(
        home: Scaffold(body: Center(child: CircularProgressIndicator())),
      ),
      error: (e, _) => MaterialApp(
        home: Scaffold(body: Center(child: Text('Error: $e'))),
      ),
      data: (settings) {
        return MaterialApp.router(
          title: 'Nulis',
          debugShowCheckedModeBanner: false,
          themeMode: settings.darkMode ? ThemeMode.dark : ThemeMode.light,
          theme: AppTheme.light(
            fontTheme: settings.fontTheme,
            accentColor: settings.accentColor,
          ),
          darkTheme: AppTheme.dark(
            fontTheme: settings.fontTheme,
            accentColor: settings.accentColor,
          ),
          routerConfig: appRouter,
        );
      },
    );
  }
}
