// lib/ui/editor/editor_notifier.dart
//
// Setara dengan EditorViewModel.kt di Kotlin.
// AsyncNotifier menggantikan ViewModel + StateFlow.

import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers/database_providers.dart';
import '../../core/utils/app_utils.dart';
import '../../data/models/note.dart';
import '../../data/repositories/note_repository.dart';

// ── Editor state ───────────────────────────────────────────────
// Setara dengan EditorState data class di EditorViewModel.kt

class EditorState {
  final int id;
  final String title;
  final String content;
  final TextEditingController contentController;
  final int folderId;
  final List<String> tags;
  final List<String> allTags;
  final bool isPinned;
  final bool isArchived;
  final bool isDeleted;
  final bool isLocked;
  final bool isAuthenticated;
  final bool isLoading;
  final bool isPreview;
  final int wordCount;
  final bool isSaved;

  const EditorState({
    this.id = 0,
    this.title = '',
    this.content = '',
    required this.contentController,
    this.folderId = -1,
    this.tags = const [],
    this.allTags = const [],
    this.isPinned = false,
    this.isArchived = false,
    this.isDeleted = false,
    this.isLocked = false,
    this.isAuthenticated = false,
    this.isLoading = true,
    this.isPreview = false,
    this.wordCount = 0,
    this.isSaved = true,
  });

  EditorState copyWith({
    int? id,
    String? title,
    String? content,
    int? folderId,
    List<String>? tags,
    List<String>? allTags,
    bool? isPinned,
    bool? isArchived,
    bool? isDeleted,
    bool? isLocked,
    bool? isAuthenticated,
    bool? isLoading,
    bool? isPreview,
    int? wordCount,
    bool? isSaved,
  }) {
    return EditorState(
      id: id ?? this.id,
      title: title ?? this.title,
      content: content ?? this.content,
      contentController: contentController,
      folderId: folderId ?? this.folderId,
      tags: tags ?? this.tags,
      allTags: allTags ?? this.allTags,
      isPinned: isPinned ?? this.isPinned,
      isArchived: isArchived ?? this.isArchived,
      isDeleted: isDeleted ?? this.isDeleted,
      isLocked: isLocked ?? this.isLocked,
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      isLoading: isLoading ?? this.isLoading,
      isPreview: isPreview ?? this.isPreview,
      wordCount: wordCount ?? this.wordCount,
      isSaved: isSaved ?? this.isSaved,
    );
  }
}

// ── Notifier ────────────────────────────────────────────────────

class EditorNotifier extends FamilyNotifier<EditorState, (int, int)> {
  // arg = (noteId, folderId) — setara dengan factory parameter di Kotlin
  late NoteRepository _noteRepo;
  late int _noteId;
  late int _defaultFolderId;
  Timer? _autoSaveTimer;
  final _controller = TextEditingController();
  StreamSubscription<List<String>>? _tagsSub;

  @override
  EditorState build((int, int) arg) {
    _noteId = arg.$1;
    _defaultFolderId = arg.$2;
    _noteRepo = ref.watch(noteRepositoryProvider);

    ref.onDispose(() {
      _autoSaveTimer?.cancel();
      _tagsSub?.cancel();
      _controller.dispose();
    });

    _loadNote();
    _loadTags();

    return EditorState(contentController: _controller);
  }

  // ── Load ────────────────────────────────────────────────────

  Future<void> _loadNote() async {
    if (_noteId > 0) {
      final note = await _noteRepo.getById(_noteId);
      if (note != null) {
        _controller.text = note.content;
        state = state.copyWith(
          id: note.id,
          title: note.title,
          content: note.content,
          folderId: note.folderId,
          tags: note.tags,
          isPinned: note.isPinned,
          isArchived: note.isArchived,
          isDeleted: note.isDeleted,
          isLocked: note.isLocked,
          isAuthenticated: false,
          isLoading: false,
          wordCount: countWords(note.content),
        );
        return;
      }
    }
    // Catatan baru
    state = state.copyWith(folderId: _defaultFolderId, isLoading: false);
  }

  void _loadTags() {
    _tagsSub = ref
        .read(databaseProvider)
        .watchAllTagNames()
        .listen((tags) => state = state.copyWith(allTags: tags));
  }

  // ── Editing ─────────────────────────────────────────────────

  void onTitleChange(String v) {
    if (state.isDeleted) return;
    state = state.copyWith(title: v, isSaved: false);
    _scheduleAutoSave();
  }

  void onContentChange(String v) {
    if (state.isDeleted) return;
    state = state.copyWith(
        content: v, isSaved: false, wordCount: countWords(v));
    _scheduleAutoSave();
  }

