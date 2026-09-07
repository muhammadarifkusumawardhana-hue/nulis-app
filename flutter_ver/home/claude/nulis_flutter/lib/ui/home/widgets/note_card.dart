// lib/ui/home/widgets/note_card.dart
//
// Setara dengan NoteCard + SwipeableNoteItem di HomeScreen.kt.
// Menggunakan Dismissible untuk swipe actions — lebih ringan dari
// AnchoredDraggableState Kotlin yang membutuhkan lebih banyak resource.

import 'package:flutter/material.dart';

import '../../../core/utils/app_utils.dart';
import '../../../data/models/note.dart';
import '../../../data/models/folder.dart';

class NoteCard extends StatelessWidget {
  final Note note;
  final List<Folder> folders;
  final bool isSelected;
  final bool isIndo;
  final bool isTrash;
  final bool isArchived;
  final VoidCallback onTap;
  final VoidCallback onLongPress;
  final VoidCallback onPin;
  final VoidCallback onArchive;
  final VoidCallback onDelete;
  final VoidCallback onRestore;
  final void Function(int folderId) onMoveToFolder;

  const NoteCard({
    super.key,
    required this.note,
    required this.folders,
    required this.isSelected,
    required this.isIndo,
    required this.isTrash,
    required this.isArchived,
    required this.onTap,
    required this.onLongPress,
    required this.onPin,
    required this.onArchive,
    required this.onDelete,
    required this.onRestore,
    required this.onMoveToFolder,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    // Trash mode: swipe-to-restore atau hapus permanen
    if (isTrash) {
      return _TrashCard(
        note: note, isIndo: isIndo, onTap: onTap,
        onLongPress: onLongPress, onRestore: onRestore,
        onDelete: onDelete, isSelected: isSelected,
      );
    }

    return Dismissible(
      key: ValueKey('note_${note.id}'),
      background: _SwipeBg(
        alignment: Alignment.centerLeft,
        color: note.isPinned ? cs.surfaceContainerHigh : cs.primaryContainer,
        icon: note.isPinned ? Icons.push_pin_outlined : Icons.push_pin,
        label: note.isPinned
            ? (isIndo ? 'Lepas pin' : 'Unpin')
            : (isIndo ? 'Pin' : 'Pin'),
      ),
      secondaryBackground: _SwipeBg(
        alignment: Alignment.centerRight,
        color: cs.errorContainer,
        icon: Icons.archive_outlined,
        label: isArchived
            ? (isIndo ? 'Keluarkan' : 'Unarchive')
            : (isIndo ? 'Arsip' : 'Archive'),
      ),
      confirmDismiss: (direction) async {
        if (direction == DismissDirection.startToEnd) {
          onPin();
        } else {
          onArchive();
        }
        return false; // Jangan dismiss, hanya trigger aksi
      },
      child: _CardBody(
        note: note,
        folders: folders,
        isSelected: isSelected,
        isIndo: isIndo,
        onTap: onTap,
        onLongPress: onLongPress,
        onPin: onPin,
        onArchive: onArchive,
        onDelete: onDelete,
        onMoveToFolder: onMoveToFolder,
      ),
    );
  }
}

// ── Card body ─────────────────────────────────────────────────────

class _CardBody extends StatelessWidget {
  final Note note;
  final List<Folder> folders;
  final bool isSelected;
  final bool isIndo;
  final VoidCallback onTap;
  final VoidCallback onLongPress;
  final VoidCallback onPin;
  final VoidCallback onArchive;
  final VoidCallback onDelete;
  final void Function(int) onMoveToFolder;

