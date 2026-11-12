# ScreenTextApp v2.0

Aplikasi Android untuk menangkap teks dari layar, diperbarui sesuai materi **Pertemuan 6 — Event Handling & Navigasi Antar Layar**.

---

## Yang Baru di v2.0 (Pertemuan 6)

| Konsep Materi | Implementasi di App |
|---------------|---------------------|
| `setOnClickListener` (Slide 5) | Semua tombol: Login, Mulai, Edit, Keluar, FAB |
| Baca EditText di dalam listener (Slide 7) | `etUsername.text` dibaca di dalam `btnLogin.setOnClickListener` |
| Toast & Snackbar umpan balik (Slide 8) | Toast validasi login, Snackbar status layanan |
| `setOnLongClickListener` (Slide 9) | Tekan lama btnLogin → hint, tekan lama btnMulai → petunjuk |
| Intent eksplisit (Slide 11) | Login→Home, Home→Profil, semua logout |
| `putExtra` + `companion object` (Slide 12) | `EXTRA_USERNAME` sebagai konstanta di companion object |
| `getStringExtra` + nilai cadangan (Slide 13) | `intent.getStringExtra(EXTRA_USERNAME) ?: "Pengguna"` |
| `finish()` & tombol Back (Slide 14) | Toolbar back di Profil, konfirmasi logout |
| Intent implisit (Slide 15) | Buka browser dari menu Bantuan, ACTION_SEND bagikan teks |

---

## Akun Demo

| Username | Password |
|----------|----------|
| `user`   | `1234`   |

---

## Cara Menjalankan

1. Extract ZIP → buka di Android Studio
2. Tunggu Gradle sync selesai
3. Klik ▶ Run → pilih device/emulator
4. Login dengan `user` / `1234`

---

## Alur Aplikasi

```
[Login]
  ├── Tekan lama MASUK → tampil hint kredensial
  └── Login berhasil → [Home]
        ├── Subtitle toolbar: "Halo, user!"
        ├── Tombol MULAI → toggle layanan (Snackbar status)
        ├── Tekan lama MULAI → petunjuk di tvStatus
        ├── FAB (+) → pilih Crop Screenshot / Bagikan App
        └── Menu ⋮
              ├── Profil → [ProfilActivity]
              │     ├── Tekan lama avatar → Snackbar info
              │     ├── EDIT PROFIL → Toast "segera hadir"
              │     └── KELUAR → kembali ke Login
              ├── Bantuan → buka browser (Intent implisit)
              └── Keluar → dialog konfirmasi → Login
```

---

## Versi

| Versi | Pertemuan | Materi |
|-------|-----------|--------|
| 1.0.0 | 5 | Layout, Activity, navigasi dasar |
| 2.0.0 | 6 | Event Handling, Intent, putExtra/getExtra |
