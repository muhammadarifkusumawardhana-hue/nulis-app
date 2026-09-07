// lib/core/utils/app_utils.dart
//
// Kumpulan helper functions yang di Kotlin tersebar di berbagai file.

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

// ── Date formatting ───────────────────────────────────────────────

/// Format tanggal untuk tampilan di card catatan
String formatNoteDate(DateTime dt, {String lang = 'en'}) {
  final locale = lang == 'id' ? 'id_ID' : 'en_US';
  final now = DateTime.now();
  final diff = now.difference(dt);

  if (diff.inMinutes < 1) {
    return lang == 'id' ? 'Baru saja' : 'Just now';
  } else if (diff.inHours < 1) {
    final m = diff.inMinutes;
    return lang == 'id' ? '$m menit lalu' : '${m}m ago';
  } else if (diff.inDays < 1) {
    final h = diff.inHours;
    return lang == 'id' ? '$h jam lalu' : '${h}h ago';
  } else if (diff.inDays < 7) {
    final d = diff.inDays;
    return lang == 'id' ? '$d hari lalu' : '${d}d ago';
  } else {
    return DateFormat('dd MMM yyyy', locale).format(dt);
  }
}

/// Format tanggal untuk label backup
String formatBackupDate(DateTime dt) =>
    DateFormat('dd MMM yyyy, HH:mm').format(dt);

// ── Color parsing ─────────────────────────────────────────────────

/// Parse hex string ke Color — digunakan untuk FolderEntity.colorHex
Color hexToColor(String hex) {
  try {
    final clean = hex.replaceFirst('#', '');
    if (clean.length == 6) {
      return Color(int.parse('FF$clean', radix: 16));
    } else if (clean.length == 8) {
      return Color(int.parse(clean, radix: 16));
    }
  } catch (_) {}
  return const Color(0xFF3D7A3D); // fallback sage600
}

/// Color ke hex string
String colorToHex(Color color) =>
    '#${color.value.toRadixString(16).padLeft(8, '0').substring(2).toUpperCase()}';

// ── Word count ────────────────────────────────────────────────────
// Setara dengan countWords() di EditorViewModel.kt

int countWords(String text) {
  if (text.trim().isEmpty) return 0;
  return text.trim().split(RegExp(r'\s+')).length;
}

// ── Safe file name ────────────────────────────────────────────────
// Setara dengan safeName() di EditorViewModel.kt

String safeFileName(String name) {
  final clean = name
      .replaceAll(RegExp(r'[^\w\s\-]'), '')
      .trim()
      .replaceAll(RegExp(r'\s+'), '_')
      .substring(0, name.length.clamp(0, 40));
  return clean.isEmpty ? 'catatan_${DateTime.now().millisecondsSinceEpoch}' : clean;
}

// ── Markdown snippet helpers ──────────────────────────────────────
// Setara dengan insertSnippet() di EditorViewModel.kt
// Dipakai di toolbar editor

class SnippetResult {
  final String text;
  final int cursorStart;
  final int cursorEnd;
  const SnippetResult(this.text, this.cursorStart, this.cursorEnd);
}

const _wrappingSnippets = {'**', '*', '`', '~~', '==', '[[', '['};

SnippetResult insertSnippet({
  required String currentText,
  required int selectionStart,
  required int selectionEnd,
  required String snippet,
}) {
  final start = selectionStart;
  final end = selectionEnd;
  final hasSelection = end > start;

  if (hasSelection) {
    final selected = currentText.substring(start, end);
    final isWrapping = _wrappingSnippets.contains(snippet);

    if (isWrapping) {
      final endSymbol = switch (snippet) {
        '[[' => ']]',
        '['  => ']',
        _    => snippet,
      };
      final newText = currentText.replaceRange(
          start, end, '$snippet$selected$endSymbol');
      return SnippetResult(
        newText,
        start + snippet.length,
        end + snippet.length,
      );
    } else {
      final newText =
          currentText.replaceRange(start, end, '$snippet$selected');
      return SnippetResult(newText, end + snippet.length, end + snippet.length);
    }
  } else {
    final newText = currentText.replaceRange(start, end, snippet);
    final pos = start + snippet.length;
    return SnippetResult(newText, pos, pos);
  }
}

