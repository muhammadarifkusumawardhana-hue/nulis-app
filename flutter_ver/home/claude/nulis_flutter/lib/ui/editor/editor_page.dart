// lib/ui/editor/editor_page.dart
//
// Setara dengan EditorScreen.kt di Kotlin.
// Fitur: editor + preview, toolbar markdown, tags, lock, export PDF/HTML.

import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:local_auth/local_auth.dart';
import 'package:printing/printing.dart';
import 'package:pdf/widgets.dart' as pw;

import '../../core/providers/settings_provider.dart';
import '../../core/utils/app_utils.dart';
import '../app_router.dart';
import 'editor_notifier.dart';
import 'widgets/editor_toolbar.dart';
import 'widgets/tag_row.dart';
import 'widgets/lock_pin_dialog.dart';

class EditorPage extends ConsumerStatefulWidget {
  final int noteId;
  final int folderId;

  const EditorPage({super.key, required this.noteId, required this.folderId});

  @override
  ConsumerState<EditorPage> createState() => _EditorPageState();
}

class _EditorPageState extends ConsumerState<EditorPage> {
  late final (int, int) _key;
  final _titleCtrl = TextEditingController();
  final _localAuth = LocalAuthentication();

  @override
  void initState() {
    super.initState();
    _key = (widget.noteId, widget.folderId);
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final edState = ref.watch(editorProvider(_key));
    final notifier = ref.read(editorProvider(_key).notifier);
    final settings = ref.watch(settingsProvider).valueOrNull;
    final isIndo = settings?.language == 'id';
    final cs = Theme.of(context).colorScheme;

    // Sync title controller saat note dimuat
    if (!edState.isLoading && _titleCtrl.text != edState.title) {
      _titleCtrl.text = edState.title;
      _titleCtrl.selection =
          TextSelection.collapsed(offset: edState.title.length);
    }

    // Tampilkan lock dialog jika perlu
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (edState.isLocked && !edState.isAuthenticated && !edState.isLoading) {
        _showLockDialog(context, notifier, settings, isIndo);
      }
    });

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (_, __) {
        notifier.saveNow();
        context.goHome();
      },
      child: Scaffold(
        backgroundColor: cs.surface,
        appBar: _EditorAppBar(
          edState: edState,
          isIndo: isIndo,
          settings: settings,
          onBack: () {
            notifier.saveNow();
            context.goHome();
          },
          onTogglePreview: notifier.togglePreview,
          onToggleLock: () => _handleLockToggle(context, notifier, edState, settings, isIndo),
          onExportPdf: () => _exportPdf(context, edState, isIndo),
          onExportHtml: () => _exportHtml(edState),
        ),
        body: edState.isLoading
            ? const Center(child: CircularProgressIndicator())
            : (edState.isLocked && !edState.isAuthenticated)
                ? _LockedPlaceholder(isIndo: isIndo)
                : Column(
                    children: [
                      // ── Title field ──────────────────────────────
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 4, 16, 0),
                        child: TextField(
                          controller: _titleCtrl,
                          readOnly: edState.isDeleted,
                          onChanged: notifier.onTitleChange,
                          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                color: cs.onSurface,
                                fontWeight: FontWeight.w600,
                              ),
                          decoration: InputDecoration(
                            hintText: isIndo ? 'Judul...' : 'Title...',
                            border: InputBorder.none,
                            enabledBorder: InputBorder.none,
                            focusedBorder: InputBorder.none,
                            filled: false,
                            contentPadding: EdgeInsets.zero,
                          ),
                          maxLines: null,
                          textInputAction: TextInputAction.next,
                        ),
                      ),

                      // ── Tag row ──────────────────────────────────
                      TagRow(
                        tags: edState.tags,
                        allTags: edState.allTags,
                        isIndo: isIndo,
                        readOnly: edState.isDeleted,
                        onAdd: notifier.addTag,
                        onRemove: notifier.removeTag,
                        onRename: notifier.renameTag,
                      ),

                      const Divider(height: 1),

                      // ── Editor / Preview ─────────────────────────
                      Expanded(
                        child: AnimatedSwitcher(
                          duration: const Duration(milliseconds: 200),
                          child: edState.isPreview
                              ? _MarkdownPreview(
                                  key: const ValueKey('preview'),
                                  content: edState.content,
                                  onTapLink: (noteTitle) =>
                                      _navigateToBacklink(context, ref, noteTitle),
                                )
                              : _MarkdownEditor(
                                  key: const ValueKey('editor'),
                                  controller: edState.contentController,
                                  readOnly: edState.isDeleted,
                                  isIndo: isIndo,
                                  onChanged: notifier.onContentChange,
                                ),
                        ),
                      ),
                    ],
                  ),

        // ── Bottom toolbar ──────────────────────────────────────
        bottomNavigationBar: (!edState.isPreview &&
                !edState.isDeleted &&
                !edState.isLoading &&
                edState.isAuthenticated)
            ? EditorToolbar(
                wordCount: edState.wordCount,
                isSaved: edState.isSaved,
                isIndo: isIndo,
                onSnippet: notifier.insertSnippet,
              )
            : null,
      ),
    );
  }

  // ── Lock handling ───────────────────────────────────────────

  Future<void> _showLockDialog(
    BuildContext context,
    EditorNotifier notifier,
    AppSettings? settings,
    bool isIndo,
  ) async {
    // Coba biometric dulu jika diaktifkan
    if (settings?.isBiometricEnabled == true) {
      try {
        final canCheck = await _localAuth.canCheckBiometrics;
        if (canCheck) {
          final auth = await _localAuth.authenticate(
            localizedReason:
                isIndo ? 'Buka kunci catatan ini' : 'Unlock this note',
            options: const AuthenticationOptions(biometricOnly: false),
          );
          if (auth) {
            notifier.setAuthenticated(true);
            return;
          }
        }
      } catch (_) {}
    }

    // Fallback ke PIN dialog
    if (!context.mounted) return;
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => LockPinDialog(
        isIndo: isIndo,
        correctPin: settings?.noteLockPin ?? '',
        onSuccess: () {
          notifier.setAuthenticated(true);
          Navigator.pop(context);
        },
        onDismiss: () {
          Navigator.pop(context);
          context.goHome();
        },
      ),
    );
  }

  Future<void> _handleLockToggle(
    BuildContext context,
    EditorNotifier notifier,
    EditorState edState,
    AppSettings? settings,
    bool isIndo,
  ) async {
    if (settings?.isNoteLockEnabled != true ||
        settings?.noteLockPin.isEmpty == true) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(isIndo
            ? 'Aktifkan kunci catatan di Pengaturan terlebih dahulu'
            : 'Enable note lock in Settings first'),
        behavior: SnackBarBehavior.floating,
      ));
      return;
    }
    if (edState.isLocked) {
      // Perlu autentikasi dulu sebelum unlock
      showDialog(
        context: context,
        builder: (_) => LockPinDialog(
          isIndo: isIndo,
          correctPin: settings?.noteLockPin ?? '',
          onSuccess: () {
            notifier.toggleLock();
            Navigator.pop(context);
          },
          onDismiss: () => Navigator.pop(context),
        ),
      );
    } else {
      notifier.toggleLock();
    }
  }

  // ── Export ─────────────────────────────────────────────────

  Future<void> _exportPdf(
      BuildContext context, EditorState edState, bool isIndo) async {
    final doc = pw.Document();
    doc.addPage(
      pw.Page(
        build: (ctx) => pw.Column(
          crossAxisAlignment: pw.CrossAxisAlignment.start,
          children: [
            pw.Text(edState.title,
                style: pw.TextStyle(
                    fontSize: 24, fontWeight: pw.FontWeight.bold)),
            pw.SizedBox(height: 12),
            pw.Text(edState.content),
          ],
        ),
      ),
    );
    await Printing.layoutPdf(onLayout: (_) async => doc.save());
  }

  void _exportHtml(EditorState edState) {
    // Di versi penuh, ini akan share file HTML via share_plus
    // Logic HTML builder sudah ada di app_utils.dart: buildExportHtml()
  }

  // ── Backlink navigation ─────────────────────────────────────
  // Setara dengan onNavigateToNote() di EditorScreen.kt

  Future<void> _navigateToBacklink(
      BuildContext context, WidgetRef ref, String noteTitle) async {
    final db = ref.read(databaseProvider);
    final notes = await db.watchAllNotes().first;
    final target = notes.where((n) => n.title == noteTitle).firstOrNull;
    if (target != null && context.mounted) {
      context.goEditor(noteId: target.id);
    }
  }
}

