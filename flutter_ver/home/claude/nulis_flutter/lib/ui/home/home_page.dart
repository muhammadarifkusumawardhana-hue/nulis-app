// lib/ui/home/home_page.dart
//
// Setara dengan HomeScreen.kt di Kotlin.
// Semua fitur dipertahankan: drawer, search, filter, selection mode,
// swipe aksi, grid/list view, quote, folder chip.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers/settings_provider.dart';
import '../../core/utils/app_utils.dart';
import '../../data/models/note.dart';
import '../../data/models/folder.dart';
import '../app_router.dart';
import 'home_notifier.dart';
import 'widgets/note_card.dart';
import 'widgets/side_drawer.dart';
import 'widgets/folder_chip_row.dart';
import 'widgets/home_top_bar.dart';
import 'widgets/folder_dialog.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final homeState = ref.watch(homeProvider);
    final notifier = ref.read(homeProvider.notifier);
    final settings = ref.watch(settingsProvider).valueOrNull;
    final isIndo = settings?.language == 'id';
    final isGrid = settings?.isGridView ?? false;
    final cs = Theme.of(context).colorScheme;

    return PopScope(
      canPop: !homeState.isSelectionMode && !homeState.isSearchVisible,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) {
          if (homeState.isSelectionMode) notifier.clearSelection();
          if (homeState.isSearchVisible) notifier.toggleSearch();
        }
      },
      child: Scaffold(
        key: _scaffoldKey,
        backgroundColor: cs.surface,
        drawer: SideDrawer(
          folders: homeState.folders,
          tags: homeState.tags,
          filter: homeState.filter,
          isIndo: isIndo,
          onAllNotes: () { Navigator.pop(context); notifier.clearAll(); },
          onFolder: (id) { Navigator.pop(context); notifier.selectFolder(id); },
          onTag: (tag) { Navigator.pop(context); notifier.selectTag(tag); },
          onNewFolder: () {
            Navigator.pop(context);
            _showFolderDialog(context, notifier, isIndo);
          },
          onEditFolder: (folder) {
            Navigator.pop(context);
            _showFolderDialog(context, notifier, isIndo, existing: folder);
          },
          onDeleteFolder: (id) => notifier.deleteFolder(id),
          onArchived: () { Navigator.pop(context); notifier.showArchived(); },
          onTrash: () { Navigator.pop(context); notifier.showTrash(); },
          onSettings: () { Navigator.pop(context); context.goSettings(); },
        ),
        body: Column(
          children: [
            HomeTopBar(
              homeState: homeState,
              isIndo: isIndo,
              onMenuTap: () => _scaffoldKey.currentState?.openDrawer(),
              onSearchToggle: notifier.toggleSearch,
              onSearchChange: notifier.setSearch,
              searchController: _searchController,
              onClearSelection: notifier.clearSelection,
              onDeleteSelected: () =>
                  _confirmDeleteSelected(context, notifier, homeState, isIndo),
              onArchiveSelected: () =>
                  _archiveSelected(context, notifier, homeState, isIndo),
              onUnarchiveSelected: notifier.unarchiveSelectedNotes,
              onRestoreSelected: notifier.restoreSelectedNotes,
              onMoveSelected: notifier.moveSelectedToFolder,
              folders: homeState.folders,
            ),
            if (!homeState.isTrash &&
                !homeState.isArchived &&
                !homeState.isSearchVisible &&
                homeState.filter.type == NoteFilter.all)
              FolderChipRow(
                folders: homeState.folders,
                selectedFolderId: homeState.filter.folderId,
                onSelect: notifier.selectFolder,
                isIndo: isIndo,
              ),
            Expanded(
              child: homeState.isLoading
                  ? const Center(child: CircularProgressIndicator())
                  : homeState.notes.isEmpty
                      ? _EmptyState(filter: homeState.filter, isIndo: isIndo)
                      : _NotesList(
                          notes: homeState.notes,
                          folders: homeState.folders,
                          filter: homeState.filter,
                          selectedNoteIds: homeState.selectedNoteIds,
                          isGrid: isGrid,
                          isIndo: isIndo,
                          quoteIndex: homeState.quoteIndex,
                          onNextQuote: notifier.nextQuote,
                          onTap: (note) {
                            if (homeState.isSelectionMode) {
                              notifier.toggleSelection(note.id);
                            } else {
                              context.goEditor(noteId: note.id);
                            }
                          },
                          onLongPress: (note) => notifier.toggleSelection(note.id),
                          onPin: (note) => notifier.togglePin(note),
                          onArchive: (note) => notifier.toggleArchive(note),
                          onDelete: (note) => notifier.deleteNote(note.id),
                          onRestore: (note) => notifier.restoreNote(note.id),
                          onMoveToFolder: notifier.moveNoteToFolder,
                        ),
            ),
          ],
        ),
        floatingActionButton: homeState.isSelectionMode || homeState.isTrash
            ? null
            : FloatingActionButton(
                onPressed: () => context.goEditor(
                  folderId: homeState.filter.folderId ?? -1,
                ),
                tooltip: isIndo ? 'Catatan baru' : 'New note',
                child: const Icon(Icons.edit_outlined),
              ),
      ),
    );
  }

  void _showFolderDialog(BuildContext context, HomeNotifier notifier,
      bool isIndo, {Folder? existing}) {
    showDialog(
      context: context,
      builder: (_) => FolderDialog(
        existing: existing,
        isIndo: isIndo,
        onCreate: (name, color, icon) =>
            notifier.createFolder(name, color, icon: icon),
        onUpdate: (id, name, color, icon) =>
            notifier.updateFolder(id, name, color, icon: icon),
      ),
    );
  }

  Future<void> _confirmDeleteSelected(BuildContext context,
      HomeNotifier notifier, HomeState state, bool isIndo) async {
    final count = state.selectedNoteIds.length;
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(isIndo ? 'Hapus catatan?' : 'Delete notes?'),
        content: Text(isIndo
            ? '$count catatan akan dipindahkan ke sampah.'
            : '$count notes will be moved to trash.'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: Text(isIndo ? 'Batal' : 'Cancel')),
          TextButton(
              onPressed: () => Navigator.pop(context, true),
              child: Text(isIndo ? 'Hapus' : 'Delete',
                  style: TextStyle(
                      color: Theme.of(context).colorScheme.error))),
        ],
      ),
    );
    if (confirm == true) await notifier.deleteSelectedNotes();
  }

  Future<void> _archiveSelected(BuildContext context, HomeNotifier notifier,
      HomeState state, bool isIndo) async {
    final ids = state.selectedNoteIds.toList();
    await notifier.archiveSelectedNotes();
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(isIndo
          ? '${ids.length} catatan diarsip'
          : '${ids.length} notes archived'),
      action: SnackBarAction(
          label: isIndo ? 'Urungkan' : 'Undo',
          onPressed: () => notifier.unarchiveMany(ids)),
      behavior: SnackBarBehavior.floating,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
    ));
  }
}

