# PersonalNotes - Panduan Setup & Build

## Struktur Project

```
MarkdownNotes/
├── build.gradle                     ← Root gradle
├── settings.gradle                  ← Module settings
├── gradle.properties
└── app/
    ├── build.gradle                 ← Dependencies utama
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/personalnotes/app/
        │   ├── PersonalNotesApp.kt  ← Application class
        │   ├── MainActivity.kt
        │   ├── data/
        │   │   ├── local/           ← Room DB, DAO, Repository
        │   │   └── remote/          ← Google Drive, Sync
        │   ├── domain/
        │   │   ├── model/           ← Data classes
        │   │   └── repository/      ← Interface repository
        │   ├── presentation/
        │   │   ├── Navigation.kt
        │   │   ├── MainViewModel.kt
        │   │   ├── home/            ← Layar utama
        │   │   ├── editor/          ← Editor markdown
        │   │   └── settings/        ← Pengaturan
        │   ├── di/                  ← Hilt DI
        │   └── ui/theme/            ← Material3 theme
        └── res/
            ├── drawable/
            └── values/
```

---

## Langkah 1 — Setup Android Studio

1. Download dan install **Android Studio** dari:
   https://developer.android.com/studio

2. Buka Android Studio → klik **"Open"** → pilih folder `MarkdownNotes`

3. Tunggu Gradle sync selesai (bisa 5-15 menit pertama kali, butuh internet)

---

## Langkah 2 — Setup Google Drive API (WAJIB untuk fitur sync)

### 2a. Buat Google Cloud Project

1. Buka: https://console.cloud.google.com
2. Klik **"New Project"** → beri nama "PersonalNotes"
3. Pilih project tersebut

### 2b. Enable Google Drive API

1. Di sidebar kiri → **"APIs & Services"** → **"Library"**
2. Cari **"Google Drive API"** → klik → **"Enable"**

### 2c. Buat OAuth Credentials

1. **"APIs & Services"** → **"Credentials"**
2. Klik **"+ Create Credentials"** → **"OAuth client ID"**
3. Application type: **Android**
4. Package name: `com.personalnotes.app`
5. SHA-1 fingerprint: jalankan perintah ini di terminal Android Studio:

   **Windows:**
   ```
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```

   **Mac/Linux:**
   ```
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```

6. Copy SHA-1 fingerprint → paste ke form Google Cloud
7. Klik **"Create"**

> ⚠️ Tanpa langkah ini, fitur Google Drive tidak akan berfungsi. Fitur offline (baca/tulis catatan lokal) tetap bekerja normal.

---

## Langkah 3 — Build APK

### Cara 1: Via Android Studio (Paling Mudah)

1. Di menu atas: **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Tunggu proses build (~2-5 menit)
3. Klik **"locate"** di notifikasi yang muncul
4. APK ada di: `app/build/outputs/apk/debug/app-debug.apk`

### Cara 2: Via Terminal (di dalam folder project)

```bash
# Windows
gradlew.bat assembleDebug

# Mac/Linux
./gradlew assembleDebug
```

---

## Langkah 4 — Install APK ke HP

### Metode A: Kabel USB (Paling Simpel)
1. Aktifkan **Developer Options** di HP:
   - Pengaturan → Tentang Ponsel → ketuk **"Nomor Build"** 7x
2. Aktifkan **USB Debugging** di Developer Options
3. Sambungkan HP ke PC via USB
4. Di Android Studio: klik tombol **▶ (Run)** — langsung install ke HP

### Metode B: Transfer File Manual
1. Copy file `app-debug.apk` ke HP (via USB, WhatsApp, Google Drive, dll)
2. Di HP: buka file manager → buka APK tersebut
3. Izinkan "Install from unknown sources" jika diminta

---

## Fitur Aplikasi

| Fitur | Status |
|-------|--------|
| Editor Markdown | ✅ |
| Preview Markdown | ✅ |
| Split View (editor + preview) | ✅ |
| Syntax highlighting toolbar | ✅ |
| Folder & subfolder | ✅ |
| Tag/label | ✅ |
| Pencarian full-text | ✅ |
| Backlink antar catatan [[judul]] | ✅ |
| Dark mode | ✅ |
| Export ke HTML | ✅ |
| Export ke PDF | ✅ |
| Auto-save (1.5 detik) | ✅ |
| Hitung kata & karakter | ✅ |
| Pin catatan | ✅ |
| Google Drive sync | ✅ |
| Mode sync: otomatis perubahan | ✅ |
| Mode sync: saat disimpan | ✅ |
| Mode sync: buka/tutup aplikasi | ✅ |
| Mode sync: terjadwal (jam tertentu) | ✅ |
| Mode sync: manual | ✅ |
| Offline-first | ✅ |

---

## Cara Update Aplikasi

Setiap kali ada perubahan kode:

1. Edit file yang ingin diubah di Android Studio
2. Klik **▶ Run** (test langsung di HP via USB), atau
3. **Build → Build APK(s)** → install APK baru ke HP

> File APK baru akan **menggantikan** versi lama secara otomatis (data tidak hilang selama package name sama).

---

## Cara Pakai Backlink

Untuk membuat backlink antar catatan seperti Obsidian:

```markdown
Ini catatan tentang [[Judul Catatan Lain]]
```

Di layar editor, tekan tombol `[[` di toolbar untuk mempercepat penulisan.
Backlink akan muncul di panel backlink (ikon link di toolbar editor).

---

## Troubleshooting

**Gradle sync gagal:**
- Pastikan koneksi internet aktif
- File → Invalidate Caches → Restart

**Google Drive tidak bisa terhubung:**
- Pastikan SHA-1 fingerprint sudah benar di Google Cloud Console
- Cek package name sama persis: `com.personalnotes.app`

**Build error "SDK not found":**
- Android Studio → SDK Manager → install Android SDK 34

---

## Teknologi yang Digunakan

- **Kotlin** + **Jetpack Compose** — UI modern
- **Room** — database lokal SQLite
- **Hilt** — dependency injection
- **Markwon** — render markdown
- **Google Drive API v3** — sinkronisasi cloud
- **WorkManager** — background sync terjadwal
- **DataStore** — simpan preferensi
- **iText7** — export PDF
- **Material3** — design system
