// lib/ui/settings/settings_page.dart
// Setara dengan SettingsDialog/SettingsScreen.kt
// Semua section: General, Appearance, Cloud Backup, Local Storage, Security, About

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/providers/settings_provider.dart';
import '../../core/utils/app_utils.dart';
import '../app_router.dart';
import 'settings_notifier.dart';

class SettingsPage extends ConsumerStatefulWidget {
  const SettingsPage({super.key});
  @override
  ConsumerState<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends ConsumerState<SettingsPage> {
  @override
  Widget build(BuildContext context) {
    final settings = ref.watch(settingsProvider).valueOrNull;
    final sNotifier = ref.read(settingsProvider.notifier);
    final backup   = ref.watch(backupProvider);
    final bNotifier = ref.read(backupProvider.notifier);
    final isIndo   = settings?.language == 'id';
    final cs       = Theme.of(context).colorScheme;

    // Tampilkan status backup/restore sebagai SnackBar
    ref.listen(backupProvider, (_, next) {
      if (next.statusMessage != null) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(next.statusMessage!),
          backgroundColor: next.isSuccess ? cs.primaryContainer : cs.errorContainer,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ));
        bNotifier.clearStatus();
      }
    });

    if (settings == null) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: () => context.goHome()),
        title: Text(isIndo ? 'Pengaturan' : 'Settings'),
      ),
      body: Stack(
        children: [
          ListView(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 88),
            children: [
              // ══════════════ GENERAL ══════════════
              _SectionHeader(label: isIndo ? 'UMUM' : 'GENERAL'),
              // Nickname
              _SettingTile(
                icon: Icons.person_outline,
                title: isIndo ? 'Nama Panggilan' : 'Nickname',
                subtitle: settings.nickname.isEmpty
                    ? (isIndo ? 'Belum diatur' : 'Not set')
                    : settings.nickname,
                onTap: () => _showTextInputDialog(
                  context, isIndo ? 'Nama Panggilan' : 'Nickname',
                  initial: settings.nickname,
                  onSave: sNotifier.setNickname,
                ),
              ),
              // Language
              _SettingTile(
                icon: Icons.language_outlined,
                title: isIndo ? 'Bahasa' : 'Language',
                subtitle: settings.language == 'id' ? 'Indonesia' : 'English',
                onTap: () => _showPickerDialog(
                  context,
                  title: isIndo ? 'Bahasa' : 'Language',
                  options: const ['English', 'Indonesia'],
                  values: const ['en', 'id'],
                  current: settings.language,
                  onSelect: sNotifier.setLanguage,
                ),
              ),
              // Font theme
              _SettingTile(
                icon: Icons.font_download_outlined,
                title: isIndo ? 'Tema Font' : 'Font Theme',
                subtitle: settings.fontTheme,
                onTap: () => _showPickerDialog(
                  context,
                  title: isIndo ? 'Tema Font' : 'Font Theme',
                  options: const ['Modern', 'Classic', 'Mono'],
                  values: const ['Modern', 'Classic', 'Mono'],
                  current: settings.fontTheme,
                  onSelect: sNotifier.setFontTheme,
                ),
              ),
              const SizedBox(height: 8),

              // ══════════════ APPEARANCE ══════════════
              _SectionHeader(label: isIndo ? 'TAMPILAN' : 'APPEARANCE'),
              // Dark mode
              SwitchListTile(
                secondary: const Icon(Icons.dark_mode_outlined),
                title: Text(isIndo ? 'Mode Gelap' : 'Dark Mode'),
                value: settings.darkMode,
                onChanged: sNotifier.setDarkMode,
              ),
              // Grid view
              SwitchListTile(
                secondary: const Icon(Icons.grid_view_outlined),
                title: Text(isIndo ? 'Tampilan Grid' : 'Grid View'),
                value: settings.isGridView,
                onChanged: sNotifier.setGridView,
              ),
              // Accent color
              _AccentColorPicker(
                current: settings.accentColor,
                isIndo: isIndo,
                onSelect: sNotifier.setAccentColor,
              ),
              const SizedBox(height: 8),

              // ══════════════ SECURITY ══════════════
              _SectionHeader(label: isIndo ? 'KEAMANAN' : 'SECURITY'),
              SwitchListTile(
                secondary: const Icon(Icons.lock_outline),
                title: Text(isIndo ? 'Kunci Catatan' : 'Note Lock'),
                subtitle: Text(isIndo ? 'Aktifkan PIN untuk catatan terkunci' : 'Enable PIN for locked notes'),
                value: settings.isNoteLockEnabled,
                onChanged: (v) {
                  if (v && settings.noteLockPin.isEmpty) {
                    _showSetPinDialog(context, isIndo, sNotifier);
                  } else {
                    sNotifier.setNoteLockEnabled(v);
                  }
                },
              ),
              if (settings.isNoteLockEnabled) ...[
                _SettingTile(
                  icon: Icons.pin_outlined,
                  title: isIndo ? 'Ubah PIN' : 'Change PIN',
                  subtitle: '••••',
                  onTap: () => _showSetPinDialog(context, isIndo, sNotifier),
                ),
                SwitchListTile(
                  secondary: const Icon(Icons.fingerprint),
                  title: Text(isIndo ? 'Biometrik' : 'Biometric'),
                  value: settings.isBiometricEnabled,
                  onChanged: sNotifier.setBiometricEnabled,
                ),
                _SettingTile(
                  icon: Icons.timer_outlined,
                  title: isIndo ? 'Timeout Kunci' : 'Lock Timeout',
                  subtitle: _lockTimeoutLabel(settings.lockTimeoutSeconds, isIndo),
                  onTap: () => _showLockTimeoutDialog(context, isIndo, settings.lockTimeoutSeconds, sNotifier),
                ),
              ],
              const SizedBox(height: 8),

              // ══════════════ CLOUD BACKUP ══════════════
              _SectionHeader(label: isIndo ? 'CADANGAN AWAN' : 'CLOUD BACKUP'),
              _CloudBackupCard(
                backup: backup,
                settings: settings,
                isIndo: isIndo,
                onSignIn: bNotifier.signIn,
                onSignOut: bNotifier.signOut,
                onBackup: bNotifier.performBackup,
                onRestore: () => _confirmRestore(context, isIndo, bNotifier),
              ),
              const SizedBox(height: 8),

              // ══════════════ LOCAL STORAGE ══════════════
              _SectionHeader(label: isIndo ? 'PENYIMPANAN LOKAL' : 'LOCAL STORAGE'),
              _SettingTile(
                icon: Icons.upload_outlined,
                title: isIndo ? 'Ekspor Database' : 'Export Database',
                subtitle: isIndo ? 'Simpan backup ke penyimpanan lokal' : 'Save backup to local storage',
                onTap: bNotifier.exportDatabase,
              ),
              _SettingTile(
                icon: Icons.download_outlined,
                title: isIndo ? 'Impor Database' : 'Import Database',
                subtitle: isIndo ? 'Pulihkan dari file backup lokal' : 'Restore from local backup file',
                onTap: () => _confirmLocalRestore(context, isIndo, bNotifier),
              ),
              const SizedBox(height: 8),

              // ══════════════ ABOUT ══════════════
              _SectionHeader(label: isIndo ? 'TENTANG' : 'ABOUT'),
              _SettingTile(
                icon: Icons.info_outline,
                title: 'Nulis Notes',
                subtitle: 'v1.2.0 — Flutter Edition',
                onTap: () => showAboutDialog(
                  context: context,
                  applicationName: 'Nulis Notes',
                  applicationVersion: '1.2.0',
                  applicationLegalese: '© 2025 Nulis App',
                ),
              ),
            ],
          ),

          // Loading overlay
          if (backup.isLoading)
            Container(
              color: Colors.black26,
              child: Center(
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Row(mainAxisSize: MainAxisSize.min, children: [
                      const CircularProgressIndicator(),
                      const SizedBox(width: 16),
                      Text(isIndo ? 'Memproses...' : 'Processing...'),
                    ]),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }

  // ── Dialogs ─────────────────────────────────────────────────────

  void _showTextInputDialog(BuildContext context, String title,
      {String initial = '', required Future<void> Function(String) onSave}) {
    final ctrl = TextEditingController(text: initial);
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(title),
        content: TextField(controller: ctrl, autofocus: true),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Batal')),
          FilledButton(onPressed: () { onSave(ctrl.text.trim()); Navigator.pop(context); },
              child: const Text('Simpan')),
        ],
      ),
    );
  }

  void _showPickerDialog(BuildContext context,
      {required String title, required List<String> options, required List<String> values,
       required String current, required Future<void> Function(String) onSelect}) {
    showDialog(
      context: context,
      builder: (_) => SimpleDialog(
        title: Text(title),
        children: List.generate(options.length, (i) => RadioListTile<String>(
          title: Text(options[i]),
          value: values[i],
          groupValue: current,
          onChanged: (v) { if (v != null) { onSelect(v); Navigator.pop(context); } },
        )),
      ),
    );
  }

  void _showSetPinDialog(BuildContext context, bool isIndo, SettingsNotifier sNotifier) {
    final ctrl = TextEditingController();
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(isIndo ? 'Atur PIN' : 'Set PIN'),
        content: TextField(
          controller: ctrl, autofocus: true, obscureText: true,
          keyboardType: TextInputType.number, maxLength: 8,
          decoration: InputDecoration(hintText: isIndo ? 'Masukkan PIN baru' : 'Enter new PIN'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: Text(isIndo ? 'Batal' : 'Cancel')),
          FilledButton(
            onPressed: () {
              if (ctrl.text.length >= 4) {
                sNotifier.setNoteLockPin(ctrl.text);
                sNotifier.setNoteLockEnabled(true);
                Navigator.pop(context);
              }
            },
            child: Text(isIndo ? 'Simpan' : 'Save'),
          ),
        ],
      ),
    );
  }

  void _showLockTimeoutDialog(BuildContext context, bool isIndo,
      int current, SettingsNotifier sNotifier) {
    final options = [
      (isIndo ? 'Saat catatan ditutup' : 'When note is closed', 0),
      (isIndo ? '30 detik' : '30 seconds', 30),
      (isIndo ? '1 menit' : '1 minute', 60),
      (isIndo ? '5 menit' : '5 minutes', 300),
      (isIndo ? 'Saat app ditutup' : 'When app is closed', -1),
    ];
    showDialog(
      context: context,
      builder: (_) => SimpleDialog(
        title: Text(isIndo ? 'Timeout Kunci' : 'Lock Timeout'),
        children: options.map((o) => RadioListTile<int>(
          title: Text(o.$1), value: o.$2, groupValue: current,
          onChanged: (v) { if (v != null) { sNotifier.setLockTimeout(v); Navigator.pop(context); } },
        )).toList(),
      ),
    );
  }

  Future<void> _confirmRestore(BuildContext context, bool isIndo, BackupNotifier bNotifier) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(isIndo ? 'Pulihkan Data?' : 'Restore Data?'),
        content: Text(isIndo
            ? 'Data lokal akan digantikan dengan data backup. Lanjutkan?'
            : 'Local data will be replaced with backup data. Continue?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false),
              child: Text(isIndo ? 'Batal' : 'Cancel')),
          FilledButton(
            style: FilledButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.error),
            onPressed: () => Navigator.pop(context, true),
            child: Text(isIndo ? 'Ya, Pulihkan' : 'Yes, Restore'),
          ),
        ],
      ),
    );
    if (confirm == true) {
      final ok = await bNotifier.performRestore();
      if (ok && context.mounted) context.goHome();
    }
  }

  Future<void> _confirmLocalRestore(BuildContext context, bool isIndo, BackupNotifier bNotifier) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(isIndo ? 'Impor Database?' : 'Import Database?'),
        content: Text(isIndo
            ? 'Data saat ini akan digantikan. Lanjutkan?'
            : 'Current data will be replaced. Continue?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false),
              child: Text(isIndo ? 'Batal' : 'Cancel')),
          FilledButton(
            style: FilledButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.error),
            onPressed: () => Navigator.pop(context, true),
            child: Text(isIndo ? 'Impor' : 'Import'),
          ),
        ],
      ),
    );
    if (confirm == true) {
      final ok = await bNotifier.importDatabase();
      if (ok && context.mounted) context.goHome();
    }
  }

  String _lockTimeoutLabel(int seconds, bool isIndo) => switch (seconds) {
    0    => isIndo ? 'Saat catatan ditutup' : 'When note is closed',
    30   => isIndo ? '30 detik' : '30 seconds',
    60   => isIndo ? '1 menit' : '1 minute',
    300  => isIndo ? '5 menit' : '5 minutes',
    -1   => isIndo ? 'Saat app ditutup' : 'When app is closed',
    _    => '$seconds s',
  };
}