// ── Notes list/grid ───────────────────────────────────────────────

class _NotesList extends StatelessWidget {
  final List<Note> notes;
  final List<Folder> folders;
  final HomeFilter filter;
  final Set<int> selectedNoteIds;
  final bool isGrid;
  final bool isIndo;
  final int quoteIndex;
  final VoidCallback onNextQuote;
  final void Function(Note) onTap;
  final void Function(Note) onLongPress;
  final void Function(Note) onPin;
  final void Function(Note) onArchive;
  final void Function(Note) onDelete;
  final void Function(Note) onRestore;
  final void Function(int, int) onMoveToFolder;

  const _NotesList({
    required this.notes, required this.folders, required this.filter,
    required this.selectedNoteIds, required this.isGrid, required this.isIndo,
    required this.quoteIndex, required this.onNextQuote, required this.onTap,
    required this.onLongPress, required this.onPin, required this.onArchive,
    required this.onDelete, required this.onRestore, required this.onMoveToFolder,
  });

  @override
  Widget build(BuildContext context) {
    final isTrash = filter.type == NoteFilter.trash;
    final isArchived = filter.type == NoteFilter.archived;
    final showHeader = filter.type == NoteFilter.all;

    Widget buildCard(Note note) => Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: NoteCard(
            note: note,
            folders: folders,
            isSelected: selectedNoteIds.contains(note.id),
            isIndo: isIndo,
            isTrash: isTrash,
            isArchived: isArchived,
            onTap: () => onTap(note),
            onLongPress: () => onLongPress(note),
            onPin: () => onPin(note),
            onArchive: () => onArchive(note),
            onDelete: () => onDelete(note),
            onRestore: () => onRestore(note),
            onMoveToFolder: (folderId) => onMoveToFolder(note.id, folderId),
          ),
        );

