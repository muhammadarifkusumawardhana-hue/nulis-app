// lib/core/providers/database_providers.dart
//
// Setara dengan AppContainer.kt di Kotlin.
// Riverpod menangani singleton lifecycle secara otomatis —
// tidak perlu synchronized {} atau manual null check.

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/database/app_database.dart';
import '../../data/repositories/note_repository.dart';
import '../../data/repositories/folder_repository.dart';

// ── Database singleton ───────────────────────────────────────────
// keepAlive: true = tidak pernah di-dispose selama app hidup

final databaseProvider = Provider<AppDatabase>(
  (ref) {
    final db = AppDatabase();
    ref.onDispose(db.close); // Tutup koneksi saat app dispose
    return db;
  },
  name: 'databaseProvider',
);

// ── Repositories ─────────────────────────────────────────────────

final noteRepositoryProvider = Provider<NoteRepository>(
  (ref) => NoteRepository(ref.watch(databaseProvider)),
  name: 'noteRepositoryProvider',
);

final folderRepositoryProvider = Provider<FolderRepository>(
  (ref) => FolderRepository(
    ref.watch(databaseProvider),
    ref.watch(noteRepositoryProvider),
  ),
  name: 'folderRepositoryProvider',
);

// ── Tag names stream ─────────────────────────────────────────────

final allTagNamesProvider = StreamProvider<List<String>>(
  (ref) => ref.watch(databaseProvider).watchAllTagNames(),
  name: 'allTagNamesProvider',
);
