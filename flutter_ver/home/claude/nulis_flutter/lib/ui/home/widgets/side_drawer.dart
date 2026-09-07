// lib/ui/home/widgets/side_drawer.dart

import 'package:flutter/material.dart';
import '../../../core/utils/app_utils.dart';
import '../../../data/models/folder.dart';
import '../home_notifier.dart';

class SideDrawer extends StatelessWidget {
  final List<Folder> folders;
  final List<String> tags;
  final HomeFilter filter;
  final bool isIndo;
  final VoidCallback onAllNotes;
  final void Function(int) onFolder;
  final void Function(String) onTag;
  final VoidCallback onNewFolder;
  final void Function(Folder) onEditFolder;
  final void Function(int) onDeleteFolder;
  final VoidCallback onArchived;
  final VoidCallback onTrash;
  final VoidCallback onSettings;

  const SideDrawer({
    super.key,
    required this.folders, required this.tags, required this.filter,
    required this.isIndo, required this.onAllNotes, required this.onFolder,
    required this.onTag, required this.onNewFolder, required this.onEditFolder,
    required this.onDeleteFolder, required this.onArchived,
    required this.onTrash, required this.onSettings,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return NavigationDrawer(
      selectedIndex: null,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 48, 16, 16),
          child: Text('Nulis',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  color: cs.primary, fontWeight: FontWeight.bold)),
        ),
        const Divider(indent: 16, endIndent: 16),

        // All notes
        ListTile(
          leading: const Icon(Icons.notes_outlined),
          title: Text(isIndo ? 'Semua catatan' : 'All notes'),
          selected: filter.type == NoteFilter.all,
          onTap: onAllNotes,
        ),

        // Folders
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 8, 4),
          child: Row(
            children: [
              Text(isIndo ? 'Folder' : 'Folders',
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: cs.onSurfaceVariant)),
              const Spacer(),
              IconButton(
                icon: const Icon(Icons.add, size: 18),
                onPressed: onNewFolder,
                tooltip: isIndo ? 'Folder baru' : 'New folder',
              ),
            ],
          ),
        ),
        ...folders.map((folder) => _FolderTile(
              folder: folder,
              isSelected: filter.folderId == folder.id,
              isIndo: isIndo,
              onTap: () => onFolder(folder.id),
              onEdit: () => onEditFolder(folder),
              onDelete: () => onDeleteFolder(folder.id),
            )),

        // Tags
        if (tags.isNotEmpty) ...[
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 8, 4),
            child: Text(isIndo ? 'Tag' : 'Tags',
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: cs.onSurfaceVariant)),
          ),
          ...tags.take(10).map((tag) => ListTile(
                dense: true,
                leading: Text('#',
                    style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold)),
                title: Text(tag),
                selected: filter.tag == tag,
                onTap: () => onTag(tag),
              )),
        ],

        const Divider(indent: 16, endIndent: 16),
        ListTile(
          leading: const Icon(Icons.archive_outlined),
          title: Text(isIndo ? 'Arsip' : 'Archive'),
          onTap: onArchived,
        ),
        ListTile(
          leading: const Icon(Icons.delete_outline),
          title: Text(isIndo ? 'Sampah' : 'Trash'),
          onTap: onTrash,
        ),
        const Divider(indent: 16, endIndent: 16),
        ListTile(
          leading: const Icon(Icons.settings_outlined),
          title: Text(isIndo ? 'Pengaturan' : 'Settings'),
          onTap: onSettings,
        ),
        const SizedBox(height: 16),
      ],
    );
  }
}

class _FolderTile extends StatelessWidget {
  final Folder folder;
  final bool isSelected;
  final bool isIndo;
  final VoidCallback onTap;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  const _FolderTile({
    required this.folder, required this.isSelected, required this.isIndo,
    required this.onTap, required this.onEdit, required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final color = hexToColor(folder.colorHex);
    return ListTile(
      dense: true,
      leading: CircleAvatar(
        radius: 8,
        backgroundColor: color,
      ),
      title: Text(folder.name),
      selected: isSelected,
      trailing: PopupMenuButton<String>(
        icon: const Icon(Icons.more_vert, size: 16),
        itemBuilder: (_) => [
          PopupMenuItem(
              value: 'edit',
              child: Text(isIndo ? 'Edit' : 'Edit')),
          PopupMenuItem(
              value: 'delete',
              child: Text(isIndo ? 'Hapus' : 'Delete')),
        ],
        onSelected: (v) {
          if (v == 'edit') onEdit();
          if (v == 'delete') onDelete();
        },
      ),
      onTap: onTap,
    );
  }
}