    if (isGrid) {
      return GridView.builder(
        padding: const EdgeInsets.fromLTRB(12, 4, 12, 88),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2, crossAxisSpacing: 8,
          mainAxisSpacing: 8, childAspectRatio: 0.85,
        ),
        itemCount: notes.length,
        itemBuilder: (_, i) => buildCard(notes[i]),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(12, 4, 12, 88),
      itemCount: notes.length + (showHeader ? 1 : 0),
      itemBuilder: (_, i) {
        if (showHeader && i == 0) {
          return _QuoteHeader(
              quoteIndex: quoteIndex, isIndo: isIndo, onNext: onNextQuote);
        }
        return buildCard(notes[showHeader ? i - 1 : i]);
      },
    );
  }
}

class _QuoteHeader extends StatelessWidget {
  final int quoteIndex;
  final bool isIndo;
  final VoidCallback onNext;

  const _QuoteHeader(
      {required this.quoteIndex, required this.isIndo, required this.onNext});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final h = DateTime.now().hour;
    final greeting = isIndo
        ? (h < 11 ? 'Selamat pagi.' : h < 15 ? 'Selamat siang.' : h < 18 ? 'Selamat sore.' : 'Selamat malam.')
        : (h < 12 ? 'Good morning.' : h < 17 ? 'Good afternoon.' : 'Good evening.');

    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 8, 4, 16),
      child: GestureDetector(
        onTap: onNext,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(greeting,
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    color: cs.onSurface, fontWeight: FontWeight.w600)),
            const SizedBox(height: 6),
            Text(getQuote(quoteIndex, isIndo ? 'id' : 'en'),
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: cs.onSurfaceVariant,
                    fontStyle: FontStyle.italic)),
          ],
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  final HomeFilter filter;
  final bool isIndo;
  const _EmptyState({required this.filter, required this.isIndo});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final (icon, label) = switch (filter.type) {
      NoteFilter.trash    => (Icons.delete_outline, isIndo ? 'Sampah kosong' : 'Trash is empty'),
      NoteFilter.archived => (Icons.archive_outlined, isIndo ? 'Tidak ada arsip' : 'No archived notes'),
      NoteFilter.search   => (Icons.search_off, isIndo ? 'Tidak ditemukan' : 'No results found'),
      NoteFilter.drafts   => (Icons.drafts_outlined, isIndo ? 'Tidak ada draft' : 'No drafts'),
      _                   => (Icons.note_add_outlined, isIndo ? 'Belum ada catatan\nTap + untuk mulai' : 'No notes yet\nTap + to start'),
    };
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 56, color: cs.onSurfaceVariant.withOpacity(0.4)),
          const SizedBox(height: 12),
          Text(label, textAlign: TextAlign.center,
              style: TextStyle(color: cs.onSurfaceVariant, height: 1.5)),
        ],
      ),
    );
  }
}