// ── Reusable widgets ──────────────────────────────────────────────

class _SectionHeader extends StatelessWidget {
  final String label;
  const _SectionHeader({required this.label});
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(0, 16, 0, 8),
      child: Text(label,
          style: Theme.of(context).textTheme.labelMedium?.copyWith(
              color: Theme.of(context).colorScheme.primary,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.8)),
    );
  }
}

class _SettingTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  const _SettingTile(
      {required this.icon, required this.title, required this.subtitle, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon),
      title: Text(title),
      subtitle: Text(subtitle, style: const TextStyle(fontSize: 12)),
      trailing: const Icon(Icons.chevron_right, size: 18),
      onTap: onTap,
      contentPadding: const EdgeInsets.symmetric(horizontal: 4),
    );
  }
}

class _AccentColorPicker extends StatelessWidget {
  final Color current;
  final bool isIndo;
  final void Function(Color) onSelect;

  const _AccentColorPicker(
      {required this.current, required this.isIndo, required this.onSelect});

  static const _colors = [
    Color(0xFF3D7A3D), Color(0xFFB85C38), Color(0xFF2196F3),
    Color(0xFF9C27B0), Color(0xFFFF9800), Color(0xFF121212),
  ];

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ListTile(
            leading: const Icon(Icons.palette_outlined),
            title: Text(isIndo ? 'Warna Aksen' : 'Accent Color'),
            contentPadding: const EdgeInsets.symmetric(horizontal: 4),
          ),
          Padding(
            padding: const EdgeInsets.only(left: 4),
            child: Row(
              children: _colors.map((c) {
                final sel = current.value == c.value;
                return GestureDetector(
                  onTap: () => onSelect(c),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 150),
                    margin: const EdgeInsets.only(right: 10),
                    width: 34, height: 34,
                    decoration: BoxDecoration(
                      color: c, shape: BoxShape.circle,
                      border: Border.all(
                          color: sel ? Colors.white : Colors.transparent, width: 2.5),
                      boxShadow: sel
                          ? [BoxShadow(color: c.withOpacity(0.5), blurRadius: 6)]
                          : null,
                    ),
                    child: sel ? const Icon(Icons.check, size: 16, color: Colors.white) : null,
                  ),
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }
}

class _CloudBackupCard extends StatelessWidget {
  final BackupState backup;
  final AppSettings settings;
  final bool isIndo;
  final Future<bool> Function() onSignIn;
  final Future<void> Function() onSignOut;
  final Future<void> Function() onBackup;
  final Future<void> Function() onRestore;

  const _CloudBackupCard({
    required this.backup, required this.settings, required this.isIndo,
    required this.onSignIn, required this.onSignOut,
    required this.onBackup, required this.onRestore,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final isConnected = backup.connectedEmail.isNotEmpty;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Account row
            Row(
              children: [
                CircleAvatar(
                  radius: 18,
                  backgroundColor: cs.primaryContainer,
                  child: Icon(Icons.cloud_outlined, size: 18, color: cs.primary),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Google Drive',
                          style: TextStyle(fontWeight: FontWeight.w600)),
                      Text(
                        isConnected ? backup.connectedEmail
                            : (isIndo ? 'Belum terhubung' : 'Not connected'),
                        style: TextStyle(fontSize: 12, color: cs.onSurfaceVariant),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                isConnected
                    ? Row(mainAxisSize: MainAxisSize.min, children: [
                        Icon(Icons.check_circle, color: Colors.green.shade600, size: 20),
                        const SizedBox(width: 4),
                        TextButton(
                            onPressed: onSignOut,
                            child: Text(isIndo ? 'Keluar' : 'Sign out',
                                style: const TextStyle(fontSize: 12))),
                      ])
                    : FilledButton.tonal(
                        onPressed: onSignIn,
                        style: FilledButton.styleFrom(
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                            minimumSize: Size.zero),
                        child: Text(isIndo ? 'Hubungkan' : 'Connect',
                            style: const TextStyle(fontSize: 13)),
                      ),
              ],
            ),

            const Divider(height: 24),

            // Backup actions
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(isIndo ? 'Terakhir Backup' : 'Last Backup',
                          style: TextStyle(fontSize: 11, color: cs.onSurfaceVariant)),
                      Text(settings.lastBackup,
                          style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
                    ],
                  ),
                ),
                Row(children: [
                  FilledButton.tonal(
                    onPressed: isConnected ? onBackup : null,
                    style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        minimumSize: Size.zero),
                    child: Row(mainAxisSize: MainAxisSize.min, children: [
                      const Icon(Icons.cloud_upload_outlined, size: 16),
                      const SizedBox(width: 6),
                      Text(isIndo ? 'Cadangkan' : 'Backup',
                          style: const TextStyle(fontSize: 13)),
                    ]),
                  ),
                  const SizedBox(width: 8),
                  IconButton.filledTonal(
                    onPressed: isConnected ? onRestore : null,
                    icon: const Icon(Icons.cloud_download_outlined, size: 18),
                    tooltip: isIndo ? 'Pulihkan' : 'Restore',
                  ),
                ]),
              ],
            ),

            const SizedBox(height: 8),
            Text(
              isIndo
                  ? 'Backup disimpan ke folder tersembunyi di Google Drive Anda.'
                  : 'Backups are stored in a hidden app folder on your Google Drive.',
              style: TextStyle(fontSize: 11, color: cs.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}
