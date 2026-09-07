// lib/data/services/google_drive_service.dart
//
// Setara dengan GoogleDriveService.kt di Kotlin.
// Menggunakan google_sign_in + googleapis (Dart).
// OAuth credentials (client ID, SHA-1) TIDAK perlu diubah.

import 'dart:io';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:googleapis/drive/v3.dart' as drive;
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;

class _GoogleAuthClient extends http.BaseClient {
  final Map<String, String> _headers;
  final http.Client _client = http.Client();
  _GoogleAuthClient(this._headers);

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) =>
      _client.send(request..headers.addAll(_headers));

  @override
  void close() { _client.close(); super.close(); }
}

class GoogleDriveService {
  static const _backupFileName = 'nulis_backup.db';
  static const _appDataFolder  = 'appDataFolder';

  static final _googleSignIn = GoogleSignIn(
    scopes: [
      drive.DriveApi.driveAppdataScope,
      drive.DriveApi.driveFileScope,
    ],
  );

  Future<GoogleSignInAccount?> signIn() async {
    try { return await _googleSignIn.signIn(); } catch (_) { return null; }
  }

  Future<GoogleSignInAccount?> getCurrentAccount() async =>
      _googleSignIn.currentUser ?? await _googleSignIn.signInSilently();

  Future<void> signOut() => _googleSignIn.signOut();

  Future<drive.DriveApi?> _getDriveApi() async {
    final account = await getCurrentAccount();
    if (account == null) return null;
    final authHeaders = await account.authHeaders;
    return drive.DriveApi(_GoogleAuthClient(authHeaders));
  }

  /// Backup DB ke appDataFolder — setara dengan backup() di Kotlin
  Future<bool> backup(File dbFile) async {
    try {
      final api = await _getDriveApi();
      if (api == null) return false;
      // Hapus backup lama
      for (final f in await _findBackupFiles(api)) {
        await api.files.delete(f.id!);
      }
      // Upload baru
      final meta  = drive.File()..name = _backupFileName..parents = [_appDataFolder];
      final media = drive.Media(dbFile.openRead(), dbFile.lengthSync(),
          contentType: 'application/x-sqlite3');
      await api.files.create(meta, uploadMedia: media);
      return true;
    } catch (_) { return false; }
  }

  /// Restore dari appDataFolder — setara dengan restore() di Kotlin
  Future<bool> restore(File targetFile) async {
    try {
      final api = await _getDriveApi();
      if (api == null) return false;
      final files = await _findBackupFiles(api);
      if (files.isEmpty) return false;
      final media = await api.files.get(files.first.id!,
          downloadOptions: drive.DownloadOptions.fullMedia) as drive.Media;
      final bytes = <int>[];
      await for (final chunk in media.stream) { bytes.addAll(chunk); }
      await targetFile.writeAsBytes(bytes);
      return true;
    } catch (_) { return false; }
  }

  Future<List<drive.File>> _findBackupFiles(drive.DriveApi api) async {
    final res = await api.files.list(
      spaces: _appDataFolder, q: "name = '$_backupFileName'", \$fields: 'files(id,name)');
    return res.files ?? [];
  }

  static Future<File> getDatabaseFile() async {
    final dir = await getApplicationDocumentsDirectory();
    return File(p.join(dir.path, 'notes'));
  }

  static Future<File> getTempRestoreFile() async {
    final dir = await getTemporaryDirectory();
    return File(p.join(dir.path, 'restore_temp.db'));
  }
}
