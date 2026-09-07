# Nulis App — Panduan Lengkap

Aplikasi catatan Markdown pribadi untuk Android.
Dibuat dengan Kotlin + Jetpack Compose + Room.

---

## Struktur File Project

```
NotesApp/
├── build.gradle                        ← root gradle config
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/
│   └── gradle-wrapper.properties       ← versi Gradle (8.4)
└── app/
    ├── build.gradle                    ← semua dependency ada di sini
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/personalnotes/
        │   ├── MainActivity.kt         ← entry point + navigasi
        │   ├── data/
        │   │   ├── Database.kt         ← Room entities, DAOs, Database
        │   │   └── AppContainer.kt     ← singleton database instance
        │   ├── theme/
        │   │   └── Theme.kt            ← Material3 light/dark theme
        │   └── ui/
        │       ├── home/
        │       │   ├── HomeScreen.kt   ← layar utama (list catatan)
        │       │   └── HomeViewModel.kt
        │       └── editor/
        │           ├── EditorScreen.kt ← editor + preview markdown
        │           └── EditorViewModel.kt
        └── res/
            ├── drawable/               ← icon vector
            ├── mipmap-*/               ← icon launcher semua density
            ├── xml/file_paths.xml      ← FileProvider paths
            └── values/
                ├── strings.xml
                └── themes.xml
```

---

## Langkah 1 — Buka di Android Studio

1. Extract ZIP ini
2. Buka **Android Studio**
3. Pilih **File → Open** → pilih folder `NotesApp`
4. Tunggu **Gradle sync** selesai (perlu internet, 5–10 menit pertama kali)

Jika muncul dialog "Trust this project?" → klik **Trust**.

---

## Langkah 2 — Build APK

### Cara paling mudah (1 klik):
```
Menu: Build → Build Bundle(s) / APK(s) → Build APK(s)
```
Tunggu ~2 menit, lalu klik **"locate"** di notifikasi bawah.

APK ada di:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Cara via terminal (opsional):
```bash
# Windows
gradlew.bat assembleDebug

# Mac / Linux
./gradlew assembleDebug
```

---

## Langkah 3 — Install ke HP

### Metode A — Langsung dari Android Studio (paling mudah)
1. Aktifkan **Developer Options** di HP:
   - Buka Pengaturan → Tentang Ponsel
   - Ketuk **"Nomor Build"** sebanyak 7 kali
2. Aktifkan **USB Debugging** di Developer Options
3. Sambungkan HP ke PC via kabel USB
4. Di Android Studio: klik tombol **▶ Run** (atau Shift+F10)
5. Pilih HP kamu → OK → aplikasi langsung terinstall

### Metode B — Transfer APK Manual
1. Copy file `app-debug.apk` ke HP (via kabel, WhatsApp, Google Drive, dll)
2. Di HP: buka File Manager → tap file APK
3. Jika muncul "Install from unknown sources" → izinkan
4. Tap **Install**

---

## Fitur Aplikasi

| Fitur | Keterangan |
|-------|-----------|
| Editor Markdown | Tulis dengan syntax Markdown lengkap |
| Preview Markdown | Lihat hasil render dengan 1 tap |
| Folder | Organisir catatan dalam folder berwarna |
| Tag | Label catatan, filter berdasarkan tag |
| Backlink `[[judul]]` | Referensi catatan lain ala Obsidian |
| Pencarian | Full-text search di judul + isi catatan |
| Pin catatan | Catatan penting selalu di atas |
| Auto-save | Simpan otomatis 1.5 detik setelah berhenti mengetik |
| Export HTML | Share catatan sebagai file HTML |
| Dark mode | Ikuti tema sistem Android |
| Word count | Hitung kata + karakter real-time |
| Offline | Semua data tersimpan lokal di HP |

---

## Cara Pakai Backlink (seperti Obsidian)

Ketik `[[` lalu judul catatan yang ingin direferensikan:

```markdown
Hari ini saya belajar tentang [[Konsep Inti Markdown]]
Terkait juga dengan [[Workflow Menulis Saya]]
```

Tekan tombol `[[` di toolbar editor untuk mempercepat.

---

## Sintaks Markdown yang Didukung

```markdown
# Judul H1
## Judul H2
### Judul H3

**teks tebal**
*teks miring*
~~teks dicoret~~

- item list biasa
- [ ] tugas belum selesai
- [x] tugas selesai

> blockquote / kutipan

`kode inline`

```kode blok```

[teks link](https://url.com)
[[backlink ke catatan lain]]

---  (garis pemisah)
```

---

## Update Aplikasi di Masa Depan

Setiap kali kamu ingin ubah / tambah fitur:

1. Edit file Kotlin yang relevan di Android Studio
2. Tekan **▶ Run** untuk test langsung di HP (via USB)
3. Atau **Build APK** → transfer file APK baru ke HP

> APK baru akan **update** aplikasi lama secara otomatis.
> Data catatan tidak hilang selama package name sama (`com.personalnotes`).

---

## Troubleshooting

**Gradle sync gagal / lambat:**
- Pastikan koneksi internet aktif
- Coba: File → Invalidate Caches → Restart

**Error "SDK location not found":**
- Android Studio → SDK Manager → install Android SDK 34

**Aplikasi crash setelah install:**
- Pastikan minSdk HP ≥ Android 8.0 (API 26)
- Cek Logcat di Android Studio untuk detail error
- Copy pesan error merahnya dan cari bantuan

**Export HTML tidak muncul:**
- Pastikan ada aplikasi browser atau file manager di HP
- Coba dengan Chrome

---

## Teknologi yang Digunakan

| Library | Fungsi |
|---------|--------|
| **Kotlin** | Bahasa pemrograman utama |
| **Jetpack Compose** | UI modern declarative |
| **Room** | Database lokal SQLite |
| **Markwon 4.6.2** | Render Markdown |
| **Gson** | Serialisasi JSON (untuk tags) |
| **Material3** | Design system Google |
| **DataStore** | Simpan preferensi |
| **FileProvider** | Share file HTML dengan aman |

---

## Rencana Pengembangan Selanjutnya

Setelah base app ini stabil, fitur yang bisa ditambahkan:

- [ ] Sinkronisasi Google Drive
- [ ] Export ke PDF
- [ ] Editor split view (kiri: tulis, kanan: preview)
- [ ] Gambar di dalam catatan
- [ ] Enkripsi catatan sensitif
- [ ] Backup/restore lokal
- [ ] Widget Android untuk catatan cepat
