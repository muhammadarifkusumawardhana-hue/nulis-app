// lib/ui/home/widgets/folder_dialog.dart

import 'package:flutter/material.dart';
import '../../../core/utils/app_utils.dart';
import '../../../data/models/folder.dart';

// Warna preset folder — setara dengan color picker di HomeScreen.kt
const _folderColors = [
  '#3D7A3D', '#B85C38', '#1565C0', '#6A1B9A',
  '#00695C', '#E65100', '#AD1457', '#37474F',
  '#F57F17', '#2E7D32',
];

class FolderDialog extends StatefulWidget {
  final Folder? existing;
  final bool isIndo;
  final void Function(String name, String color, String? icon) onCreate;
  final void Function(int id, String name, String color, String? icon) onUpdate;

  const FolderDialog({
    super.key, this.existing, required this.isIndo,
    required this.onCreate, required this.onUpdate,
  });

  @override
  State<FolderDialog> createState() => _FolderDialogState();
}

class _FolderDialogState extends State<FolderDialog> {
  late final TextEditingController _nameCtrl;
  late String _selectedColor;

  @override
  void initState() {
    super.initState();
    _nameCtrl = TextEditingController(text: widget.existing?.name ?? '');
    _selectedColor = widget.existing?.colorHex ?? _folderColors.first;
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isEdit = widget.existing != null;
    final isIndo = widget.isIndo;
    return AlertDialog(
      title: Text(isEdit
          ? (isIndo ? 'Edit folder' : 'Edit folder')
          : (isIndo ? 'Folder baru' : 'New folder')),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextField(
            controller: _nameCtrl,
            autofocus: true,
            decoration: InputDecoration(
              labelText: isIndo ? 'Nama folder' : 'Folder name',
            ),
          ),
          const SizedBox(height: 16),
          Text(isIndo ? 'Warna' : 'Color',
              style: Theme.of(context).textTheme.labelMedium),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: _folderColors.map((hex) {
              final color = hexToColor(hex);
              final isSelected = _selectedColor == hex;
              return GestureDetector(
                onTap: () => setState(() => _selectedColor = hex),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 150),
                  width: 28,
                  height: 28,
                  decoration: BoxDecoration(
                    color: color,
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: isSelected ? Colors.white : Colors.transparent,
                      width: 2,
                    ),
                    boxShadow: isSelected
                        ? [BoxShadow(color: color.withOpacity(0.5), blurRadius: 4)]
                        : null,
                  ),
                  child: isSelected
                      ? const Icon(Icons.check, size: 16, color: Colors.white)
                      : null,
                ),
              );
            }).toList(),
          ),
        ],
      ),
      actions: [
        TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(isIndo ? 'Batal' : 'Cancel')),
        FilledButton(
          onPressed: () {
            final name = _nameCtrl.text.trim();
            if (name.isEmpty) return;
            if (isEdit) {
              widget.onUpdate(widget.existing!.id, name, _selectedColor, null);
            } else {
              widget.onCreate(name, _selectedColor, null);
            }
            Navigator.pop(context);
          },
          child: Text(isEdit
              ? (isIndo ? 'Simpan' : 'Save')
              : (isIndo ? 'Buat' : 'Create')),
        ),
      ],
    );
  }
}
