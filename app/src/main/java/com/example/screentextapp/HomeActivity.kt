package com.example.screentextapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.screentextapp.databinding.ActivityHomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var isServiceRunning = false

    // SLIDE 12: companion object — simpan kunci putExtra sebagai konstanta agar tidak salah ketik
    companion object {
        const val EXTRA_USERNAME = "extra_username"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // SLIDE 13: getStringExtra — ambil data dari Intent pengirim, siapkan nilai cadangan dengan ?:
        val username = intent.getStringExtra(EXTRA_USERNAME) ?: "Pengguna"

        binding.toolbar.subtitle = "Halo, $username!"

        // Menu toolbar
        binding.toolbar.inflateMenu(R.menu.menu_home)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_profil -> {
                    // SLIDE 11: Intent eksplisit pindah ke ProfilActivity
                    val intent = Intent(this, ProfilActivity::class.java)
                    // SLIDE 12: putExtra kirim username ke Profil
                    intent.putExtra(ProfilActivity.EXTRA_USERNAME, username)
                    startActivity(intent)
                    true
                }
                R.id.action_bantuan -> {
                    // SLIDE 15: Intent implisit — buka URL di browser, tidak sebut Activity tujuan
                    val lihatWeb = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://developer.android.com/guide/components/intents-filters")
                    )
                    try {
                        startActivity(lihatWeb)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Tidak ada browser yang tersedia.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.action_logout -> {
                    konfirmasiLogout()
                    true
                }
                else -> false
            }
        }

        // SLIDE 5: setOnClickListener tombol MULAI
        binding.btnMulai.setOnClickListener {
            if (!isServiceRunning) {
                // Cek izin SYSTEM_ALERT_WINDOW sebelum menjalankan overlay
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    // Minta izin overlay ke Settings
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Izin Diperlukan")
                        .setMessage("Layanan scan membutuhkan izin 'Tampil di atas aplikasi lain' untuk menampilkan tombol overlay.\n\nTekan OK untuk membuka Pengaturan.")
                        .setPositiveButton("Buka Pengaturan") { _, _ ->
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                    return@setOnClickListener
                }
                // Mulai FloatingOverlayService
                val serviceIntent = Intent(this, FloatingOverlayService::class.java).apply {
                    action = FloatingOverlayService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } else {
                // Hentikan FloatingOverlayService
                startService(Intent(this, FloatingOverlayService::class.java).apply {
                    action = FloatingOverlayService.ACTION_STOP
                })
            }
            isServiceRunning = !isServiceRunning
            updateStatusUI()

            // SLIDE 8: Snackbar sebagai alternatif Toast — pesan di tepi bawah layar
            val pesan = if (isServiceRunning) "Layanan aktif — ikon scan muncul di layar" else "Layanan dimatikan"
            Snackbar.make(binding.root, pesan, Snackbar.LENGTH_SHORT).show()
        }

        // SLIDE 9: setOnLongClickListener — tekan lama tombol MULAI untuk petunjuk
        binding.btnMulai.setOnLongClickListener {
            binding.tvStatus.text = "ℹ Layanan ini menampilkan ikon di atas semua app untuk scan teks"
            Toast.makeText(this, "Tekan sekali untuk mengaktifkan overlay scan", Toast.LENGTH_LONG).show()
            true
        }

        // Tombol Scan Teks — navigasi ke ScanActivity (fitur utama baru)
        binding.btnScanTeks.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivity(intent)
        }

        // SLIDE 5: setOnClickListener FAB (+)
        binding.fabMenu.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Menu ScreenText")
                .setItems(arrayOf("🔍  Scan Teks dari Gambar", "✂  Crop Screenshot", "ℹ  Tentang Aplikasi")) { _, which ->
                    when (which) {
                        0 -> {
                            // Intent eksplisit ke ScanActivity
                            val intent = Intent(this, ScanActivity::class.java)
                            startActivity(intent)
                        }
                        1 -> Toast.makeText(this, "Fitur Crop Screenshot segera hadir!", Toast.LENGTH_LONG).show()
                        2 -> {
                            // SLIDE 15: Intent implisit ACTION_SEND — bagikan teks ke aplikasi lain
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.type = "text/plain"
                            shareIntent.putExtra(Intent.EXTRA_TEXT, "ScreenTextApp v3.0 — Aplikasi penangkap & scan teks dari gambar")
                            startActivity(Intent.createChooser(shareIntent, "Bagikan via"))
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun updateStatusUI() {
        if (isServiceRunning) {
            binding.btnMulai.text = "BERHENTI"
            binding.btnMulai.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336"))
            // SLIDE 8: tvStatus sebagai teks status menetap di layar
            binding.tvStatus.text = "● Aktif — Berjalan di Latar Belakang"
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#388E3C"))
        } else {
            binding.btnMulai.text = "MULAI"
            binding.btnMulai.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2"))
            binding.tvStatus.text = "● Tidak Aktif"
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
        }
    }

    private fun konfirmasiLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Keluar")
            .setMessage("Yakin ingin keluar dari aplikasi?")
            .setPositiveButton("Keluar") { _, _ ->
                // SLIDE 11: Intent eksplisit kembali ke Login
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                // SLIDE 14: finish() menutup Activity ini
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
