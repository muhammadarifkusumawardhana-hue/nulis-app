// lib/data/models/folder.dart
//
// Setara dengan FolderEntity.kt

class Folder {
  final int id;
  final String name;
  final String colorHex;
  final String? icon;

  const Folder({
    this.id = 0,
    required this.name,
    this.colorHex = '#3D7A3D',
    this.icon,
  });

  Folder copyWith({
    int? id,
    String? name,
    String? colorHex,
    String? icon,
    bool clearIcon = false,
  }) {
    return Folder(
      id: id ?? this.id,
      name: name ?? this.name,
      colorHex: colorHex ?? this.colorHex,
      icon: clearIcon ? null : (icon ?? this.icon),
    );
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) || (other is Folder && other.id == id);

  @override
  int get hashCode => id.hashCode;
}
