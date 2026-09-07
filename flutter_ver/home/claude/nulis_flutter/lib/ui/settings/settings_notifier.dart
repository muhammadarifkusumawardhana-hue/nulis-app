// lib/ui/settings/settings_notifier.dart
// Setara dengan SettingsViewModel.kt

import 'dart:io';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:file_picker/file_picker.dart';

import '../../core/providers/database_providers.dart';
import '../../core/providers/settings_provider.dart';
import '../../core/utils/app_utils.dart';
import '../../data/services/google_drive_service.dart';

class BackupState {
  final bool isLoading;
  final String? statusMessage;
  final bool isSuccess;
  final String connectedEmail;

  const BackupState({
    this.isLoading = false, this.statusMessage, this.isSuccess = false, this.connectedEmail = '',
  });

  BackupState copyWith({bool? isLoading, String? statusMessage, bool? isSuccess,
      String? connectedEmail, bool clearStatus = false}) {
    return BackupState(
      isLoading: isLoading ?? this.isLoading,
      statusMessage: clearStatus ? null : (statusMessage ?? this.statusMessage),
      isSuccess: isSuccess ?? this.isSuccess,
      connectedEmail: connectedEmail ?? this.connectedEmail,
    );
  }
}

class BackupNotifier extends Notifier<BackupState> {
  final _driveService = GoogleDriveService();

  @override
  BackupState build() {
    _checkCurrentAccount();
    return const BackupState();
  }

  Future<void> _checkCurrentAccount() async {
    final account = await _driveService.getCurrentAccount();
    if (account != null) state = state.copyWith(connectedEmail: account.email);
  }

  Future<bool> signIn() async {
    final account = await _driveService.signIn();
    if (account != null) { state = state.copyWith(connectedEmail: account.email); return true; }
    return false;
  }

  Future<void> signOut() async {
    await _driveService.signOut();
    state = state.copyWith(connectedEmail: '');
  }

  /// Setara dengan performBackup() di Kotlin
  Future<void> performBackup() async {
    state = state.copyWith(isLoading: true);
    try {
      final dbFile = await GoogleDriveService.getDatabaseFile();
      if (!await dbFile.exists()) {
        state = state.copyWith(isLoading: false, statusMessage: 'File database tidak ditemukan', isSuccess: false);
        return;
      }
      final success = await _driveService.backup(dbFile);
      if (success) {
        final now = formatBackupDate(DateTime.now());
        await ref.read(settingsProvider.notifier).setLastBackup(now);
        state = state.copyWith(isLoading: false, statusMessage: 'Backup berhasil', isSuccess: true);
      } else {
        state = state.copyWith(isLoading: false, statusMessage: 'Backup gagal. Pastikan terhubung ke Google Drive.', isSuccess: false);
      }
    } catch (e) {
      state = state.copyWith(isLoading: false, statusMessage: 'Error: $e', isSuccess: false);
    }
  }

  /// Setara dengan performRestore() di Kotlin
  Future<bool> performRestore() async {
    state = state.copyWith(isLoading: true);
    try {
      final tempFile = await GoogleDriveService.getTempRestoreFile();
      final success = await _driveService.restore(tempFile);
      if (success) {
        final db = ref.read(databaseProvider);
        await db.close();
        final dbFile = await GoogleDriveService.getDatabaseFile();
        await tempFile.copy(dbFile.path);
        await tempFile.delete();
        state = state.copyWith(isLoading: false, statusMessage: 'Restore berhasil. Memuat ulang...', isSuccess: true);
        return true;
      } else {
        state = state.copyWith(isLoading: false, statusMessage: 'Restore gagal. File backup tidak ditemukan.', isSuccess: false);
        return false;
      }
    } catch (e) {
      state = state.copyWith(isLoading: false, statusMessage: 'Error: $e', isSuccess: false);
      return false;
    }
  }

  /// Setara dengan exportDatabase() di Kotlin
  Future<void> exportDatabase() async {
    state = state.copyWith(isLoading: true);
    try {
      final dbFile = await GoogleDriveService.getDatabaseFile();
      if (!await dbFile.exists()) {
        state = state.copyWith(isLoading: false, statusMessage: 'File database tidak ditemukan'); return;
      }
      final ts = DateTime.now().toIso8601String().replaceAll(':', '-').substring(0, 19);
      final savePath = await FilePicker.platform.saveFile(
        dialogTitle: 'Simpan backup', fileName: 'nulis_backup_$ts.db',
        allowedExtensions: ['db'], type: FileType.custom,
      );
      if (savePath != null) {
        await dbFile.copy(savePath);
        state = state.copyWith(isLoading: false, statusMessage: 'Ekspor berhasil', isSuccess: true);
      } else { state = state.copyWith(isLoading: false); }
    } catch (e) {
      state = state.copyWith(isLoading: false, statusMessage: 'Ekspor gagal: $e', isSuccess: false);
    }
  }

  /// Setara dengan importDatabase() di Kotlin
  Future<bool> importDatabase() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom, allowedExtensions: ['db'], dialogTitle: 'Pilih file backup');
    if (result == null || result.files.single.path == null) return false;
    state = state.copyWith(isLoading: true);
    try {
      final importFile = File(result.files.single.path!);
      final db = ref.read(databaseProvider);
      await db.close();
      final dbFile = await GoogleDriveService.getDatabaseFile();
      await importFile.copy(dbFile.path);
      state = state.copyWith(isLoading: false, statusMessage: 'Impor berhasil. Memuat ulang...', isSuccess: true);
      return true;
    } catch (e) {
      state = state.copyWith(isLoading: false, statusMessage: 'Impor gagal: $e', isSuccess: false);
      return false;
    }
  }

  void clearStatus() => state = state.copyWith(clearStatus: true);
}

final backupProvider = NotifierProvider<BackupNotifier, BackupState>(
  BackupNotifier.new, name: 'backupProvider');
