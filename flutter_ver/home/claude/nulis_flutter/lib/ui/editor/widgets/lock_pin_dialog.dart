// lib/ui/editor/widgets/lock_pin_dialog.dart
//
// Setara dengan LockPinDialog di SettingsScreen.kt

import 'package:flutter/material.dart';

class LockPinDialog extends StatefulWidget {
  final bool isIndo;
  final String correctPin;
  final VoidCallback onSuccess;
  final VoidCallback onDismiss;

  const LockPinDialog({
    super.key,
    required this.isIndo,
    required this.correctPin,
    required this.onSuccess,
    required this.onDismiss,
  });

  @override
  State<LockPinDialog> createState() => _LockPinDialogState();
}

class _LockPinDialogState extends State<LockPinDialog> {
  final _ctrl = TextEditingController();
  bool _error = false;

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  void _verify() {
    if (_ctrl.text == widget.correctPin) {
      widget.onSuccess();
    } else {
      setState(() => _error = true);
      _ctrl.clear();
    }
  }

  @override
  Widget build(BuildContext context) {
    final isIndo = widget.isIndo;
    return AlertDialog(
      title: Row(
        children: [
          Icon(Icons.lock_outlined,
              color: Theme.of(context).colorScheme.primary),
          const SizedBox(width: 8),
          Text(isIndo ? 'Masukkan PIN' : 'Enter PIN'),
        ],
      ),
      content: TextField(
        controller: _ctrl,
        autofocus: true,
        obscureText: true,
        keyboardType: TextInputType.number,
        maxLength: 8,
        decoration: InputDecoration(
          hintText: isIndo ? 'PIN...' : 'PIN...',
          errorText: _error
              ? (isIndo ? 'PIN salah' : 'Incorrect PIN')
              : null,
          counterText: '',
        ),
        onSubmitted: (_) => _verify(),
        onChanged: (_) {
          if (_error) setState(() => _error = false);
        },
      ),
      actions: [
        TextButton(
          onPressed: widget.onDismiss,
          child: Text(isIndo ? 'Batal' : 'Cancel'),
        ),
        FilledButton(
          onPressed: _verify,
          child: Text(isIndo ? 'Buka' : 'Unlock'),
        ),
      ],
    );
  }
}
