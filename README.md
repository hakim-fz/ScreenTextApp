# ScreenTextApp

Aplikasi Android untuk menangkap teks dari layar menggunakan fitur Crop Screenshot, dengan tampilan yang berjalan di latar belakang melalui floating menu.

---

## Fitur

- **Login** — Autentikasi sederhana sebelum masuk ke aplikasi
- **Home** — Tombol MULAI untuk mengaktifkan layanan latar belakang
- **Floating Menu (+)** — Pop-up bulat untuk memilih aksi Crop Screenshot
- **Profil** — Informasi akun pengguna dan tombol keluar

---

## Akun Demo

| Username | Password |
|----------|----------|
| `user`   | `1234`   |

---

## Struktur Project

```
ScreenTextApp/
├── app/
│   └── src/main/
│       ├── java/com/example/screentextapp/
│       │   ├── LoginActivity.kt       # Halaman login
│       │   ├── HomeActivity.kt        # Halaman utama + FAB menu
│       │   └── ProfilActivity.kt      # Halaman profil pengguna
│       ├── res/
│       │   ├── layout/
│       │   │   ├── activity_login.xml
│       │   │   ├── activity_home.xml
│       │   │   └── activity_profil.xml
│       │   ├── drawable/
│       │   │   ├── circle_avatar.xml
│       │   │   └── ic_back.xml
│       │   ├── menu/
│       │   │   └── menu_home.xml
│       │   └── values/
│       │       ├── strings.xml
│       │       └── themes.xml
│       └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## Persyaratan

| Komponen | Versi |
|----------|-------|
| Android Studio | Hedgehog 2023.1.1 atau lebih baru |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| Gradle | 9.x |
| Java | 17 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |

---

## Cara Install & Jalankan

### 1. Clone atau Extract Project
```bash
# Jika dari ZIP:
unzip ScreenTextApp_fixed4.zip -d ~/Documents/
```

### 2. Buka di Android Studio
- Buka Android Studio
- Pilih **File → Open**
- Arahkan ke folder `ScreenTextApp`
- Klik **OK** dan tunggu Gradle sync selesai

### 3. Siapkan Device
**HP Fisik:**
- Aktifkan **Developer Options** di HP
- Aktifkan **USB Debugging**
- Colokkan HP ke laptop via USB
- Izinkan debugging saat muncul dialog di HP

**Emulator:**
- Buka **Tools → Device Manager**
- Klik **Create Virtual Device**
- Pilih model HP → pilih system image → Finish

### 4. Run Aplikasi
- Pastikan device muncul di dropdown toolbar
- Klik tombol **▶ Run** (atau tekan `Shift + F10`)
- Tunggu proses build dan install (~30–60 detik)

---

## Alur Penggunaan

```
[Login]
  └── Masukkan user / 1234
        └── [Home]
              ├── Tombol MULAI → aktifkan layanan latar belakang
              ├── FAB (+) pojok kanan bawah → popup Crop Screenshot
              └── Menu ⋮ kanan atas
                    ├── Profil → lihat info akun
                    └── Keluar → kembali ke Login
```

---

## Izin Aplikasi

| Izin | Kegunaan |
|------|----------|
| `FOREGROUND_SERVICE` | Menjalankan layanan di latar belakang |
| `SYSTEM_ALERT_WINDOW` | Menampilkan floating menu di atas aplikasi lain |

> **Catatan:** Izin `SYSTEM_ALERT_WINDOW` perlu diaktifkan manual di **Pengaturan HP → Izin Khusus → Tampil di atas aplikasi lain**.

---

## Troubleshooting

| Masalah | Solusi |
|---------|--------|
| Gradle sync gagal | File → Invalidate Caches → Invalidate and Restart |
| `dataBinding` error | Pastikan tidak ada flag `dataBinding=true` di `gradle.properties` |
| Plugin Kotlin not found | Pastikan versi Kotlin di `build.gradle` root adalah `2.0.21` |
| HP tidak terdeteksi | Cek kabel USB, aktifkan USB Debugging, coba `adb devices` di terminal |
| App crash saat buka | Pastikan `compileSdk 35` dan Java 17 sudah diset di `app/build.gradle` |

---

## Rencana Pengembangan

- [ ] Implementasi Crop Screenshot dengan overlay sesungguhnya
- [ ] OCR (pengenalan teks) dari hasil crop menggunakan ML Kit
- [ ] Copy-paste teks hasil screenshot ke clipboard
- [ ] Riwayat hasil crop
- [ ] Login dengan akun nyata (Firebase Auth)

---

## Versi

| Versi | Tanggal | Keterangan |
|-------|---------|------------|
| 1.0.0 | Oktober 2026 | Rilis pertama: Login, Home, Profil, FAB menu |

---

## Lisensi

Project ini dibuat untuk keperluan pengembangan pribadi.