// ── AppBar ────────────────────────────────────────────────────────

class _EditorAppBar extends StatelessWidget implements PreferredSizeWidget {
  final EditorState edState;
  final bool isIndo;
  final AppSettings? settings;
  final VoidCallback onBack;
  final VoidCallback onTogglePreview;
  final VoidCallback onToggleLock;
  final void Function(BuildContext, EditorState, bool) onExportPdf;
  final void Function(EditorState) onExportHtml;

  const _EditorAppBar({
    required this.edState, required this.isIndo, required this.settings,
    required this.onBack, required this.onTogglePreview,
    required this.onToggleLock, required this.onExportPdf,
    required this.onExportHtml,
  });

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return AppBar(
      leading: IconButton(
          icon: const Icon(Icons.arrow_back), onPressed: onBack),
      title: AnimatedSwitcher(
        duration: const Duration(milliseconds: 150),
        child: edState.isPreview
            ? Container(
                key: const ValueKey('preview_badge'),
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                decoration: BoxDecoration(
                  color: cs.primaryContainer,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text('PREVIEW',
                    style: TextStyle(
                        fontSize: 12,
                        color: cs.primary,
                        fontWeight: FontWeight.w600)),
              )
            : Container(
                key: const ValueKey('editor_badge'),
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                decoration: BoxDecoration(
                  color: cs.surfaceContainerHigh,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text('EDITOR',
                    style: TextStyle(
                        fontSize: 12, color: cs.onSurfaceVariant)),
              ),
      ),
      actions: [
        // Preview toggle
        IconButton(
          icon: Icon(edState.isPreview
              ? Icons.edit_outlined
              : Icons.visibility_outlined),
          tooltip:
              edState.isPreview ? (isIndo ? 'Edit' : 'Edit') : 'Preview',
          onPressed: onTogglePreview,
        ),
        // Lock
        if (settings?.isNoteLockEnabled == true)
          IconButton(
            icon: Icon(edState.isLocked
                ? Icons.lock_outlined
                : Icons.lock_open_outlined),
            onPressed: onToggleLock,
          ),
        // More menu
        PopupMenuButton<String>(
          icon: const Icon(Icons.more_vert),
          itemBuilder: (_) => [
            PopupMenuItem(
              value: 'pdf',
              child: ListTile(
                  dense: true,
                  leading: const Icon(Icons.picture_as_pdf_outlined),
                  title: Text(isIndo ? 'Ekspor PDF' : 'Export PDF')),
            ),
            PopupMenuItem(
              value: 'html',
              child: ListTile(
                  dense: true,
                  leading: const Icon(Icons.html_outlined),
                  title: Text(isIndo ? 'Ekspor HTML' : 'Export HTML')),
            ),
          ],
          onSelected: (v) {
            if (v == 'pdf') onExportPdf(context, edState, isIndo);
            if (v == 'html') onExportHtml(edState);
          },
        ),
      ],
    );
  }
}

// ── Markdown editor ───────────────────────────────────────────────

class _MarkdownEditor extends StatelessWidget {
  final TextEditingController controller;
  final bool readOnly;
  final bool isIndo;
  final void Function(String) onChanged;

