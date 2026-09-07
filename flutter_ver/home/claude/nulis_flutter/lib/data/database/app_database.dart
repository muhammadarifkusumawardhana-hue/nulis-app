// lib/data/database/app_database.dart
//
// Setara dengan Database.kt di Kotlin (Room → Drift).
// Setelah edit file ini, jalankan:
//   flutter pub run build_runner build --delete-conflicting-outputs
// untuk generate app_database.g.dart

import 'dart:convert';
import 'package:drift/drift.dart';
import 'package:drift_flutter/drift_flutter.dart';
import 'package:flutter/foundation.dart';

import '../models/note.dart' as model;
import '../models/folder.dart' as model;

part 'app_database.g.dart';

// ── Tables ──────────────────────────────────────────────────────
// Setara dengan @Entity di Room

class Notes extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get title => text().withDefault(const Constant(''))();
  TextColumn get content => text().withDefault(const Constant(''))();
  IntColumn get folderId => integer().withDefault(const Constant(-1))();
  // Tags disimpan sebagai JSON string, sama seperti tagsJson di Kotlin
  TextColumn get tagsJson => text().withDefault(const Constant('[]'))();
  TextColumn get createdAt => text()();
  TextColumn get updatedAt => text()();
  BoolColumn get isPinned => boolean().withDefault(const Constant(false))();
  BoolColumn get isArchived => boolean().withDefault(const Constant(false))();
  BoolColumn get isLocked => boolean().withDefault(const Constant(false))();
  BoolColumn get isDeleted => boolean().withDefault(const Constant(false))();
  TextColumn get deletedAt => text().nullable()();
}

class Folders extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text()();
  TextColumn get colorHex =>
      text().withDefault(const Constant('#3D7A3D'))();
  TextColumn get icon => text().nullable()();
}

class Tags extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text().unique()();
}

// ── Database ─────────────────────────────────────────────────────
// Setara dengan AppDatabase : RoomDatabase() di Kotlin
// versi: 7 (sama dengan Room schema version terakhir)

