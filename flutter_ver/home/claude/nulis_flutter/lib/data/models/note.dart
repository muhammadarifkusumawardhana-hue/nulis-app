// lib/data/models/note.dart
//
// Setara dengan NoteEntity.kt di Kotlin.
// Menggunakan plain Dart class — Drift akan generate
// table dari database/app_database.dart (bukan dari sini langsung).

class Note {
  final int id;
  final String title;
  final String content;
  final int folderId;       // -1 = tanpa folder
  final List<String> tags;
  final DateTime createdAt;
  final DateTime updatedAt;
  final bool isPinned;
  final bool isArchived;
  final bool isLocked;
  final bool isDeleted;
  final DateTime? deletedAt;

  const Note({
    this.id = 0,
    this.title = '',
    this.content = '',
    this.folderId = -1,
    this.tags = const [],
    required this.createdAt,
    required this.updatedAt,
    this.isPinned = false,
    this.isArchived = false,
    this.isLocked = false,
    this.isDeleted = false,
    this.deletedAt,
  });

  /// Word count — setara dengan countWords() di EditorViewModel.kt
  int get wordCount {
    if (content.trim().isEmpty) return 0;
    return content.trim().split(RegExp(r'\s+')).length;
  }

  /// Cek apakah catatan ini adalah draft (judul & isi kosong)
  bool get isDraft => title.isEmpty && content.isEmpty;

  Note copyWith({
    int? id,
    String? title,
    String? content,
    int? folderId,
    List<String>? tags,
    DateTime? createdAt,
    DateTime? updatedAt,
    bool? isPinned,
    bool? isArchived,
    bool? isLocked,
    bool? isDeleted,
    DateTime? deletedAt,
    bool clearDeletedAt = false,
  }) {
    return Note(
      id: id ?? this.id,
      title: title ?? this.title,
      content: content ?? this.content,
      folderId: folderId ?? this.folderId,
      tags: tags ?? this.tags,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      isPinned: isPinned ?? this.isPinned,
      isArchived: isArchived ?? this.isArchived,
      isLocked: isLocked ?? this.isLocked,
      isDeleted: isDeleted ?? this.isDeleted,
      deletedAt: clearDeletedAt ? null : (deletedAt ?? this.deletedAt),
    );
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) || (other is Note && other.id == id);

  @override
  int get hashCode => id.hashCode;
}
