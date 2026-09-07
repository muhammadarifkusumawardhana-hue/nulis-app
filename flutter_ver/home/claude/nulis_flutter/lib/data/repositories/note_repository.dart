// lib/data/repositories/note_repository.dart
//
// Repository pattern — memisahkan logika data dari UI.
// Di Kotlin, logika ini tersebar di HomeViewModel & EditorViewModel
// langsung mengakses db. Di Flutter kita pisahkan agar lebih bersih
// dan mudah di-test.

import 'dart:convert';
import 'package:drift/drift.dart';

import '../database/app_database.dart';
import '../models/note.dart';

class NoteRepository {
  final AppDatabase _db;

  const NoteRepository(this._db);

  // ── Streams (reactive) ───────────────────────────────────────
  // Setara dengan Flow<List<NoteEntity>> di Kotlin DAO

  Stream<List<Note>> watchAll() => _db.watchAllNotes();
  Stream<List<Note>> watchByFolder(int folderId) =>
      _db.watchNotesByFolder(folderId);
  Stream<List<Note>> watchWithoutFolder() => _db.watchNotesWithoutFolder();
  Stream<List<Note>> watchSearch(String query) => _db.watchSearch(query);
  Stream<List<Note>> watchByTag(String tag) => _db.watchNotesByTag(tag);
  Stream<List<Note>> watchDrafts() => _db.watchDrafts();
  Stream<List<Note>> watchArchived() => _db.watchArchivedNotes();
  Stream<List<Note>> watchDeleted() => _db.watchDeletedNotes();

  // ── One-shot reads ───────────────────────────────────────────

  Future<Note?> getById(int id) => _db.getNoteById(id);

  // ── Write operations ─────────────────────────────────────────

  /// Insert catatan baru, kembalikan id yang digenerate
  Future<int> insert(Note note) {
    final companion = noteToCompanion(note, isNew: true);
    return _db.insertNote(companion);
  }

  /// Update catatan yang sudah ada
  Future<void> update(Note note) {
    final companion = noteToCompanion(note);
    return _db.updateNote(companion);
  }

  /// Soft-delete: pindahkan ke Trash
  /// Setara dengan logika deleteNote() di HomeViewModel.kt
  Future<void> softDelete(int id) async {
    final note = await getById(id);
    if (note == null) return;
    if (note.isDeleted) {
      // Sudah di trash → hapus permanen
      await _db.deleteNoteById(id);
    } else {
      await update(note.copyWith(
        isDeleted: true,
        deletedAt: DateTime.now(),
      ));
    }
  }

  /// Restore dari Trash
  Future<void> restore(int id) async {
    final note = await getById(id);
    if (note == null) return;
    await update(note.copyWith(
      isDeleted: false,
      clearDeletedAt: true,
    ));
  }

  /// Toggle pin
  Future<void> togglePin(Note note) =>
      update(note.copyWith(isPinned: !note.isPinned));

  /// Toggle arsip
  Future<void> toggleArchive(Note note) =>
      update(note.copyWith(isArchived: !note.isArchived));

  /// Toggle lock
  Future<void> toggleLock(int id) async {
    final note = await getById(id);
    if (note == null) return;
    await update(note.copyWith(isLocked: !note.isLocked));
  }

  /// Pindah folder
  Future<void> moveToFolder(int noteId, int folderId) async {
    final note = await getById(noteId);
    if (note == null) return;
    await update(note.copyWith(folderId: folderId));
  }

  /// Purge trash > 30 hari — dipanggil saat app start
  Future<void> purgeOldTrash() {
    final threshold = DateTime.now().subtract(const Duration(days: 30));
    return _db.purgeOldDeletedNotes(threshold);
  }

  // ── Batch operations (untuk selection mode di HomeScreen) ────

  Future<void> softDeleteMany(List<int> ids) async {
    for (final id in ids) {
      await softDelete(id);
    }
  }

  Future<void> restoreMany(List<int> ids) async {
    for (final id in ids) {
      await restore(id);
    }
  }

  Future<void> archiveMany(List<int> ids) async {
    for (final id in ids) {
      final note = await getById(id);
      if (note != null) await update(note.copyWith(isArchived: true));
    }
  }

  Future<void> unarchiveMany(List<int> ids) async {
    for (final id in ids) {
      final note = await getById(id);
      if (note != null) await update(note.copyWith(isArchived: false));
    }
  }

  Future<void> moveManyToFolder(List<int> ids, int folderId) async {
    for (final id in ids) {
      await moveToFolder(id, folderId);
    }
  }
}
