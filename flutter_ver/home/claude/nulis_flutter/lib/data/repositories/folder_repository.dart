// lib/data/repositories/folder_repository.dart

import '../database/app_database.dart';
import '../models/folder.dart';
import 'note_repository.dart';

class FolderRepository {
  final AppDatabase _db;
  final NoteRepository _noteRepo;

  const FolderRepository(this._db, this._noteRepo);

  Stream<List<Folder>> watchAll() => _db.watchAllFolders();

  Future<int> insert(Folder folder) =>
      _db.insertFolder(folderToCompanion(folder, isNew: true));

  Future<void> update(Folder folder) =>
      _db.updateFolder(folderToCompanion(folder));

  /// Hapus folder: pindahkan semua catatan ke "tanpa folder" dulu
  /// Setara dengan deleteFolder() di HomeViewModel.kt
  Future<void> delete(int id) async {
    // Ambil semua catatan di folder ini
    final notes = await _noteRepo.watchByFolder(id).first;
    for (final note in notes) {
      await _noteRepo.update(note.copyWith(folderId: -1));
    }
    await _db.deleteFolderById(id);
  }
}