// ── HTML export ───────────────────────────────────────────────────
// Setara dengan buildHtml() di EditorViewModel.kt
// Dioptimasi: konversi Markdown ke HTML lebih lengkap

String buildExportHtml(String title, String content, List<String> tags) {
  String body = content
      .replaceAll(RegExp(r'^# (.+)$', multiLine: true), '<h1>\$1</h1>')
      .replaceAll(RegExp(r'^## (.+)$', multiLine: true), '<h2>\$1</h2>')
      .replaceAll(RegExp(r'^### (.+)$', multiLine: true), '<h3>\$1</h3>')
      .replaceAll(RegExp(r'\*\*(.+?)\*\*'), '<strong>\$1</strong>')
      .replaceAll(RegExp(r'\*(.+?)\*'), '<em>\$1</em>')
      .replaceAll(RegExp(r'~~(.+?)~~'), '<del>\$1</del>')
      .replaceAll(RegExp(r'`(.+?)`'), '<code>\$1</code>')
      .replaceAll(RegExp(r'^> (.+)$', multiLine: true), '<blockquote>\$1</blockquote>')
      .replaceAll(RegExp(r'^- \[x\] (.+)$', multiLine: true), '<li class="done">✓ \$1</li>')
      .replaceAll(RegExp(r'^- \[ \] (.+)$', multiLine: true), '<li>☐ \$1</li>')
      .replaceAll(RegExp(r'^- (.+)$', multiLine: true), '<li>\$1</li>')
      .replaceAll(RegExp(r'^\[\[(.+?)\]\]'), '<a href="#">\$1</a>')
      .replaceAll('\n\n', '</p><p>');

  final escaped = title.replaceAll('<', '&lt;').replaceAll('>', '&gt;');
  final tagHtml = tags.isNotEmpty
      ? '<p>${tags.map((t) => '<code>#$t</code>').join(' ')}</p>'
      : '';

  return '''<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>$escaped</title>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, sans-serif;
         max-width: 800px; margin: 40px auto; padding: 0 20px; line-height: 1.7;
         color: #1a3d1a; background: #faf8f3; }
  h1, h2, h3 { color: #2c5f2c; }
  code { background: #dceedc; padding: 2px 6px; border-radius: 4px;
         font-family: monospace; font-size: 0.9em; }
  blockquote { border-left: 4px solid #3d7a3d; margin: 0;
               padding-left: 16px; color: #5b6e5b; }
  li.done { color: #5b6e5b; text-decoration: line-through; }
  a { color: #3d7a3d; }
  hr { border: none; border-top: 1px solid #c2dec2; margin: 24px 0; }
</style>
</head>
<body>
<h1>$escaped</h1>
$tagHtml
<p>$body</p>
</body>
</html>''';
}

// ── Quotes ────────────────────────────────────────────────────────
// Setara dengan quotesEn / quotesId di HomeViewModel.kt

const _quotesEn = [
  'The best way to predict the future is to invent it.',
  'Your mind is for having ideas, not holding them.',
  'Simplicity is the ultimate sophistication.',
  'Write it down, make it happen.',
  'Creativity is intelligence having fun.',
  'The secret of getting ahead is getting started.',
];

const _quotesId = [
  'Cara terbaik untuk memprediksi masa depan adalah dengan menciptakannya.',
  'Pikiranmu adalah untuk menghasilkan ide, bukan untuk menyimpannya.',
  'Kesederhanaan adalah kecanggihan tertinggi.',
  'Tuliskanlah, dan wujudkanlah.',
  'Kreativitas adalah kecerdasan yang sedang bersenang-senang.',
  'Rahasia untuk maju adalah dengan memulai.',
];

String getQuote(int index, String lang) {
  final list = lang == 'id' ? _quotesId : _quotesEn;
  return list[index % list.length];
}

int get quoteCount => _quotesEn.length;