  const _CardBody({
    required this.note, required this.folders, required this.isSelected,
    required this.isIndo, required this.onTap, required this.onLongPress,
    required this.onPin, required this.onArchive, required this.onDelete,
    required this.onMoveToFolder,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final folder = folders.where((f) => f.id == note.folderId).firstOrNull;

    return RepaintBoundary(
      // RepaintBoundary = optimasi utama untuk device low-end:
      // mencegah repaint kartu lain saat satu kartu berubah state
      child: Card(
        margin: EdgeInsets.zero,
        color: isSelected ? cs.primaryContainer : cs.surfaceContainerLow,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(
            color: isSelected ? cs.primary : cs.outlineVariant,
            width: isSelected ? 1.5 : 0.5,
          ),
        ),
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: onTap,
          onLongPress: onLongPress,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // ── Header row ──────────────────────────────────
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        note.title.isEmpty
                            ? (isIndo ? '(Tanpa judul)' : '(Untitled)')
                            : note.title,
                        style: Theme.of(context).textTheme.titleSmall?.copyWith(
                              color: note.title.isEmpty
                                  ? cs.onSurfaceVariant
                                  : cs.onSurface,
                              fontWeight: FontWeight.w600,
                            ),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    if (note.isPinned) ...[
                      const SizedBox(width: 4),
                      Icon(Icons.push_pin, size: 14, color: cs.primary),
                    ],
                    if (note.isLocked) ...[
                      const SizedBox(width: 4),
                      Icon(Icons.lock_outline, size: 14, color: cs.onSurfaceVariant),
                    ],
                  ],
                ),

                // ── Preview content ──────────────────────────────
                if (note.content.isNotEmpty) ...[
                  const SizedBox(height: 6),
                  Text(
                    note.isLocked
                        ? '••••••••••••'
                        : _stripMarkdown(note.content),
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: cs.onSurfaceVariant,
                        ),
                    maxLines: 3,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],

                const SizedBox(height: 10),

                // ── Footer: date + tags + folder ─────────────────
                Row(
                  children: [
                    Text(
                      formatNoteDate(note.updatedAt, lang: isIndo ? 'id' : 'en'),
                      style: Theme.of(context).textTheme.labelSmall?.copyWith(
                            color: cs.onSurfaceVariant,
                          ),
                    ),
                    if (folder != null) ...[
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: hexToColor(folder.colorHex).withOpacity(0.15),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          folder.name,
                          style: TextStyle(
                            fontSize: 10,
                            color: hexToColor(folder.colorHex),
                          ),
                        ),
                      ),
                    ],
                    const Spacer(),
                    if (note.tags.isNotEmpty)
                      Text(
                        note.tags.take(2).map((t) => '#$t').join(' '),
                        style: Theme.of(context).textTheme.labelSmall?.copyWith(
                              color: cs.primary.withOpacity(0.8),
                            ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// Strip basic markdown symbols untuk preview bersih
  String _stripMarkdown(String text) {
    return text
        .replaceAll(RegExp(r'#{1,6}\s'), '')
        .replaceAll(RegExp(r'\*\*(.+?)\*\*'), r'$1')
        .replaceAll(RegExp(r'\*(.+?)\*'), r'$1')
        .replaceAll(RegExp(r'~~(.+?)~~'), r'$1')
        .replaceAll(RegExp(r'`(.+?)`'), r'$1')
        .replaceAll(RegExp(r'^\s*[-*+]\s', multiLine: true), '')
        .replaceAll(RegExp(r'^\s*>\s', multiLine: true), '')
        .replaceAll(RegExp(r'\[\[(.+?)\]\]'), r'$1')
        .trim();
  }
}

// ── Trash card ────────────────────────────────────────────────────

class _TrashCard extends StatelessWidget {
  final Note note;
  final bool isIndo;
  final bool isSelected;
  final VoidCallback onTap;
  final VoidCallback onLongPress;
  final VoidCallback onRestore;
  final VoidCallback onDelete;

  const _TrashCard({
    required this.note, required this.isIndo, required this.isSelected,
    required this.onTap, required this.onLongPress,
    required this.onRestore, required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final daysLeft = note.deletedAt != null
        ? 30 - DateTime.now().difference(note.deletedAt!).inDays
        : 30;

    return Card(
      margin: EdgeInsets.zero,
      color: isSelected ? cs.primaryContainer : cs.surfaceContainerLow,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: cs.outlineVariant, width: 0.5),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        onLongPress: onLongPress,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                note.title.isEmpty ? (isIndo ? '(Tanpa judul)' : '(Untitled)') : note.title,
                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      color: cs.onSurface, fontWeight: FontWeight.w600),
                maxLines: 2, overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Icon(Icons.timer_outlined, size: 12, color: cs.error),
                  const SizedBox(width: 4),
                  Text(
                    isIndo
                        ? 'Dihapus dalam ${daysLeft.clamp(0, 30)} hari'
                        : 'Deleted in ${daysLeft.clamp(0, 30)} days',
                    style: TextStyle(fontSize: 11, color: cs.error),
                  ),
                  const Spacer(),
                  TextButton.icon(
                    onPressed: onRestore,
                    icon: const Icon(Icons.restore, size: 14),
                    label: Text(isIndo ? 'Pulihkan' : 'Restore',
                        style: const TextStyle(fontSize: 12)),
                    style: TextButton.styleFrom(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 4),
                      minimumSize: Size.zero,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Swipe background ──────────────────────────────────────────────

class _SwipeBg extends StatelessWidget {
  final AlignmentGeometry alignment;
  final Color color;
  final IconData icon;
  final String label;

  const _SwipeBg(
      {required this.alignment, required this.color,
       required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      alignment: alignment,
      padding: const EdgeInsets.symmetric(horizontal: 20),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: Theme.of(context).colorScheme.onPrimaryContainer),
          const SizedBox(height: 2),
          Text(label,
              style: TextStyle(
                  fontSize: 10,
                  color:
                      Theme.of(context).colorScheme.onPrimaryContainer)),
        ],
      ),
    );
  }
}