  const _MarkdownEditor({
    super.key, required this.controller, required this.readOnly,
    required this.isIndo, required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return TextField(
      controller: controller,
      readOnly: readOnly,
      onChanged: onChanged,
      maxLines: null,
      expands: true,
      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: cs.onSurface,
            height: 1.7,
          ),
      decoration: InputDecoration(
        hintText: isIndo
            ? 'Mulai menulis... (Markdown didukung)'
            : 'Start writing... (Markdown supported)',
        border: InputBorder.none,
        enabledBorder: InputBorder.none,
        focusedBorder: InputBorder.none,
        filled: false,
        contentPadding: const EdgeInsets.all(16),
      ),
    );
  }
}

// ── Markdown preview ──────────────────────────────────────────────
// flutter_markdown menggantikan Markwon library dari Kotlin

class _MarkdownPreview extends StatelessWidget {
  final String content;
  final void Function(String noteTitle) onTapLink;

  const _MarkdownPreview({super.key, required this.content, required this.onTapLink});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    // Konversi backlink [[judul]] → markdown link [judul](backlink://judul)
    final processedContent = content.replaceAllMapped(
      RegExp(r'\[\[(.+?)\]\]'),
      (m) => '[${m.group(1)}](backlink://${m.group(1)})',
    );

    return Markdown(
      data: processedContent,
      padding: const EdgeInsets.all(16),
      styleSheet: MarkdownStyleSheet(
        p: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: cs.onSurface, height: 1.7),
        h1: Theme.of(context).textTheme.headlineMedium?.copyWith(
              color: cs.onSurface, fontWeight: FontWeight.bold),
        h2: Theme.of(context).textTheme.headlineSmall?.copyWith(
              color: cs.onSurface, fontWeight: FontWeight.w600),
        h3: Theme.of(context).textTheme.titleLarge?.copyWith(
              color: cs.onSurface, fontWeight: FontWeight.w600),
        code: Theme.of(context).textTheme.bodyMedium?.copyWith(
              fontFamily: 'monospace',
              backgroundColor: cs.surfaceContainerHigh),
        blockquoteDecoration: BoxDecoration(
          border: Border(
              left: BorderSide(color: cs.primary, width: 4)),
          color: cs.surfaceContainerLow,
        ),
        checkbox: TextStyle(color: cs.primary),
        a: TextStyle(color: cs.primary),
      ),
      onTapLink: (text, href, _) {
        if (href?.startsWith('backlink://') == true) {
          onTapLink(href!.replaceFirst('backlink://', ''));
        }
      },
    );
  }
}

// ── Locked placeholder ────────────────────────────────────────────

class _LockedPlaceholder extends StatelessWidget {
  final bool isIndo;
  const _LockedPlaceholder({required this.isIndo});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.lock_outlined, size: 64, color: cs.onSurfaceVariant),
          const SizedBox(height: 12),
          Text(
            isIndo ? 'Catatan ini terkunci' : 'This note is locked',
            style: TextStyle(color: cs.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}
