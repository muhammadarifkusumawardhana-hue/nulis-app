// lib/ui/editor/widgets/editor_toolbar.dart
//
// Setara dengan MarkdownToolbar di EditorScreen.kt
// Toolbar markdown yang di-scroll horizontal — dioptimasi dengan
// const di semua elemen statis agar tidak di-rebuild.

import 'package:flutter/material.dart';

class EditorToolbar extends StatelessWidget {
  final int wordCount;
  final bool isSaved;
  final bool isIndo;
  final void Function(String snippet) onSnippet;

  const EditorToolbar({
    super.key,
    required this.wordCount,
    required this.isSaved,
    required this.isIndo,
    required this.onSnippet,
  });

  // Toolbar buttons — setara dengan toolbar di EditorScreen.kt
  static const _buttons = [
    ('**', Icons.format_bold, 'Bold'),
    ('*', Icons.format_italic, 'Italic'),
    ('~~', Icons.format_strikethrough, 'Strikethrough'),
    ('`', Icons.code, 'Code'),
    ('[[', Icons.link, 'Backlink'),
    ('# ', Icons.title, 'H1'),
    ('## ', Icons.title, 'H2'),
    ('- ', Icons.format_list_bulleted, 'List'),
    ('- [ ] ', Icons.check_box_outlined, 'Todo'),
    ('> ', Icons.format_quote, 'Quote'),
    ('---\n', Icons.horizontal_rule, 'Divider'),
  ];

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return SafeArea(
      top: false,
      child: Container(
        height: 48,
        decoration: BoxDecoration(
          color: cs.surface,
          border: Border(top: BorderSide(color: cs.outlineVariant, width: 0.5)),
        ),
        child: Row(
          children: [
            // Scroll toolbar buttons
            Expanded(
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 4),
                children: _buttons
                    .map((b) => _ToolbarBtn(
                          snippet: b.$1,
                          icon: b.$2,
                          tooltip: b.$3,
                          onTap: () => onSnippet(b.$1),
                        ))
                    .toList(),
              ),
            ),
            // Word count + save indicator
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (!isSaved)
                    Padding(
                      padding: const EdgeInsets.only(right: 4),
                      child: SizedBox(
                        width: 10,
                        height: 10,
                        child: CircularProgressIndicator(
                            strokeWidth: 1.5, color: cs.primary),
                      ),
                    ),
                  Text(
                    '$wordCount ${isIndo ? 'kata' : 'words'}',
                    style: TextStyle(fontSize: 11, color: cs.onSurfaceVariant),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ToolbarBtn extends StatelessWidget {
  final String snippet;
  final IconData icon;
  final String tooltip;
  final VoidCallback onTap;

  const _ToolbarBtn({
    required this.snippet,
    required this.icon,
    required this.tooltip,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      message: tooltip,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
          child: Icon(icon, size: 20,
              color: Theme.of(context).colorScheme.onSurfaceVariant),
        ),
      ),
    );
  }
}