@DriftDatabase(tables: [Notes, Folders, Tags])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 7;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (m) => m.createAll(),
        // fallbackToDestructiveMigration — sama seperti di AppContainer.kt
        onUpgrade: (m, from, to) async {
          await m.recreateAllViews();
        },
        beforeOpen: (details) async {
          await customStatement('PRAGMA foreign_keys = ON');
          await customStatement('PRAGMA journal_mode = WAL');
          await customStatement('PRAGMA synchronous = NORMAL');
          // WAL + NORMAL sync = performa lebih baik di perangkat low-end
        },
      );

  // ── Note Queries ────────────────────────────────────────────
  // Setara dengan NoteDao di Kotlin

  /// Semua catatan aktif, pin dulu lalu terbaru
  Stream<List<Note>> watchAllNotes() {
    return (select(notes)
          ..where((n) => n.isDeleted.equals(false) & n.isArchived.equals(false))
          ..orderBy([
            (n) => OrderingTerm.desc(n.isPinned),
            (n) => OrderingTerm.desc(n.updatedAt),
          ]))
        .watch()
        .map((rows) => rows.map(_rowToNote).toList());
  }

  /// Catatan berdasarkan folder
  Stream<List<Note>> watchNotesByFolder(int folderId) {
    return (select(notes)
          ..where((n) =>
              n.folderId.equals(folderId) &
              n.isArchived.equals(false) &
              n.isDeleted.equals(false))
          ..orderBy([
            (n) => OrderingTerm.desc(n.isPinned),
            (n) => OrderingTerm.desc(n.updatedAt),
          ]))
        .watch()
        .map((rows) => rows.map(_rowToNote).toList());
  }

  /// Catatan tanpa folder
  Stream<List<Note>> watchNotesWithoutFolder() {
    return (select(notes)
          ..where((n) =>
              n.folderId.equals(-1) &
              n.isArchived.equals(false) &
              n.isDeleted.equals(false))
          ..orderBy([
            (n) => OrderingTerm.desc(n.isPinned),
            (n) => OrderingTerm.desc(n.updatedAt),
          ]))
        .watch()
        .map((rows) => rows.map(_rowToNote).toList());
  }

  /// Full-text search di judul, isi, dan tags
  Stream<List<Note>> watchSearch(String query) {
    final q = '%$query%';
    return (select(notes)
          ..where((n) =>
              (n.title.like(q) | n.content.like(q) | n.tagsJson.like(q)) &
              n.isArchived.equals(false) &
              n.isDeleted.equals(false))
          ..orderBy([(n) => OrderingTerm.desc(n.updatedAt)]))
        .watch()
        .map((rows) => rows.map(_rowToNote).toList());
  }

  /// Filter berdasarkan tag
  Stream<List<Note>> watchNotesByTag(String tag) {
    return (select(notes)
          ..where((n) =>
              n.tagsJson.like('%"$tag"%') &
              n.isArchived.equals(false) &
              n.isDeleted.equals(false))
          ..orderBy([(n) => OrderingTerm.desc(n.updatedAt)]))
        .watch()
        .map((rows) => rows.map(_rowToNote).toList());
  }

  /// Draft (judul & isi kosong)
  Stream<List<Note>> watchDrafts() {
    return (select(notes)
          ..where((n) =>
              n.title.equals('') &
              n.content.equals('') &
              n.isArchived.equals(false) &
              n.isDeleted.equals(false))
          ..orderBy([(n) => OrderingTerm.desc(n.updatedAt)]))
        .watch()
        .map((rows) => rows.map(_rowToNote).toList());
  }

  Stream<List<Note>> watchArchivedNotes() {
    return (select(notes)
          ..where((n) => n.isArchived.equals(true) & n.isDeleted.equals(false))
          ..orderBy([(n) => OrderingTerm.desc(n.updatedAt)]))
        .watch()
        .map((rows) => rows.map(_rowToNote).toList());
  }

  Stream<List<Note>> watchDeletedNotes() {
    return (select(notes)
          ..where((n) => n.isDeleted.equals(true))
          ..orderBy([(n) => OrderingTerm.desc(n.deletedAt)]))
        .watch()
        .map((rows) => rows.map(_rowToNote).toList());
  }

  Future<model.Note?> getNoteById(int id) async {
    final row = await (select(notes)..where((n) => n.id.equals(id)))
        .getSingleOrNull();
    return row == null ? null : _rowToNote(row);
  }

  Future<int> insertNote(NotesCompanion companion) =>
      into(notes).insert(companion);

  Future<void> updateNote(NotesCompanion companion) =>
      (update(notes)..where((n) => n.id.equals(companion.id.value)))
          .write(companion);

  Future<void> deleteNoteById(int id) =>
      (delete(notes)..where((n) => n.id.equals(id))).go();

  /// Purge trash > 30 hari — setara dengan purgeOldDeletedNotes() di Kotlin
  Future<void> purgeOldDeletedNotes(DateTime threshold) {
    return (delete(notes)
          ..where((n) =>
              n.isDeleted.equals(true) &
              n.deletedAt.isSmallerOrEqual(
                  Variable(threshold.toIso8601String()))))
        .go();
  }

  // ── Folder Queries ──────────────────────────────────────────

  Stream<List<model.Folder>> watchAllFolders() {
    return (select(folders)
          ..orderBy([(f) => OrderingTerm.asc(f.name)]))
        .watch()
        .map((rows) => rows.map(_rowToFolder).toList());
  }

  Future<int> insertFolder(FoldersCompanion companion) =>
      into(folders).insert(companion);

  Future<void> updateFolder(FoldersCompanion companion) =>
      (update(folders)..where((f) => f.id.equals(companion.id.value)))
          .write(companion);

  Future<void> deleteFolderById(int id) =>
      (delete(folders)..where((f) => f.id.equals(id))).go();

  // ── Tag Queries ─────────────────────────────────────────────

  Stream<List<String>> watchAllTagNames() {
    return (select(tags)..orderBy([(t) => OrderingTerm.asc(t.name)]))
        .watch()
        .map((rows) => rows.map((r) => r.name).toList());
  }

  Future<void> insertTagIfNotExists(String name) async {
    await into(tags).insertOnConflictUpdate(
      TagsCompanion(name: Value(name.trim().toLowerCase())),
    );
  }

  Future<void> deleteTagByName(String name) =>
      (delete(tags)..where((t) => t.name.equals(name))).go();

  // ── Helpers ─────────────────────────────────────────────────

  model.Note _rowToNote(Note row) {
    List<String> parsedTags = [];
    try {
      final decoded = jsonDecode(row.tagsJson) as List;
      parsedTags = decoded
          .map((e) => e.toString().trim().toLowerCase())
          .where((t) => t.isNotEmpty)
          .toList();
    } catch (_) {}

    return model.Note(
      id: row.id,
      title: row.title,
      content: row.content,
      folderId: row.folderId,
      tags: parsedTags,
      createdAt: DateTime.tryParse(row.createdAt) ?? DateTime.now(),
      updatedAt: DateTime.tryParse(row.updatedAt) ?? DateTime.now(),
      isPinned: row.isPinned,
      isArchived: row.isArchived,
      isLocked: row.isLocked,
      isDeleted: row.isDeleted,
      deletedAt: row.deletedAt != null
          ? DateTime.tryParse(row.deletedAt!)
          : null,
    );
  }

  model.Folder _rowToFolder(Folder row) {
    return model.Folder(
      id: row.id,
      name: row.name,
      colorHex: row.colorHex,
      icon: row.icon,
    );
  }
}

// ── Connection ───────────────────────────────────────────────────
// Setara dengan AppContainer.kt (singleton database)

QueryExecutor _openConnection() {
  return driftDatabase(name: 'notes');
  // Drift secara otomatis menyimpan di lokasi yang tepat
  // di Android: getDatabasePath('notes.db') — sama persis dengan Room
}

// ── Companion helpers ────────────────────────────────────────────
// Fungsi pembantu untuk membuat Companion dari model

NotesCompanion noteToCompanion(model.Note note, {bool isNew = false}) {
  final tagsJson = jsonEncode(note.tags);
  final now = DateTime.now().toIso8601String();
  return NotesCompanion(
    id: isNew ? const Value.absent() : Value(note.id),
    title: Value(note.title),
    content: Value(note.content),
    folderId: Value(note.folderId),
    tagsJson: Value(tagsJson),
    createdAt: Value(isNew ? now : note.createdAt.toIso8601String()),
    updatedAt: Value(now),
    isPinned: Value(note.isPinned),
    isArchived: Value(note.isArchived),
    isLocked: Value(note.isLocked),
    isDeleted: Value(note.isDeleted),
    deletedAt: Value(note.deletedAt?.toIso8601String()),
  );
}

FoldersCompanion folderToCompanion(model.Folder folder,
    {bool isNew = false}) {
  return FoldersCompanion(
    id: isNew ? const Value.absent() : Value(folder.id),
    name: Value(folder.name),
    colorHex: Value(folder.colorHex),
    icon: Value(folder.icon),
  );
}
