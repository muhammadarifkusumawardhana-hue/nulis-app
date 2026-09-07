# Nulis App — Flutter (Fase 1: Data Layer)

Konversi dari Kotlin + Jetpack Compose + Room ke Flutter + Riverpod + Drift.

---

## Struktur File yang Dihasilkan di Fase 1

```
lib/
├── main.dart                          ← Entry point (setara MainActivity.kt)
├── data/
│   ├── models/
│   │   ├── note.dart                  ← Setara NoteEntity.kt
│   │   └── folder.dart                ← Setara FolderEntity.kt
│   ├── database/
│   │   ├── app_database.dart          ← Setara Database.kt (Room → Drift)
│   │   └── app_database.g.dart        ← AUTO-GENERATED (jangan diedit)
│   └── repositories/
│       ├── note_repository.dart       ← Logika CRUD catatan
│       └── folder_repository.dart     ← Logika CRUD folder
├── core/
│   ├── providers/
│   │   ├── database_providers.dart    ← Setara AppContainer.kt
│   │   └── settings_provider.dart    ← Setara AppSettingsState + SharedPrefs
│   ├── theme/
│   │   └── app_theme.dart             ← Setara Theme.kt (palet sama persis)
│   └── utils/
│       └── app_utils.dart             ← Helper functions dari berbagai file Kotlin
└── ui/
    ├── app_router.dart                ← Setara navigasi di MainActivity.kt
    ├── home/home_page.dart            ← Placeholder (Fase 2)
    ├── editor/editor_page.dart        ← Placeholder (Fase 2)
    └── settings/settings_page.dart   ← Placeholder (Fase 3)
```

---

## Langkah Setup

### 1. Install Flutter SDK
```
https://docs.flutter.dev/get-started/install
```
Gunakan Flutter 3.19+ dan Dart 3.3+

### 2. Install dependencies
```bash
flutter pub get
```

### 3. Generate kode Drift (WAJIB setelah edit app_database.dart)
```bash
dart run build_runner build --delete-conflicting-outputs
```
Perintah ini akan menghasilkan file `app_database.g.dart`.

### 4. Jalankan aplikasi
```bash
flutter run
```

### 5. Konfigurasi package name
Pastikan `applicationId` di `android/app/build.gradle` adalah:
```
applicationId = "com.nu.lis"
```
Ini penting agar user lama bisa update tanpa install ulang.

---

## Mapping Kotlin → Flutter

| Kotlin | Flutter | Catatan |
|--------|---------|---------|
| `Room @Entity` | `Drift Table` | Sintaks mirip |
| `@Dao interface` | Method di `AppDatabase` | Langsung di class DB |
| `Flow<List<T>>` | `Stream<List<T>>` | Setara langsung |
| `StateFlow` | `StreamProvider` / `AsyncNotifier` | Riverpod |
| `SharedPreferences` | `SharedPreferences` | Package sama |
| `AppContainer.kt` | `databaseProvider` (Riverpod) | Singleton otomatis |
| `LocalDateTime.now()` | `DateTime.now().toIso8601String()` | Format string sama |
| `tagsJson: String` | `tagsJson: TextColumn` | JSON tetap, parsing sama |

---

## Optimasi untuk Perangkat Low-End

1. **WAL mode** diaktifkan di `app_database.dart` → tulis lebih cepat
2. **`PRAGMA synchronous = NORMAL`** → I/O lebih efisien
3. **Riverpod** lebih ringan dari ViewModel + LiveData karena tidak perlu lifecycle owner
4. **`const`** di semua model yang tidak berubah → tidak ada rebuild tidak perlu
5. Database hanya dibuka sekali (singleton via Riverpod)

---

## Fase Berikutnya

- **Fase 2**: HomeScreen + EditorScreen (UI + state management)
- **Fase 3**: Settings + Google Drive integration
- **Fase 4**: Theme finalisasi + performance profiling + release APK
