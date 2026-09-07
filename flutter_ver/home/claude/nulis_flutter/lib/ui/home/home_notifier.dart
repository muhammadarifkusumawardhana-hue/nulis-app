// lib/ui/home/home_notifier.dart
//
// Setara dengan HomeViewModel.kt di Kotlin.
// Riverpod AsyncNotifier menggantikan ViewModel + StateFlow.

import 'dart:math';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers/database_providers.dart';
import '../../core/utils/app_utils.dart';
import '../../data/models/note.dart';
import '../../data/models/folder.dart';
import '../../data/repositories/note_repository.dart';
import '../../data/repositories/folder_repository.dart';

// ── Filter state ──────────────────────────────────────────────────
// Setara dengan sekumpulan MutableStateFlow di HomeViewModel.kt

enum NoteFilter { all, folder, tag, drafts, archived, trash, search }

class HomeFilter {
  final NoteFilter type;
  final int? folderId;
  final String? tag;
  final String searchQuery;

  const HomeFilter({
    this.type = NoteFilter.all,
    this.folderId,
    this.tag,
    this.searchQuery = '',
  });

  HomeFilter copyWith({
    NoteFilter? type,
    int? folderId,
    String? tag,
    String? searchQuery,
    bool clearFolder = false,
    bool clearTag = false,
  }) {
    return HomeFilter(
      type: type ?? this.type,
      folderId: clearFolder ? null : (folderId ?? this.folderId),
      tag: clearTag ? null : (tag ?? this.tag),
      searchQuery: searchQuery ?? this.searchQuery,
    );
  }
}

// ── Home state ────────────────────────────────────────────────────

class HomeState {
  final List<Note> notes;
  final List<Folder> folders;
  final List<String> tags;
  final HomeFilter filter;
  final Set<int> selectedNoteIds;
  final bool isSearchVisible;
  final int quoteIndex;
  final bool isLoading;

  const HomeState({
    this.notes = const [],
    this.folders = const [],
    this.tags = const [],
    this.filter = const HomeFilter(),
    this.selectedNoteIds = const {},
    this.isSearchVisible = false,
    this.quoteIndex = 0,
    this.isLoading = true,
  });

  bool get isSelectionMode => selectedNoteIds.isNotEmpty;
  bool get isTrash => filter.type == NoteFilter.trash;
  bool get isArchived => filter.type == NoteFilter.archived;

  HomeState copyWith({
    List<Note>? notes,
    List<Folder>? folders,
    List<String>? tags,
    HomeFilter? filter,
    Set<int>? selectedNoteIds,
    bool? isSearchVisible,
    int? quoteIndex,
    bool? isLoading,
  }) {
    return HomeState(
      notes: notes ?? this.notes,
      folders: folders ?? this.folders,
      tags: tags ?? this.tags,
      filter: filter ?? this.filter,
      selectedNoteIds: selectedNoteIds ?? this.selectedNoteIds,
      isSearchVisible: isSearchVisible ?? this.isSearchVisible,
      quoteIndex: quoteIndex ?? this.quoteIndex,
      isLoading: isLoading ?? this.isLoading,
    );
  }
}

// ── Notifier ──────────────────────────────────────────────────────

class HomeNotifier extends Notifier<HomeState> {
  late NoteRepository _noteRepo;
  late FolderRepository _folderRepo;

  @override
  HomeState build() {
    _noteRepo = ref.watch(noteRepositoryProvider);
    _folderRepo = ref.watch(folderRepositoryProvider);

    // Purge trash > 30 hari saat app start
    _noteRepo.purgeOldTrash();

    // Watch notes stream secara reaktif — setara dengan flatMapLatest di Kotlin
    _subscribeNotes(const HomeFilter());
    _subscribeFolders();
    _subscribeTags();

    return HomeState(quoteIndex: Random().nextInt(quoteCount));
  }

  // ── Stream subscriptions ─────────────────────────────────────

  void _subscribeNotes(HomeFilter filter) {
    final stream = _notesStream(filter);
    ref.listen(
      // Buat provider sementara dari stream
      StreamProvider.autoDispose((ref) => stream),
      (_, next) {
        next.whenData((notes) {
          state = state.copyWith(notes: notes, isLoading: false);
        });
      },
    );
    // Subscribe langsung
    stream.listen((notes) {
      state = state.copyWith(notes: notes, isLoading: false);
    });
  }

  void _subscribeFolders() {
    _folderRepo.watchAll().listen((folders) {
      state = state.copyWith(folders: folders);
    });
  }

  void _subscribeTags() {
    // Derive tags dari semua catatan aktif — sama dengan logic di HomeViewModel.kt
    _noteRepo.watchAll().listen((notes) {
      final tags = notes
          .expand((n) => n.tags)
          .map((t) => t.trim().toLowerCase())
          .where((t) => t.isNotEmpty)
          .toSet()
          .toList()
        ..sort();
      state = state.copyWith(tags: tags);
    });
  }

