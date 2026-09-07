// lib/ui/home/widgets/folder_chip_row.dart

import 'package:flutter/material.dart';
import '../../../core/utils/app_utils.dart';
import '../../../data/models/folder.dart';

class FolderChipRow extends StatelessWidget {
  final List<Folder> folders;
  final int? selectedFolderId;
  final void Function(int?) onSelect;
  final bool isIndo;

  const FolderChipRow({
    super.key, required this.folders, required this.selectedFolderId,
    required this.onSelect, required this.isIndo,
  });

  @override
  Widget build(BuildContext context) {
    if (folders.isEmpty) return const SizedBox.shrink();
    return SizedBox(
      height: 44,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 12),
        children: [
          Padding(
            padding: const EdgeInsets.only(right: 6),
            child: FilterChip(
              label: Text(isIndo ? 'Semua' : 'All'),
              selected: selectedFolderId == null,
              onSelected: (_) => onSelect(null),
            ),
          ),
          ...folders.map((folder) {
            final color = hexToColor(folder.colorHex);
            return Padding(
              padding: const EdgeInsets.only(right: 6),
              child: FilterChip(
                avatar: CircleAvatar(radius: 6, backgroundColor: color),
                label: Text(folder.name),
                selected: selectedFolderId == folder.id,
                onSelected: (_) => onSelect(folder.id),
              ),
            );
          }),
        ],
      ),
    );
  }
}