  void togglePreview() =>
      state = state.copyWith(isPreview: !state.isPreview);

  void setAuthenticated(bool v) =>
      state = state.copyWith(isAuthenticated: v);

  void toggleLock() {
    if (state.isDeleted) return;
    state = state.copyWith(
        isLocked: !state.isLocked,
        isAuthenticated: true,
        isSaved: false);
    _scheduleAutoSave();
  }

  void addTag(String tag) {
    if (state.isDeleted) return;
    final trimmed =
        tag.trim().toLowerCase().replaceFirst(RegExp('^#'), '');
    if (trimmed.isEmpty || state.tags.contains(trimmed)) return;
    state = state.copyWith(
        tags: [...state.tags, trimmed], isSaved: false);
    ref.read(databaseProvider).insertTagIfNotExists(trimmed);
    _scheduleAutoSave();
  }

  void removeTag(String tag) {
    if (state.isDeleted) return;
    state = state.copyWith(
        tags: state.tags.where((t) => t != tag).toList(), isSaved: false);
    _scheduleAutoSave();
  }

  void renameTag(String oldTag, String newTag) {
    if (state.isDeleted) return;
    final trimmed =
        newTag.trim().toLowerCase().replaceFirst(RegExp('^#'), '');
    if (trimmed.isEmpty || trimmed == oldTag) return;
    state = state.copyWith(
      tags: state.tags.map((t) => t == oldTag ? trimmed : t).toList(),
      isSaved: false,
    );
    ref.read(databaseProvider).insertTagIfNotExists(trimmed);
    _scheduleAutoSave();
  }

  // ── Snippet insertion ─────────────────────────────────────────
  // Setara dengan insertSnippet() di EditorViewModel.kt

  void insertSnippet(String snippet) {
    if (state.isDeleted) return;
    final ctrl = _controller;
    final sel = ctrl.selection;
    if (!sel.isValid) {
      ctrl.text += snippet;
      return;
    }
    final result = insertSnippet(
      currentText: ctrl.text,
      selectionStart: sel.start,
      selectionEnd: sel.end,
      snippet: snippet,
    );
    ctrl.value = TextEditingValue(
      text: result.text,
      selection: TextSelection(
          baseOffset: result.cursorStart,
          extentOffset: result.cursorEnd),
    );
    onContentChange(result.text);
  }

  void insertImage(String uri) {
    if (state.isDeleted) return;
    final pattern = RegExp(r'!\[image-(\d+)\]');
    final matches = pattern.allMatches(state.content);
    final nextIndex = matches.isEmpty
        ? 1
        : matches.map((m) => int.tryParse(m.group(1) ?? '0') ?? 0).reduce((a, b) => a > b ? a : b) + 1;
    insertSnippet('\n![image-$nextIndex]($uri)\n');
  }

  // ── Save ──────────────────────────────────────────────────────
  // Setara dengan scheduleAutoSave() + doSave() di EditorViewModel.kt

  void _scheduleAutoSave() {
    if (state.isDeleted) return;
    _autoSaveTimer?.cancel();
    _autoSaveTimer = Timer(const Duration(milliseconds: 1500), _doSave);
  }

  void saveNow() {
    _autoSaveTimer?.cancel();
    _doSave();
  }

  Future<void> _doSave() async {
    final s = state;
    if (s.isDeleted) return;

    final now = DateTime.now();

    if (s.id == 0) {
      // INSERT baru
      if (s.title.trim().isEmpty && s.content.trim().isEmpty) return;
      final newNote = Note(
        title: s.title,
        content: s.content,
        folderId: s.folderId,
        tags: s.tags,
        createdAt: now,
        updatedAt: now,
        isPinned: s.isPinned,
        isArchived: s.isArchived,
        isLocked: s.isLocked,
      );
      final newId = await _noteRepo.insert(newNote);
      state = state.copyWith(id: newId, isSaved: true);
    } else {
      // UPDATE
      final existing = await _noteRepo.getById(s.id);
      if (existing == null) return;
      await _noteRepo.update(existing.copyWith(
        title: s.title,
        content: s.content,
        folderId: s.folderId,
        tags: s.tags,
        isPinned: s.isPinned,
        isArchived: s.isArchived,
        isLocked: s.isLocked,
        updatedAt: now,
      ));
      state = state.copyWith(isSaved: true);
    }
  }
}

// ── Provider ─────────────────────────────────────────────────────
// Family provider: satu instance per (noteId, folderId)
// Setara dengan key = "note_$id" di Kotlin

final editorProvider = NotifierProviderFamily<EditorNotifier, EditorState, (int, int)>(
  EditorNotifier.new,
  name: 'editorProvider',
);