  Stream<List<Note>> _notesStream(HomeFilter filter) {
    return switch (filter.type) {
      NoteFilter.trash    => _noteRepo.watchDeleted(),
      NoteFilter.archived => _noteRepo.watchArchived(),
      NoteFilter.drafts   => _noteRepo.watchDrafts(),
      NoteFilter.search   => _noteRepo.watchSearch(filter.searchQuery),
      NoteFilter.tag      => _noteRepo.watchByTag(filter.tag ?? ''),
      NoteFilter.folder   => filter.folderId == -1
          ? _noteRepo.watchWithoutFolder()
          : _noteRepo.watchByFolder(filter.folderId ?? 0),
      NoteFilter.all      => _noteRepo.watchAll(),
    };
  }

  // ── Filter actions ────────────────────────────────────────────
  // Setara dengan selectFolder(), selectTag(), dll di HomeViewModel.kt

  void _applyFilter(HomeFilter filter) {
    state = state.copyWith(
      filter: filter,
      isSearchVisible: false,
      selectedNoteIds: {},
      isLoading: true,
    );
    _subscribeNotes(filter);
  }

  void clearAll() => _applyFilter(const HomeFilter());

  void selectFolder(int? id) => _applyFilter(HomeFilter(
        type: id == null ? NoteFilter.all : NoteFilter.folder,
        folderId: id,
      ));

  void selectTag(String? tag) => _applyFilter(HomeFilter(
        type: tag == null ? NoteFilter.all : NoteFilter.tag,
        tag: tag,
      ));

  void showDrafts() => _applyFilter(const HomeFilter(type: NoteFilter.drafts));
  void showArchived() =>
      _applyFilter(const HomeFilter(type: NoteFilter.archived));
  void showTrash() => _applyFilter(const HomeFilter(type: NoteFilter.trash));

  void toggleSearch() {
    if (state.isSearchVisible) {
      state = state.copyWith(isSearchVisible: false);
      _applyFilter(const HomeFilter());
    } else {
      state = state.copyWith(isSearchVisible: true);
    }
  }

  void setSearch(String query) {
    if (query.isBlank) {
      _applyFilter(const HomeFilter());
      return;
    }
    _applyFilter(HomeFilter(type: NoteFilter.search, searchQuery: query));
  }

  // ── Quote ─────────────────────────────────────────────────────

  void nextQuote() {
    var next = Random().nextInt(quoteCount);
    while (next == state.quoteIndex && quoteCount > 1) {
      next = Random().nextInt(quoteCount);
    }
    state = state.copyWith(quoteIndex: next);
  }

  // ── Selection ─────────────────────────────────────────────────

  void toggleSelection(int id) {
    final current = Set<int>.from(state.selectedNoteIds);
    if (current.contains(id)) {
      current.remove(id);
    } else {
      current.add(id);
    }
    state = state.copyWith(selectedNoteIds: current);
  }

  void clearSelection() => state = state.copyWith(selectedNoteIds: {});

  // ── CRUD ──────────────────────────────────────────────────────
  // Setara dengan deleteNote(), restoreNote(), dll di HomeViewModel.kt

  Future<void> deleteNote(int id) => _noteRepo.softDelete(id);
  Future<void> restoreNote(int id) => _noteRepo.restore(id);
  Future<void> togglePin(Note note) => _noteRepo.togglePin(note);
  Future<void> toggleArchive(Note note) => _noteRepo.toggleArchive(note);
  Future<void> toggleLock(int id) => _noteRepo.toggleLock(id);
  Future<void> archiveNote(int id) async {
    final note = await _noteRepo.getById(id);
    if (note != null) await _noteRepo.update(note.copyWith(isArchived: true));
  }

  Future<void> unarchiveNote(int id) async {
    final note = await _noteRepo.getById(id);
    if (note != null) await _noteRepo.update(note.copyWith(isArchived: false));
  }

  Future<void> moveNoteToFolder(int noteId, int folderId) =>
      _noteRepo.moveToFolder(noteId, folderId);

  // ── Batch selection actions ───────────────────────────────────

  Future<void> deleteSelectedNotes() async {
    await _noteRepo.softDeleteMany(state.selectedNoteIds.toList());
    clearSelection();
  }

  Future<void> restoreSelectedNotes() async {
    await _noteRepo.restoreMany(state.selectedNoteIds.toList());
    clearSelection();
  }

  Future<void> archiveSelectedNotes() async {
    await _noteRepo.archiveMany(state.selectedNoteIds.toList());
    clearSelection();
  }

  Future<void> unarchiveSelectedNotes() async {
    await _noteRepo.unarchiveMany(state.selectedNoteIds.toList());
    clearSelection();
  }

  Future<void> moveSelectedToFolder(int folderId) async {
    await _noteRepo.moveManyToFolder(state.selectedNoteIds.toList(), folderId);
    clearSelection();
  }

  // ── Folder CRUD ───────────────────────────────────────────────

  Future<void> createFolder(String name, String color, {String? icon}) =>
      _folderRepo.insert(
          Folder(name: name, colorHex: color, icon: icon));

  Future<void> updateFolder(int id, String name, String color,
          {String? icon}) =>
      _folderRepo
          .update(Folder(id: id, name: name, colorHex: color, icon: icon));

  Future<void> deleteFolder(int id) => _folderRepo.delete(id);
}

// ── Provider ──────────────────────────────────────────────────────

final homeProvider = NotifierProvider<HomeNotifier, HomeState>(
  HomeNotifier.new,
  name: 'homeProvider',
);

// ── Helpers ───────────────────────────────────────────────────────

extension on String {
  bool get isBlank => trim().isEmpty;
}
