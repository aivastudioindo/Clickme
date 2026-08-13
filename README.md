# Clickme

Aplikasi Android ringan dan berprioritas privasi yang membaca serta menampilkan **seluruh** notifikasi di perangkat Anda secara lokal. Dibangun dengan Kotlin + Material 3, tanpa server, tanpa panggilan jaringan.

![License](https://img.shields.io/badge/license-MIT-green) ![Platform](https://img.shields.io/badge/platform-Android-34a853)

## Fitur

- Menangkap setiap notifikasi melalui `NotificationListenerService` sistem
- Daftar diperbarui langsung (notifikasi baru muncul instan)
- Membaca isi pesan lengkap, bukan sekadar judul (`bigText` dan `textLines` bertumpuk diekstrak)
- Deduplikasi (per-aplikasi, jendela 30 detik) agar kiriman berulang tidak membanjiri daftar
- Mengelompokkan notifikasi bertumpuk menggunakan `groupKey`
- Antarmuka Material 3 modern dengan navigation drawer hamburger
- Slot fitur masa depan (Cari/Filter, Ekspor) sudah ada di drawer
- APK rilis kecil (minify + shrink, filter ABI)

## Cara kerja

1. Sistem menayangkan notifikasi.
2. `NotificationService` (`NotificationListenerService`) menerimanya di thread latar.
3. Konten diekstrak dengan fallback (`title → titleBig → conversationTitle`, `text → bigText → textLines → infoText`).
4. Item disimpan di repository dan dikirim ke antarmuka melalui observer.
5. Daftar di layar Notifikasi diperbarui langsung.
6. Saat aplikasi dibuka kembali, notifikasi yang masih aktif di sistem ditarik ulang (catch-up) agar riwayat tidak kosong.

## Izin

| Izin | Keperluan |
| --- | --- |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Wajib untuk membaca notifikasi. Diaktifkan pengguna di **Setelan → Akses notifikasi → Clickme**. |
| `POST_NOTIFICATIONS` | Izin tayang notifikasi runtime Android 13+ (dipakai bila aplikasi pernah men notify Anda). |
| `RECEIVE_BOOT_COMPLETED` | Mengaktifkan kembali listener setelah reboot agar tidak ada yang terlewat. |

Tidak ada konten notifikasi yang keluar dari perangkat.

## Build

### Prasyarat

- Android SDK (platform API 34 + build-tools)
- JDK 17

### Lokal

```bash
./gradlew assembleDebug      # APK debug
./gradlew assembleRelease   # APK release (perlu konfigurasi signing)
```

### Signing

Untuk rilis yang ditandatangani, sediakan variabel lingkungan berikut (atau GitHub Secrets):

| Variabel | Keterangan |
| --- | --- |
| `SIGNING_STORE_FILE` | Path ke keystore (atau base64-nya di CI) |
| `SIGNING_STORE_PASSWORD` | Kata sandi keystore |
| `SIGNING_KEY_ALIAS` | Alias key |
| `SIGNING_KEY_PASSWORD` | Kata sandi key |

Buat keystore secara lokal:

```bash
keytool -genkeypair -v -keystore release-key.jks -keyalg RSA \
  -keysize 2048 -validity 10000 -alias clickme
```

### CI

Push ke `main` memicu `.github/workflows/android.yml` yang membangun APK rilis yang ditandatangani dan mengunggahnya sebagai artifact (`clickme-release-apk`).

## Struktur proyek

```
app/src/main/
  java/com/clickme/app/
    MainActivity.kt          # host drawer + navigasi
    NotificationService.kt   # listener (dedup, ekstraksi, rebind, catch-up)
    BootReceiver.kt          # aktifkan kembali listener setelah reboot
    NotificationAdapter.kt    # adapter list
    model/NotificationItem.kt
    repo/NotificationRepository.kt
    ui/NotificationsFragment.kt
    ui/SettingsFragment.kt
  res/                       # layout, theme, menu, drawable, font
```

## Lisensi

MIT
