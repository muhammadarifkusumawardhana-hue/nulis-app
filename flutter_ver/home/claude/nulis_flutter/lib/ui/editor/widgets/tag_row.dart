// lib/ui/editor/widgets/tag_row.dart

import 'package:flutter/material.dart';

class TagRow extends StatefulWidget {
  final List<String> tags;
  final List<String> allTags;
  final bool isIndo;
  final bool readOnly;
  final void Function(String) onAdd;
  final void Function(String) onRemove;
  final void Function(String, String) onRename;

  const TagRow({
    super.key,
    required this.tags, required this.allTags, required this.isIndo,
    required this.readOnly, required this.onAdd, required this.onRemove,
    required this.onRename,
  });

  @override
  State<TagRow> createState() => _TagRowState();
}

class _TagRowState extends State<TagRow> {
  bool _showInput = false;
  final _ctrl = TextEditingController();

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: Row(
        children: [
          ...widget.tags.map((tag) => Padding(
                padding: const EdgeInsets.only(right: 6),
                child: InputChip(
                  label: Text('#$tag',
                      style: const TextStyle(fontSize: 12)),
                  onDeleted: widget.readOnly ? null : () => widget.onRemove(tag),
                  onPressed: widget.readOnly
                      ? null
                      : () => _showRenameDialog(context, tag),
                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
              )),
          if (!widget.readOnly)
            _showInput
                ? SizedBox(
                    width: 120,
                    child: Autocomplete<String>(
                      optionsBuilder: (v) => widget.allTags
                          .where((t) => t.contains(v.text.toLowerCase()) &&
                              !widget.tags.contains(t))
                          .take(5),
                      onSelected: (v) {
                        widget.onAdd(v);
                        setState(() { _showInput = false; _ctrl.clear(); });
                      },
                      fieldViewBuilder: (_, ctrl, focus, onSubmit) {
                        return TextField(
                          controller: ctrl,
                          focusNode: focus,
                          autofocus: true,
                          style: const TextStyle(fontSize: 12),
                          decoration: InputDecoration(
                            hintText: widget.isIndo ? 'tag...' : 'tag...',
                            isDense: true,
                            contentPadding: const EdgeInsets.symmetric(
                                horizontal: 8, vertical: 6),
                          ),
                          onSubmitted: (v) {
                            if (v.isNotEmpty) widget.onAdd(v);
                            setState(() { _showInput = false; ctrl.clear(); });
                          },
                        );
                      },
                    ),
                  )
                : ActionChip(
                    avatar: Icon(Icons.add, size: 14, color: cs.primary),
                    label: Text(widget.isIndo ? 'Tag' : 'Tag',
                        style: TextStyle(fontSize: 12, color: cs.primary)),
                    onPressed: () => setState(() => _showInput = true),
                    materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
        ],
      ),
    );
  }

  void _showRenameDialog(BuildContext context, String tag) {
    final ctrl = TextEditingController(text: tag);
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(widget.isIndo ? 'Ganti nama tag' : 'Rename tag'),
        content: TextField(
          controller: ctrl,
          autofocus: true,
          decoration: const InputDecoration(prefixText: '#'),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context),
              child: Text(widget.isIndo ? 'Batal' : 'Cancel')),
          FilledButton(
            onPressed: () {
              widget.onRename(tag, ctrl.text);
              Navigator.pop(context);
            },
            child: Text(widget.isIndo ? 'Simpan' : 'Save'),
          ),
        ],
      ),
    );
  }
}
