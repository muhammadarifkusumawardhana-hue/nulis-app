// lib/ui/home/widgets/home_top_bar.dart

import 'package:flutter/material.dart';
import '../../../data/models/folder.dart';
import '../home_notifier.dart';

class HomeTopBar extends StatelessWidget {
  final HomeState homeState;
  final bool isIndo;
  final VoidCallback onMenuTap;
  final VoidCallback onSearchToggle;
  final void Function(String) onSearchChange;
  final TextEditingController searchController;
  final VoidCallback onClearSelection;
  final VoidCallback onDeleteSelected;
  final VoidCallback onArchiveSelected;
  final VoidCallback onUnarchiveSelected;
  final VoidCallback onRestoreSelected;
  final void Function(int) onMoveSelected;
  final List<Folder> folders;

  const HomeTopBar({
    super.key,
    required this.homeState, required this.isIndo,
    required this.onMenuTap, required this.onSearchToggle,
    required this.onSearchChange, required this.searchController,
    required this.onClearSelection, required this.onDeleteSelected,
    required this.onArchiveSelected, required this.onUnarchiveSelected,
    required this.onRestoreSelected, required this.onMoveSelected,
    required this.folders,
  });

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      bottom: false,
      child: AnimatedSwitcher(
        duration: const Duration(milliseconds: 200),
        child: homeState.isSelectionMode
            ? _SelectionBar(
                key: const ValueKey('selection'),
                count: homeState.selectedNoteIds.length,
                isIndo: isIndo,
                isTrash: homeState.isTrash,
                isArchived: homeState.isArchived,
                folders: folders,
                onClear: onClearSelection,
                onDelete: onDeleteSelected,
                onArchive: onArchiveSelected,
                onUnarchive: onUnarchiveSelected,
                onRestore: onRestoreSelected,
                onMove: onMoveSelected,
              )
            : homeState.isSearchVisible
                ? _SearchBar(
                    key: const ValueKey('search'),
                    controller: searchController,
                    isIndo: isIndo,
                    onChanged: onSearchChange,
                    onClose: onSearchToggle,
                  )
                : _DefaultBar(
                    key: const ValueKey('default'),
                    isIndo: isIndo,
                    isTrash: homeState.isTrash,
                    isArchived: homeState.isArchived,
                    onMenu: onMenuTap,
                    onSearch: onSearchToggle,
                  ),
      ),
    );
  }
}

class _DefaultBar extends StatelessWidget {
  final bool isIndo;
  final bool isTrash;
  final bool isArchived;
  final VoidCallback onMenu;
  final VoidCallback onSearch;

  const _DefaultBar({super.key, required this.isIndo, required this.isTrash,
      required this.isArchived, required this.onMenu, required this.onSearch});

  @override
  Widget build(BuildContext context) {
    return AppBar(
      leading: IconButton(
          icon: const Icon(Icons.menu), onPressed: onMenu),
      title: isTrash
          ? Text(isIndo ? 'Sampah' : 'Trash')
          : isArchived
              ? Text(isIndo ? 'Arsip' : 'Archive')
              : null,
      actions: [
        IconButton(icon: const Icon(Icons.search), onPressed: onSearch),
      ],
    );
  }
}

class _SearchBar extends StatelessWidget {
  final TextEditingController controller;
  final bool isIndo;
  final void Function(String) onChanged;
  final VoidCallback onClose;

  const _SearchBar({super.key, required this.controller, required this.isIndo,
      required this.onChanged, required this.onClose});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 8, 8, 4),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: controller,
              autofocus: true,
              onChanged: onChanged,
              decoration: InputDecoration(
                hintText: isIndo ? 'Cari catatan...' : 'Search notes...',
                prefixIcon: const Icon(Icons.search, size: 20),
                isDense: true,
              ),
            ),
          ),
          const SizedBox(width: 8),
          IconButton(icon: const Icon(Icons.close), onPressed: onClose),
        ],
      ),
    );
  }
}

class _SelectionBar extends StatelessWidget {
  final int count;
  final bool isIndo;
  final bool isTrash;
  final bool isArchived;
  final List<Folder> folders;
  final VoidCallback onClear;
  final VoidCallback onDelete;
  final VoidCallback onArchive;
  final VoidCallback onUnarchive;
  final VoidCallback onRestore;
  final void Function(int) onMove;

  const _SelectionBar({
    super.key, required this.count, required this.isIndo,
    required this.isTrash, required this.isArchived, required this.folders,
    required this.onClear, required this.onDelete, required this.onArchive,
    required this.onUnarchive, required this.onRestore, required this.onMove,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return AppBar(
      backgroundColor: cs.primaryContainer,
      leading: IconButton(icon: const Icon(Icons.close), onPressed: onClear),
      title: Text('$count ${isIndo ? 'dipilih' : 'selected'}'),
      actions: [
        if (isTrash) ...[
          IconButton(
              icon: const Icon(Icons.restore), tooltip: isIndo ? 'Pulihkan' : 'Restore',
              onPressed: onRestore),
          IconButton(
              icon: const Icon(Icons.delete_forever),
              tooltip: isIndo ? 'Hapus permanen' : 'Delete permanently',
              onPressed: onDelete),
        ] else if (isArchived) ...[
          IconButton(
              icon: const Icon(Icons.unarchive_outlined),
              tooltip: isIndo ? 'Keluarkan dari arsip' : 'Unarchive',
              onPressed: onUnarchive),
          IconButton(
              icon: const Icon(Icons.delete_outline),
              tooltip: isIndo ? 'Hapus' : 'Delete',
              onPressed: onDelete),
        ] else ...[
          if (folders.isNotEmpty)
            PopupMenuButton<int>(
              icon: const Icon(Icons.drive_file_move_outlined),
              tooltip: isIndo ? 'Pindah folder' : 'Move to folder',
              itemBuilder: (_) => [
                PopupMenuItem(value: -1, child: Text(isIndo ? 'Tanpa folder' : 'No folder')),
                ...folders.map((f) => PopupMenuItem(value: f.id, child: Text(f.name))),
              ],
              onSelected: onMove,
            ),
          IconButton(
              icon: const Icon(Icons.archive_outlined),
              tooltip: isIndo ? 'Arsip' : 'Archive',
              onPressed: onArchive),
          IconButton(
              icon: const Icon(Icons.delete_outline),
              tooltip: isIndo ? 'Hapus' : 'Delete',
              onPressed: onDelete),
        ],
      ],
    );
  }
}
